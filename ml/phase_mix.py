"""The phase mix of BOTH seats, read from the observation (the picks are revealed, so the opponent's are public).

Why: the owner's NETWORK_V2 profile is "phase 1 (cheap green builds) + multiply income, finish by ~25". Whether the current benchmark does the same is a
measurable fact, not a guess: every row carries the previous round's picks for each side (`extra:my_previous_phase_*`, `extra:opponent_previous_phase_*`),
because the picks are simultaneous and then revealed. This tool averages those one-hot vectors into a mix per seat.

    py -3 ml/phase_mix.py --data D:/ares/data/1120_vs_network_full
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

PHASES = 5


def columns(names):
    index = {name: i for i, name in enumerate(names)}
    mine = [index[f"extra:my_previous_phase_{p}"] for p in range(1, PHASES + 1)]
    theirs = [index[f"extra:opponent_previous_phase_{p}"] for p in range(1, PHASES + 1)]
    return mine, theirs


def mix(rows, mine_columns, theirs_columns, min_round=2):
    """Mean pick distribution per seat. Rows before `min_round` have no revealed picks, and empty vectors are skipped."""
    ours = np.zeros(PHASES)
    theirs = np.zeros(PHASES)
    used = 0
    for row in rows:
        if int(row["round"]) < min_round:
            continue
        a = np.array([float(row["obs"][i]) for i in mine_columns])
        b = np.array([float(row["obs"][i]) for i in theirs_columns])
        if a.sum() <= 0 or b.sum() <= 0:
            continue
        ours += a / a.sum()
        theirs += b / b.sum()
        used += 1
    if used == 0:
        return np.zeros(PHASES), np.zeros(PHASES), 0
    return ours / used, theirs / used, used


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--chunks", type=int, default=0, help="0 = every chunk")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    mine_columns, theirs_columns = columns(names)

    files = sorted(glob.glob(os.path.join(args.data, "PICK_PHASE.*.ares")))
    if args.chunks:
        files = files[:args.chunks]
    ours = np.zeros(PHASES)
    theirs = np.zeros(PHASES)
    used = 0
    for path in files:
        _, rows = ares_data.read_decisions(path)
        a, b, n = mix(rows, mine_columns, theirs_columns)
        if n == 0:
            continue
        ours += a * n
        theirs += b * n
        used += n
    if used == 0:
        raise SystemExit("no revealed phase picks in this corpus")

    print(f"{args.data}: {used} decisions with revealed picks")
    print("  phase   ours    opponent")
    for p in range(PHASES):
        print(f"  {p + 1:>5}  {100 * ours[p] / used:5.1f}%  {100 * theirs[p] / used:7.1f}%")


if __name__ == "__main__":
    main()
