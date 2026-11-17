"""Every observation value, its observed range, and whether that range survives the model's own preprocessing.

The owner's requirement (2026-11-17): if the difference between 50 and 200 MC is lost anywhere, that is a catastrophe - and the
same holds for every other quantity. A value can be lost in two places:

1. the ENCODER, if it divides by a constant that saturates (or is capped) - the raw number would never reach the vector;
2. the MODEL's preprocessing, which standardises every column and clips at +/- CLIP sigmas
   (`torch.clamp((x - mean) / std, -CLIP, CLIP)` in ValueNet). A column whose top end is many sigmas away from the mean is
   flattened there: 50 and 200 MC would become the same input.

So this reports, per feature: the observed min/mean/max, the standard deviation, the largest |z| that occurs, and the share of
rows that hit the clip. A high clipped share on an economically important value is a defect of the representation, not of the
training.

    py -3 ml/feature_ranges.py --pack C:/ares/pack/mirror_allrows --names D:/ares/tmp/names_now.json
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

CLIP = 5.0   # train_phase / train_value CLIP, mirrored by the exported model


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--rows", type=int, default=300_000)
    parser.add_argument("--min-std", type=float, default=1e-6)
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    manifest = ares_data.open_pack(args.pack)
    width = manifest["obsSize"]
    if len(names) != width:
        raise SystemExit(f"{args.names} has {len(names)} names, the pack has {width} features")

    total = 0
    count = np.zeros(width, dtype=np.int64)
    total_sum = np.zeros(width, dtype=np.float64)
    total_sq = np.zeros(width, dtype=np.float64)
    minimum = np.full(width, np.inf)
    maximum = np.full(width, -np.inf)
    for decision in sorted(manifest["types"]):
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = len(data["obs"])
        step = max(1, rows // max(1, args.rows // max(1, len(manifest["types"]))))
        for start in range(0, rows, 4096 * step):
            block = np.arange(start, min(start + 4096 * step, rows), step)
            if not len(block):
                continue
            values = np.asarray(data["obs"][block], dtype=np.float32)
            count += len(values)
            total_sum += values.sum(axis=0, dtype=np.float64)
            total_sq += (values.astype(np.float64) ** 2).sum(axis=0)
            minimum = np.minimum(minimum, values.min(axis=0))
            maximum = np.maximum(maximum, values.max(axis=0))
        if total == 0:
            total = 1
    mean = total_sum / np.maximum(count, 1)
    std = np.sqrt(np.maximum(total_sq / np.maximum(count, 1) - mean ** 2, 0.0))
    z_high = np.where(std > args.min_std, (maximum - mean) / np.maximum(std, 1e-9), 0.0)
    z_low = np.where(std > args.min_std, (minimum - mean) / np.maximum(std, 1e-9), 0.0)

    # bit columns: report them as a group (the owner already knows they are 0/1)
    bits = [i for i, n in enumerate(names) if n.startswith("cards:") or n.startswith("op_table_")
            or n.startswith("table_phase_") or n.startswith("milestone_") or n.startswith("award_")]
    bitset = set(bits)
    print(f"{args.pack}: {int(count.sum()) if count.sum() else 0} values sampled, {width} features "
          f"({len(bits)} one-hot bits summarised separately)")
    print()
    print(f"{'feature':38s} {'min':>8s} {'mean':>9s} {'max':>9s} {'std':>8s} {'max|z|':>7s}  clipped?")
    print('-' * 96)
    for index, name in enumerate(names):
        if index in bitset or count[index] == 0:
            continue
        if maximum[index] == minimum[index]:
            continue
        worst = max(abs(z_low[index]), abs(z_high[index]))
        flag = "**AT THE CLIP**" if worst > CLIP else ("near" if worst > 0.8 * CLIP else "")
        print(f"{name:38s} {minimum[index]:8.2f} {mean[index]:9.3f} {maximum[index]:9.2f} "
              f"{std[index]:8.4f} {worst:7.2f}  {flag}")

    live_bits = [i for i in bits if count[i] and maximum[i] > 0]
    print()
    print(f"one-hot bits: {len(bits)} total, {len(live_bits)} ever set in this corpus, "
          f"{len(bits) - len(live_bits)} never fire")
    print("  (a bit that never fires carries no information in this corpus; it is listed by ml/feature_audit.py)")
    clipped = [names[i] for i in range(width) if i not in bitset and count[i]
               and max(abs(z_low[i]), abs(z_high[i])) > CLIP]
    print()
    print(f"features whose top or bottom is cut by the +/-{CLIP} sigma clip: {len(clipped)}")
    for name in clipped[:40]:
        print(f"  {name}")


if __name__ == "__main__":
    main()
