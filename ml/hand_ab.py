"""Conditional A/B of the hand identity bits: only the rows where the hand is actually playable.

Why this exists. The first A/B (`--drop-prefix cards:in_my_hand_` over every row) said the identity bits do not improve
the critic's fit at 150k games. That test averaged over all rows, and on most of them the hand is irrelevant: no money to
pay for anything, or cards that will never be built. The owner's objection is precise: a hand is held *in order to* play
the cards later, e.g. build the card that raises steel income and then the card that wants steel. That interaction can only
show up where the hand is playable.

FALSIFIER, written before the run. If the hand identity carries information about future material, then on the rows where
at least one hand card is affordable (`hand:affordable_cards/10 >= 0.1`) the critic WITH the identity bits must have a
lower mean squared error on the held-out games than the critic WITHOUT them, by more than the run-to-run band of the same
recipe (about +/- 0.001 in loss, +/- 0.001 in AUC, docs/RESULTS.md). The comparison is paired: the same rows, the same
seed, the same validation split; the error bar is a game-level bootstrap of the per-row squared-error difference, because
rows of one game are not independent.

If the difference stays inside that band, the honest conclusion is: the identity of the hand cards is not converted into
value accuracy even where the hand is playable - not that the hand is unimportant for the *policy*, which keeps the bits.

    py -3 ml/hand_ab.py --pack C:/ares/pack/scale1090 --names D:/ares/tmp/names_v10.json \
        --out D:/ares/tmp/hand_ab.json
"""
import argparse
import json
import os
import sys
import time

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import train_value  # noqa: E402


class Without:
    """The pool's observation, with a set of columns zeroed on every read (the A/B of one block of the representation)."""

    def __init__(self, array, columns):
        self.array = array
        self.columns = np.asarray(sorted(columns), dtype=np.int64)
        self.shape = array.shape
        self.dtype = np.dtype(np.float32)

    def __len__(self):
        return self.shape[0]

    def __getitem__(self, item):
        out = np.array(self.array[item], dtype=np.float32)
        if out.ndim == 1:
            return out
        out[:, self.columns] = 0.0
        return out


def train_arm(obs, pool, hidden, epochs, seed, device, train_idx, val_idx, stats_idx=None):
    started = time.time()
    target = np.clip(pool["margin"] / 20.0, -3.0, 3.0).astype(np.float32)
    fitted = train_value.fit_value(obs, pool["outcome"], hidden=hidden, epochs=epochs, lr=1e-3,
                                   weight_decay=1e-5, batch=4096, seed=seed, device=device,
                                   train_idx=train_idx, val_idx=val_idx,
                                   label_mean=float(pool["outcome"][train_idx].mean()),
                                   target=target, target_kind="margin", verbose=True, stats_idx=stats_idx)
    predicted = train_value.predict(fitted["params"], obs[val_idx], squash=False)
    return {"params": fitted["params"], "history": fitted["history"], "predicted": predicted,
            "truth": target[val_idx], "seconds": round(time.time() - started, 1)}


def column(obs, index, block=1 << 18):
    """One feature of the whole pool, read in ascending blocks (sequential for a lazy pool)."""
    out = np.empty(len(obs), dtype=np.float32)
    for start in range(0, len(obs), block):
        rows = np.arange(start, min(start + block, len(obs)))
        out[rows] = np.asarray(obs[rows][:, index], dtype=np.float32)
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", required=True)
    parser.add_argument("--prefix", default="cards:in_my_hand_")
    parser.add_argument("--min-affordable", type=float, default=1.0,
                        help="keep rows where hand:affordable_cards/10 is at least this (1 = one playable card)")
    parser.add_argument("--rows-per-game", type=int, default=16)
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--hidden", default="512,256")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--out", default="")
    parser.add_argument("--stats", choices=("head", "random"), default="random",
                        help="how the standardisation sample is drawn: `head` is the first 200k rows of the pool (the "
                             "opening of the first games, where the hand block is nearly empty), `random` is a uniform "
                             "sample of the training rows")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    drop = [i for i, name in enumerate(names) if name.startswith(args.prefix)]
    if not drop:
        raise SystemExit(f"no feature starts with {args.prefix}")
    affordable_at = names.index("hand:affordable_cards/10")
    cards_at = names.index("hand:cards/10")

    rng = np.random.default_rng(args.seed)
    pool = train_value.load_packed(args.pack, args.rows_per_game, rng, 0.1)
    megabytes = len(pool["obs"]) * pool["obsSize"] * 2 / 2 ** 20
    if megabytes <= 8 * 1024:
        pool["obs"] = pool["obs"].to_ram()
    obs = pool["obs"]
    affordable = column(obs, affordable_at)
    cards = column(obs, cards_at)

    keep = (affordable >= args.min_affordable / 10.0) & (cards >= 0.2)  # at least two cards in hand
    train_idx = np.flatnonzero(keep & ~pool["validation"])
    val_idx = np.flatnonzero(keep & pool["validation"])
    print(json.dumps({"poolRows": int(keep.sum()), "of": int(len(keep)), "train": int(len(train_idx)),
                      "val": int(len(val_idx)),
                      "handCardsMean": round(float(cards[keep].mean() * 10), 2),
                      "affordableMean": round(float(affordable[keep].mean() * 10), 2)}, indent=1), flush=True)

    hidden = [int(x) for x in args.hidden.split(",")]
    import torch
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    stats_idx = None
    if args.stats == "random":
        # The identity bits are zero at the start of a game and dense later, and the pool is in (game, seat, step) order:
        # statistics taken from its head would enter the first layer unnormalised. A uniform sample of the training rows is
        # the honest normalisation, and it is the same sample for both arms.
        stats_idx = np.random.default_rng(args.seed + 11).choice(train_idx, size=min(200_000, len(train_idx)),
                                                                 replace=False)
    full = train_arm(obs, pool, hidden, args.epochs, args.seed, device, train_idx, val_idx, stats_idx)
    nohand = train_arm(Without(obs, drop), pool, hidden, args.epochs, args.seed, device, train_idx, val_idx, stats_idx)

    # paired, game-level bootstrap: rows of the same game are not independent
    games = np.asarray(pool["game"][val_idx])
    e_full = (full["predicted"] - full["truth"]) ** 2
    e_nohand = (nohand["predicted"] - nohand["truth"]) ** 2
    difference = e_full - e_nohand
    unique, inverse = np.unique(games, return_inverse=True)
    sums = np.bincount(inverse, weights=difference)
    counts = np.bincount(inverse)
    rng_b = np.random.default_rng(args.seed + 7)
    draws = np.sort(np.array([float(sums[p].sum() / counts[p].sum())
                              for p in (rng_b.integers(0, len(unique), len(unique)) for _ in range(2000))]))
    low, high = float(draws[int(0.025 * len(draws))]), float(draws[int(0.975 * len(draws))])
    result = {
        "rows": {"pool": int(keep.sum()), "train": int(len(train_idx)), "val": int(len(val_idx)),
                 "games": int(len(unique))},
        "full": {"val_loss": full["history"][-1]["val_loss"],
                 "best_loss": min(h["val_loss"] for h in full["history"]),
                 "auc": full["history"][-1]["auc"], "best_auc": max(h["auc"] for h in full["history"]),
                 "seconds": full["seconds"], "history": full["history"]},
        "nohand": {"val_loss": nohand["history"][-1]["val_loss"],
                   "best_loss": min(h["val_loss"] for h in nohand["history"]),
                   "auc": nohand["history"][-1]["auc"], "best_auc": max(h["auc"] for h in nohand["history"]),
                   "seconds": nohand["seconds"], "history": nohand["history"]},
        "difference_mse_full_minus_nohand": float(difference.mean()),
        "bootstrap_95": [low, high],
        "verdict": ("the hand identity bits help" if high < 0 else
                    ("the hand identity bits hurt" if low > 0 else
                     "no difference beyond the game-level noise")),
    }
    print(json.dumps(result, indent=1), flush=True)
    if args.out:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        json.dump(result, open(args.out, "w", encoding="utf-8"), indent=1)


if __name__ == "__main__":
    main()
