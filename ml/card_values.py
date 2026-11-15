"""How much is a card worth? Ridge regression of the final margin on the set of built cards.

Motivation (owner, 2026-11-14): cards are very unequal -- one card can move the value signal of the network by 10-20%.
The encoder gives the policy a 64-bit hash signature of every offered card (CardSignature), so the identity is
*representable*, but the only label in the pipeline is the game result: with one outcome per ~500 decisions the net
has almost no signal to learn a per-card value, and the usefulness of a card is exactly the part that has to be
learned. This script measures the per-card value from the data instead of hoping the policy picks it up:

    margin(game, player) ~ intercept + beta * (number of cards built) + sum_c coefficient[c] * built[c]

Coefficients are "points of final margin that a game with this card built has, compared with a game without it, at
equal number of built cards". It is a correlation, not a proof of causation: a card that is only affordable when the
player is already ahead gets credit for that. Still, the ordering is the useful signal and it is testable
(out-of-fold R^2, and the ranking of the top cards by support).

    python ml/card_values.py --data DIR1,DIR2 --out card_values.json [--folds 5]
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import open_memmap  # noqa: E402
from train_phase import is_validation_game, list_chunks  # noqa: E402

STOP_ID = 0xFFFF


def read_built_cards(data_dirs, decision="BUILD_CARD"):
    """{(game, seat): set of built card ids} plus the margin of that player's game."""
    built = {}
    margin = {}
    for directory in [d for d in data_dirs.split(",") if d]:
        files = list_chunks(directory, decision)
        print(f"{directory}: {len(files)} {decision} chunks", flush=True)
        for path in files:
            schema, rows = open_memmap(path)
            games = rows["game"]
            seats = rows["seat"]
            margins = rows["margin"]
            chosen = rows["chosen"]
            ids = rows["ids"]
            for i in range(len(rows)):
                key = (int(games[i]), int(seats[i]))
                card = int(ids[i][chosen[i]])
                if card == STOP_ID:
                    continue
                built.setdefault(key, set()).add(card)
                margin[key] = float(margins[i])
    return built, margin


def ridge_fit(x, y, alpha):
    """Closed form ridge on (1 + features) columns; the intercept is not penalised."""
    gram = x.T @ x
    penalty = np.eye(gram.shape[0]) * alpha
    penalty[0, 0] = 0.0
    return np.linalg.solve(gram + penalty, x.T @ y)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", default="card_values.json")
    parser.add_argument("--alpha", type=float, default=50.0, help="ridge penalty; higher shrinks rare cards to 0")
    parser.add_argument("--folds", type=int, default=5)
    parser.add_argument("--min-support", type=int, default=200,
                        help="cards built in fewer games are reported separately, their coefficient is not trusted")
    args = parser.parse_args()

    built, margin = read_built_cards(args.data)
    print(f"{len(built)} player-games, {len(set(c for s in built.values() for c in s))} distinct cards", flush=True)
    cards = sorted({c for s in built.values() for c in s})
    index = {c: i + 2 for i, c in enumerate(cards)}  # column 0 = intercept, 1 = number of cards built
    keys = [k for k in built if k in margin]
    x = np.zeros((len(keys), len(cards) + 2), dtype=np.float64)
    y = np.zeros(len(keys), dtype=np.float64)
    game_of = np.zeros(len(keys), dtype=np.uint64)
    for row, key in enumerate(keys):
        s = built[key]
        x[row, 0] = 1.0
        x[row, 1] = len(s)
        for c in s:
            x[row, index[c]] = 1.0
        y[row] = margin[key]
        game_of[row] = key[0]

    # out-of-fold quality: the same split by game that the policy training uses
    folds = np.zeros(len(keys), dtype=np.int64)
    for f in range(args.folds):
        folds[is_validation_game(game_of, (f + 1) / args.folds) & (folds == 0)] = f + 1
    prediction = np.zeros_like(y)
    for f in range(1, args.folds + 1):
        train, test = folds != f, folds == f
        if test.sum() == 0 or train.sum() == 0:
            continue
        beta = ridge_fit(x[train], y[train], args.alpha)
        prediction[test] = x[test] @ beta
    residual = float(np.sum((y - prediction) ** 2))
    total = float(np.sum((y - y.mean()) ** 2))
    r2 = 1.0 - residual / max(total, 1e-9)
    baseline = float(np.sum((y - y.mean()) ** 2))
    beta = ridge_fit(x, y, args.alpha)

    support = np.zeros(len(cards), dtype=np.int64)
    for s in built.values():
        for c in s:
            support[index[c] - 2] += 1
    rows = []
    for i, c in enumerate(cards):
        rows.append({"card": c, "coefficient": round(float(beta[index[c]]), 2),
                     "games": int(support[i]), "trusted": bool(support[i] >= args.min_support)})
    rows.sort(key=lambda r: -r["coefficient"])
    trusted = [r for r in rows if r["trusted"]]
    report = {"player_games": len(keys), "cards": len(cards), "alpha": args.alpha,
              "cards_built_coefficient": round(float(beta[1]), 2), "intercept": round(float(beta[0]), 2),
              "out_of_fold_r2": round(r2, 4), "out_of_fold_baseline_mse": round(baseline / len(keys), 2),
              "out_of_fold_mse": round(residual / len(keys), 2),
              "min_support": args.min_support, "top": trusted[:20], "bottom": trusted[-10:],
              "untrusted_top": [r for r in rows if not r["trusted"]][:10]}
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump({**report, "all": rows}, f, indent=1)
    print(json.dumps({k: v for k, v in report.items() if k not in ("top", "bottom", "untrusted_top")}, indent=1))
    print("top cards (points of margin, at equal number of cards built):")
    for r in report["top"]:
        print(f"  card {r['card']:6d}  {r['coefficient']:+6.2f}  ({r['games']} games)")
    print("worst cards:")
    for r in report["bottom"]:
        print(f"  card {r['card']:6d}  {r['coefficient']:+6.2f}  ({r['games']} games)")
    print("rare cards with the highest coefficients (not trusted):")
    for r in report["untrusted_top"]:
        print(f"  card {r['card']:6d}  {r['coefficient']:+6.2f}  ({r['games']} games)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
