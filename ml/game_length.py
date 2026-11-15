"""Does a longer game favour the card table? A look at the games we recorded.

Terraforming ends the game: every point of TR, every forest and every ocean shortens it. A player that converts its money
into TR closes the game before its own card engine has paid off, while a player that keeps the game going gets more income
rounds, more drafts and more cards. This script tests that from the recorded games: per game, the length, both scores, both
card tables, and the correlation between them.

    py -3 ml/game_length.py DIR [DIR ...]
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402

NAMES = ["D:/ares/tmp/names_v5.json", "D:/ares/tmp/names_v4.json", "D:/ares/tmp/names795b.json"]


def names_for(size):
    for path in NAMES:
        if os.path.exists(path):
            names = json.load(open(path, encoding="utf-8"))
            if len(names) == size:
                return {name: index for index, name in enumerate(names)}
    raise SystemExit(f"no names file with {size} features")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directories", nargs="+")
    args = parser.parse_args()

    games = {}
    for directory in args.directories:
        for path in glob.glob(os.path.join(directory, "*.ares")):
            _, rows = ares_data.read_decisions(path)
            for row in rows:
                key = (int(row["game"]), int(row["seat"]))
                game = games.setdefault(key, {"turns": 0, "obs": None, "step": -1, "outcome": 0.0, "margin": 0.0})
                game["turns"] = max(game["turns"], int(row["round"]))
                game["outcome"] = float(row["outcome"])
                game["margin"] = float(row["margin"])
                if int(row["step"]) >= game["step"]:
                    game["step"] = int(row["step"])
                    game["obs"] = row["obs"].astype(np.float32)
    if not games:
        raise SystemExit("nothing recorded")

    index = names_for(len(next(iter(games.values()))["obs"]))
    seats = list(games.values())
    turns = np.array([s["turns"] for s in seats], dtype=float)
    margin = np.array([s["margin"] for s in seats])
    cards = np.array([s["obs"][index["played_cards_count"]] for s in seats])
    tr = np.array([s["obs"][index["mc_income"]] for s in seats])  # mc_income = mcIncome + TR
    points = np.array([s["obs"][index["score:my_winning_points"]] for s in seats])

    print(f"{len(seats)} seats, game length {turns.min():.0f}..{turns.max():.0f} turns, mean {turns.mean():.1f}")
    print(f"correlation of the final margin with: turns {np.corrcoef(turns, margin)[0, 1]:+.3f}, "
          f"cards on the table {np.corrcoef(cards, margin)[0, 1]:+.3f}, "
          f"money production {np.corrcoef(tr, margin)[0, 1]:+.3f}")

    # long games against short ones: who wins, and with what table?
    third = np.quantile(turns, [1 / 3, 2 / 3])
    short = turns <= third[0]
    long_ = turns >= third[1]
    for label, mask in (("short games", short), ("long games", long_)):
        print(f"\n{label}: {int(mask.sum())} seats, mean turns {turns[mask].mean():.0f}")
        print(f"  mean margin {margin[mask].mean():+.1f}, cards {cards[mask].mean():.2f}, "
              f"money production {tr[mask].mean():.1f}, points {points[mask].mean():.1f}")

    # within a game, who has the bigger table: the winner or the loser?
    by_game = {}
    for key, seat in games.items():
        by_game.setdefault(key[0], []).append(seat)
    bigger_wins = smaller_wins = equal = 0
    for sides in by_game.values():
        if len(sides) != 2:
            continue
        a, b = sides
        if abs(a["margin"]) < 1e-6:
            equal += 1
            continue
        winner = a if a["margin"] > 0 else b
        loser = b if winner is a else a
        if winner["obs"][index["played_cards_count"]] > loser["obs"][index["played_cards_count"]]:
            bigger_wins += 1
        elif winner["obs"][index["played_cards_count"]] < loser["obs"][index["played_cards_count"]]:
            smaller_wins += 1
        else:
            equal += 1
    print(f"\nthe seat with more cards on the table won {bigger_wins} games, the one with fewer {smaller_wins}, "
          f"equal {equal}")


if __name__ == "__main__":
    main()
