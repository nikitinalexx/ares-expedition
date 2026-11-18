"""Merge several packs of the same encoder into one, so the POLICY trainer can replay old corpora.

Why: `train_policy_ce` cannot concatenate packs by numpy, because a pack keeps `cands` (`PackedCandidates`) and `ids`
(`PackedIdView`) as lazy row views - concatenating them by numpy kept the first pack's views while the row arrays carried
both packs, and every label pointed at the wrong candidates (a silent misalignment that surfaced as a CUDA out-of-bounds
gather, 18/11). The honest fix is one pack on disk that keeps every view consistent, which is what this does at the file
level: append the observations, append the labels, and append the candidate rows WHILE SHIFTING THEIR OFFSETS, keeping the
single trailing zero row that the padded gather reads.

    py -3 ml/pack_merge.py --packs C:/ares/pack/A,C:/ares/pack/B --out C:/ares/pack/merged

`ml/tests/test_pack_merge.py` proves the result row by row against the sources; without that test the merge is exactly the
kind of "different representation that silently loses a bit" this project forbids.
"""
import argparse
import json
import os
import shutil
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

LABEL_FIELDS = ("outcome", "margin", "seat", "context", "round", "game", "step", "keep")


def merge(packs, out):
    manifests = [ares_data.open_pack(p) for p in packs]
    obs_size = manifests[0]["obsSize"]
    for path, manifest in zip(packs, manifests):
        if manifest["obsSize"] != obs_size:
            raise SystemExit(f"{path}: observation size {manifest['obsSize']} against {obs_size} - refusing to merge")

    os.makedirs(out, exist_ok=True)
    types = {}
    all_types = sorted({name for manifest in manifests for name in manifest["types"]})
    for name in all_types:
        sources = [(path, manifest) for path, manifest in zip(packs, manifests)
                   if name in manifest["types"] and manifest["types"][name].get("rows", 0) > 0]
        if not sources:
            continue
        rows, candidate_rows = _merge_type(name, sources, out, obs_size)
        first = sources[0][1]["types"][name]
        types[name] = {"rows": rows,
                       "hash": first.get("hash"),
                       # the trainer reads the per-type layout to name the observation columns
                       "layout": first.get("layout"),
                       "obsSize": first.get("obsSize", obs_size),
                       "chunks": sum(int(manifest["types"][name].get("chunks", 0)) for _, manifest in sources)}
        if any("candidates" in manifest["types"][name] for _, manifest in sources):
            features = next(manifest["types"][name]["candidates"]["features"] for _, manifest in sources
                            if "candidates" in manifest["types"][name])
            # `rows` is the number of candidate rows WITHOUT the trailing zero row: the reader maps `rows + 1` rows.
            types[name]["candidates"] = {"features": features, "rows": candidate_rows}
        print(f"  {name:22s} {rows:8d} rows", flush=True)

    manifest = {"obsSize": obs_size,
                "types": types,
                "rows": int(sum(value["rows"] for value in types.values())),
                "observationHash": manifests[0]["observationHash"],
                "observationLayout": manifests[0].get("observationLayout"),
                "source": [os.path.abspath(p) for p in packs],
                "merged": True}
    if "breakdown" in manifests[0]:
        manifest["breakdown"] = manifests[0]["breakdown"]
    with open(os.path.join(out, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=1)
    return manifest


def _merge_type(name, sources, out, obs_size):
    """Appends the observation bytes, the labels and (if present) the candidate rows of one decision type."""
    total = 0
    with open(os.path.join(out, f"{name}.obs.f16"), "wb") as sink:
        for path, manifest in sources:
            rows = int(manifest["types"][name]["rows"])
            total += rows
            with open(os.path.join(path, f"{name}.obs.f16"), "rb") as source:
                remaining = rows * obs_size * 2
                while remaining:
                    chunk = source.read(min(1 << 24, remaining))
                    if not chunk:
                        raise SystemExit(f"{path}/{name}.obs.f16 is shorter than its manifest says")
                    sink.write(chunk)
                    remaining -= len(chunk)

    labels = {}
    for path, manifest in sources:
        with np.load(os.path.join(path, f"{name}.lab.npz")) as data:
            for field in LABEL_FIELDS:
                labels.setdefault(field, []).append(np.asarray(data[field]))
            if "breakdown" in data.files:
                labels.setdefault("breakdown", []).append(np.asarray(data["breakdown"]))
    merged = {}
    for field, parts in labels.items():
        if len(parts) != len(sources):
            continue                                  # a field missing in one source is dropped, never padded silently
        merged[field] = np.concatenate(parts) if parts[0].ndim >= 1 else parts[0]
    np.savez(os.path.join(out, f"{name}.lab.npz"), **merged)

    if not any("candidates" in manifest["types"][name] for _, manifest in sources):
        return total, 0
    return total, _merge_candidates(name, sources, out)


def _merge_candidates(name, sources, out):
    """Concatenates the candidate rows and rebuilds the CSR `offset` array.

    `offset` has `rows + 1` entries: row i starts at `offset[i]` and `offset[rows]` is the total number of candidate rows
    (the pack stores one extra all-zero row at the end, which the padded gather reads for rows with fewer candidates). Both
    facts matter - a per-row array plus a naive concatenation would shift every row's candidates by an off-by-one and no
    label would point where it should.
    """
    starts, counts, chosens = [], [], []
    shift = 0
    with open(os.path.join(out, f"{name}.cand.f16"), "wb") as flat_sink, \
            open(os.path.join(out, f"{name}.cids.u2"), "wb") as id_sink:
        zero_row = None
        for path, manifest in sources:
            meta = manifest["types"][name]["candidates"]
            with np.load(os.path.join(path, f"{name}.cand.npz")) as data:
                offset = np.asarray(data["offset"], dtype=np.int64)
                count = np.asarray(data["count"], dtype=np.int64)
                chosen = np.asarray(data["chosen"], dtype=np.int64)
            features = int(meta["features"])
            with open(os.path.join(path, f"{name}.cand.f16"), "rb") as source:
                raw = source.read()
            file_rows = len(raw) // (features * 2)
            data_rows = int(offset[-1]) if offset.size else 0
            if data_rows + 1 != file_rows:
                raise SystemExit(f"{path}/{name}: {data_rows} candidate rows in offsets, {file_rows} in the flat file")
            zero_row = raw[data_rows * features * 2:(data_rows + 1) * features * 2]
            flat_sink.write(raw[:data_rows * features * 2])
            with open(os.path.join(path, f"{name}.cids.u2"), "rb") as source:
                ids = source.read()
            id_sink.write(ids[:data_rows * 2])
            if int((offset[:-1] + count).max(initial=0)) > data_rows:
                raise SystemExit(f"{path}/{name}: a row's candidates reach past the flat block")
            starts.append(offset[:-1] + shift)                                # per-row starts, shifted by this pack
            counts.append(count)
            chosens.append(chosen)
            shift += data_rows
        if zero_row is not None:
            flat_sink.write(zero_row)                                        # one zero row at the very end
            id_sink.write(b"\x00\x00")
    np.savez(os.path.join(out, f"{name}.cand.npz"),
             offset=np.concatenate(starts + [np.array([shift], dtype=np.int64)]),
             count=np.concatenate(counts), chosen=np.concatenate(chosens))
    return shift


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--packs", required=True, help="comma-separated pack directories")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    packs = [part.strip() for part in args.packs.split(",") if part.strip()]
    if len(packs) < 2:
        raise SystemExit("nothing to merge: give at least two packs")
    if os.path.isdir(args.out):
        shutil.rmtree(args.out)
    print(f"merging {len(packs)} packs into {args.out}")
    manifest = merge(packs, args.out)
    print(f"merged: {manifest['rows']} rows, {len(manifest['types'])} types")


if __name__ == "__main__":
    main()
