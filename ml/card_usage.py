"""Which cards are actually built, and how many printed points they carry.

Reads BUILD_CARD rows of a recorded dataset (`SMART:SMART` for the benchmark, an `AGENT` run for us) and counts the
chosen cards. The printed victory points come from the last candidate feature, which is the card's own points/10, so the
comparison needs no card table: it is the same feature the policy now sees.

    py -3 ml/card_usage.py DIR [DIR ...]
"""
import collections
import glob
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def analyse(directory):
    games = set()
    built = collections.Counter()
    points_of = {}
    printed = []
    for path in glob.glob(os.path.join(directory, "BUILD_CARD.*.ares")):
        schema, rows = ares_data.read_decisions(path)
        for row in rows:
            games.add(int(row["game"]))
            ids = row["ids"][: int(row["count"])]
            features = row["feat"][: int(row["count"])]
            chosen = int(row["chosen"])
            card = int(ids[chosen])
            if card == 0xFFFF:
                continue
            built[card] += 1
            points_of[card] = float(features[chosen][-1]) * 10.0
            printed.append(points_of[card])
    per_game = len(games) or 1
    print(f"\n{directory}: {per_game} games, {sum(built.values()) / per_game:.1f} builds per game, "
          f"printed points per game {sum(printed) / per_game:.1f} "
          f"(per build {np.mean(printed) if printed else 0:.2f})")
    print("  most built cards (id, points, per game):")
    for card, count in built.most_common(15):
        print(f"    {card:5d}  {points_of.get(card, 0):4.0f}  {count / per_game:5.2f}")
    if printed:
        share = np.array([p > 0 for p in printed]).mean()
        print(f"  share of builds that carry printed points: {share:.2f}")
        high = np.array([p for p in printed if p >= 2])
        print(f"  builds worth 2+ points: {len(high)} ({len(high) / len(printed):.2f})")


def main():
    for directory in sys.argv[1:]:
        analyse(directory)


if __name__ == "__main__":
    main()
