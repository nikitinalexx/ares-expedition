"""How much money does the end-of-round card sale raise over a whole game?

Checked in the code (2026-11-17, second look): `StateTransitionService.performStateTransferFromPhase` enters
`SellExtraCardsState` after EVERY round, whenever some player holds more than MAX_HAND_SIZE_LAST_ROUND = 10 cards - not only
at the end of the game, despite the name of the turn. So the excess is trimmed to 10 and paid at 3 MC per card, and the money
is spendable in the rounds that follow. That makes a large hand a per-round income stream, which is exactly the owner's point.

This measures it per game: for every round, the largest hand seen, the cards sold above the limit, and the money raised over
the whole game.

    py -3 ml/sale_report.py --pack C:/ares/pack/<name> --per-round
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

LIMIT = 10   # MAX_HAND_SIZE_LAST_ROUND
PRICE = 3    # payout of one sold card, before sell-price effects


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    args = parser.parse_args()
    manifest = ares_data.open_pack(args.pack)
    names = json.load(open(args.names, encoding="utf-8"))
    hand = names.index("hand:cards/10")

    # the largest hand per (game, round): that is what the end-of-round trim sees
    per_round = {}
    margin = {}
    for decision in ("PICK_PHASE", "COLLECT_INCOME", "SELL_CARDS", "SELL_CARDS_LAST_ROUND", "DRAFT_CARDS"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        games = np.asarray(data["game"])
        rounds = np.asarray(data["round"], dtype=np.int32)
        margins = np.asarray(data["margin"], dtype=np.float32)
        obs = data["obs"]
        step = max(1, len(games) // 200_000)
        for start in range(0, len(games), 4096 * step):
            block = np.arange(start, min(start + 4096 * step, len(games)), step)
            cards = (obs[block][:, hand] * 10).astype(np.int32)
            for game, rnd, m, c in zip(games[block].tolist(), rounds[block].tolist(), margins[block].tolist(),
                                       cards.tolist()):
                key = (game, rnd)
                if per_round.get(key, -1) < c:
                    per_round[key] = c
                margin[game] = m

    sold = np.array([max(0, hand_size - LIMIT) for hand_size in per_round.values()])
    games = sorted(margin)
    money_per_game = {}
    cards_per_game = {}
    rounds_per_game = {}
    for (game, _), value in zip(per_round.keys(), sold):
        money_per_game[game] = money_per_game.get(game, 0) + value * PRICE
        cards_per_game[game] = cards_per_game.get(game, 0) + value
        rounds_per_game[game] = rounds_per_game.get(game, 0) + 1

    money = np.array([money_per_game[g] for g in games])
    cards = np.array([cards_per_game[g] for g in games])
    margins = np.array([margin[g] for g in games])
    rounds = np.array([rounds_per_game[g] for g in games])
    print(f"{args.pack}: {len(games)} games, {rounds.mean():.1f} rounds each")
    print(f"cards sold over a game: mean {cards.mean():.1f}  -> money {money.mean():.1f} MC "
          f"(median {np.median(money):.0f}, max {money.max():.0f})")
    print(f"per round: {cards.mean() / max(rounds.mean(), 1):.2f} cards, "
          f"{money.mean() / max(rounds.mean(), 1):.2f} MC")
    print(f"correlation(money, margin) = {np.corrcoef(money, margins)[0, 1]:+.3f}")
    print()
    for low, high in ((0, 0), (1, 20), (21, 60), (61, 10_000)):
        mask = (money >= low) & (money <= high)
        if mask.sum() >= 50:
            print(f"  {low:3d}-{high:5d} MC from sales: n={int(mask.sum()):6d}  mean margin {margins[mask].mean():+7.2f}")


if __name__ == "__main__":
    main()
