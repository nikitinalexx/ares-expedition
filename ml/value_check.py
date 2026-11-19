"""How good is our VALUE, really - and is it bound to our own plan?

`V(s)` is trained as "who wins if BOTH sides play like our policy", which makes it policy-conditional: a position that is
winning in general but not convertible by our player reads as winning, and the reverse. The owner's question (19/11): run our V
over finished games of AGENT vs NETWORK and see whether it mis-scores the other side - because it only ever saw phase-3 games.

What this prints, from a packed corpus of finished games (any matchup):

  * AUC of V against the actual sign of the margin, PER SEAT;
  * the disagreement map: positions V calls clearly winning (+0.8 logit) that were LOST, and clearly losing (-0.8) that were WON
    - the first is "our policy cannot convert", the second is "our V does not understand what happened";
  * mean V by round, so a systematic drift is visible.

    py -3 ml/value_check.py --pack C:/ares/pack/x --models DIR1,DIR2 [--type PICK_PHASE] [--limit 200000]
"""
import argparse
import json
import os
import sys

import numpy as np
import torch

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402
from train_policy_ce import load_value_params, value_of  # noqa: E402


def auc(scores, labels):
    """Rank-based AUC: P(score of a positive > score of a negative). labels are 1/0."""
    order = np.argsort(scores)
    ranks = np.empty(len(scores), dtype=np.float64)
    ranks[order] = np.arange(1, len(scores) + 1)
    positives = labels == 1
    n_pos = int(positives.sum())
    n_neg = int((~positives).sum())
    if n_pos == 0 or n_neg == 0:
        return float("nan")
    return float((ranks[positives].sum() - n_pos * (n_pos + 1) / 2) / (n_pos * n_neg))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--models", required=True, help="comma-separated value model directories (ensemble)")
    parser.add_argument("--type", default="PICK_PHASE")
    parser.add_argument("--limit", type=int, default=200_000)
    args = parser.parse_args()

    params = [load_value_params(part.strip()) for part in args.models.split(",") if part.strip()]
    if not params:
        raise SystemExit("no model")
    manifest = ares_data.open_pack(args.pack)
    if args.type not in manifest["types"]:
        raise SystemExit(f"{args.pack}: no {args.type}")
    data = ares_data.open_packed_type(args.pack, args.type, manifest)
    obs = data["obs"]
    margin = np.asarray(data["margin"], dtype=np.float32)
    seat = np.asarray(data["seat"], dtype=np.int32)
    round_no = np.asarray(data["round"], dtype=np.int32)
    step = max(1, len(obs) // args.limit)
    index = np.arange(0, len(obs), step)

    logits = np.zeros(len(index), dtype=np.float32)
    for start in range(0, len(index), 2048):
        block = index[start:start + 2048]
        batch = torch.as_tensor(np.stack([np.asarray(obs[i], dtype=np.float32) for i in block]))
        total = torch.zeros(len(block))
        for value in params:
            total += value_of(batch, value, raw=True)
        logits[start:start + len(block)] = (total / len(params)).numpy()

    labels = (margin[index] > 0).astype(np.int32)
    print(f"{args.pack}: {len(index)} {args.type} rows ({len(obs)} available)")
    print(f"  AUC(V -> win) overall: {auc(logits, labels):.3f}")
    for seat_id in (0, 1):
        mask = seat[index] == seat_id
        if mask.sum() > 100:
            print(f"  seat {seat_id}: n={int(mask.sum()):6d}  AUC {auc(logits[mask], labels[mask]):.3f}  "
                  f"mean V {logits[mask].mean():+.2f}  mean margin {margin[index][mask].mean():+.2f}")
    print("  mean V by round:")
    for low, high in ((1, 5), (6, 10), (11, 15), (16, 20), (21, 99)):
        mask = (round_no[index] >= low) & (round_no[index] <= high)
        if mask.sum() > 50:
            print(f"    rounds {low:2d}-{high:2d}: n={int(mask.sum()):6d}  mean V {logits[mask].mean():+.2f}  "
                  f"win share {labels[mask].mean():.2f}")
    confident = int((np.abs(logits) > 0.8).sum())
    won_but_called_lost = int(((logits < -0.8) & (labels == 1)).sum())
    lost_but_called_won = int(((logits > 0.8) & (labels == 0)).sum())
    print(f"  disagreement of {confident} confident rows: V>+0.8 but lost {lost_but_called_won}, "
          f"V<-0.8 but won {won_but_called_lost}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
