"""Evaluate a NETWORK_V2 value zip on a `netdata.bin`: loss, AUC, label mean - on games generated AFTER training.

Why: the trainer's validation split is by game id INSIDE the same corpus. That answers "did it memorise these games", not
"does the value score new games". The night script generates a few hundred fresh games of the base and runs this, so the
offline number is comparable to the play measurement instead of to the training distribution.

    py -3 ml/value_eval_bin.py --csv WORK/data/fresh/netdata.bin --model WORK/models/trained.zip
"""
import argparse
import json
import os
import sys

import numpy as np
import torch

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from train_network_v2 import HAND, TABLE, read_binary  # noqa: E402
from train_network_v2_torch import Towers, load_exported  # noqa: E402


def auc(scores, labels):
    """Rank AUC with ties averaged; rows labelled 0.5 (draws) are excluded from the ranking entirely."""
    mask = labels != 0.5
    scores = scores[mask]
    labels = labels[mask]
    order = np.argsort(scores, kind="mergesort")
    s = scores[order]
    rank_sorted = np.empty(len(s), dtype=np.float64)
    i = 0
    while i < len(s):
        j = i
        while j + 1 < len(s) and s[j + 1] == s[i]:
            j += 1
        rank_sorted[i:j + 1] = (i + 1 + j + 1) / 2.0
        i = j + 1
    lab = labels[order]
    pos = lab > 0.5
    neg = lab < 0.5
    npos, nneg = int(pos.sum()), int(neg.sum())
    if npos == 0 or nneg == 0:
        return None
    return float((rank_sorted[pos].sum() - npos * (npos + 1) / 2.0) / (npos * nneg))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True, help="netdata.bin of fresh games")
    parser.add_argument("--model", required=True, help="trained value zip")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--batch", type=int, default=4096)
    args = parser.parse_args()

    labels, features, ids = read_binary(args.csv, limit=args.limit)
    labels = np.asarray(labels, dtype=np.float32)
    n = len(labels)
    model = Towers()
    model.load_state_dict(load_exported(args.model))
    model.eval()

    logits = np.empty(n, dtype=np.float32)
    with torch.no_grad():
        for start in range(0, n, args.batch):
            stop = min(n, start + args.batch)
            block = np.asarray(features[start:stop], dtype=np.float32)
            table = torch.from_numpy(block[:, :TABLE])
            hand = torch.from_numpy(block[:, TABLE:TABLE + HAND])
            logits[start:stop] = model(table, hand).numpy()

    mean = float(labels.mean())
    constant = float(-(mean * np.log(max(mean, 1e-9)) + (1 - mean) * np.log(max(1 - mean, 1e-9))))
    loss = float(np.mean(np.maximum(logits, 0) - logits * labels + np.log1p(np.exp(-np.abs(logits)))))
    info = {
        "rows": int(n),
        "games": int(np.max(ids)) + 1 if n else 0,
        "label_mean": round(mean, 4),
        "loss": round(loss, 4),
        "constant_loss": round(constant, 4),
        "auc": None if (a := auc(logits, labels)) is None else round(a, 4),
        "model": args.model,
    }
    print(json.dumps(info))


if __name__ == "__main__":
    main()
