"""Is the player card-rich? Hand size at the end of the game, and what it is worth.

The extra-draw cards correlate with the final margin at -0.001 in our own games (ml/blue_report.py), which is odd if a card in
hand is free value. One candidate explanation: the hand is already full of cards that never get built, so one more adds
nothing. This measures the hand at the last decision of each game against the margin, and against the number of cards built.

    py -3 ml/hand_report.py --pack C:/ares/pack/<name>
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    args = parser.parse_args()
    manifest = ares_data.open_pack(args.pack)
    names = json.load(open(args.names, encoding="utf-8"))
    hand = names.index("hand:cards/10")
    affordable = names.index("hand:affordable_cards/10")
    blue = [i for i, n in enumerate(names) if n.startswith("cards:my_blue_")]

    last = {}
    for decision in ("COLLECT_INCOME", "PICK_PHASE", "SELL_CARDS", "BUILD_CARD"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        games = np.asarray(data["game"])
        margin = np.asarray(data["margin"], dtype=np.float32)
        step = max(1, len(games) // 200_000)
        for start in range(0, len(games), 4096 * step):
            block = np.arange(start, min(start + 4096 * step, len(games)), step)
            obs = data["obs"][block]
            cards = obs[:, hand] * 10
            pay = obs[:, affordable] * 10
            blue_count = obs[:, blue].sum(axis=1)
            for game, m, c, p, b in zip(games[block].tolist(), margin[block].tolist(), cards.tolist(),
                                        pay.tolist(), blue_count.tolist()):
                current = last.get(game)
                entry = {"hand": c, "affordable": p, "blue": b, "margin": m}
                if current is None or c > current["hand"]:
                    last[game] = entry

    values = list(last.values())
    hands = np.array([v["hand"] for v in values])
    margin = np.array([v["margin"] for v in values])
    affordable = np.array([v["affordable"] for v in values])
    print(f"{args.pack}: {len(values)} games")
    print(f"hand at its largest: mean {hands.mean():.1f}, median {np.median(hands):.0f}, "
          f"share with >10 cards {(hands > 10).mean():.1%}")
    print(f"of them affordable at that moment: mean {affordable.mean():.1f} "
          f"({(affordable >= 1).mean():.1%} of games had at least one playable card)")
    print(f"correlation(hand, margin) = {np.corrcoef(hands, margin)[0, 1]:+.3f}")
    print()
    for low, high in ((0, 5), (6, 8), (9, 11), (12, 99)):
        mask = (hands >= low) & (hands <= high)
        if mask.sum() >= 50:
            print(f"  hand {low:2d}-{high:2d}: n={int(mask.sum()):6d}  mean margin {margin[mask].mean():+7.2f}  "
                  f"mean affordable {affordable[mask].mean():.1f}")


if __name__ == "__main__":
    main()
