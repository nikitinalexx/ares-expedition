"""What does the end-of-game card sale actually raise, and what is it worth?

Rules (checked in the code): MAX_HAND_SIZE_LAST_ROUND = 10, SELL_EXTRA_CARDS_PHASE = 6, and every sold card pays 3 MC plus the
sell-price effects. So a large hand is money at the end of the game - the owner's point. This measures it per game from our own
corpus: the hand size at the last round, the cards sold above the limit, the money raised, and the association with the margin.

    py -3 ml/sale_report.py --pack C:/ares/pack/<name>
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

LIMIT = 10      # MAX_HAND_SIZE_LAST_ROUND
PRICE = 3       # the payout of one sold card (plus sell-price effects, not modelled here)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    args = parser.parse_args()
    manifest = ares_data.open_pack(args.pack)
    names = json.load(open(args.names, encoding="utf-8"))
    hand = names.index("hand:cards/10")

    # the last round of each game: the row with the largest round number, and within it the first one seen
    last = {}
    for decision in ("SELL_CARDS_LAST_ROUND", "SELL_CARDS", "COLLECT_INCOME"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        games = np.asarray(data["game"])
        rounds = np.asarray(data["round"], dtype=np.int32)
        margin = np.asarray(data["margin"], dtype=np.float32)
        obs = data["obs"]
        step = max(1, len(games) // 150_000)
        for start in range(0, len(games), 4096 * step):
            block = np.arange(start, min(start + 4096 * step, len(games)), step)
            cards = (obs[block][:, hand] * 10).astype(np.int32)
            for game, rnd, m, c in zip(games[block].tolist(), rounds[block].tolist(), margin[block].tolist(),
                                       cards.tolist()):
                current = last.get(game)
                # prefer the LAST_ROUND menu (that is where the sale happens), then the deepest round
                rank = (1 if decision == "SELL_CARDS_LAST_ROUND" else 0, rnd)
                if current is None or rank > current["rank"]:
                    last[game] = {"rank": rank, "hand": c, "margin": m}

    values = list(last.values())
    hands = np.array([v["hand"] for v in values])
    sold = np.maximum(0, hands - LIMIT)
    money = sold * PRICE
    margin = np.array([v["margin"] for v in values])
    print(f"{args.pack}: {len(values)} games, last-round hand mean {hands.mean():.1f}")
    print(f"cards sold above the limit {LIMIT}: mean {sold.mean():.1f}  (>=1 card in {(sold >= 1).mean():.1%} of games)")
    print(f"money raised by the sale: mean {money.mean():.1f} MC, median {np.median(money):.0f}, max {money.max():.0f}")
    print(f"correlation(money raised, margin) = {np.corrcoef(money, margin)[0, 1]:+.3f}")
    print()
    for low, high in ((0, 0), (1, 2), (3, 5), (6, 99)):
        mask = (sold >= low) & (sold <= high)
        if mask.sum() >= 50:
            print(f"  {low}-{high} cards sold: n={int(mask.sum()):6d}  mean margin {margin[mask].mean():+7.2f}")


if __name__ == "__main__":
    main()
