"""Premise 1 (owner's question, 20/11): WHEN are the points created?

Why this decides the plan: the owner's jump from 90 to 180 in the last round is not an anecdote, it is a claim about the game's structure.
The measurement separates two very different meanings of it:

  * points made LATE by late play - then the object to learn is the end-game sequence, and whole-game plans are secondary;
  * points SETTLED late on a position built over the whole game (final card conversions, awards) - then the position IS the object, the
    plan-space attack is aimed correctly, and "the last round decides" is a statement about settlement, not about decisions.

The round grid comes from PICK_PHASE rows (once per round per seat) and the live score columns of the observation; see
`round_scores.load_round_grid` for why the pack's `breakdown` cannot be used for this.

    py -3 ml/points_by_round.py --packed D:/ares/pack/1119-2204_22 --names D:/ares/tmp/names_now.json
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import round_scores  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--packed", required=True)
    parser.add_argument("--names", required=True, help="the observation names file the pack was built with")
    parser.add_argument("--type", default="PICK_PHASE")
    parser.add_argument("--tail", type=int, default=3)
    parser.add_argument("--max-seats", type=int, default=20000, help="analyse at most this many game-seats (0 = all)")
    args = parser.parse_args()

    with open(args.names, encoding="utf-8") as handle:
        names = json.load(handle)
    grid = round_scores.load_round_grid(args.packed, names, args.type)
    seats = round_scores.points_per_round(grid)
    if args.max_seats and len(seats) > args.max_seats:
        step = max(1, len(seats) // args.max_seats)
        seats = seats[::step]
    if not seats:
        raise SystemExit("no game-seats")

    rounds = np.array([len(s["rounds"]) for s in seats])
    finals = np.array([s["final"] for s in seats])
    settlements = np.array([s["settlement"] for s in seats])
    play = finals - settlements                      # points created during the rounds
    settlement_share = np.where(finals > 0, settlements / finals, np.nan)

    print(f"pack {args.packed}")
    print(f"game-seats {len(seats)}")
    print(f"rounds per seat: mean {rounds.mean():.1f}, median {np.median(rounds):.0f}, "
          f"p10 {np.percentile(rounds, 10):.0f}, p90 {np.percentile(rounds, 90):.0f}")
    print(f"final score: mean {finals.mean():.1f}, sd {finals.std():.1f}")
    print(f"points made during the rounds: mean {play.mean():.1f}")
    print(f"points settled at the end: mean {settlements.mean():.1f}"
          f"  -> {np.nanmean(settlement_share) * 100:.1f}% of the final score")
    print(f"  (per seat: median {np.nanmedian(settlement_share) * 100:.1f}%,"
          f" p10 {np.nanpercentile(settlement_share, 10) * 100:.1f}%,"
          f" p90 {np.nanpercentile(settlement_share, 90) * 100:.1f}%)")
    print()

    # points per round position, counted from the START and from the END (games have different lengths)
    max_rounds = int(rounds.max())
    from_start = np.zeros(max_rounds, dtype=np.float64)
    counts_start = np.zeros(max_rounds, dtype=np.int64)
    from_end = np.zeros(max_rounds, dtype=np.float64)
    counts_end = np.zeros(max_rounds, dtype=np.int64)
    for s in seats:
        for i, p in enumerate(s["points"]):
            if i < max_rounds:
                from_start[i] += p
                counts_start[i] += 1
            back = len(s["points"]) - 1 - i
            if back < max_rounds:
                from_end[back] += p
                counts_end[back] += 1

    print("points created per round, counted from the START of the game:")
    print("  round   mean points   share of a game's points")
    total_mean = np.nansum(np.where(counts_start > 0, from_start / np.maximum(counts_start, 1), np.nan))
    for i in range(max_rounds):
        if counts_start[i] == 0:
            continue
        mean = from_start[i] / counts_start[i]
        print(f"  {i:>5}   {mean:>11.2f}   {mean / total_mean * 100:>11.1f}%"
              f"   (games reaching it: {counts_start[i]})")
    print()

    print("points created per round, counted from the END of the game (0 = the last round, settlement included):")
    for back in range(max_rounds):
        if counts_end[back] == 0:
            continue
        mean = from_end[back] / counts_end[back]
        print(f"  -{back:<4}   {mean:>11.2f}   {mean / total_mean * 100:>11.1f}%")
    print()

    tail = np.array([sum(s["points"][-args.tail:]) / s["final"] if s["final"] > 0 else np.nan for s in seats])
    print(f"share of a seat's points created in its last {args.tail} rounds (settlement included):")
    print(f"  mean {np.nanmean(tail) * 100:.1f}%  median {np.nanmedian(tail) * 100:.1f}%")
    print()
    print("reading: a large settlement share means the final score is decided by the POSITION, not by the last round's play; a large")
    print("last-rounds share WITH a small settlement means late play decides, and planning should target the end-game sequence.")


if __name__ == "__main__":
    main()
