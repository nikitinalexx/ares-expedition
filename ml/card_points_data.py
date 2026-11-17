"""Does the corpus contain the fact a critic would have to learn?

The critic ranks cards, and the channel breakdown says the whole deficit is in printed card points (9.3 against SMART's
18.8 at the same number of builds). Before blaming the network, measure the data: a model trained on *who won our games*
can only learn that point cards pay if (a) point cards are actually built and (b) the side that built them wins more often.
This prints both, from a pack, with no new games:

    py -3 ml/card_points_data.py --pack C:/ares/pack/bd40

Coverage of the missing behaviour is the number to compare against `ml/action_coverage.py`: if point cards are offered in
half the menus and taken in 5%, that is a collapsed option, and it is the measured reason to explore that decision type.
"""
import argparse
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402
from card_value_check import printed_points  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAYABLE = os.path.join(ROOT, "src", "main", "java", "com", "terraforming", "ares", "services", "agent",
                        "PlayableCards.java")


def card_order():
    """The identity index of a card is its position in `PlayableCards.ALL`, which is the candidate one-hot."""
    text = open(PLAYABLE, encoding="utf-8").read()
    block = text.split("List<Class<? extends Card>> ALL = List.of(", 1)[1].split(");", 1)[0]
    return re.findall(r"(\w+)\.class", block)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--type", default="BUILD_CARD")
    parser.add_argument("--chunk", type=int, default=50000)
    args = parser.parse_args()

    order = card_order()
    points = printed_points()
    per_card = np.array([points.get(name, 0) for name in order], dtype=np.int32)
    print(f"{len(order)} playable cards, {int((per_card > 0).sum())} with printed points", flush=True)

    manifest = ares_data.open_pack(args.pack)
    data = ares_data.open_packed_type(args.pack, args.type, manifest)
    cands = data["cands"]
    chosen = data["chosen"]
    margin = np.asarray(data["margin"], dtype=np.float64)
    outcome = np.asarray(data["outcome"], dtype=np.float64)
    game = np.asarray(data["game"])
    seat = np.asarray(data["seat"])
    keep = np.asarray(data["keep"], dtype=bool)
    rows = len(chosen)
    print(f"{rows} {args.type} rows, {len(np.unique(game))} games", flush=True)

    picked = np.zeros(rows, dtype=np.int32)       # printed points of the chosen card
    best_available = np.zeros(rows, dtype=np.int32)
    any_available = np.zeros(rows, dtype=np.int32)
    for start in range(0, rows, args.chunk):
        stop = min(start + args.chunk, rows)
        for i in range(start, stop):
            base, count, pick = cands.offsets[i], cands.count[i], chosen[i]
            if count <= 0 or pick < 0 or pick >= count:
                continue
            features = cands.flat[base:base + count]
            # one-hot identity per candidate (the last column is the stop flag)
            index = np.asarray(features[:, :-1], dtype=np.float32).argmax(axis=1)
            costs = per_card[index]
            picked[i] = costs[pick]
            best_available[i] = costs.max()
            any_available[i] = int((costs >= 2).sum())

    used = keep
    print()
    print("printed points of the card this decision BUILT")
    values, counts = np.unique(picked[used], return_counts=True)
    for value, count in zip(values.tolist(), counts.tolist()):
        share = 100.0 * count / used.sum()
        print(f"  {value:3d} points  {count:8d} rows  {share:5.1f}%")
    print(f"  mean picked {picked[used].mean():+.2f}, mean best offered {best_available[used].mean():+.2f}")
    print(f"  menus with a >=2-point card offered: {100.0 * (any_available[used] > 0).mean():5.1f}%")

    # the causal question: among rows where a >=2-point card was OFFERED, does taking it end better?
    offered = used & (any_available > 0)
    took = offered & (picked >= 2)
    print()
    print("among decisions where a >=2-point card was on the menu")
    print(f"  took one:      {took.sum():8d} rows, mean margin {margin[took].mean():+6.2f}, win {outcome[took].mean():.3f}")
    print(f"  took another:  {(offered & ~took).sum():8d} rows, mean margin "
          f"{margin[offered & ~took].mean():+6.2f}, win {outcome[offered & ~took].mean():.3f}")
    print(f"  correlation(picked points, margin)  = {np.corrcoef(picked[used], margin[used])[0, 1]:+.3f}")
    print(f"  correlation(best offered, margin)   = {np.corrcoef(best_available[used], margin[used])[0, 1]:+.3f}")

    # per seat over a whole game: total printed points of the cards built, winner against loser
    total = {}
    for i in np.flatnonzero(used):
        key = (int(game[i]), int(seat[i]))
        total[key] = total.get(key, 0) + int(picked[i])
    won, lost = [], []
    for (g, s), value in total.items():
        other = total.get((g, 1 - s))
        if other is None:
            continue
        index = np.flatnonzero((game == g) & (seat == s))[0]
        (won if margin[index] > 0 else lost).append(value)
    if won and lost:
        print()
        print("across games (only the decisions logged in the pack)")
        print(f"  winners built {np.mean(won):6.2f} printed points, losers {np.mean(lost):6.2f}, "
              f"paired difference {np.mean(won) - np.mean(lost):+6.2f}")


if __name__ == "__main__":
    main()
