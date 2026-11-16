"""Which blocks of the observation does the critic actually use? One paired ablation per block.

The owner's permission (2026-11-16): "change the critic as you see fit, based on this". The measured starting point is that
the 268 hand identity bits cost the critic 0.002 loss on playable hands (docs/RESULTS.md). The same question can be asked of
every sparse identity block - the opponent table (178), the new blue table (166), milestones (58), the context switches - and
the pack makes each arm a ~40 s experiment instead of a 40 min one.

Method (same as ml/hand_ab.py): one reference arm trained on every feature, then one arm per block with those columns
zeroed. Same rows, same seed, same split; the error bar is a game-level bootstrap of the per-row squared-error difference
(rows of one game are not independent). A block with a clearly positive delta costs the critic; a block with a negative delta
is information the critic cannot use without it.

MULTIPLICITY: with six arms, one 95% interval is a weak reason to act. The screen only nominates a block; adopting a change
needs a single-variable run (that block alone, at the corpus scale that will be deployed) and, for the player, a ranking-role
measurement at 10 000 games - a 0.002 difference in loss is not a win by itself.

    py -3 ml/critic_ablation.py --pack C:/ares/pack/boot1 --names D:/ares/tmp/names_now.json \
        --out D:/ares/tmp/ablation_boot1.json
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

# name prefix -> the block. A block is a set of columns that encode one kind of fact, so zeroing it is a clean question.
BLOCKS = {
    "hand identity": ("cards:in_my_hand_",),
    "opponent table": ("op_table_",),
    "blue table": ("cards:my_blue_", "cards:opponent_blue_"),
    "milestones and awards": ("milestone_", "award_"),
    "context switches": ("ctx:",),
    "tags": ("tags:",),
    "double payment maxima": ("cards:my_max_double_payment_", "cards:opponent_max_double_payment_"),
    "score": ("score:",),
}


class Without:
    """The pool's observation with a set of columns zeroed on every read."""

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


def column(obs, index, block=1 << 18):
    out = np.empty(len(obs), dtype=np.float32)
    for start in range(0, len(obs), block):
        rows = np.arange(start, min(start + block, len(obs)))
        out[rows] = np.asarray(obs[rows][:, index], dtype=np.float32)
    return out


def train_arm(obs, pool, target, hidden, epochs, seed, device, train_idx, val_idx, stats_idx):
    started = time.time()
    fitted = train_value.fit_value(obs, pool["outcome"], hidden=hidden, epochs=epochs, lr=1e-3,
                                   weight_decay=1e-5, batch=4096, seed=seed, device=device,
                                   train_idx=train_idx, val_idx=val_idx,
                                   label_mean=float(pool["outcome"][train_idx].mean()),
                                   target=target, target_kind="margin", verbose=False, stats_idx=stats_idx)
    predicted = train_value.predict(fitted["params"], obs[val_idx], squash=False)
    return {"history": fitted["history"], "predicted": predicted, "seconds": round(time.time() - started, 1)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", required=True)
    parser.add_argument("--out", default="")
    parser.add_argument("--rows-per-game", type=int, default=16)
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--hidden", default="512,256")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--blocks", default="", help="comma-separated block keys (default: all)")
    parser.add_argument("--playable-hand", action="store_true",
                        help="only rows where the hand is playable (>=2 cards, >=1 affordable)")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    chosen = [b for b in BLOCKS if not args.blocks or b in args.blocks.split(",")]
    blocks = {b: [i for i, n in enumerate(names) if n.startswith(BLOCKS[b])] for b in chosen}
    empty = [b for b, cols in blocks.items() if not cols]
    if empty:
        raise SystemExit(f"these blocks matched no column of {args.names}: {empty}")
    print(json.dumps({b: len(cols) for b, cols in blocks.items()}, indent=1), flush=True)

    rng = np.random.default_rng(args.seed)
    pool = train_value.load_packed(args.pack, args.rows_per_game, rng, 0.1)
    if len(pool["obs"]) * pool["obsSize"] * 2 <= 8 * 2 ** 30:
        pool["obs"] = pool["obs"].to_ram()
    obs = pool["obs"]
    keep = np.ones(len(obs), dtype=bool)
    if args.playable_hand:
        affordable = column(obs, names.index("hand:affordable_cards/10")) if "hand:affordable_cards/10" in names else None
        cards = column(obs, names.index("hand:cards/10"))
        keep = (cards >= 0.2) & (affordable >= 0.1 if affordable is not None else True)
    train_idx = np.flatnonzero(keep & ~pool["validation"])
    val_idx = np.flatnonzero(keep & pool["validation"])
    target = np.clip(pool["margin"] / 20.0, -3.0, 3.0).astype(np.float32)
    stats_idx = np.random.default_rng(args.seed + 11).choice(train_idx, size=min(200_000, len(train_idx)),
                                                             replace=False)
    print(json.dumps({"rows": int(keep.sum()), "train": int(len(train_idx)), "val": int(len(val_idx)),
                      "games": int(len(np.unique(np.asarray(pool["game"][val_idx]))))}), flush=True)

    import torch
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    hidden = [int(x) for x in args.hidden.split(",")]
    reference = train_arm(obs, pool, target, hidden, args.epochs, args.seed, device, train_idx, val_idx, stats_idx)
    truth = target[val_idx]
    base = (reference["predicted"] - truth) ** 2
    games = np.asarray(pool["game"][val_idx])
    unique, inverse = np.unique(games, return_inverse=True)

    rows = []
    for block, cols in blocks.items():
        arm = train_arm(Without(obs, cols), pool, target, hidden, args.epochs, args.seed, device, train_idx, val_idx,
                        stats_idx)
        difference = (arm["predicted"] - truth) ** 2 - base
        sums = np.bincount(inverse, weights=difference)
        counts = np.bincount(inverse)
        rng_b = np.random.default_rng(args.seed + 7)
        draws = np.sort(np.array([float(sums[p].sum() / counts[p].sum())
                                  for p in (rng_b.integers(0, len(unique), len(unique)) for _ in range(2000))]))
        low, high = float(draws[int(0.025 * len(draws))]), float(draws[int(0.975 * len(draws))])
        rows.append({"block": block, "columns": len(cols),
                     "delta_loss": float(difference.mean()), "ci95": [low, high],
                     "loss": min(h["val_loss"] for h in arm["history"]),
                     "auc": max(h["auc"] for h in arm["history"]), "seconds": arm["seconds"],
                     "verdict": ("costs the critic" if low > 0 else
                                 ("the critic cannot use it" if high < 0 else "no difference"))})
        print(json.dumps(rows[-1], indent=1), flush=True)

    result = {"pack": args.pack, "reference": {"loss": min(h["val_loss"] for h in reference["history"]),
                                               "auc": max(h["auc"] for h in reference["history"]),
                                               "seconds": reference["seconds"]},
              "rows": {"train": int(len(train_idx)), "val": int(len(val_idx)), "games": int(len(unique))},
              "epochs": args.epochs, "blocks": rows}
    print(json.dumps(result, indent=1), flush=True)
    if args.out:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        json.dump(result, open(args.out, "w", encoding="utf-8"), indent=1)


if __name__ == "__main__":
    main()
