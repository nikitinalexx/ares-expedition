"""Trainer of the PICK_PHASE model for the night pipeline (follows the trainer contract of docs/NIGHTLY.md).

    python ml/train_phase.py --data DIR1,DIR2 --out MODEL_DIR [--prev PREVIOUS_MODEL_DIR] [--epochs 3] [--hidden 256,128]

What is learned: the probability that the player WINS the game given what it saw and the phase it picked
(binary cross entropy against the result of the game: 1 win, 0.5 draw, 0 loss). Only the chosen candidate of every
row is a training example. Rows are streamed chunk by chunk from the disk (memmap): the RAM holds one block, not the data.

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
from ares_data import chunk_paths, open_memmap, validate  # noqa: E402
from phase_model import export_from_arrays, load_exported, numpy_forward  # noqa: E402

BLOCK_ROWS = 100_000
CLIP = 5.0


def is_validation_game(game_ids, fraction):
    """Deterministic split by game: the same game is always on the same side, in every epoch and every iteration."""
    mixed = (game_ids.astype(np.uint64) * np.uint64(0x9E3779B97F4A7C15)) >> np.uint64(40)
    return (mixed % np.uint64(1000)) < np.uint64(int(fraction * 1000))


def list_chunks(data_dirs):
    files = []
    for directory in [d for d in data_dirs.split(",") if d]:
        files += chunk_paths(directory, "PICK_PHASE")
    return files


def iterate_blocks(files, validation, fraction, rng, shuffle_files):
    """Yields (obs float32 [n, obs], cand float32 [n, F], outcome float32 [n]) of the chosen candidates of one side of the split."""
    order = list(range(len(files)))
    if shuffle_files:
        rng.shuffle(order)
    for index in order:
        _, rows = open_memmap(files[index])
        for start in range(0, len(rows), BLOCK_ROWS):
            block = np.asarray(rows[start:start + BLOCK_ROWS])
            mask = is_validation_game(block["game"], fraction)
            if not validation:
                mask = ~mask
            if not mask.any():
                continue
            block = block[mask]
            chosen = block["chosen"].astype(np.int64)
            yield (block["obs"].astype(np.float32), block["feat"][np.arange(len(block)), chosen].astype(np.float32),
                   block["outcome"].astype(np.float32))


def standardisation(files, fraction):
    """Mean and std of every observation feature over the TRAINING games (streamed, float64 accumulators)."""
    count, total, total_sq = 0, None, None
    for obs, _, _ in iterate_blocks(files, False, fraction, None, False):
        o = obs.astype(np.float64)
        total = o.sum(0) if total is None else total + o.sum(0)
        total_sq = (o * o).sum(0) if total_sq is None else total_sq + (o * o).sum(0)
        count += len(o)
    if count == 0:
        raise SystemExit("no training rows (are there finished chunks and more than one game?)")
    mean = total / count
    std = np.sqrt(np.maximum(total_sq / count - mean * mean, 0.0))
    # a feature that never changes carries no information: std 1 turns it into a constant zero instead of a division by ~0
    std = np.where(std < 1e-3, 1.0, std)
    return mean.astype(np.float32), std.astype(np.float32), count


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
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
    files = list_chunks(args.data)
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
        mean, std, rows_train = standardisation(files, args.val_fraction)
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
        total, n, base_total, outcome_sum, shuffled_total = 0.0, 0, 0.0, 0.0, 0.0
        batches = list(iterate_blocks(files, True, args.val_fraction, None, False))
        mean_outcome = float(np.concatenate([b[2] for b in batches]).mean()) if batches else 0.5
        with torch.no_grad():
            for obs, cand, outcome in batches:
                logits = model(torch.from_numpy(obs).to(device), torch.from_numpy(cand).to(device))
                target = torch.from_numpy(outcome).to(device)
                total += float(nn.functional.binary_cross_entropy_with_logits(logits, target, reduction="sum"))
                # the same rows with the candidates swapped between the rows: how much does the model really use WHAT was chosen?
                swapped = torch.from_numpy(cand).to(device)[torch.randperm(len(cand), device=device)]
                shuffled_total += float(nn.functional.binary_cross_entropy_with_logits(model(torch.from_numpy(obs).to(device), swapped), target, reduction="sum"))
                p = min(max(mean_outcome, 1e-4), 1 - 1e-4)
                base_total += float(-(outcome * math.log(p) + (1 - outcome) * math.log(1 - p)).sum())
                n += len(outcome)
        model.train()
        return (total / n, base_total / n, n, shuffled_total / n) if n else (float("nan"), float("nan"), 0, float("nan"))

    history = []
    for epoch in range(start_epoch, args.epochs):
        model.train()
        running, seen = 0.0, 0
        for obs, cand, outcome in iterate_blocks(files, False, args.val_fraction, rng, True):
            permutation = rng.permutation(len(obs))
            for s in range(0, len(obs), args.batch):
                idx = permutation[s:s + args.batch]
                logits = model(torch.from_numpy(obs[idx]).to(device), torch.from_numpy(cand[idx]).to(device))
                loss = loss_fn(logits, torch.from_numpy(outcome[idx]).to(device))
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
    for obs, cand, _ in iterate_blocks(files, True, args.val_fraction, None, False):
        sample_obs, sample_cands = obs[:16], cand[:16]
        break
    if sample_obs is None:
        sample_obs, sample_cands = first["obs"][:16].astype(np.float32), first["feat"][np.arange(min(16, len(first))), first["chosen"][:16].astype(int)].astype(np.float32)
    with torch.no_grad():
        torch_logits = model(torch.from_numpy(sample_obs).to(device), torch.from_numpy(sample_cands).to(device)).cpu().numpy()
    reference = [{"obs": o.tolist(), "cands": [c.tolist()], "logits": [float(l)]} for o, c, l in zip(sample_obs, sample_cands, torch_logits)]
    export_from_arrays(args.out, schema["hash"], obs_size, cand_features, hidden, CLIP, params, reference)

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
              f"(loss growth when the chosen phase is swapped between rows; ~0 means the model ignores the action); export check {worst:.2e}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
