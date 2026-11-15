"""Pad an exported model to a longer observation: the new columns start with weight 0, mean 0, std 1.

Columns are only ever APPENDED to the observation (never reordered), so padding is exact: the padded model computes the
same function as the original, and the new features get a meaning only when a training run gives them weight. The engine
itself accepts only models whose observation matches its encoder; conversion belongs here, where the artifact is made.

    py -3 ml/pad_model.py --model DIR [--out DIR] [--extra N]

`--extra` defaults to the current encoder size (ObservationBuilder.size()) minus the model's own, and is refused when the
model is already that size. Works for both kinds: `policy` (`model/<TYPE>/`) and `value_mlp`.
"""
import argparse
import hashlib
import json
import os
import shutil
import struct


def read_model(model_dir):
    meta = json.load(open(os.path.join(model_dir, "model.json"), encoding="utf-8"))
    with open(os.path.join(model_dir, "weights.bin"), "rb") as f:
        raw = f.read()
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


def pad(meta, values, extra, new_hash=None):
    """mean[obs] std[obs] W[h1][obs] b[h1] ... -> the same with `extra` columns appended."""
    obs = meta["obsSize"]
    h1 = meta["hidden"][0]
    out = []
    pos = 0
    # mean, std
    out.extend(values[pos:pos + obs])
    out.extend([0.0] * extra)
    pos += obs
    out.extend(values[pos:pos + obs])
    out.extend([1.0] * extra)
    pos += obs
    # W_obs, one row per hidden unit: append the zeros at the end of every row
    for _ in range(h1):
        out.extend(values[pos:pos + obs])
        out.extend([0.0] * extra)
        pos += obs
    # everything else keeps its place
    out.extend(values[pos:])
    meta = dict(meta)
    meta["obsSize"] = obs + extra
    if new_hash:
        meta["schemaHash"] = new_hash
    return meta, out


def current_schemas(directory):
    """schema hash per decision type: either from a `--mode schemas` JSON file, or from a freshly generated chunk set."""
    import sys
    if os.path.isfile(directory):
        return {name: {"hash": value} for name, value in json.load(open(directory, encoding="utf-8")).items()}
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
    import ares_data
    result = {}
    for name in os.listdir(directory):
        if not name.endswith(".ares"):
            continue
        schema, _ = ares_data.read_decisions(os.path.join(directory, name))
        result[schema["type"]] = schema
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, help="exported model folder (policy type dir or value model)")
    parser.add_argument("--out", default=None, help="new folder (default: <model>_padded)")
    parser.add_argument("--extra", type=int, default=0, help="columns to append (0 = whatever the encoder needs)")
    parser.add_argument("--expected", type=int, default=0,
                        help="target observation size; when 0 the metadata is padded but the engine will check it")
    parser.add_argument("--schemas", default=None,
                        help="directory with a freshly generated chunk set: the new schema hash per type is taken from it "
                             "(required, the hash covers the observation layout)")
    args = parser.parse_args()

    model_dir = args.model
    out_dir = args.out or (model_dir.rstrip("/\\") + "_padded")
    single = os.path.exists(os.path.join(model_dir, "model.json"))
    schemas = current_schemas(args.schemas) if args.schemas else {}

    if single:
        meta, values = read_model(model_dir)
        obs = meta["obsSize"]
        extra = args.extra or max(0, args.expected - obs)
        if extra <= 0:
            raise SystemExit(f"{model_dir}: nothing to pad (obsSize {obs}, extra {extra})")
        padded_meta, padded = pad(meta, values, extra)
        if os.path.exists(out_dir):
            shutil.rmtree(out_dir)
        write_model(out_dir, padded_meta, padded)
        # reference.json describes the OLD weights on OLD observations: it must not travel with the padded model
        print(f"{model_dir}: {obs} -> {padded_meta['obsSize']} ({extra} new columns at zero) -> {out_dir}")
        return

    # A policy bundle is a directory of decision-type folders.
    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    padded_any = 0
    for name in sorted(os.listdir(model_dir)):
        source = os.path.join(model_dir, name)
        if not os.path.isdir(source):
            if name in ("DONE", "MODEL_INFO.json"):
                shutil.copy2(source, os.path.join(out_dir, name))
            continue
        sub_meta, sub_values = read_model(source)
        obs = sub_meta["obsSize"]
        extra = args.extra or max(0, args.expected - obs)
        if extra <= 0:
            raise SystemExit(f"{source}: nothing to pad (obsSize {obs}, extra {extra})")
        padded_meta, padded = pad(sub_meta, sub_values, extra, (schemas.get(name) or {}).get("hash"))
        write_model(os.path.join(out_dir, name), padded_meta, padded)
        padded_any += 1
    print(f"{model_dir}: policy bundle, {padded_any} decision types padded by {extra} columns -> {out_dir}")


if __name__ == "__main__":
    main()
