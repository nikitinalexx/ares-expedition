"""Blue cards and extra card draw: how many does each side build, and what do they buy?

Owner's questions (2026-11-17): does the game value the extra draw in phase 5 (many blue cards provide it), and why did phase 2
(blue/red builds) fall to 8% of our phase calls. The observation carries the blue-card identities of BOTH tables, so both
sides can be counted from our own games - no opponent model needed.

    py -3 ml/blue_report.py --pack C:/ares/pack/<name>
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

# Blue cards whose text adds cards to the hand (grep of cards/blue for getHand().addCard).
DRAW_CARDS = ("Decomposers", "EnergySubsidies", "ImpactAnalysis", "InterplanetaryConference", "MarsUniversity",
              "OlympusConference", "RecycledDetritus")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    names = json.load(open(args.names, encoding="utf-8"))
    mine = [i for i, n in enumerate(names) if n.startswith("cards:my_blue_")]
    theirs = [i for i, n in enumerate(names) if n.startswith("cards:opponent_blue_")]
    draw_mine = [i for i, n in enumerate(names) if any(n.endswith("_blue_" + card) for card in DRAW_CARDS)]

    per_game = {}
    for decision in ("BUILD_CARD", "PICK_PHASE", "COLLECT_INCOME", "STANDARD_ACTION"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        games = np.asarray(data["game"])
        margin = np.asarray(data["margin"], dtype=np.float32)
        step = max(1, len(games) // 200_000)
        for start in range(0, len(games), 4096 * step):
            block = np.arange(start, min(start + 4096 * step, len(games)), step)
            obs = data["obs"][block]
            blue_mine = obs[:, mine].sum(axis=1)
            blue_theirs = obs[:, theirs].sum(axis=1)
            draws = obs[:, draw_mine].sum(axis=1) if draw_mine else np.zeros(len(block))
            for game, m, bm, bt, d in zip(games[block].tolist(), margin[block].tolist(),
                                          blue_mine.tolist(), blue_theirs.tolist(), draws.tolist()):
                best = per_game.get(game)
                if best is None or bm > best["blue"]:
                    per_game[game] = {"blue": bm, "opponent": bt, "draw": d, "margin": m}

    values = list(per_game.values())
    blue = np.array([v["blue"] for v in values])
    opponent = np.array([v["opponent"] for v in values])
    draws = np.array([v["draw"] for v in values])
    margin = np.array([v["margin"] for v in values])
    print(f"{args.pack}: {len(values)} games")
    print(f"blue cards on our table at the end: mean {blue.mean():.2f}  (>=1: {(blue >= 1).mean():.1%})")
    print(f"blue cards on the opponent's table:  mean {opponent.mean():.2f}  (>=1: {(opponent >= 1).mean():.1%})")
    print(f"of them, extra-draw blue cards:      mean {draws.mean():.2f}  (>=1: {(draws >= 1).mean():.1%})")
    print()
    for label, mask in (("no blue card", blue == 0), ("1-2 blue", (blue >= 1) & (blue <= 2)), ("3+ blue", blue >= 3)):
        if mask.sum() >= 20:
            print(f"  {label:14s} n={int(mask.sum()):6d}  mean margin {margin[mask].mean():+7.2f}")
    print()
    for label, mask in (("no draw card", draws == 0), ("with a draw card", draws >= 1)):
        if mask.sum() >= 20:
            print(f"  {label:16s} n={int(mask.sum()):6d}  mean margin {margin[mask].mean():+7.2f}")
    if len(values) > 100:
        print()
        print(f"correlation(blue cards, margin) = {np.corrcoef(blue, margin)[0, 1]:+.3f}")
        print(f"correlation(draw cards, margin) = {np.corrcoef(draws, margin)[0, 1]:+.3f}")


if __name__ == "__main__":
    main()
