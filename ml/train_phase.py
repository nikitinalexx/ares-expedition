"""Trainer of the PICK_PHASE model for the night pipeline (follows the trainer contract of docs/NIGHTLY.md).

    python ml/train_phase.py --data DIR1,DIR2 --out MODEL_DIR [--decision PICK_PHASE] [--prev PREVIOUS_MODEL_DIR] [--epochs 3] [--hidden 256,128]

What is learned: the probability that the player WINS the game given what it saw and the candidate it picked.
The same trainer serves every decision type (--decision names the chunks); each type has its own candidate features.
(binary cross entropy against the result of the game: 1 win, 0.5 draw, 0 loss). Only the chosen candidate of every
row is a training example. Every chunk file is read ONCE, sequentially, with a single buffered `file.read()`
(ares_data.read_decisions) straight into an in-RAM pool split into train/val; every epoch then trains from RAM, with no
further disk access. This is deliberate: on a hard disk, `np.memmap`-based re-reading (the previous design, and still how
`train_all.py`'s multi-type pools used to work before the same fix) services a sequential scan as many small page faults
instead of one buffered read with normal OS read-ahead, and re-reading per epoch (as this file used to) turns one disk
pass into `epochs` of them. See docs/EXPERIMENTS.md, "I/O", for the numbers that motivated this.

Safety:
  * the validation split is by GAME (a hash of the game id), never by row: rows of one game must not be on both sides;
  * the loss on the validation games is compared with the loss of "always predict the average result" - a model that does not
    beat it is reported (and still written: the pipeline decides by playing, not by the loss);
  * a checkpoint is written atomically after every epoch and a restarted trainer continues from it;
  * the exported files are read back by an independent numpy implementation and compared with PyTorch: a mismatch is an error;
  * DONE is written last.
"""
import argparse
import json
import math
import os
import sys
import time

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import chunk_paths, open_memmap, read_decisions, validate  # noqa: E402
from phase_model import export_from_arrays, load_exported, numpy_forward  # noqa: E402

BLOCK_ROWS = 100_000
CLIP = 5.0


def is_validation_game(game_ids, fraction):
    """Deterministic split by game: the same game is always on the same side, in every epoch and every iteration."""
    mixed = (game_ids.astype(np.uint64) * np.uint64(0x9E3779B97F4A7C15)) >> np.uint64(40)
    return (mixed % np.uint64(1000)) < np.uint64(int(fraction * 1000))


def list_chunks(data_dirs, decision):
    files = []
    for directory in [d for d in data_dirs.split(",") if d]:
        files += chunk_paths(directory, decision)
    return files


def build_pool(files, fraction):
    """ONE sequential buffered read of every chunk file (read_decisions — a single `file.read()`, not `np.memmap`;
    see the module docstring), split by game into a train and a validation pool.

    Returns (train_pool, val_pool); either is (obs float32 [n,obs], cand float32 [n,F], outcome float32 [n], game uint64 [n])
    or None if that side has no rows."""
    collected = {False: ([], [], [], []), True: ([], [], [], [])}
    for path in files:
        _, rows = read_decisions(path)
        for start in range(0, len(rows), BLOCK_ROWS):
            block = rows[start:start + BLOCK_ROWS]
            games = np.asarray(block["game"])
            side = is_validation_game(games, fraction)
            for validation, mask in ((False, ~side), (True, side)):
                if not mask.any():
                    continue
                picked = np.asarray(block[mask])
                chosen = picked["chosen"].astype(np.int64)
                obs, cand, outcome, game = collected[validation]
                obs.append(picked["obs"].astype(np.float32))
                cand.append(picked["feat"][np.arange(len(picked)), chosen].astype(np.float32))
                outcome.append(picked["outcome"].astype(np.float32))
                game.append(games[mask])

    def finish(parts):
        obs, cand, outcome, game = parts
        if not obs:
            return None
        return np.concatenate(obs), np.concatenate(cand), np.concatenate(outcome), np.concatenate(game)

    return finish(collected[False]), finish(collected[True])


def standardisation_from_pool(obs):
    """Mean and std of every observation feature over the TRAINING pool (already in RAM; chunked float64 accumulation
    so a huge pool is not doubled in memory by one big float64 cast)."""
    n = len(obs)
    if n == 0:
        raise SystemExit("no training rows (are there finished chunks and more than one game?)")
    total = np.zeros(obs.shape[1], dtype=np.float64)
    total_sq = np.zeros(obs.shape[1], dtype=np.float64)
    for start in range(0, n, BLOCK_ROWS):
        chunk = obs[start:start + BLOCK_ROWS].astype(np.float64)
        total += chunk.sum(0)
        total_sq += (chunk * chunk).sum(0)
    mean = total / n
    std = np.sqrt(np.maximum(total_sq / n - mean * mean, 0.0))
    # a feature that never changes carries no information: std 1 turns it into a constant zero instead of a division by ~0
    std = np.where(std < 1e-3, 1.0, std)
    return mean.astype(np.float32), std.astype(np.float32), n


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--decision", default="PICK_PHASE", help="which decision type to train; the chunks are named <decision>.NNNNNN.ares")
    parser.add_argument("--prev", default="")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--hidden", default="256,128")
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-5)
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--device", default="auto")
    args = parser.parse_args()

    import torch
    from torch import nn

    started = time.time()
    files = list_chunks(args.data, args.decision)
    if not files:
        print("no finished chunks in " + args.data, file=sys.stderr)
        return 2
    schema, first = open_memmap(files[0])
    problems = validate(schema, first[: min(len(first), 20000)])
    if problems:
        print(f"{files[0]}: {problems}", file=sys.stderr)
        return 3
    for f in files[1:]:
        other, _ = open_memmap(f)
        if other["hash"] != schema["hash"]:
            print(f"{f} has the schema hash {other['hash']}, expected {schema['hash']}: data of different encoders must not be mixed", file=sys.stderr)
            return 3
    obs_size, cand_features = schema["obsSize"], schema["candidateFeatures"]
    hidden = [int(x) for x in args.hidden.split(",")]
    device = torch.device("cuda" if args.device == "auto" and torch.cuda.is_available() else ("cpu" if args.device == "auto" else args.device))
    torch.manual_seed(args.seed)
    rng = np.random.default_rng(args.seed)
    os.makedirs(args.out, exist_ok=True)

    train_data, val_data = build_pool(files, args.val_fraction)  # one disk pass, total, regardless of --epochs
    if train_data is None:
        raise SystemExit("no training rows (are there finished chunks and more than one game?)")
    train_obs, train_cand, train_outcome, _train_game = train_data
    val_obs, val_cand, val_outcome, _val_game = val_data if val_data is not None else (None, None, None, None)

    class PhaseNet(nn.Module):
        def __init__(self, mean, std):
            super().__init__()
            self.register_buffer("mean", torch.as_tensor(mean))
            self.register_buffer("std", torch.as_tensor(std))
            self.obs_layer = nn.Linear(obs_size, hidden[0])
            self.cand_layer = nn.Linear(cand_features, hidden[0], bias=False)
            self.mid = nn.Linear(hidden[0], hidden[1])
            self.out = nn.Linear(hidden[1], 1)

        def forward(self, obs, cand):  # one candidate per row -> logits [B]
            z = torch.clamp((obs - self.mean) / self.std, -CLIP, CLIP)
            h = torch.relu(self.obs_layer(z) + self.cand_layer(cand))
            return self.out(torch.relu(self.mid(h))).squeeze(-1)

    checkpoint_path = os.path.join(args.out, "checkpoint.pt")
    start_epoch = 0
    resumed = None
    if os.path.exists(checkpoint_path):
        resumed = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
        if resumed["schema_hash"] != schema["hash"] or resumed["hidden"] != hidden:
            print("checkpoint of a different schema/architecture is ignored", file=sys.stderr)
            resumed = None
    if resumed is not None:
        mean, std, rows_train = resumed["mean"], resumed["std"], resumed["rows_train"]
        start_epoch = resumed["epoch"]
        print(f"resuming from the checkpoint after epoch {start_epoch}")
    else:
        mean, std, rows_train = standardisation_from_pool(train_obs)
    model = PhaseNet(mean, std).to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    if resumed is not None:
        model.load_state_dict(resumed["model"])
        optimizer.load_state_dict(resumed["optimizer"])
    elif args.prev and os.path.exists(os.path.join(args.prev, "model.pt")):
        previous = torch.load(os.path.join(args.prev, "model.pt"), map_location="cpu", weights_only=False)
        if previous["schema_hash"] == schema["hash"] and previous["hidden"] == hidden:
            model.load_state_dict(previous["model"])  # continue from the best model (its own standardisation is kept inside)
            print("warm start from " + args.prev)
        else:
            print("the previous model has another schema/architecture: training from scratch")

    loss_fn = nn.BCEWithLogitsLoss()

    def evaluate():
        model.eval()
        total, n, base_total, shuffled_total = 0.0, 0, 0.0, 0.0
        mean_outcome = float(val_outcome.mean()) if val_outcome is not None and len(val_outcome) else 0.5
        p = min(max(mean_outcome, 1e-4), 1 - 1e-4)
        with torch.no_grad():
            if val_obs is not None:
                for s in range(0, len(val_obs), BLOCK_ROWS):
                    obs, cand, outcome = val_obs[s:s + BLOCK_ROWS], val_cand[s:s + BLOCK_ROWS], val_outcome[s:s + BLOCK_ROWS]
                    logits = model(torch.from_numpy(obs).to(device), torch.from_numpy(cand).to(device))
                    target = torch.from_numpy(outcome).to(device)
                    total += float(nn.functional.binary_cross_entropy_with_logits(logits, target, reduction="sum"))
                    # the same rows with the candidates swapped between the rows: how much does the model really use WHAT was chosen?
                    swapped = torch.from_numpy(cand).to(device)[torch.randperm(len(cand), device=device)]
                    shuffled_total += float(nn.functional.binary_cross_entropy_with_logits(model(torch.from_numpy(obs).to(device), swapped), target, reduction="sum"))
                    base_total += float(-(outcome * math.log(p) + (1 - outcome) * math.log(1 - p)).sum())
                    n += len(outcome)
        model.train()
        return (total / n, base_total / n, n, shuffled_total / n) if n else (float("nan"), float("nan"), 0, float("nan"))

    history = []
    for epoch in range(start_epoch, args.epochs):
        model.train()
        running, seen = 0.0, 0
        # one global shuffle of the whole in-RAM pool per epoch (strictly more mixing than the old per-block shuffle,
        # since there is no disk-sized block to stay within any more)
        permutation = rng.permutation(len(train_obs))
        for s in range(0, len(permutation), args.batch):
            idx = permutation[s:s + args.batch]
            logits = model(torch.from_numpy(train_obs[idx]).to(device), torch.from_numpy(train_cand[idx]).to(device))
            loss = loss_fn(logits, torch.from_numpy(train_outcome[idx]).to(device))
            if not torch.isfinite(loss):
                print(f"the loss is {float(loss)} in epoch {epoch}: the training diverged", file=sys.stderr)
                return 4
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            running += float(loss) * len(idx)
            seen += len(idx)
        val_loss, base_loss, val_rows, shuffled_loss = evaluate()
        history.append({"epoch": epoch + 1, "train_loss": running / max(seen, 1), "val_loss": val_loss, "val_baseline_loss": base_loss, "val_rows": val_rows,
                        "val_loss_with_swapped_candidates": shuffled_loss, "action_effect": shuffled_loss - val_loss})
        print(json.dumps(history[-1]), flush=True)
        tmp = checkpoint_path + ".tmp"
        torch.save({"model": model.state_dict(), "optimizer": optimizer.state_dict(), "epoch": epoch + 1, "schema_hash": schema["hash"],
                    "hidden": hidden, "mean": mean, "std": std, "rows_train": rows_train}, tmp)
        os.replace(tmp, checkpoint_path)

    # ---- export, then verify the exported files with an independent implementation
    model.eval()
    sd = {k: v.detach().cpu().numpy() for k, v in model.state_dict().items()}
    params = {"mean": sd["mean"], "std": sd["std"], "w_obs": sd["obs_layer.weight"], "b_obs": sd["obs_layer.bias"], "w_cand": sd["cand_layer.weight"],
              "w_mid": sd["mid.weight"], "b_mid": sd["mid.bias"], "w_out": sd["out.weight"].reshape(-1), "b_out": sd["out.bias"].reshape(-1)}
    sample_obs, sample_cands = None, None
    if val_obs is not None and len(val_obs):
        sample_obs, sample_cands = val_obs[:16], val_cand[:16]
    elif len(train_obs):
        sample_obs, sample_cands = train_obs[:16], train_cand[:16]
    else:
        sample_obs, sample_cands = first["obs"][:16].astype(np.float32), first["feat"][np.arange(min(16, len(first))), first["chosen"][:16].astype(int)].astype(np.float32)
    with torch.no_grad():
        torch_logits = model(torch.from_numpy(sample_obs).to(device), torch.from_numpy(sample_cands).to(device)).cpu().numpy()
    reference = [{"obs": o.tolist(), "cands": [c.tolist()], "logits": [float(l)]} for o, c, l in zip(sample_obs, sample_cands, torch_logits)]
    export_from_arrays(args.out, schema["hash"], obs_size, cand_features, hidden, CLIP, params, reference, decision_type=schema["type"])

    meta, loaded = load_exported(args.out)
    worst = 0.0
    for o, c, l in zip(sample_obs, sample_cands, torch_logits):
        worst = max(worst, abs(float(numpy_forward(loaded, meta["clip"], o.astype(np.float64), c.astype(np.float64)[None, :])[0]) - float(l)))
    if worst > 1e-3:
        print(f"the exported model disagrees with PyTorch by {worst}: refusing to finish", file=sys.stderr)
        return 5
    torch.save({"model": model.state_dict(), "schema_hash": schema["hash"], "hidden": hidden}, os.path.join(args.out, "model.pt"))
    last = history[-1] if history else {}
    with open(os.path.join(args.out, "train_metrics.json"), "w") as f:
        json.dump({"rows_train": int(rows_train), "chunks": len(files), "seconds": round(time.time() - started, 1), "export_max_abs_diff": worst,
                   "device": str(device), "history": history,
                   "beats_baseline": bool(last and last["val_loss"] < last["val_baseline_loss"]),
                   "action_effect": last.get("action_effect") if last else None}, f, indent=1)
    with open(os.path.join(args.out, "DONE"), "w") as f:
        f.write("done")
    if last:
        print(f"done: val loss {last['val_loss']:.4f} vs baseline {last['val_baseline_loss']:.4f}; action effect {last['action_effect']:.4f} "
              f"(loss growth when the chosen candidate is swapped between rows; ~0 means the model ignores the action); export check {worst:.2e}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
