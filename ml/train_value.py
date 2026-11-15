"""Value model V(s): how likely is the decision maker to win from this observation?

Why a separate model (docs/SEARCH.md, docs/EXPERIMENTS.md): night 8 trained the *policy* on games against SMART for
8.7 h and every one of its 62 iterations came out weaker than the parent (86.7% -> ~78% vs RANDOM, 23.8% -> ~18% vs
SMART) while the value loss kept improving. The loss is informative about the *state*, but a binary outcome applied to
the single chosen candidate of ~500 decisions per game cannot give the candidate ranking a direction -- it only blurs
an already good one.

V(s) is dense supervision instead: every recorded state gets the game's result, no candidate and no ranking involved,
so training cannot destroy a policy. It is the baseline the current training lacks (`train_all.py --adv-weight`), and
the leaf evaluation of search.

    z      = clip((obs - mean) / std, -clip, clip)
    h1     = relu(W1 z + b1)
    h2     = relu(W2 h1 + b2)
    logit  = w3 . h2 + b3                    V = sigmoid(logit)

    python ml/train_value.py --data DIR1,DIR2 --out DIR [--epochs 6] [--rows-per-game 16]

Files written: model.json / weights.bin (the contract with MlpValueModel.java), model.pt, train_metrics.json,
reference.json, history.json.
"""
import argparse
import hashlib
import json
import os
import sys
import time

import numpy as np
import torch
from torch import nn

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import discover_decisions, read_decisions  # noqa: E402
from train_phase import CLIP, is_validation_game, list_chunks  # noqa: E402

KIND = "value_mlp"
FORMAT_VERSION = 1
BLOCK_ROWS = 200_000


def value_weight_layout(obs_size, hidden):
    h1, h2 = hidden
    return [("mean", (obs_size,)), ("std", (obs_size,)),
            ("w1", (h1, obs_size)), ("b1", (h1,)),
            ("w2", (h2, h1)), ("b2", (h2,)),
            ("w3", (h2,)), ("b3", (1,))]


def numpy_forward(params, clip, obs):
    """Independent implementation of the forward pass: obs [obs_size] -> logit."""
    z = np.clip((obs - params["mean"]) / params["std"], -clip, clip)
    h1 = np.maximum(params["w1"] @ z + params["b1"], 0.0)
    h2 = np.maximum(params["w2"] @ h1 + params["b2"], 0.0)
    return params["w3"] @ h2 + params["b3"][0]


def export_value(out_dir, observation_hash, obs_size, hidden, clip, params, reference):
    layout = value_weight_layout(obs_size, hidden)
    chunks = []
    for name, shape in layout:
        arr = np.asarray(params[name], dtype="<f4")
        if arr.shape != tuple(shape):
            raise ValueError(f"{name}: expected shape {shape}, got {arr.shape}")
        if not np.isfinite(arr).all():
            raise ValueError(f"{name} contains NaN/Inf: the training diverged, the model is not exported")
        chunks.append(arr.reshape(-1).tobytes())
    raw = b"".join(chunks)
    meta = {"kind": KIND, "version": FORMAT_VERSION, "observationHash": observation_hash, "obsSize": obs_size,
            "hidden": list(hidden), "clip": clip, "weightsCount": len(raw) // 4,
            "weightsSha256": hashlib.sha256(raw).hexdigest()}
    for name, data in (("weights.bin", raw), ("model.json", json.dumps(meta, indent=1).encode()),
                       ("reference.json", json.dumps(reference).encode())):
        tmp = os.path.join(out_dir, name + ".tmp")
        with open(tmp, "wb") as f:
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, os.path.join(out_dir, name))
    return meta


def auc(scores, labels):
    """ROC AUC by rank (labels are 0 / 0.5 / 1; a 0.5 counts as half a win)."""
    order = np.argsort(scores)
    ranks = np.empty(len(scores), dtype=np.float64)
    ranks[order] = np.arange(1, len(scores) + 1)
    positive = labels > 0.5
    n_pos, n_neg = int(positive.sum()), int((~positive).sum())
    if not n_pos or not n_neg:
        return float("nan")
    return float((ranks[positive].sum() - n_pos * (n_pos + 1) / 2) / (n_pos * n_neg))


class ValueNet(nn.Module):
    """obs -> logit of P(win). Same shape as the Java MlpValueModel reads."""

    def __init__(self, obs_size, hidden, mean, std):
        super().__init__()
        self.register_buffer("mean", torch.as_tensor(mean))
        self.register_buffer("std", torch.as_tensor(std))
        self.w1 = nn.Linear(obs_size, hidden[0])
        self.w2 = nn.Linear(hidden[0], hidden[1])
        self.w3 = nn.Linear(hidden[1], 1)

    def forward(self, x):
        z = torch.clamp((x - self.mean) / self.std, -CLIP, CLIP)
        h1 = torch.relu(self.w1(z))
        h2 = torch.relu(self.w2(h1))
        return self.w3(h2).squeeze(-1)


def standardize(obs):
    sample = obs if len(obs) <= BLOCK_ROWS else obs[:BLOCK_ROWS]
    mean = sample.astype(np.float32).mean(0)
    std = sample.astype(np.float32).std(0)
    return mean, np.where(std < 1e-3, 1.0, std).astype(np.float32)


def fit_value(obs, outcome, hidden=(256, 128), epochs=6, lr=1e-3, weight_decay=1e-5, batch=4096,
              seed=1, device="cpu", val_fraction=0.1, steps=0, label_mean=None, verbose=True,
              train_idx=None, val_idx=None, target=None, target_kind="outcome"):
    """Trains V on (obs f16/f32 [n, obsSize], outcome f32 [n]). Returns the best epoch as numpy parameters.

    The split is by game (`train_idx` / `val_idx` from the caller, or a hash of the row's game id), so the same game
    never sits on both sides. `label_mean` is the constant predictor of the reference loss.

    `target_kind="margin"` trains on the final score difference instead of the win, as a regression: the score is the
    game's own objective and carries how *dominant* a position was, which a binary label throws away. `outcome` is still
    used for the reported AUC, so both targets are compared on the same footing.
    """
    rng = np.random.default_rng(seed)
    device = torch.device(device)
    target = outcome if target is None else target
    mean, std = standardize(obs)
    if train_idx is None or val_idx is None:
        split = is_validation_game(np.arange(len(obs)).astype(np.uint64), val_fraction) if val_fraction else \
            np.zeros(len(obs), dtype=bool)
        train_idx, val_idx = np.flatnonzero(~split), np.flatnonzero(split)
    if len(val_idx) < 10:
        train_idx, val_idx = np.arange(len(obs)), np.arange(min(len(obs), 1000))
    model = ValueNet(obs.shape[1], list(hidden), mean, std).to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=weight_decay)
    regression = target_kind == "margin"
    loss_fn = nn.MSELoss() if regression else nn.BCEWithLogitsLoss()
    steps = steps or (len(train_idx) + batch - 1) // batch
    label_mean = float(target[train_idx].mean()) if label_mean is None else label_mean
    constant = (float(np.mean((target[train_idx] - label_mean) ** 2)) if regression
                else float(-(label_mean * np.log(max(label_mean, 1e-9))
                             + (1 - label_mean) * np.log(max(1 - label_mean, 1e-9)))))

    def tensor(array, idx):
        return torch.from_numpy(array[idx].astype(np.float32)).to(device)

    def evaluate():
        model.eval()
        logits = []
        with torch.no_grad():
            for s in range(0, len(val_idx), BLOCK_ROWS):
                logits.append(model(tensor(obs, val_idx[s:s + BLOCK_ROWS])).float().cpu().numpy())
        model.train()
        return np.concatenate(logits)

    history, best = [], {"val_loss": float("inf"), "epoch": 0, "state": None}
    for epoch in range(1, epochs + 1):
        model.train()
        running, seen = 0.0, 0
        permutation = rng.permutation(train_idx)
        for b in range(steps):
            slice_ = permutation[b * batch:(b + 1) * batch]
            if len(slice_) < 2:
                break
            logits = model(tensor(obs, slice_))
            loss = loss_fn(logits, tensor(target, slice_))
            if not torch.isfinite(loss):
                raise SystemExit(f"the value loss is {float(loss)} in epoch {epoch}: the training diverged")
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            running += float(loss) * len(slice_)
            seen += len(slice_)
        val_logits = evaluate()
        if regression:
            val_loss = float(nn.functional.mse_loss(
                torch.from_numpy(val_logits), torch.from_numpy(target[val_idx].astype(np.float32))))
        else:
            val_loss = float(nn.functional.binary_cross_entropy_with_logits(
                torch.from_numpy(val_logits), torch.from_numpy(outcome[val_idx].astype(np.float32))))
        row = {"epoch": epoch, "train_loss": running / max(seen, 1), "val_loss": val_loss,
               "constant_val_loss": constant, "auc": auc(val_logits, outcome[val_idx]),
               "brier": float(np.mean((1 / (1 + np.exp(-val_logits)) - outcome[val_idx]) ** 2))}
        history.append(row)
        if verbose:
            print(json.dumps({k: (round(v, 4) if isinstance(v, float) else v) for k, v in row.items()}), flush=True)
        if val_loss < best["val_loss"]:
            best = {"val_loss": val_loss, "epoch": epoch,
                    "state": {k: v.detach().clone() for k, v in model.state_dict().items()}}
    model.load_state_dict(best["state"])
    model.eval()
    state = {k: v.cpu() for k, v in model.state_dict().items()}
    params = {"mean": mean, "std": std,
              "w1": state["w1.weight"].numpy(), "b1": state["w1.bias"].numpy(),
              "w2": state["w2.weight"].numpy(), "b2": state["w2.bias"].numpy(),
              "w3": state["w3.weight"].numpy().reshape(-1), "b3": state["w3.bias"].numpy()}
    return {"params": params, "state": state, "hidden": list(hidden), "mean": mean, "std": std,
            "history": history, "best_epoch": best["epoch"], "best_val_loss": best["val_loss"],
            "label_mean": label_mean, "constant_loss": constant, "device": str(device)}


def fit_stream(data_dirs, rows_per_game, hidden, epochs, lr, weight_decay, batch, seed, device,
               target_kind="outcome", margin_scale=20.0, val_every=10, stats_per_type=2, verbose=True):
    """Trains V by streaming the chunk files: read one file, take its rows, update, throw it away.

    The dataset of a serious night does not fit in RAM (30 000 games with every decision is ~58 GB). Nothing about the
    value model needs all of it at once: the normalisation comes from a sample of files, the validation set is a
    HELD-OUT set of files (whole games, so no leakage), and an epoch is one pass over the remaining files in random
    order. Peak memory is one chunk plus the model.
    """
    rng = np.random.default_rng(seed)
    device = torch.device(device)
    by_type = {}
    for decision in discover_decisions(data_dirs):
        by_type[decision] = list_chunks(data_dirs, decision)
    train_files, val_files = [], []
    for decision, files in by_type.items():
        for index, path in enumerate(files):
            (val_files if index % val_every == val_every - 1 else train_files).append((decision, path))
    if not train_files or not val_files:
        raise SystemExit(f"streaming needs several chunks per type: {len(train_files)} train / {len(val_files)} val files")

    def read_rows(path):
        schema, rows = read_decisions(path)
        if not len(rows):
            return None
        if rows_per_game:
            keep = cap_per_game(rows["game"].astype(np.uint64), rows_per_game, rng)
            rows = rows[keep]
            if not len(rows):
                return None
        return rows

    # ---- normalisation from a sample of files (cheap and stable: the values are bounded and ~90% zeros)
    total, sums, squares = 0, None, None
    for decision, files in by_type.items():
        for path in files[:stats_per_type]:
            rows = read_rows(path)
            if rows is None:
                continue
            x = rows["obs"].astype(np.float32)
            if sums is None:
                sums, squares = np.zeros(x.shape[1], np.float64), np.zeros(x.shape[1], np.float64)
            sums += x.sum(axis=0)
            squares += (x * x).sum(axis=0)
            total += len(x)
    if not total:
        raise SystemExit("no rows to compute the normalisation from")
    mean = (sums / total).astype(np.float32)
    std = np.sqrt(np.maximum(squares / total - (mean.astype(np.float64) ** 2), 0)).astype(np.float32)
    std[std < 1e-6] = 1.0

    model = ValueNet(sums.shape[0], list(hidden), mean, std).to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=weight_decay)
    regression = target_kind == "margin"
    loss_fn = nn.MSELoss() if regression else nn.BCEWithLogitsLoss()

    def to_target(rows):
        if regression:
            return torch.from_numpy(np.clip(rows["margin"] / margin_scale, -3.0, 3.0).astype(np.float32)).to(device)
        return torch.from_numpy(rows["outcome"].astype(np.float32)).to(device)

    def validate():
        model.eval()
        losses, logits, labels, margins = [], [], [], []
        with torch.no_grad():
            for _, path in val_files:
                rows = read_rows(path)
                if rows is None:
                    continue
                x = torch.from_numpy(rows["obs"].astype(np.float32)).to(device)
                out = model(x)
                y = to_target(rows)
                losses.append(float(loss_fn(out, y)) * len(rows))
                logits.append(out.float().cpu().numpy())
                labels.append(rows["outcome"].astype(np.float32))
                margins.append(rows["margin"].astype(np.float32))
                model.train()
        model.train()
        if not logits:
            return float("nan"), 0.0, 0.0, 0.0, None, None
        rows_seen = sum(len(l) for l in labels)
        scores = np.concatenate(logits)
        outcome = np.concatenate(labels)
        margin = np.concatenate(margins)
        return (sum(losses) / rows_seen, auc(scores, outcome), float(outcome.mean()),
                float(np.mean((scores - outcome) ** 2)) if not regression else float(np.mean((scores - margin / margin_scale) ** 2)),
                scores, outcome)

    history, best = [], {"val_loss": float("inf"), "epoch": 0, "state": None}
    label_mean = 0.0
    for epoch in range(1, epochs + 1):
        model.train()
        order = rng.permutation(len(train_files))
        running, seen, batches = 0.0, 0, 0
        for index in order:
            _, path = train_files[index]
            rows = read_rows(path)
            if rows is None:
                continue
            x_all = rows["obs"].astype(np.float32)
            y_all = to_target(rows)
            # Shuffle the rows of the chunk: they arrive grouped by game, and training on consecutive records of one game
            # is exactly how a model memorises that game (owner's warning, and the validation split by whole chunks would
            # show it as rising val loss - but no reason to create the problem).
            order = rng.permutation(len(x_all))
            x_all, y_all = x_all[order], y_all[order]
            for start in range(0, len(x_all), batch):
                x = torch.from_numpy(x_all[start:start + batch]).to(device)
                y = y_all[start:start + batch]
                if len(x) < 2:
                    continue
                loss = loss_fn(model(x), y)
                if not torch.isfinite(loss):
                    raise SystemExit(f"the value loss is {float(loss)} in epoch {epoch}: the training diverged")
                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(model.parameters(), 5.0)
                optimizer.step()
                running += float(loss) * len(x)
                seen += len(x)
                batches += 1
        val_loss, val_auc, observed_mean, val_brier, _, _ = validate()
        label_mean = observed_mean
        row = {"epoch": epoch, "train_loss": running / max(seen, 1), "val_loss": val_loss, "auc": val_auc,
               "rows_seen": seen, "batches": batches}
        history.append(row)
        if verbose:
            print(json.dumps({k: (round(v, 4) if isinstance(v, float) else v) for k, v in row.items()}), flush=True)
        if val_loss < best["val_loss"]:
            best = {"val_loss": val_loss, "epoch": epoch,
                    "state": {k: v.detach().clone() for k, v in model.state_dict().items()}}
    model.load_state_dict(best["state"])
    model.eval()
    state = {k: v.cpu() for k, v in model.state_dict().items()}
    params = {"mean": mean, "std": std,
              "w1": state["w1.weight"].numpy(), "b1": state["w1.bias"].numpy(),
              "w2": state["w2.weight"].numpy(), "b2": state["w2.bias"].numpy(),
              "w3": state["w3.weight"].numpy().reshape(-1), "b3": state["w3.bias"].numpy()}
    return {"params": params, "state": state, "hidden": list(hidden), "mean": mean, "std": std,
            "history": history, "best_epoch": best["epoch"], "best_val_loss": best["val_loss"],
            "label_mean": label_mean, "constant_loss": float("nan"), "device": str(device),
            "train_files": len(train_files), "val_files": len(val_files)}


def predict(params, obs, batch=200_000, squash=True):
    """V of every row, batched, with the exported numpy parameters (no torch). `squash=False` keeps the raw output, which
    is what a margin model predicts."""
    mean, std = params["mean"], params["std"]
    result = np.empty(len(obs), dtype=np.float32)
    for s in range(0, len(obs), batch):
        z = np.clip((obs[s:s + batch].astype(np.float32) - mean) / std, -CLIP, CLIP)
        h1 = np.maximum(z @ params["w1"].T + params["b1"], 0.0)
        h2 = np.maximum(h1 @ params["w2"].T + params["b2"], 0.0)
        raw = h2 @ params["w3"] + params["b3"][0]
        result[s:s + batch] = 1.0 / (1.0 + np.exp(-raw)) if squash else raw
    return result


def cap_per_game(games, k, rng):
    """Positions of at most k randomly chosen rows of every game (sorted, so a file is read forward)."""
    if not k:
        return np.arange(len(games))
    order = np.lexsort((rng.random(len(games)), games))
    g = games[order]
    first = np.r_[True, g[1:] != g[:-1]]
    group_start = np.maximum.accumulate(np.where(first, np.arange(len(g)), 0))
    return np.sort(order[(np.arange(len(g)) - group_start) < k])


def load_pool(data_dirs, rows_per_game, rng, val_fraction, keep_round=False):
    """Reads every decision type, de-duplicates states, caps the rows of a game, splits by game.

    The value model sees only the observation, so the candidate layout (and with it the per-type schema hash) is
    irrelevant. The schema's observationLayout describes the observation first and the candidates after
    "; candidate=" -- that shared prefix is the identity of a value model.
    """
    obs, outcome, game, round_no, context, margin = [], [], [], [], [], []
    obs_size, observation_layout = None, None
    seen_states = set()
    for decision in discover_decisions(data_dirs):
        files = list_chunks(data_dirs, decision)
        for path in files:
            schema, rows = read_decisions(path)
            layout = schema["observationLayout"].split("; candidate=")[0]
            if obs_size is None:
                obs_size, observation_layout = schema["obsSize"], layout
            elif schema["obsSize"] != obs_size or layout != observation_layout:
                raise SystemExit(f"{path}: the observation layout differs from the first chunk; mixed data is not supported")
            if not len(rows):
                continue
            keys = rows["game"].astype(np.uint64) * np.uint64(1 << 16) + rows["step"].astype(np.uint64)
            fresh = np.array([k not in seen_states for k in keys.tolist()], dtype=bool)
            seen_states.update(keys[fresh].tolist())
            rows = rows[fresh]
            if not len(rows):
                continue
            # The cap is applied PER FILE, not after loading everything: a chunk holds whole games, so this is exact, and
            # it is the difference between a 23 GB pool and a pool that fits when the dataset has every decision of every
            # game. `rows_per_game=0` keeps everything.
            if rows_per_game:
                keep = cap_per_game(rows["game"].astype(np.uint64), rows_per_game, rng)
                rows = rows[keep]
                if not len(rows):
                    continue
            obs.append(rows["obs"].astype(np.float16))
            outcome.append(rows["outcome"].astype(np.float32))
            margin.append(rows["margin"].astype(np.float32))
            game.append(rows["game"].astype(np.uint64))
            context.append(rows["context"].astype(np.uint8))
            if keep_round:
                round_no.append(rows["round"].astype(np.uint16))
        if files:
            print(f"  {decision:22s} {len(files):4d} chunks", flush=True)
    if not obs:
        raise SystemExit(f"no decision chunks in {data_dirs}")
    obs = np.concatenate(obs)
    outcome = np.concatenate(outcome)
    margin = np.concatenate(margin)
    game = np.concatenate(game)
    context = np.concatenate(context)
    round_no = np.concatenate(round_no) if keep_round else np.zeros(len(obs), dtype=np.uint16)
    if rows_per_game:
        picked = cap_per_game(game, rows_per_game, rng)
    else:
        picked = np.arange(len(obs))
    obs, outcome, margin, game, context, round_no = (obs[picked], outcome[picked], margin[picked], game[picked],
                                                    context[picked], round_no[picked])
    return {"obs": obs, "outcome": outcome, "margin": margin, "game": game, "context": context, "round": round_no,
            "validation": is_validation_game(game, val_fraction), "obsSize": obs_size,
            "observationHash": hashlib.sha256(observation_layout.encode("utf-8")).hexdigest()[:16],
            "observationLayout": observation_layout}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="comma-separated directories with *.NNNNNN.ares chunks")
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=6)
    parser.add_argument("--steps", type=int, default=0, help="batches between two validations (0 = one pass)")
    parser.add_argument("--hidden", default="256,128")
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-5)
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--rows-per-game", type=int, default=16,
                        help="at most this many states of one game (0 = all); the label is the same for all of them")
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--target", default="outcome", choices=["outcome", "margin"],
                        help="`outcome`: predict the win (BCE). `margin`: regress the final score difference "
                             "(scaled by --margin-scale) - the game's own objective, carrying how dominant a position was")
    parser.add_argument("--stream", action="store_true",
                        help="read the chunks one file at a time instead of loading the whole dataset into RAM "
                             "(the only option for a night of games: 30 000 games with every decision is ~58 GB)")
    parser.add_argument("--val-every", type=int, default=10,
                        help="every N-th chunk file per type is held out for validation (whole games, no leakage)")
    parser.add_argument("--margin-scale", type=float, default=20.0,
                        help="margin target is margin/scale; only used with --target margin")
    args = parser.parse_args()

    started = time.time()
    os.makedirs(args.out, exist_ok=True)
    rng = np.random.default_rng(args.seed)
    hidden = [int(x) for x in args.hidden.split(",")]
    device = (torch.device("cuda" if torch.cuda.is_available() else "cpu")
              if args.device == "auto" else torch.device(args.device))
    print(f"device {device}, reading {args.data}", flush=True)
    if args.stream:
        fitted = fit_stream(args.data, args.rows_per_game, hidden, epochs=args.epochs, lr=args.lr,
                            weight_decay=args.weight_decay, batch=args.batch, seed=args.seed, device=device,
                            target_kind=args.target, margin_scale=args.margin_scale, val_every=args.val_every)
        # one validation file supplies the export reference (the Java loader replays it against the exported weights)
        reference_rows, reference_scores = None, None
        for decision in discover_decisions(args.data):
            for index, path in enumerate(list_chunks(args.data, decision)):
                if index % args.val_every != args.val_every - 1:
                    continue
                schema, rows = read_decisions(path)
                if not len(rows):
                    continue
                sample = np.unique(np.linspace(0, len(rows) - 1, min(8, len(rows))).astype(int))
                obs = rows["obs"][sample].astype(np.float32)
                layout = schema["observationLayout"].split("; candidate=")[0]
                observation_hash = hashlib.sha256(layout.encode("utf-8")).hexdigest()[:16]
                scores = predict(fitted["params"], obs, squash=args.target == "outcome")
                reference_rows = obs
                reference_scores = (scores if args.target == "margin"
                                    else np.log(np.clip(scores, 1e-9, 1 - 1e-9) / np.clip(1 - scores, 1e-9, 1 - 1e-9)))
                break
            if reference_rows is not None:
                break
        if reference_rows is None:
            raise SystemExit("no validation rows for the export reference")
        logits = reference_scores.astype(float)
        reference = {"inputs": reference_rows.astype(np.float32).tolist(), "logits": logits.tolist()}
        meta = export_value(args.out, observation_hash, fitted["params"]["mean"].shape[0], hidden, CLIP,
                            fitted["params"], reference)
        worst = max(abs(float(numpy_forward(fitted["params"], CLIP, o.astype(np.float64))) - float(l))
                    for o, l in zip(reference_rows.astype(np.float64), logits))
        if worst > 1e-3:
            raise SystemExit(f"the exported model disagrees with numpy by {worst}: refusing to finish")
        torch.save({"model": fitted["state"], "hidden": hidden, "epoch": fitted["best_epoch"],
                    "target": args.target, "stream": True}, os.path.join(args.out, "model.pt"))
        metrics = {"rows_per_game_cap": args.rows_per_game, "data": args.data, "target": args.target,
                   "stream": True, "train_files": fitted["train_files"], "val_files": fitted["val_files"],
                   "best_epoch": fitted["best_epoch"], "best_val_loss": round(fitted["best_val_loss"], 4),
                   "history": fitted["history"], "export_check": worst,
                   "seconds": round(time.time() - started, 1), "weightsSha256": meta["weightsSha256"]}
        with open(os.path.join(args.out, "train_metrics.json"), "w", encoding="utf-8") as f:
            json.dump(metrics, f, indent=1)
        with open(os.path.join(args.out, "history.json"), "w", encoding="utf-8") as f:
            json.dump(fitted["history"], f, indent=1)
        print(json.dumps({k: v for k, v in metrics.items() if k != "history"}, indent=1), flush=True)
        return 0
    pool = load_pool(args.data, args.rows_per_game, rng, args.val_fraction, keep_round=True)
    split = pool["validation"]
    train, val = pool["obs"][~split], pool["obs"][split]
    y_val = pool["outcome"][split]
    if len(train) < 1000 or len(val) < 100:
        raise SystemExit(f"not enough data: {len(train)} train / {len(val)} validation rows")
    target = pool["outcome"]
    if args.target == "margin":
        target = np.clip(pool["margin"] / args.margin_scale, -3.0, 3.0).astype(np.float32)
    fitted = fit_value(pool["obs"], pool["outcome"], hidden=hidden, epochs=args.epochs, lr=args.lr,
                       weight_decay=args.weight_decay, batch=args.batch, seed=args.seed, device=device,
                       train_idx=np.flatnonzero(~split), val_idx=np.flatnonzero(split),
                       label_mean=float(pool["outcome"][~split].mean()),
                       target=target, target_kind=args.target)
    params, history = fitted["params"], fitted["history"]
    predicted = predict(params, val, squash=args.target == "outcome")
    if args.target == "outcome":
        val_loss = float(nn.functional.binary_cross_entropy_with_logits(
            torch.from_numpy(np.log(np.clip(predicted, 1e-9, 1 - 1e-9) / np.clip(1 - predicted, 1e-9, 1))),
            torch.from_numpy(y_val.astype(np.float32))))
    else:
        val_loss = float(nn.functional.mse_loss(
            torch.from_numpy(predicted), torch.from_numpy((pool["margin"][split] / args.margin_scale)
                                                          .astype(np.float32))))
    auc_of_prediction = auc(predicted, y_val)

    # Where does V have information? Mean prediction against the observed result, per round band and per opponent.
    # For a margin model `predicted` is not a probability, so the band table reports the mean margin instead.
    calibration = []
    for lo, hi in ((1, 4), (5, 8), (9, 12), (13, 16), (17, 99)):
        group = (pool["round"][split] >= lo) & (pool["round"][split] <= hi)
        if group.sum() < 50:
            continue
        calibration.append({"rounds": f"{lo}-{hi}", "rows": int(group.sum()),
                            "predicted": round(float(predicted[group].mean()), 4),
                            "observed": round(float(y_val[group].mean()), 4)})
    for context in sorted(set(pool["context"][split].tolist())):
        group = pool["context"][split] == context
        calibration.append({"context": int(context), "rows": int(group.sum()),
                            "predicted": round(float(predicted[group].mean()), 4),
                            "observed": round(float(y_val[group].mean()), 4)})

    sample = np.unique(np.linspace(0, len(val) - 1, min(8, len(val))).astype(int))
    logits = (predicted[sample] if args.target == "margin"
              else np.log(np.clip(predicted[sample], 1e-9, 1 - 1e-9) / np.clip(1 - predicted[sample], 1e-9, 1 - 1e-9)))
    reference = {"inputs": val[sample].astype(np.float32).tolist(), "logits": logits.astype(float).tolist()}
    meta = export_value(args.out, pool["observationHash"], pool["obsSize"], hidden, CLIP, params, reference)
    worst = max(abs(float(numpy_forward(params, CLIP, o.astype(np.float64))) - float(l))
                for o, l in zip(val[sample].astype(np.float64), logits))
    if worst > 1e-3:
        raise SystemExit(f"the exported model disagrees with numpy by {worst}: refusing to finish")

    torch.save({"model": fitted["state"], "observation_hash": pool["observationHash"], "hidden": hidden,
                "epoch": fitted["best_epoch"], "target": args.target}, os.path.join(args.out, "model.pt"))
    metrics = {"rows_train": int(len(train)), "rows_val": int(len(val)), "target": args.target,
               "games": int(len(np.unique(pool["game"]))), "label_mean": round(fitted["label_mean"], 4),
               "constant_val_loss": round(fitted["constant_loss"], 4),
               "best_epoch": fitted["best_epoch"], "best_val_loss": round(val_loss, 4),
               "auc": round(auc_of_prediction, 4),
               "brier": round(float(np.mean((predicted - y_val) ** 2)), 4) if args.target == "outcome" else None,
               "export_check": worst, "calibration": calibration, "rows_per_game_cap": args.rows_per_game,
               "data": args.data, "observation_hash": pool["observationHash"],
               "seconds": round(time.time() - started, 1), "weightsSha256": meta["weightsSha256"]}
    with open(os.path.join(args.out, "train_metrics.json"), "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=1)
    with open(os.path.join(args.out, "history.json"), "w", encoding="utf-8") as f:
        json.dump(history, f, indent=1)
    print(json.dumps({k: v for k, v in metrics.items() if k != "calibration"}, indent=1), flush=True)
    print(json.dumps(metrics["calibration"], indent=1), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
