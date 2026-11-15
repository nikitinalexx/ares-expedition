"""Which cards does the player win with? A card-level look at the recorded games.

For every card id it builds, the mean final margin of the games in which it was built. This is correlation, not proof (a
strong position builds strong cards), but it is the only card-level signal available without a card-value table, and it
shows cheaply whether the player's card choice carries any information at all.

    py -3 ml/card_value.py DATA_DIR [--min-games 60]
"""
import argparse
import collections
import glob
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory")
    parser.add_argument("--min-games", type=int, default=60)
    args = parser.parse_args()

    files = sorted(glob.glob(os.path.join(args.directory, "BUILD_CARD.*.ares")))
    if not files:
        raise SystemExit(f"no BUILD_CARD chunks in {args.directory}")

    wins = collections.defaultdict(float)
    counts = collections.Counter()
    games = set()
    for path in files:
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            games.add(int(row["game"]))
            card = int(row["ids"][int(row["chosen"])])
            if card == 0xFFFF:
                continue
            wins[card] += float(row["margin"])
            counts[card] += 1

    total = sum(counts.values())
    print(f"{args.directory}: {len(games)} games, {total} builds of {len(counts)} distinct cards")
    usable = [(counts[card], wins[card] / counts[card], card) for card in counts if counts[card] >= args.min_games]
    usable.sort(key=lambda item: -item[1])
    print(f"\ncards built at least {args.min_games} times, by mean final margin:")
    print(f"  {'card':>6s} {'builds':>7s} {'mean margin':>12s}")
    for count, mean, card in usable[:15]:
        print(f"  {card:6d} {count:7d} {mean:+12.1f}")
    print("  ...")
    for count, mean, card in usable[-10:]:
        print(f"  {card:6d} {count:7d} {mean:+12.1f}")
    if usable:
        means = np.array([mean for _, mean, _ in usable])
        print(f"\nspread over cards: sd {means.std():.2f} points, best {means.max():+.1f}, worst {means.min():+.1f}")


if __name__ == "__main__":
    main()
