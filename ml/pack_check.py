"""Checks that a pack reproduces the raw chunks exactly: rows, observation values, labels and the real candidates.

The pack drops padding and computes a de-duplication mask, so a mistake there would silently train on the wrong data.
This reads the raw files and the pack side by side and fails loudly on the first disagreement.

    py -3 ml/pack_check.py --pack C:/ares/pack/xtest --data D:/ares/data/scale1090 --limit-chunks 2
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--data", required=True)
    parser.add_argument("--limit-chunks", type=int, default=0)
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    checked_rows, checked_candidates = 0, 0
    for decision in sorted(manifest["types"]):
        files = sorted(glob.glob(os.path.join(args.data, f"{decision}.[0-9]*.ares")))
        if args.limit_chunks:
            files = files[:args.limit_chunks]
        packed = ares_data.open_packed_type(args.pack, decision, manifest)
        offset = 0
        for path in files:
            _, raw = ares_data.read_decisions(path)
            count = len(raw)
            if not count:
                continue
            want = np.asarray(raw["obs"], dtype=np.float16)
            got = packed["obs"][np.arange(offset, offset + count)]
            if not np.array_equal(want, got.astype(np.float16)):
                bad = np.flatnonzero((want != got.astype(np.float16)).any(axis=1))[:3]
                raise SystemExit(f"{decision} {os.path.basename(path)}: observation differs at rows {bad}")
            for field in ("outcome", "margin", "seat", "round", "game", "step"):
                if not np.array_equal(np.asarray(raw[field]), np.asarray(packed[field][offset:offset + count])):
                    raise SystemExit(f"{decision} {os.path.basename(path)}: label {field} differs")
            if "cands" in packed:
                if not np.array_equal(np.asarray(raw["chosen"]), np.asarray(packed["chosen"][offset:offset + count])):
                    raise SystemExit(f"{decision} {os.path.basename(path)}: chosen differs")
                if not np.array_equal(np.asarray(raw["count"]), np.asarray(packed["count"][offset:offset + count])):
                    raise SystemExit(f"{decision} {os.path.basename(path)}: count differs")
                rows = np.arange(offset, offset + count)
                cands = packed["cands"][rows]
                ids = packed["cands"].gather_ids(rows)
                counts = np.asarray(raw["count"]).astype(np.int64)
                for i in range(count):
                    if not np.array_equal(np.asarray(raw["feat"][i][:counts[i]], dtype=np.float16),
                                          cands[i][:counts[i]].astype(np.float16)):
                        raise SystemExit(f"{decision} {os.path.basename(path)}: candidate features differ at row {i}")
                    if not np.array_equal(np.asarray(raw["ids"][i][:counts[i]]), ids[i][:counts[i]]):
                        raise SystemExit(f"{decision} {os.path.basename(path)}: candidate ids differ at row {i}")
                    if np.any(cands[i][counts[i]:] != 0) or np.any(ids[i][counts[i]:] != 0):
                        raise SystemExit(f"{decision} {os.path.basename(path)}: padding is not zero at row {i}")
                    checked_candidates += int(counts[i])
                checked_rows += count
            offset += count
        if offset != manifest["types"][decision]["rows"]:
            raise SystemExit(f"{decision}: pack has {manifest['types'][decision]['rows']} rows, "
                             f"the checked chunks have {offset}")
        print(f"  {decision:22s} {offset:7d} rows, {checked_candidates:8d} candidates checked", flush=True)
    print(json.dumps({"rows": checked_rows, "candidates": checked_candidates, "pack": args.pack}, indent=1))


if __name__ == "__main__":
    main()
