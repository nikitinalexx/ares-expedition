"""Train the NETWORK_V2 value (two towers: table 417 + hand 481) and export it in the format `NNService` loads.

Why: the owner's recipe - games first, then a value learned from them - has to run through HIS processors (`network2` + `NNService`), and the vector
has to be the V2 encoder with the hand (the owner's point: without the hand the player is much weaker). The dataset is written by
`NetDataWriter` (`-Dares.netdata.dir`): one CSV row = label (1 / 0 / 0.5) + 898 values, already normalised by `AiConstants.NORMALIZATION_VECTOR`.

The export is a DL4J `coefficients.bin`: ND4J writes a shape buffer (mode UTF, long length, type UTF, elements) and then a float buffer (mode UTF,
long length, "FLOAT" UTF, big-endian floats). `PureJavaValueNetwork.load` skips the first and reads the second. The parameter order is topological:
W_table (f-order [417 x 512]), b_table, W_hand (f-order [481 x 512]), b_hand, W_merge (f-order [1024 x 1024]), b_merge, W_out (1024), b_out.

    py -3 ml/train_network_v2.py --csv D:/ares/data/netd_rand5k/netdata.csv --out D:/ares/runs/network3/net3.zip
"""
import argparse
import os
import struct
import sys
import zipfile

import numpy as np

TABLE = 417
HAND = 481
H1 = 512
H2 = 512
H3 = 1024


MAGIC = b"ND2BIN01"
RECORD = np.dtype([("game", "<i4"), ("label", "<f2"), ("x", "<f2", (TABLE + HAND,))])


def read_binary(path, limit=0, dtype=np.float32):
    """Read `netdata.bin` (NetDataWriter's default): magic `ND2BIN01`, uint16 dimension, then little-endian records
    `int32 gameId, float16 label, float16 x dim`.

    The id is an integer here, which is what the float16 CSV parse got wrong; the writer stores it as int32 for that reason.
    """
    with open(path, "rb") as f:
        magic = f.read(len(MAGIC))
        if magic != MAGIC:
            raise ValueError(f"{path}: not a {MAGIC!r} file (got {magic!r})")
        dimension = int.from_bytes(f.read(2), "little")
        if dimension != TABLE + HAND:
            raise ValueError(f"{path}: dimension {dimension}, expected {TABLE + HAND}")
        payload = os.path.getsize(path) - len(MAGIC) - 2
        if payload < 0 or payload % RECORD.itemsize:
            raise ValueError(f"{path}: {payload} payload bytes is not a whole number of {RECORD.itemsize}-byte records "
                             f"- a truncated or wrong-endian write")
        count = payload // RECORD.itemsize
        if limit:
            count = min(count, limit)
        records = np.fromfile(f, dtype=RECORD, count=count)
    if count and len(records) != count:
        raise ValueError(f"{path}: {len(records)} records read, {count} expected - a truncated write")
    labels = records["label"].astype(dtype)
    if labels.size and (labels.min() < 0.0 or labels.max() > 1.0):
        raise ValueError(f"{path}: labels outside [0,1] ({labels.min()}..{labels.max()}) - the file layout is wrong")
    return labels, records["x"].astype(dtype), records["game"].astype(np.int64)


def read_dataset(path, limit=0, dtype=np.float32):
    """CSV or binary by content, so a mixed directory of old and new corpora still trains."""
    with open(path, "rb") as f:
        head = f.read(len(MAGIC))
    if head == MAGIC:
        return read_binary(path, limit=limit, dtype=dtype)
    return read_csv(path, limit=limit, dtype=dtype)


def read_csv(path, limit=0, dtype=np.float32):
    rows = []
    ids = []
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f):
            if limit and i >= limit:
                break
            values = np.fromstring(line, sep=",", dtype=dtype)
            if values.size == 1 + TABLE + HAND:
                rows.append(values)
                ids.append(-1)
            elif values.size == 2 + TABLE + HAND:
                # gameId,label,features...  The id must be parsed as an INTEGER: float16 rounds everything above 2048 to a multiple of 4,
                # which merged 50 000 games into 6 682 and leaked the train/validation split (found 21/11).
                rows.append(np.concatenate([values[1:2], values[2:]]))
                ids.append(int(line.split(",", 1)[0]))
            else:
                raise ValueError(f"{path}:{i + 1} has {values.size} values, expected {1 + TABLE + HAND} or {2 + TABLE + HAND}")
    data = np.stack(rows)
    labels = data[:, 0].astype(np.float32)
    if labels.min() < 0.0 or labels.max() > 1.0:
        raise ValueError(f"{path}: labels outside [0,1] ({labels.min()}..{labels.max()}) - the column order is wrong")
    return labels, data[:, 1:], np.array(ids)


class Towers:
    """A two-tower value net in numpy, trained by hand-rolled Adam (no torch dependency needed for this small model)."""

    def __init__(self, seed):
        rng = np.random.default_rng(seed)

        def dense(n_in, n_out):
            return (rng.normal(0, np.sqrt(2.0 / n_in), (n_in, n_out)).astype(np.float32),
                    np.zeros(n_out, dtype=np.float32))

        self.p = {
            "w1": dense(TABLE, H1), "w2": dense(HAND, H2),
            "w3": dense(H1 + H2, H3), "w4": dense(H3, 1),
        }

    def forward(self, table, hand):
        w1, b1 = self.p["w1"]
        w2, b2 = self.p["w2"]
        w3, b3 = self.p["w3"]
        w4, b4 = self.p["w4"]
        a1 = np.maximum(table @ w1 + b1, 0.01 * (table @ w1 + b1))
        a2 = np.maximum(hand @ w2 + b2, 0.01 * (hand @ w2 + b2))
        merged = np.concatenate([a1, a2], axis=1)
        pre3 = merged @ w3 + b3
        a3 = np.maximum(pre3, 0.01 * pre3)
        logit = (a3 @ w4 + b4)[:, 0]
        return logit, (table, hand, a1, a2, merged, pre3, a3)


def train(csv, out, epochs=3, batch=4096, lr=1e-3, seed=1, limit=0, holdout=0.1):
    label, x, ids = read_dataset(csv, limit)
    table, hand = x[:, :TABLE], x[:, TABLE:]
    rng = np.random.default_rng(seed)
    print(f"rows {len(label)}, games {len(np.unique(ids)) if (ids >= 0).all() else 'n/a'}, label mean {label.mean():.4f}", flush=True)
    if (ids >= 0).all():
        # Split by GAME: rows of one game share its outcome, so a row split leaks and inflates the validation metric.
        games = np.unique(ids)
        rng.shuffle(games)
        cut = int(len(games) * (1.0 - holdout))
        train_idx = np.flatnonzero(np.isin(ids, games[:cut]))
        val_idx = np.flatnonzero(np.isin(ids, games[cut:]))
    else:
        idx = rng.permutation(len(label))
        cut = int(len(label) * (1.0 - holdout))
        train_idx, val_idx = idx[:cut], idx[cut:]

    net = Towers(seed)
    state = {k: (np.zeros_like(w), np.zeros_like(w)) for k, (w, b) in net.p.items()}
    state_b = {k: (np.zeros_like(b), np.zeros_like(b)) for k, (w, b) in net.p.items()}
    step = 0

    def sigmoid(z):
        return 1.0 / (1.0 + np.exp(-z))

    for epoch in range(1, epochs + 1):
        order = rng.permutation(train_idx)
        total = 0.0
        for start in range(0, len(order), batch):
            take = order[start:start + batch]
            t, h, y = table[take], hand[take], label[take]
            logit, cache = net.forward(t, h)
            p = sigmoid(logit)
            # BCE with soft labels (draws are 0.5)
            loss = -np.mean(y * np.log(np.clip(p, 1e-7, 1 - 1e-7)) + (1 - y) * np.log(np.clip(1 - p, 1e-7, 1 - 1e-7)))
            total += loss * len(take)
            dtype = (p - y) / len(take)
            table_in, hand_in, a1, a2, merged, pre3, a3 = cache
            w1, b1 = net.p["w1"]
            w2, b2 = net.p["w2"]
            w3, b3 = net.p["w3"]
            w4, b4 = net.p["w4"]
            g_w4 = a3.T @ dtype[:, None]
            g_b4 = np.array([dtype.sum()], dtype=np.float32)
            d_a3 = dtype[:, None] * w4.T
            d_pre3 = d_a3 * np.where(pre3 > 0, 1.0, 0.01)
            g_w3 = merged.T @ d_pre3
            g_b3 = d_pre3.sum(0)
            d_merged = d_pre3 @ w3.T
            d_a1 = d_merged[:, :H1]
            d_a2 = d_merged[:, H1:]
            d_pre1 = d_a1 * np.where(a1 > 0, 1.0, 0.01)
            d_pre2 = d_a2 * np.where(a2 > 0, 1.0, 0.01)
            g_w1 = table_in.T @ d_pre1
            g_b1 = d_pre1.sum(0)
            g_w2 = hand_in.T @ d_pre2
            g_b2 = d_pre2.sum(0)
            grads = {"w1": (g_w1, g_b1), "w2": (g_w2, g_b2), "w3": (g_w3, g_b3), "w4": (g_w4, g_b4)}
            # Gradient clipping: at 1.3M rows a few pathological batches produce huge gradients and Adam then chases them (train loss 31,
            # validation worsening). Clip the batch norm to 1 before the update.
            norm = float(np.sqrt(sum(float((g * g).sum()) for pair in grads.values() for g in pair)))
            scale = min(1.0, 1.0 / (norm + 1e-6))
            grads = {k: (gw * scale, gb * scale) for k, (gw, gb) in grads.items()}
            step += 1
            for k, (gw, gb) in grads.items():
                for arr, grad, st in ((net.p[k][0], gw, state[k]), (net.p[k][1], gb, state_b[k])):
                    st[0][...] = 0.9 * st[0] + 0.1 * grad
                    st[1][...] = 0.999 * st[1] + 0.001 * (grad * grad)
                    m = st[0] / (1 - 0.9 ** step)
                    v = st[1] / (1 - 0.999 ** step)
                    arr -= (lr * m / (np.sqrt(v) + 1e-8)).astype(arr.dtype)
        val_logit, _ = net.forward(table[val_idx], hand[val_idx])
        val_p = sigmoid(val_logit)
        val_y = label[val_idx]
        val_loss = -np.mean(val_y * np.log(np.clip(val_p, 1e-7, 1 - 1e-7))
                            + (1 - val_y) * np.log(np.clip(1 - val_p, 1e-7, 1 - 1e-7)))
        order_ = np.argsort(val_logit)
        ranks = np.empty_like(order_)
        ranks[order_] = np.arange(len(order_))
        pos = val_y > 0.5
        auc = (ranks[pos].mean() - (pos.sum() - 1) / 2) / max((~pos).sum(), 1) if pos.any() and (~pos).any() else float("nan")
        print(f"epoch {epoch}: train {total / cut:.4f}  val {val_loss:.4f}  auc {auc:.4f}", flush=True)

    export(net, out)
    return net


def _write_nd4j_shape(f):
    f.write(_utf("HEAP"))
    f.write(struct.pack(">q", 1))
    f.write(_utf("INT"))
    f.write(struct.pack(">i", 0))


def _utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def export(net, path):
    w1, b1 = net.p["w1"]
    w2, b2 = net.p["w2"]
    w3, b3 = net.p["w3"]
    w4, b4 = net.p["w4"]
    params = np.concatenate([
        w1.T.reshape(-1),  # f-order [table x h1]: element (i, j) at j * table + i -> w1.T.reshape is [h1][table] row-major = j*table+i
        b1,
        w2.T.reshape(-1),
        b2,
        w3.T.reshape(-1),
        b3,
        w4.reshape(-1),
        b4,
    ]).astype(">f4")
    expected = TABLE * H1 + H1 + HAND * H2 + H2 + (H1 + H2) * H3 + H3 + H3 + 1
    if params.size != expected:
        raise ValueError(f"{params.size} params, expected {expected}")
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as z:
        with z.open("coefficients.bin", "w") as raw:
            _write_nd4j_shape(raw)
            raw.write(_utf("HEAP"))
            raw.write(struct.pack(">q", params.size))
            raw.write(_utf("FLOAT"))
            raw.write(params.tobytes())
    print(f"wrote {path} ({params.size} params)", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--limit", type=int, default=0)
    args = parser.parse_args()
    train(args.csv, args.out, args.epochs, args.batch, args.lr, args.seed, args.limit)


if __name__ == "__main__":
    main()
