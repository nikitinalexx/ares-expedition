"""How much does an 11-game AUC say? The owner's question (23/11).

`seen` in the trainers was `tr[-4096:]`, and rows are written game by game, so that is the LAST ~11 games of the corpus, not
a random sample. Its AUC has an effective sample of 11 outcomes, so it swings wildly: 0.99 there is not evidence that the
model "learned the game", and it is not evidence that it did not.

This trains one value model on a pack (the critic recipe: all rows, one game-level split, WD) and then reports:
  * the held-out val AUC and the train AUC over ALL rows;
  * the distribution of AUC over many RANDOM groups of 11 games (train and val), the same size as the tail sample;
  * the same for 200-game groups, the proposed `seen` size;
  * the actual tail group (the 11 largest train game ids) and the last 4096 train rows.

    py -3 ml/seen_sample_probe.py --pack D:/ares/pack/1123_champ20k --groups 300
"""
import argparse
import json
import os
import sys

import numpy as np
import torch
import torch.nn as nn

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from train_value import ValueNet, auc, load_packed, standardize  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--out", default="")
    parser.add_argument("--epochs", type=float, default=1.0)
    parser.add_argument("--steps", type=int, default=0, help="cap the optimizer steps of the single epoch (0 = all)")
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-3)
    parser.add_argument("--hidden", default="256,128")
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--groups", type=int, default=300, help="random groups drawn per size")
    parser.add_argument("--small", type=int, default=11, help="the group size that mirrors the old tail sample")
    parser.add_argument("--large", type=int, default=200, help="the proposed random game sample")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--device", default="cpu")
    args = parser.parse_args()

    rng = np.random.default_rng(args.seed)
    torch.manual_seed(args.seed)
    device = torch.device(args.device)

    print(f"reading {args.pack}", flush=True)
    pool = load_packed(args.pack, 0, rng, args.val_fraction)
    obs = pool["obs"]
    if hasattr(obs, "to_ram"):
        print(f"  reading {len(obs)} rows into RAM", flush=True)
        obs = obs.to_ram()
    outcome = pool["outcome"].astype(np.float32)
    game = pool["game"].astype(np.int64)
    split = pool["validation"].astype(bool)
    print(f"  {len(obs)} rows, {len(np.unique(game))} games, obs {obs.shape[1]}, "
          f"val {int(split.sum())} rows / {len(np.unique(game[split]))} games", flush=True)

    if not np.all(np.diff(game) >= 0):
        order = np.argsort(game, kind="stable")
        obs, outcome, game, split = obs[order], outcome[order], game[order], split[order]

    # per-game contiguous ranges (the pool is sorted by game, seat, step)
    starts = np.r_[0, np.flatnonzero(np.diff(game)) + 1]
    ends = np.r_[starts[1:], len(game)]
    unique_games = game[starts]
    ranges = {int(g): (int(s), int(e)) for g, s, e in zip(unique_games, starts, ends)}
    train_games = np.asarray([g for g in unique_games if not split[ranges[int(g)][0]]], dtype=np.int64)
    val_games = np.asarray([g for g in unique_games if split[ranges[int(g)][0]]], dtype=np.int64)
    train_idx = np.flatnonzero(~split)
    print(f"  train games {len(train_games)}, val games {len(val_games)}", flush=True)

    stats = rng.choice(train_idx, size=min(200_000, len(train_idx)), replace=False)
    mean, std = standardize(obs, stats)
    model = ValueNet(obs.shape[1], [int(x) for x in args.hidden.split(",")], mean, std).to(device)

    def tensor(idx):
        return torch.from_numpy(np.asarray(obs[idx], dtype=np.float32)).to(device)

    def logits(idx, block=1 << 16):
        model.eval()
        parts = []
        with torch.no_grad():
            for start in range(0, len(idx), block):
                parts.append(model(tensor(idx[start:start + block])).cpu().numpy())
        model.train()
        return np.concatenate(parts) if parts else np.zeros(0, dtype=np.float32)

    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    loss_fn = nn.BCEWithLogitsLoss()
    steps = args.steps or int(len(train_idx) / args.batch * args.epochs)
    print(f"training {steps} steps of batch {args.batch}, lr {args.lr}, wd {args.weight_decay}", flush=True)
    model.train()
    best = float("inf")
    for step in range(1, steps + 1):
        idx = rng.choice(train_idx, size=args.batch, replace=False)
        yb = torch.from_numpy(outcome[idx]).to(device)
        loss = loss_fn(model(tensor(idx)), yb)
        optimizer.zero_grad()
        loss.backward()
        nn.utils.clip_grad_norm_(model.parameters(), 5.0)
        optimizer.step()
        if step % max(1, steps // 10) == 0:
            print(f"  step {step}: train {float(loss):.4f}", flush=True)

    scores = logits(np.arange(len(obs)))
    report = {
        "rows": int(len(obs)),
        "games": int(len(unique_games)),
        "train_rows_auc": auc(scores[train_idx], outcome[train_idx]),
        "val_auc": auc(scores[~split], outcome[~split]),
        "val_rows": int((~split).sum()),
        "val_games": int(len(val_games)),
    }
    print(f"train AUC {report['train_rows_auc']:.4f}  val AUC {report['val_auc']:.4f}", flush=True)

    def group_auc(ids, draw):
        out = []
        for _ in range(draw):
            pick = rng.choice(ids, size=min(size, len(ids)), replace=False)
            rows = np.concatenate([np.arange(*ranges[int(g)]) for g in pick])
            out.append(auc(scores[rows], outcome[rows]))
        return np.asarray(out, dtype=np.float64)

    def describe(values):
        if not len(values) or np.all(np.isnan(values)):
            return None
        values = values[~np.isnan(values)]
        return {"n": int(len(values)), "mean": float(values.mean()), "sd": float(values.std()),
                "p5": float(np.percentile(values, 5)), "p25": float(np.percentile(values, 25)),
                "p50": float(np.percentile(values, 50)), "p75": float(np.percentile(values, 75)),
                "p95": float(np.percentile(values, 95)), "max": float(values.max())}

    distributions = {}
    for label, ids, size, draw in (
            (f"train_per_game_{args.small}", train_games, args.small, args.groups),
            (f"val_per_game_{args.small}", val_games, args.small, args.groups),
            (f"train_per_game_{args.large}", train_games, args.large, max(20, args.groups // 10)),
            (f"val_per_game_{args.large}", val_games, args.large, max(20, args.groups // 10)),
            ("val_per_game_1000", val_games, 1000, max(10, args.groups // 30))):
        distributions[label] = describe(group_auc(ids, draw))

    tail_games = np.sort(train_games)[-args.small:]
    tail_rows = np.concatenate([np.arange(*ranges[int(g)]) for g in tail_games])
    tail_11 = {"games": tail_games.tolist(), "rows": int(len(tail_rows)),
               "auc": auc(scores[tail_rows], outcome[tail_rows])}
    last_rows = train_idx[-4096:]
    report["tail_games"] = tail_11
    report["last_4096_train_rows"] = {"rows": int(len(last_rows)),
                                      "auc": auc(scores[last_rows], outcome[last_rows])}
    report["distributions"] = distributions

    for label, stats_ in distributions.items():
        if stats_:
            print(f"  {label}: mean {stats_['mean']:.4f} sd {stats_['sd']:.4f} "
                  f"p5 {stats_['p5']:.4f} p50 {stats_['p50']:.4f} p95 {stats_['p95']:.4f} "
                  f"max {stats_['max']:.4f} (n {stats_['n']})", flush=True)
    print(f"  tail {args.small} train games: AUC {tail_11['auc']:.4f} over {tail_11['rows']} rows", flush=True)
    print(f"  last 4096 train rows: AUC {report['last_4096_train_rows']['auc']:.4f}", flush=True)

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=1)
        print(f"wrote {args.out}", flush=True)


if __name__ == "__main__":
    main()
