"""What share of the rows lies beyond the model's input clip, per economically important feature?

The model standardises every column and clamps it to mean +/- CLIP * sigma (CLIP = 5 in train_phase/train_value and in the
exported Java model). A column whose tail is beyond that point is FLAT there: 60 MC and 200 MC produce the same input. This
prints, for the counters that decide games, the clip point in encoded units and the share of rows above it.
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

CLIP = 5.0
WATCH = ("mc", "mc_income", "plants", "heat", "forests", "win_points", "hand_cards_count", "blue_cards_count",
         "played_cards_count", "tr", "steel_income", "titanium_income", "cards")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--rows", type=int, default=400_000)
    args = parser.parse_args()
    names = json.load(open(args.names, encoding="utf-8"))
    manifest = ares_data.open_pack(args.pack)
    wanted = {names.index(name): name for name in WATCH if name in names}

    samples = {index: [] for index in wanted}
    for decision in sorted(manifest["types"]):
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = len(data["obs"])
        step = max(1, rows // max(1, args.rows // len(manifest["types"])))
        for start in range(0, rows, 4096 * step):
            block = np.arange(start, min(start + 4096 * step, rows), step)
            if not len(block):
                continue
            values = np.asarray(data["obs"][block])
            for index in wanted:
                samples[index].append(values[:, index].astype(np.float32))

    print(f"{'feature':20s} {'max':>7s} {'mean':>7s} {'std':>7s} {'clip at':>8s} {'rows above clip':>16s}")
    print('-' * 74)
    for index, name in wanted.items():
        values = np.concatenate(samples[index])
        mean, std = float(values.mean()), float(values.std())
        if std < 1e-9:
            print(f"{name:20s} {values.max():7.2f} {mean:7.3f} {std:7.4f} {'-':>8s} {'constant':>16s}")
            continue
        clip = mean + CLIP * std
        share = float((values > clip).mean())
        print(f"{name:20s} {values.max():7.2f} {mean:7.3f} {std:7.4f} {clip:8.2f} {share * 100:15.1f}%")


if __name__ == "__main__":
    main()
