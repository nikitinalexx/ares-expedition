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
from train_network_v2 import HAND, H1, H2, H3, MAGIC, TABLE, read_dataset  # noqa: E402


class Towers(nn.Module):
    def __init__(self, dropout=0.0):
        super().__init__()
        self.table = nn.Linear(TABLE, H1)
        self.hand = nn.Linear(HAND, H2)
        self.merge = nn.Linear(H1 + H2, H3)
        self.out = nn.Linear(H3, 1)
        # Train-time only: dropout has no parameters, so the exported coefficients are identical with or without it. It
        # exists because a dense corpus has ~415 states per game with ONE label, so the effective sample is the number of
        # games (10 000) while the rows are millions - the model can memorise games and the val loss explodes.
        self.dropout = nn.Dropout(dropout) if dropout else nn.Identity()

    def forward(self, table, hand):
        a = self.dropout(F.leaky_relu(self.table(table), 0.01))
        b = self.dropout(F.leaky_relu(self.hand(hand), 0.01))
        c = self.dropout(F.leaky_relu(self.merge(torch.cat([a, b], dim=1)), 0.01))
        return self.out(c).squeeze(1)


def _utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def load_exported(path):
    """Read a `coefficients.bin` back into a state_dict, for --warm-start: the owner's incremental recipe is "generate
    10 M, train, generate more, train again" (22/11), so the second chunk continues the first model instead of restarting."""
    import torch
    p = read_params(path)
    at = 0

    def take(n):
        nonlocal at
        block = p[at:at + n]
        at += n
        return block

    w1 = take(H1 * TABLE).reshape(H1, TABLE)
    b1 = take(H1)
    w2 = take(H2 * HAND).reshape(H2, HAND)
    b2 = take(H2)
    w3 = take(H3 * (H1 + H2)).reshape(H3, H1 + H2)
    b3 = take(H3)
    w4 = take(H3).reshape(1, H3)
    b4 = take(1)
    if at != p.size:
        raise ValueError(f"{path}: {p.size} parameters, expected {at}")
    return {"table.weight": torch.from_numpy(w1.copy()), "table.bias": torch.from_numpy(b1.copy()),
            "hand.weight": torch.from_numpy(w2.copy()), "hand.bias": torch.from_numpy(b2.copy()),
            "merge.weight": torch.from_numpy(w3.copy()), "merge.bias": torch.from_numpy(b3.copy()),
            "out.weight": torch.from_numpy(w4.copy()), "out.bias": torch.from_numpy(b4.copy())}


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
    """Read a `coefficients.bin` as one float array, exactly as PureJavaValueNetwork does.

    The ND4J file is a shape buffer (UTF name, long length, UTF type, length elements) followed by a float buffer. Our own
    export writes "HEAP"/"INT"/"HEAP"/"FLOAT", the owner's DL4J files write "MIXED_DATA_TYPES"/"LONG"/"MIXED_DATA_TYPES"/
    "FLOAT"; the Java loader skips the first buffer by its declared type and size, so this parses both. The old version
    assumed `length * 4` for the shape buffer, which is why the recovered iter2 model could be PLAYED but not used as a
    warm start.
    """
    with zipfile.ZipFile(path) as z:
        raw = z.read("coefficients.bin")
    b = io.BytesIO(raw)

    def utf(f):
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode("utf-8", "replace")

    def skip_buffer(f):
        utf(f)
        length = struct.unpack(">q", f.read(8))[0]
        kind = utf(f)
        elem = {"LONG": 8, "DOUBLE": 8, "INT": 4, "FLOAT": 4}.get(kind)
        if elem is None:
            raise ValueError(f"{path}: unsupported shape buffer type {kind}")
        f.seek(length * elem, io.SEEK_CUR)

    skip_buffer(b)
    utf(b)
    length = struct.unpack(">q", b.read(8))[0]
    kind = utf(b)
    if kind != "FLOAT":
        raise ValueError(f"{path}: expected FLOAT parameters, got {kind}")
    data = b.read(length * 4)
    if len(data) != length * 4:
        raise ValueError(f"{path}: {len(data)} parameter bytes, expected {length * 4} - a truncated write")
    return np.frombuffer(data, dtype=">f4").astype(np.float32)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=6)
    parser.add_argument("--batch", type=int, default=8192)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--holdout", type=float, default=0.1)
    parser.add_argument("--optimizer", choices=("adam", "sgd"), default="adam")
    parser.add_argument("--momentum", type=float, default=0.9)
    parser.add_argument("--val-steps", type=int, default=0,
                        help="validate every N optimizer steps inside an epoch (0 = only at the epoch end). A dense corpus "
                             "has ~415 rows per game with one label, so it memorises games long before an epoch of 65 000 "
                             "steps ends; without this the best checkpoint is never seen")
    parser.add_argument("--dropout", type=float, default=0.0,
                        help="dropout on the hidden activations, train-time only (no parameters, the export is unchanged)")
    parser.add_argument("--val-rows", type=int, default=50000,
                        help="cap the validation set for checkpoint selection (0 = all): a dense corpus has ~415k validation "
                             "rows and every mid-epoch check would cost a minute; 50k ranks checkpoints the same way")
    parser.add_argument("--patience", type=int, default=0,
                        help="stop after this many validations without improvement (0 = never): the dense corpus starts "
                             "memorising after a few thousand steps, so the rest of an epoch is wasted compute")
    parser.add_argument("--games-limit", type=int, default=0,
                        help="keep only the first N game ids (ascending), a NESTED subset for volume comparisons")
    parser.add_argument("--rows-per-game", type=int, default=0,
                        help="cap rows per GAME before the split (0 = all). The corpus has hundreds of correlated rows per "
                             "game and ONE outcome, so all rows memorise the games; the agent critic's cap 16 measured +5 play")
    parser.add_argument("--weight-decay", type=float, default=0.0,
                        help="Adam/SGD L2 weight decay (the NETWORK_V2 trainer had none; the owner's CONFIG_A used 5e-5)")
    parser.add_argument("--save-checkpoints", type=int, default=0,
                        help="save this many evenly spaced checkpoint zips, plus the last state, for the external play eval")
    parser.add_argument("--red-rise", type=float, default=0.03,
                        help="RED FLAG when val >= best + this AND there are --red-rises consecutive rises (a plain wobble is ignored)")
    parser.add_argument("--red-rises", type=int, default=2,
                        help="consecutive val rises required for the RED FLAG")
    parser.add_argument("--in-ram", action="store_true",
                        help="read the binary corpus into RAM in one sequential pass instead of mapping it (a 20k-game "
                             "corpus is 14 GB; every batch is then memory-speed)")
    parser.add_argument("--warm-start", default="",
                        help="start from an exported coefficients.bin zip: the incremental recipe (train on one chunk, then "
                             "continue on the next) instead of restarting on the union")
    args = parser.parse_args()

    torch.manual_seed(args.seed)
    cache = args.csv + ".npz"
    with open(args.csv, "rb") as probe:
        binary_corpus = probe.read(len(MAGIC)) == MAGIC
    if binary_corpus:
        # The binary corpus is already a compact float16 array of the record layout: reading it is one sequential pass, so
        # an npz copy would only double the disk (15 GB at 8M rows) and the start time.
        label, x, ids = read_dataset(args.csv, dtype=np.float16, in_ram=args.in_ram)
        print(f"read {args.csv} (binary, no cache)", flush=True)
    elif os.path.exists(cache):
        z = np.load(cache)
        label, x, ids = z["label"], z["x"], z["ids"]
        print(f"loaded {cache}", flush=True)
    else:
        label, x, ids = read_dataset(args.csv, dtype=np.float16, in_ram=args.in_ram)
        np.savez(cache, label=label, x=x, ids=ids)
        print(f"cached {cache}", flush=True)
    label = label.astype(np.float32)
    if args.games_limit and (ids >= 0).all():
        # A NESTED subset (the first N ids in ascending order): the 1 000-game run is inside the 20 000-game run, so the
        # only variable between two volumes is the volume, not a different sample.
        wanted = np.unique(ids)[:args.games_limit]
        keep = np.isin(ids, wanted)
        label, x, ids = label[keep], x[keep], ids[keep]
        print(f"games-limit {args.games_limit}: {len(label)} rows kept", flush=True)
    if args.rows_per_game and (ids >= 0).all():
        # ONE outcome labels every row of a game, so hundreds of rows per game are copies for the loss: the model memorises
        # the games and the held-out games stop improving (the 50k run's val rose from 0.33 at step 6k to 0.78 at the end).
        # The agent critic's --rows-per-game 16 measured 40.51% vs 35.30% for all rows; the NETWORK_V2 corpus has ~375/game.
        order = np.argsort(ids, kind="mergesort")
        sorted_ids = ids[order]
        starts = np.r_[0, np.flatnonzero(np.diff(sorted_ids)) + 1]
        ends = np.r_[starts[1:], len(ids)]
        keep = np.zeros(len(ids), dtype=bool)
        cap_rng = np.random.default_rng(args.seed + 13)
        for start, end in zip(starts, ends):
            span = int(end - start)
            if span <= args.rows_per_game:
                keep[order[start:end]] = True
            else:
                pick = cap_rng.choice(span, args.rows_per_game, replace=False)
                keep[order[start:end][pick]] = True
        label, x, ids = label[keep], x[keep], ids[keep]
        print(f"rows per game cap {args.rows_per_game}: {len(label)} rows kept", flush=True)
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
    features = x if binary_corpus else torch.from_numpy(np.ascontiguousarray(x))
    y = torch.from_numpy(label)
    tr = torch.from_numpy(np.flatnonzero(train_mask))
    va = torch.from_numpy(np.flatnonzero(~train_mask))
    if args.val_rows and len(va) > args.val_rows:
        generator = torch.Generator().manual_seed(args.seed + 7)
        va = va[torch.randperm(len(va), generator=generator)[:args.val_rows]]

    model = Towers(args.dropout).to(device)
    if args.warm_start:
        state = load_exported(args.warm_start)
        missing = model.load_state_dict(state, strict=True)
        print(f"warm start from {args.warm_start}", flush=True)
    if args.optimizer == "sgd":
        opt = torch.optim.SGD(model.parameters(), lr=args.lr, momentum=args.momentum, weight_decay=args.weight_decay)
    else:
        opt = torch.optim.Adam(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    loss_fn = nn.BCEWithLogitsLoss()
    best_val = float("inf")
    best_state = None

    def evaluate():
        model.eval()
        with torch.no_grad():
            parts = []
            for start in range(0, len(va), args.batch):
                take = va[start:start + args.batch]
                batch = (features[take].to(device).float() if isinstance(features, torch.Tensor)
                     else torch.from_numpy(np.ascontiguousarray(features[take.numpy()])).to(device).float())
                parts.append(model(batch[:, :TABLE], batch[:, TABLE:]))
            logit = torch.cat(parts)
            vy = y[va].to(device)
            value = float(loss_fn(logit, vy))
            order_ = torch.argsort(logit)
            ranks = torch.empty_like(order_)
            ranks[order_] = torch.arange(len(order_), device=device)
            pos = vy > 0.5
            area = float((ranks[pos].float().mean() - (pos.sum() - 1) / 2) / max(int((~pos).sum()), 1))
        return value, area

    step = 0
    since_best = 0
    stopped = False
    last_val = None
    rises = 0
    red = False
    steps_per_epoch = (len(tr) + args.batch - 1) // args.batch
    save_every = 0
    if args.save_checkpoints > 0:
        save_every = max(1, (args.epochs * steps_per_epoch) // args.save_checkpoints)
    ckpt_base = os.path.join(os.path.dirname(args.out), os.path.basename(args.out).replace(".zip", ""))

    def register_val(value, where):
        nonlocal best_val, best_state, since_best, last_val, rises, red
        if value < best_val:
            best_val = value
            best_state = {k: v.detach().clone() for k, v in model.state_dict().items()}
            print(f"  best so far (val {value:.4f} at {where})", flush=True)
            since_best = 0
            rises = 0
        else:
            since_best += 1
            rises = rises + 1 if (last_val is not None and value > last_val) else 0
        last_val = value
        if value >= best_val + args.red_rise and rises >= args.red_rises:
            print(f"RED FLAG: overfitting (val {value:.4f} vs best {best_val:.4f}, +{value - best_val:.4f}, "
                  f"{rises} consecutive rises)", flush=True)
            red = True

    for epoch in range(1, args.epochs + 1):
        model.train()
        order = tr[torch.randperm(len(tr))]
        total = 0.0
        since, since_n = 0.0, 0
        for start in range(0, len(order), args.batch):
            take = order[start:start + args.batch]
            batch = (features[take].to(device).float() if isinstance(features, torch.Tensor)
                     else torch.from_numpy(np.ascontiguousarray(features[take.numpy()])).to(device).float())
            opt.zero_grad()
            logit = model(batch[:, :TABLE], batch[:, TABLE:])
            loss = loss_fn(logit, y[take].to(device))
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            opt.step()
            total += float(loss.item()) * len(take)
            since += float(loss.item()) * len(take)
            since_n += len(take)
            step += 1
            if save_every and step % save_every == 0:
                ckpt = f"{ckpt_base}_ckpt_{step}.zip"
                export(model, ckpt)
                print(f"  checkpoint {ckpt}", flush=True)
            if args.val_steps and step % args.val_steps == 0:
                # A dense corpus has ~415 rows per game with ONE label: the effective sample is the number of games, so
                # the model starts memorising them long before an epoch ends (measured: 65 000 steps at batch 64, train
                # 0.075 against val 2.07 and AUC 0.80). Validating every `--val-steps` steps is what makes early stopping
                # possible at that granularity.
                value, area = evaluate()
                print(f"step {step}: train {since / max(since_n, 1):.4f}  val {value:.4f}  auc {area:.4f}", flush=True)
                register_val(value, f"step {step}")
                model.train()
                since, since_n = 0.0, 0
                if red:
                    stopped = True
                    break
                if args.patience and since_best >= args.patience:
                    print(f"early stop: {since_best} validations without improvement at step {step}", flush=True)
                    stopped = True
                    break
        if stopped:
            break
        value, area = evaluate()
        print(f"epoch {epoch}: train {total / len(tr):.4f}  val {value:.4f}  auc {area:.4f}", flush=True)
        register_val(value, f"epoch {epoch}")
        if red:
            break

    # The last state is saved BEFORE the best-val state is loaded, so the external play eval can compare "last" with "best-val".
    if args.save_checkpoints > 0:
        export(model, f"{ckpt_base}_ckpt_last.zip")
        print(f"  checkpoint {ckpt_base}_ckpt_last.zip (last)", flush=True)
    if best_state is not None:
        model.load_state_dict(best_state)
        print(f"using the best epoch (val {best_val:.4f})", flush=True)
    export(model, args.out)
    if red:
        print("RED FLAG: training stopped as overfitting; the best checkpoint is saved but the run is red", flush=True)
        sys.exit(42)


if __name__ == "__main__":
    main()

