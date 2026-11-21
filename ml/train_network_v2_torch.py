"""GPU training for the NETWORK_V2 value, same architecture and export as `ml/train_network_v2.py`.

Why a second trainer: the numpy one is honest but slow and numerically fragile at 1M+ rows (train loss 30+, validation worsening). Torch on the
available CUDA device trains the same two-tower net in seconds per epoch, with gradient clipping and a game-level split, and exports the same
`coefficients.bin` that `NNService`/`PureJavaValueNetwork` read.

    py -3 ml/train_network_v2_torch.py --csv D:/ares/data/netd_rand20k/netdata.csv --out D:/ares/runs/network3/net3.zip --epochs 6
"""
import argparse
import io
import os
import struct
import sys
import zipfile

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from train_network_v2 import HAND, H1, H2, H3, TABLE, read_csv  # noqa: E402


class Towers(nn.Module):
    def __init__(self):
        super().__init__()
        self.table = nn.Linear(TABLE, H1)
        self.hand = nn.Linear(HAND, H2)
        self.merge = nn.Linear(H1 + H2, H3)
        self.out = nn.Linear(H3, 1)

    def forward(self, table, hand):
        a = F.leaky_relu(self.table(table), 0.01)
        b = F.leaky_relu(self.hand(hand), 0.01)
        c = F.leaky_relu(self.merge(torch.cat([a, b], dim=1)), 0.01)
        return self.out(c).squeeze(1)


def _utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def export(model, path):
    w1 = model.table.weight.detach().cpu().numpy().astype(np.float32)
    b1 = model.table.bias.detach().cpu().numpy().astype(np.float32)
    w2 = model.hand.weight.detach().cpu().numpy().astype(np.float32)
    b2 = model.hand.bias.detach().cpu().numpy().astype(np.float32)
    w3 = model.merge.weight.detach().cpu().numpy().astype(np.float32)
    b3 = model.merge.bias.detach().cpu().numpy().astype(np.float32)
    w4 = model.out.weight.detach().cpu().numpy().astype(np.float32)
    b4 = model.out.bias.detach().cpu().numpy().astype(np.float32)
    # The Java loader expects index `out * in + in` (it transposes f-order into input-major). The numpy net stores w1 as (in, out) and needs
    # `.T`; torch stores (out, in) already, so it must NOT be transposed here (the round-trip test caught this).
    params = np.concatenate([w1.reshape(-1), b1, w2.reshape(-1), b2,
                             w3.reshape(-1), b3, w4.reshape(-1), b4]).astype(">f4")
    expected = TABLE * H1 + H1 + HAND * H2 + H2 + (H1 + H2) * H3 + H3 + H3 + 1
    if params.size != expected:
        raise ValueError(f"{params.size} params, expected {expected}")
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as z:
        with z.open("coefficients.bin", "w") as raw:
            raw.write(_utf("HEAP"))
            raw.write(struct.pack(">q", 1))
            raw.write(_utf("INT"))
            raw.write(struct.pack(">i", 0))
            raw.write(_utf("HEAP"))
            raw.write(struct.pack(">q", params.size))
            raw.write(_utf("FLOAT"))
            raw.write(params.tobytes())
    print(f"wrote {path} ({params.size} params)", flush=True)


def read_params(path):
    """Read back a `coefficients.bin` as one float array (the loader's view), for round-trip tests."""
    with zipfile.ZipFile(path) as z:
        raw = z.read("coefficients.bin")
    b = io.BytesIO(raw)

    def utf(f):
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode()

    utf(b)
    shape_len = struct.unpack(">q", b.read(8))[0]
    utf(b)
    b.read(shape_len * 4)
    utf(b)
    length = struct.unpack(">q", b.read(8))[0]
    utf(b)
    return np.frombuffer(b.read(length * 4), dtype=">f4").astype(np.float32)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=6)
    parser.add_argument("--batch", type=int, default=8192)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--holdout", type=float, default=0.1)
    args = parser.parse_args()

    torch.manual_seed(args.seed)
    cache = args.csv + ".npz"
    if os.path.exists(cache):
        z = np.load(cache)
        label, x, ids = z["label"], z["x"], z["ids"]
        print(f"loaded {cache}", flush=True)
    else:
        label, x, ids = read_csv(args.csv, dtype=np.float16)
        np.savez(cache, label=label, x=x, ids=ids)
        print(f"cached {cache}", flush=True)
    label = label.astype(np.float32)
    print(f"rows {len(label)}, games {len(np.unique(ids)) if (ids >= 0).all() else 'n/a'}, label mean {label.mean():.4f}", flush=True)
    if (ids >= 0).all():
        games = np.unique(ids)
        rng = np.random.default_rng(args.seed)
        rng.shuffle(games)
        cut = int(len(games) * (1.0 - args.holdout))
        train_mask = np.isin(ids, games[:cut])
    else:
        rng = np.random.default_rng(args.seed)
        perm = rng.permutation(len(label))
        train_mask = np.zeros(len(label), dtype=bool)
        train_mask[perm[:int(len(label) * (1.0 - args.holdout))]] = True

    device = "cuda" if torch.cuda.is_available() else "cpu"
    # x stays float16 on the CPU; each batch is moved to the device and cast, so 3M rows of 898 values fit in memory.
    features = torch.from_numpy(np.ascontiguousarray(x))
    y = torch.from_numpy(label)
    tr = torch.from_numpy(np.flatnonzero(train_mask))
    va = torch.from_numpy(np.flatnonzero(~train_mask))

    model = Towers().to(device)
    opt = torch.optim.Adam(model.parameters(), lr=args.lr)
    loss_fn = nn.BCEWithLogitsLoss()

    for epoch in range(1, args.epochs + 1):
        model.train()
        order = tr[torch.randperm(len(tr))]
        total = 0.0
        for start in range(0, len(order), args.batch):
            take = order[start:start + args.batch]
            batch = features[take].to(device).float()
            opt.zero_grad()
            logit = model(batch[:, :TABLE], batch[:, TABLE:])
            loss = loss_fn(logit, y[take].to(device))
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            opt.step()
            total += float(loss.item()) * len(take)
        model.eval()
        with torch.no_grad():
            parts = []
            for start in range(0, len(va), args.batch):
                take = va[start:start + args.batch]
                batch = features[take].to(device).float()
                parts.append(model(batch[:, :TABLE], batch[:, TABLE:]))
            logit = torch.cat(parts)
            vy = y[va].to(device)
            val = float(loss_fn(logit, vy))
            order_ = torch.argsort(logit)
            ranks = torch.empty_like(order_)
            ranks[order_] = torch.arange(len(order_), device=device)
            pos = vy > 0.5
            auc = float((ranks[pos].float().mean() - (pos.sum() - 1) / 2) / max(int((~pos).sum()), 1))
        print(f"epoch {epoch}: train {total / len(tr):.4f}  val {val:.4f}  auc {auc:.4f}", flush=True)

    export(model, args.out)


if __name__ == "__main__":
    main()
