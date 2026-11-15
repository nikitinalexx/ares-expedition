"""Move an exported model to another observation layout by NAME, without retraining from zero.

`pad_model.py` appends columns; this one handles the general case: a feature removed here, another added there. Every
weight, mean and std is carried over for the features that still exist under the same name, and the new features start
with weight 0, mean 0, std 1 - so the remapped model computes exactly the same function as the original on the part of the
state both encoders describe.

    py -3 ml/remap_model.py --model DIR --old-names FILE --new-names FILE [--out DIR]
    py -3 ml/remap_model.py --value DIR --old-names FILE --new-names FILE [--out DIR]

Both name files are written by `SelfPlayMain --mode names` (one per encoder version). The candidate layer is not touched:
a decision type whose candidate width changed needs a retrain, and that is reported instead of silently reshaped.
"""
import argparse
import hashlib
import json
import os
import struct
import sys

CLIP_KEYS = ("obsSize", "candFeatures", "hidden", "clip")


def read_model(model_dir):
    meta = json.load(open(os.path.join(model_dir, "model.json"), encoding="utf-8"))
    raw = open(os.path.join(model_dir, "weights.bin"), "rb").read()
    values = list(struct.unpack("<%df" % (len(raw) // 4), raw))
    return meta, values


def write_model(model_dir, meta, values):
    raw = struct.pack("<%df" % len(values), *values)
    meta = dict(meta)
    meta["weightsCount"] = len(values)
    meta["weightsSha256"] = hashlib.sha256(raw).hexdigest()
    os.makedirs(model_dir, exist_ok=True)
    with open(os.path.join(model_dir, "weights.bin"), "wb") as f:
        f.write(raw)
    json.dump(meta, open(os.path.join(model_dir, "model.json"), "w", encoding="utf-8"), indent=1)


def pad_candidate_slots(values, meta, extra):
    """NOTE: only valid for slots appended at the END. The card menus do that (`points/10` is the last slot, after `stop`);
    a slot inserted in the middle must be remapped by name, which this file does for the observation."""
    """Zero-pads the candidate layer: candidate features are only ever appended, so the new slots are zeros at the end of
    every row of the transposed candidate block."""
    obs = int(meta["obsSize"])
    h1 = int(meta["hidden"][0])
    cand = int(meta["candFeatures"])
    head = values[:2 * obs]
    w_obs = values[2 * obs:2 * obs + h1 * obs]
    b_obs = values[2 * obs + h1 * obs:2 * obs + h1 * obs + h1]
    start = 2 * obs + h1 * obs + h1
    w_cand = values[start:start + h1 * cand]
    rest = values[start + h1 * cand:]
    padded = []
    for unit in range(h1):
        padded.extend(w_cand[unit * cand:(unit + 1) * cand])
        padded.extend([0.0] * extra)
    return head + w_obs + b_obs + padded + rest


def remap(meta, values, old_names, new_names, new_hash=None):
    """mean[old] std[old] W[h1][old] b[h1] (cand...) -> the same arrays on the new name order."""
    obs, h1 = meta["obsSize"], meta["hidden"][0]
    if obs != len(old_names):
        raise SystemExit(f"the model was trained on {obs} values, the old names file has {len(old_names)}")
    index = {name: i for i, name in enumerate(old_names)}
    missing = [name for name in new_names if name not in index]
    order = [index.get(name, -1) for name in new_names]

    pos = 0
    mean = values[pos:pos + obs]
    pos += obs
    std = values[pos:pos + obs]
    pos += obs
    rows = []
    for _ in range(h1):
        rows.append(values[pos:pos + obs])
        pos += obs
    rest = values[pos:]                      # b_obs, cand layer, mid, out
    if any(i >= 0 and std[i] == 0 for i in order):
        raise SystemExit("a kept feature has std 0 in the old model: the file is damaged")

    new_mean = [mean[i] if i >= 0 else 0.0 for i in order]
    new_std = [std[i] if i >= 0 else 1.0 for i in order]
    new_rows = []
    for row in rows:
        new_rows.extend([row[i] if i >= 0 else 0.0 for i in order])

    out = list(new_mean) + list(new_std) + new_rows + list(rest)
    meta = dict(meta)
    meta["obsSize"] = len(new_names)
    if new_hash:
        meta["schemaHash"] = new_hash
    return meta, out, missing


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="", help="exported policy bundle (a directory of decision types)")
    parser.add_argument("--value", default="", help="exported value model (a single folder) instead of a bundle")
    parser.add_argument("--old-names", required=True, help="names file of the layout the model was trained on")
    parser.add_argument("--new-names", required=True, help="names file of the layout the encoder produces now")
    parser.add_argument("--schemas", default="", help="schemas file of the current encoder (for the schema hashes)")
    parser.add_argument("--pad-candidates", action="store_true",
                        help="give new candidate slots zero weight instead of refusing a grown candidate vector")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    if not args.model and not args.value:
        raise SystemExit("give --model (policy bundle) or --value (value model)")

    old_names = json.load(open(args.old_names, encoding="utf-8"))
    new_names = json.load(open(args.new_names, encoding="utf-8"))
    schemas = json.load(open(args.schemas, encoding="utf-8")) if args.schemas else {}

    if args.value:
        meta, values = read_model(args.value)
        new_meta, out_values, missing = remap(meta, values, old_names, new_names)
        write_model(args.out, new_meta, out_values)
        print(f"{args.value}: {meta['obsSize']} -> {new_meta['obsSize']} values, "
              f"{len(missing)} new features at zero -> {args.out}")
        return

    os.makedirs(args.out, exist_ok=True)
    for name in sorted(os.listdir(args.model)):
        source = os.path.join(args.model, name)
        if not os.path.isdir(source):
            if name in ("DONE", "MODEL_INFO.json"):
                import shutil
                shutil.copy2(source, os.path.join(args.out, name))
            continue
        meta, values = read_model(source)
        wanted = (schemas.get(name) or {}).get("candidateFeatures")
        if wanted is not None and wanted != meta.get("candFeatures"):
            if not args.pad_candidates:
                raise SystemExit(f"{name}: candidate width {meta.get('candFeatures')} -> {wanted}: retrain this head, "
                                 f"the candidate features changed (or pass --pad-candidates for zero weights)")
            extra = int(wanted) - int(meta["candFeatures"])
            if extra < 0:
                raise SystemExit(f"{name}: the candidate vector shrank ({meta['candFeatures']} -> {wanted})")
            values = pad_candidate_slots(values, meta, extra)
            meta["candFeatures"] = int(wanted)
        target_hash = (schemas.get(name) or {}).get("hash")
        new_meta, out_values, missing = remap(meta, values, old_names, new_names, target_hash)
        write_model(os.path.join(args.out, name), new_meta, out_values)
    print(f"{args.model}: bundle remapped to {len(new_names)} values ({len(missing)} new features at zero) -> {args.out}")


if __name__ == "__main__":
    sys.exit(main())
