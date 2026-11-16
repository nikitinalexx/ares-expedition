"""Train one Q-network per decision type that has finished chunks, with a shared observation trunk.

    python ml/train_all.py --data DIR1,DIR2 --out MODEL_DIR [--prev PREV] [--epochs 3] [--hidden 256,128]
    python ml/train_all.py ... --independent   # fallback: call train_phase.py once per type (no shared trunk)

Layout written to MODEL_DIR (Java loads it with -Dares.agent.models=MODEL_DIR):
    MODEL_DIR/<DECISION_TYPE>/{model.json,weights.bin,model.pt,train_metrics.json,DONE}
    MODEL_DIR/DONE
    MODEL_DIR/train_metrics.json

Shared trunk (default): one mean/std and W_obs for every type; each type keeps its own candidate head.
Data: ONE sequential buffered read of every chunk file builds the in-RAM train AND validation pool together (at most
--rows-per-game rows of a game per type); every batch is then drawn at random from the whole pool, so it mixes thousands
of games (hypothesis H-D1 in docs/EXPERIMENTS.md: the rows of one game in a row are memorised; --feed sequential is the
ablation that tests it). The read uses a single `file.read()` per chunk (ares_data.read_decisions), not `np.memmap`: for a
once-through sequential scan — which this is — a plain buffered read lets the OS do normal read-ahead, while `np.memmap`
services the same scan as a stream of small page faults; on a spinning disk the difference is the gap between a few
seconds and several minutes (see docs/EXPERIMENTS.md, "I/O"). Building train and val in the same pass (not two separate
passes) also halves the bytes read.
Independent mode: each type is a full separate network (same as repeated train_phase.py).
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import chunk_paths, discover_decisions, open_memmap, read_decisions, validate  # noqa: E402
from phase_model import export_from_arrays, load_exported, numpy_forward  # noqa: E402
from train_phase import BLOCK_ROWS, CLIP, is_validation_game, list_chunks  # noqa: E402


def load_type_schemas(data, decisions):
    """Returns {type: (schema, files)} for types that have readable finished chunks."""
    result = {}
    for decision in decisions:
        files = list_chunks(data, decision)
        if not files:
            continue
        schema, first = open_memmap(files[0])
        problems = validate(schema, first[: min(len(first), 20000)])
        if problems:
            raise SystemExit(f"{files[0]}: {problems}")
        for path in files[1:]:
            other, _ = open_memmap(path)
            if other["hash"] != schema["hash"]:
                raise SystemExit(f"{path} schema {other['hash']} != {schema['hash']}")
            if other["type"] != decision:
                raise SystemExit(f"{path} declares type {other['type']}, expected {decision}")
        result[decision] = (schema, files)
    return result


MANIFEST = None  # the opened pack, when --packed is used
SEQUENTIAL_RUN = 25  # night 1: a 100k-row block of ~250 games = ~25 batches of 4096 in a row


def cap_per_game(games, k, rng):
    """Positions of at most k randomly chosen rows of every game (sorted, so a memmap is read forward)."""
    order = np.lexsort((rng.random(len(games)), games))
    g = games[order]
    first = np.r_[True, g[1:] != g[:-1]]
    group_start = np.maximum.accumulate(np.where(first, np.arange(len(g)), 0))
    return np.sort(order[(np.arange(len(g)) - group_start) < k])


def build_pools(files, fraction, rng, rows_per_game):
    """The rows of one decision type for training AND validation in RAM: {(obs f16, chosen candidate f32, outcome f32, game)}.

    ONE sequential buffered read per file (ares_data.read_decisions — a single `file.read()`, not `np.memmap`: see the
    module docstring for why that matters on a hard disk). The rows of a game are near-duplicates with the same label, so
    at most `rows_per_game` random rows of each game are kept: batches drawn from the pool then mix thousands of games
    instead of feeding hundreds of steps of the same few games in a row (which is memorised, not learned).

    Returns (train_pool, val_pool); either is None if that side has no rows."""
    collected = {False: ([], [], [], []), True: ([], [], [], [])}
    for path in files:
        _, rows = read_decisions(path)
        for start in range(0, len(rows), BLOCK_ROWS):
            block = rows[start:start + BLOCK_ROWS]
            games = np.asarray(block["game"])
            side = is_validation_game(games, fraction)
            for validation, mask in ((False, ~side), (True, side)):
                positions = np.nonzero(mask)[0]
                if rows_per_game:
                    positions = positions[cap_per_game(games[positions], rows_per_game, rng)]
                if not len(positions):
                    continue
                picked = np.asarray(block[positions])
                chosen = picked["chosen"].astype(np.int64)
                obs, cand, outcome, game = collected[validation]
                obs.append(picked["obs"].astype(np.float16))
                cand.append(picked["feat"][np.arange(len(picked)), chosen].astype(np.float32))
                outcome.append(picked["outcome"].astype(np.float32))
                game.append(games[positions])

    def finish(parts):
        obs, cand, outcome, game = parts
        if not obs:
            return None
        return np.concatenate(obs), np.concatenate(cand), np.concatenate(outcome), np.concatenate(game)

    return finish(collected[False]), finish(collected[True])


def build_packed_pools(packed_dir, decision, fraction, rng, rows_per_game, manifest):
    """The same pools as build_pools, from a pack: obs + the candidate each row took, never the padded block.

    The shared-trunk trainer and the per-type trainer both read only the chosen candidate, so the pack can serve them
    directly. The raw loader needed one padded read of every chunk per run (119.7 GiB for the 150k-game corpus); this
    reads the observation file and gathers the chosen rows, which is 26 GiB for the same corpus.
    """
    import ares_data

    data = ares_data.open_packed_type(packed_dir, decision, manifest)
    if "cands" not in data:
        raise SystemExit(f"{packed_dir}: {decision} was packed without --candidates")
    total = len(data["obs"])
    collected = {False: ([], [], [], []), True: ([], [], [], [])}
    for start in range(0, total, BLOCK_ROWS):
        stop = min(start + BLOCK_ROWS, total)
        rows = np.arange(start, stop)
        games = np.asarray(data["game"][rows])
        side = is_validation_game(games, fraction)
        for validation, mask in ((False, ~side), (True, side)):
            positions = np.nonzero(mask)[0]
            if rows_per_game and len(positions):
                positions = positions[cap_per_game(games[positions], rows_per_game, rng)]
            if not len(positions):
                continue
            picked = rows[positions]
            obs, cand, outcome, game = collected[validation]
            obs.append(data["obs"][picked])
            cand.append(data["cands"].chosen(picked))
            outcome.append(np.asarray(data["outcome"])[picked].astype(np.float32))
            game.append(np.asarray(data["game"])[picked])

    def finish(parts):
        obs, cand, outcome, game = parts
        if not obs:
            return None
        return np.concatenate(obs), np.concatenate(cand), np.concatenate(outcome), np.concatenate(game)

    return finish(collected[False]), finish(collected[True])


def train_independent(args, type_files):
    """One subprocess of train_phase.py per type. No shared weights."""
    results = {}
    for decision, (schema, files) in type_files.items():
        out = os.path.join(args.out, decision)
        os.makedirs(out, exist_ok=True)
        prev = os.path.join(args.prev, decision) if args.prev and os.path.isdir(os.path.join(args.prev, decision)) else ""
        cmd = [sys.executable, os.path.join(os.path.dirname(__file__), "train_phase.py"),
               ("--packed" if args.packed else "--data"), (args.packed or args.data), "--out", out, "--decision", decision,
               "--epochs", str(args.epochs), "--hidden", args.hidden, "--lr", str(args.lr),
               "--weight-decay", str(args.weight_decay), "--batch", str(args.batch),
               "--val-fraction", str(args.val_fraction), "--seed", str(args.seed), "--device", args.device]
        if prev:
            cmd += ["--prev", prev]
        print(f"=== independent train {decision} ({len(files)} chunks) ===", flush=True)
        proc = subprocess.run(cmd)
        if proc.returncode != 0:
            return proc.returncode, results
        with open(os.path.join(out, "train_metrics.json"), encoding="utf-8") as f:
            results[decision] = json.load(f)
    return 0, results


def train_shared(args, type_files):
    """Joint training: shared observation encoder, per-type candidate heads."""
    import torch
    from torch import nn

    started = time.time()
    device = torch.device("cuda" if args.device == "auto" and torch.cuda.is_available() else ("cpu" if args.device == "auto" else args.device))
    torch.manual_seed(args.seed)
    rng = np.random.default_rng(args.seed)
    hidden = [int(x) for x in args.hidden.split(",")]
    h1, h2 = hidden
    decisions = list(type_files.keys())
    schemas = {d: type_files[d][0] for d in decisions}
    files_by = {d: type_files[d][1] for d in decisions}
    # Each DecisionType has its own schema hash (candidate layout is part of the hash). The shared trunk only
    # requires the same observation vector width; per-type hashes are kept when exporting each head.
    obs_size = schemas[decisions[0]]["obsSize"]
    for d in decisions:
        if schemas[d]["obsSize"] != obs_size:
            raise SystemExit(f"{d} has obsSize {schemas[d]['obsSize']}, expected {obs_size}")

    pool_rng = np.random.default_rng(args.seed)  # the same pools after a resume
    train_pool, val_pool = {}, {}
    for d in decisions:
        if args.packed:
            train_pool[d], val_pool[d] = build_packed_pools(args.packed, d, args.val_fraction, pool_rng,
                                                            args.rows_per_game, MANIFEST)
        else:
            train_pool[d], val_pool[d] = build_pools(files_by[d], args.val_fraction, pool_rng, args.rows_per_game)
    sizes = {d: (0 if train_pool[d] is None else len(train_pool[d][0]), 0 if val_pool[d] is None else len(val_pool[d][0])) for d in decisions}
    n_train = sum(s[0] for s in sizes.values())
    if n_train == 0:
        raise SystemExit("no training rows (are there finished chunks and more than one game?)")
    games = len(np.unique(np.concatenate([train_pool[d][3] for d in decisions if train_pool[d] is not None])))
    gb = sum(p[0].nbytes + p[1].nbytes for pools in (train_pool, val_pool) for p in pools.values() if p is not None) / 1e9
    val_games = [val_pool[d][3] for d in decisions if val_pool[d] is not None]
    label_sum = sum(float(train_pool[d][2].sum()) for d in decisions if train_pool[d] is not None)
    pool_info = {"pool_train_rows": n_train, "pool_val_rows": sum(s[1] for s in sizes.values()), "train_games": games,
                 "val_games": len(np.unique(np.concatenate(val_games))) if val_games else 0,
                 "rows_per_game_mean": round(n_train / games, 1), "rows_per_game_cap": args.rows_per_game,
                 "label_mean": round(label_sum / n_train, 4), "pool_gb": round(gb, 2), "feed": args.feed,
                 "train_rows_by_type": {d: s[0] for d, s in sizes.items()}}
    print(json.dumps(pool_info), flush=True)

    # Advantage weighting (night 8's lesson, docs/EXPERIMENTS.md): a binary outcome applied to the single chosen
    # candidate is the same label for every decision of a game, so training on games the agent loses mostly teaches
    # "this game was lost" -- which is a statement about the state, not about the move, and it blurs an already good
    # candidate ranking. V(s) is fitted on a held-out quarter of the *same* games (no extra data, no leakage) and every
    # policy row is weighted by its advantage outcome - V(s): rows where the game went better than the state promised
    # keep their weight, the rest are down-weighted (--adv-negative).
    adv_weights = None
    if args.adv_weight:
        from train_phase import is_validation_game
        from train_value import fit_value, predict
        value_rows = {d: (np.zeros(0, dtype=bool) if train_pool[d] is None
                          else is_validation_game(train_pool[d][3], args.adv_holdout)) for d in decisions}
        value_obs = np.concatenate([train_pool[d][0][value_rows[d]] for d in decisions if train_pool[d] is not None])
        value_outcome = np.concatenate([train_pool[d][2][value_rows[d]] for d in decisions if train_pool[d] is not None])
        value_games = np.concatenate([train_pool[d][3][value_rows[d]] for d in decisions if train_pool[d] is not None])
        if len(value_obs) < 500:
            raise SystemExit(f"only {len(value_obs)} states for the value model; raise --adv-holdout")
        inner_val = is_validation_game(value_games, 0.2)
        fitted = fit_value(value_obs, value_outcome, hidden=[int(x) for x in args.adv_hidden.split(",")],
                           epochs=args.adv_epochs, lr=1e-3, seed=args.seed, device=str(device),
                           train_idx=np.flatnonzero(~inner_val), val_idx=np.flatnonzero(inner_val),
                           label_mean=float(value_outcome[~inner_val].mean()))
        adv_weights = {}
        positive, kept = 0, 0
        for d in decisions:
            if train_pool[d] is None:
                adv_weights[d] = None
                continue
            v_all = predict(fitted["params"], train_pool[d][0]).astype(np.float32)
            advantage = train_pool[d][2] - v_all
            w = np.where(value_rows[d], 0.0, np.where(advantage > 0, 1.0, args.adv_negative)).astype(np.float32)
            policy = ~value_rows[d]
            positive += int((advantage[policy] > 0).sum())
            kept += int(policy.sum())
            adv_weights[d] = w / max(float(w.mean()), 1e-9)  # mean weight 1: the learning rate stays comparable
        print(json.dumps({"adv_holdout": args.adv_holdout, "value_rows": int(len(value_obs)),
                          "value_auc": round(float(fitted["history"][-1]["auc"]), 4),
                          "value_best_epoch": fitted["best_epoch"],
                          "policy_rows": kept, "adv_positive_fraction": round(positive / max(kept, 1), 4),
                          "adv_negative_weight": args.adv_negative}), flush=True)

    checkpoint_path = os.path.join(args.out, "checkpoint_shared.pt")
    start_epoch = 0
    resumed = None
    if os.path.exists(checkpoint_path):
        resumed = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
        if (resumed.get("obs_size") != obs_size or resumed.get("hidden") != hidden
                or set(resumed.get("decisions", [])) != set(decisions)):
            print("shared checkpoint ignored (obs size / architecture / types differ)", file=sys.stderr)
            resumed = None
    if resumed is not None:
        mean, std = resumed["mean"], resumed["std"]
        start_epoch = resumed["epoch"]
        print(f"resuming shared trunk from epoch {start_epoch}")
    else:
        total, total_sq = np.zeros(obs_size), np.zeros(obs_size)
        for p in train_pool.values():
            if p is None:
                continue
            for s in range(0, len(p[0]), BLOCK_ROWS):
                o = p[0][s:s + BLOCK_ROWS].astype(np.float64)
                total += o.sum(0)
                total_sq += (o * o).sum(0)
        mean = total / n_train
        std = np.sqrt(np.maximum(total_sq / n_train - mean * mean, 0.0))
        # a feature that never changes carries no information: std 1 turns it into a constant zero instead of a division by ~0
        std = np.where(std < 1e-3, 1.0, std)
        mean, std = mean.astype(np.float32), std.astype(np.float32)

    class Head(nn.Module):
        def __init__(self, cand_features):
            super().__init__()
            self.cand = nn.Linear(cand_features, h1, bias=False)
            self.mid = nn.Linear(h1, h2)
            self.out = nn.Linear(h2, 1)

        def forward(self, trunk, cand):
            h = torch.relu(trunk + self.cand(cand))
            return self.out(torch.relu(self.mid(h))).squeeze(-1)

    class SharedNet(nn.Module):
        def __init__(self):
            super().__init__()
            self.register_buffer("mean", torch.as_tensor(mean))
            self.register_buffer("std", torch.as_tensor(std))
            self.obs_layer = nn.Linear(obs_size, h1)
            self.heads = nn.ModuleDict({d: Head(schemas[d]["candidateFeatures"]) for d in decisions})

        def trunk(self, obs):
            z = torch.clamp((obs - self.mean) / self.std, -CLIP, CLIP)
            return self.obs_layer(z)

        def forward(self, decision, obs, cand):
            return self.heads[decision](self.trunk(obs), cand)

    model = SharedNet().to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    if resumed is not None:
        model.load_state_dict(resumed["model"])
        optimizer.load_state_dict(resumed["optimizer"])
    elif args.prev:
        loaded_any = False
        for d in decisions:
            prev_pt = os.path.join(args.prev, d, "model.pt")
            if not os.path.exists(prev_pt):
                continue
            previous = torch.load(prev_pt, map_location="cpu", weights_only=False)
            sd = previous["model"]
            same_candidates = previous.get("hidden") == hidden and all(
                key in sd and sd[key].shape == target.shape
                for key, target in (
                    ("cand_layer.weight", model.heads[d].cand.weight),
                    ("mid.weight", model.heads[d].mid.weight),
                    ("out.weight", model.heads[d].out.weight)))
            if not same_candidates:
                continue
            # The observation may have GROWN since that model was trained (columns are only ever appended, never
            # reordered). The new columns start with weight 0 and mean 0 / std 1, so the warm start is exactly the old
            # model until training gives the new columns a meaning. This is the trainer's job: the engine itself refuses
            # a model whose observation does not match its encoder.
            src = sd["obs_layer.weight"]
            with torch.no_grad():
                if src.shape[0] == model.obs_layer.weight.shape[0] and src.shape[1] <= model.obs_layer.weight.shape[1]:
                    target = model.obs_layer.weight
                    target.zero_()
                    target[:, :src.shape[1]].copy_(src)
                    model.obs_layer.bias.copy_(sd["obs_layer.bias"])
                    old = src.shape[1]
                    model.mean.zero_()
                    model.mean[:old].copy_(sd["mean"])
                    model.std.fill_(1.0)
                    model.std[:old].copy_(sd["std"])
                    if old != model.mean.numel():
                        print(f"warm start: {model.mean.numel() - old} new observation columns start at zero")
                # train_phase / exported shared state_dict keys: cand_layer, mid, out
                model.heads[d].cand.weight.copy_(sd["cand_layer.weight"])
                model.heads[d].mid.weight.copy_(sd["mid.weight"])
                model.heads[d].mid.bias.copy_(sd["mid.bias"])
                model.heads[d].out.weight.copy_(sd["out.weight"])
                model.heads[d].out.bias.copy_(sd["out.bias"])
                loaded_any = True
        if loaded_any:
            print("warm start of shared trunk / heads from " + args.prev)

    # night 8 evidence: fine-tuning a model from RANDOM data (label mean 0.85) on SMART data (label mean 0.26) first
    # recalibrates the value and drags the observation projection with it, which wrecks the candidate ranking (86.7% ->
    # 78.6% vs RANDOM). Freezing the projection lets the heads absorb the calibration instead. It is applied after the
    # optimizer state is restored, so a resumed run keeps the same parameter groups.
    if args.freeze_trunk:
        for p in model.obs_layer.parameters():
            p.requires_grad = False
        print(f"trunk frozen: the observation projection stays at the warm start, only the {len(decisions)} heads train")

    loss_fn = nn.BCEWithLogitsLoss()

    def batches():
        """Endless batches. mixed: each pass permutes every type's pool and interleaves the batches of all types at random.
        sequential (ablation, how night 1 fed the data): rows in recording order, so the rows of a game come in a row, in runs of
        SEQUENTIAL_RUN batches of one type; only the order of the runs is random."""
        while True:
            if args.feed == "mixed":
                plan = []
                for d in decisions:
                    if train_pool[d] is None:
                        continue
                    permutation = rng.permutation(len(train_pool[d][0]))
                    plan += [(d, permutation[s:s + args.batch]) for s in range(0, len(permutation), args.batch)]
                for i in rng.permutation(len(plan)):
                    yield plan[i]
            else:
                runs = []
                for d in decisions:
                    if train_pool[d] is None:
                        continue
                    n = len(train_pool[d][0])
                    in_order = [(d, np.arange(s, min(n, s + args.batch))) for s in range(0, n, args.batch)]
                    runs += [in_order[s:s + SEQUENTIAL_RUN] for s in range(0, len(in_order), SEQUENTIAL_RUN)]
                for i in rng.permutation(len(runs)):
                    yield from runs[i]

    def tensor(array, idx=None):
        return torch.from_numpy((array if idx is None else array[idx]).astype(np.float32)).to(device)

    batches_per_pass = sum(-(-s[0] // args.batch) for s in sizes.values())
    steps = args.steps or batches_per_pass  # batches between two validations ("epoch")
    stream = batches()
    for _ in range(start_epoch * steps):  # a resumed run continues the same stream
        next(stream)

    history = list(resumed.get("history", [])) if resumed is not None else []
    best_val = resumed.get("best_val", float("inf")) if resumed is not None else float("inf")
    best_state = resumed.get("best_state") if resumed is not None else None
    best_epoch = resumed.get("best_epoch", 0) if resumed is not None else 0
    for epoch in range(start_epoch, args.epochs):
        if best_epoch and epoch - best_epoch >= args.patience:
            print(f"early stop: no val_loss improvement for {args.patience} epochs (best epoch {best_epoch})", flush=True)
            break
        model.train()
        running, seen = 0.0, 0
        for _ in range(steps):
            d, idx = next(stream)
            obs, cand, outcome, _ = train_pool[d]
            logits = model(d, tensor(obs, idx), tensor(cand, idx))
            if adv_weights is not None:
                target = tensor(outcome, idx)
                weight = tensor(adv_weights[d], idx)
                loss = (nn.functional.binary_cross_entropy_with_logits(logits, target, reduction="none")
                        * weight).sum() / weight.sum().clamp_min(1e-9)
            else:
                loss = loss_fn(logits, tensor(outcome, idx))
            if not torch.isfinite(loss):
                print(f"loss diverged on {d} in epoch {epoch}", file=sys.stderr)
                return 4, {}
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            running += float(loss) * len(idx)
            seen += len(idx)

        # validation: mean loss over types that have val rows
        model.eval()
        val_parts, constant_parts = [], []
        with torch.no_grad():
            for d in decisions:
                total, n, wins = 0.0, 0, 0.0
                if val_pool[d] is None:
                    continue
                v_obs, v_cand, v_out, _ = val_pool[d]
                for s in range(0, len(v_obs), 65536):
                    outcome = v_out[s:s + 65536]
                    logits = model(d, tensor(v_obs[s:s + 65536]), tensor(v_cand[s:s + 65536]))
                    total += float(nn.functional.binary_cross_entropy_with_logits(
                        logits, tensor(outcome), reduction="sum"))
                    n += len(outcome)
                    wins += float(outcome.sum())
                if n:
                    val_parts.append(total / n)
                    p = min(max(wins / n, 1e-6), 1 - 1e-6)
                    constant_parts.append(-(p * np.log(p) + (1 - p) * np.log(1 - p)))
        model.train()
        val_loss = float(np.mean(val_parts)) if val_parts else float("nan")
        # the loss of always predicting the val win rate: a model above it has learned nothing that generalises
        constant_loss = float(np.mean(constant_parts)) if constant_parts else float("nan")
        history.append({"epoch": epoch + 1, "train_loss": running / max(seen, 1), "val_loss": val_loss,
                        "constant_val_loss": constant_loss, "rows_seen": seen})
        print(json.dumps(history[-1]), flush=True)
        # no val rows (tiny data) -> every epoch counts as the best, i.e. the last one is exported
        if not (val_loss >= best_val):
            best_val, best_epoch = val_loss, epoch + 1
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}
        tmp = checkpoint_path + ".tmp"
        torch.save({"model": model.state_dict(), "optimizer": optimizer.state_dict(), "epoch": epoch + 1,
                    "obs_size": obs_size, "hidden": hidden, "mean": mean, "std": std,
                    "decisions": decisions, "history": history,
                    "best_val": best_val, "best_epoch": best_epoch, "best_state": best_state}, tmp)
        os.replace(tmp, checkpoint_path)

    # export the epoch with the lowest validation loss, not the last (later epochs overfit)
    if best_state is not None:
        model.load_state_dict(best_state)
        print(f"exporting epoch {best_epoch} (val_loss {best_val:.4f})", flush=True)
    model.eval()
    sd = {k: v.detach().cpu().numpy() for k, v in model.state_dict().items()}
    results = {}
    for d in decisions:
        schema = schemas[d]
        params = {
            "mean": sd["mean"], "std": sd["std"],
            "w_obs": sd["obs_layer.weight"], "b_obs": sd["obs_layer.bias"],
            "w_cand": sd[f"heads.{d}.cand.weight"],
            "w_mid": sd[f"heads.{d}.mid.weight"], "b_mid": sd[f"heads.{d}.mid.bias"],
            "w_out": sd[f"heads.{d}.out.weight"].reshape(-1), "b_out": sd[f"heads.{d}.out.bias"].reshape(-1),
        }
        out = os.path.join(args.out, d)
        os.makedirs(out, exist_ok=True)
        sample_obs, sample_cands = None, None
        sample_pool = val_pool[d] if val_pool[d] is not None else train_pool[d]
        if sample_pool is not None:
            sample_obs, sample_cands = sample_pool[0][:16].astype(np.float32), sample_pool[1][:16].astype(np.float32)
        if sample_obs is None:
            _, first = open_memmap(files_by[d][0])
            sample_obs = first["obs"][:16].astype(np.float32)
            sample_cands = first["feat"][np.arange(min(16, len(first))), first["chosen"][:16].astype(int)].astype(np.float32)
        with torch.no_grad():
            torch_logits = model(d, torch.from_numpy(sample_obs).to(device), torch.from_numpy(sample_cands).to(device)).cpu().numpy()
        reference = [{"obs": o.tolist(), "cands": [c.tolist()], "logits": [float(l)]} for o, c, l in zip(sample_obs, sample_cands, torch_logits)]
        export_from_arrays(out, schema["hash"], obs_size, schema["candidateFeatures"], hidden, CLIP, params, reference, decision_type=d)
        meta, loaded = load_exported(out)
        worst = 0.0
        for o, c, l in zip(sample_obs, sample_cands, torch_logits):
            worst = max(worst, abs(float(numpy_forward(loaded, meta["clip"], o.astype(np.float64), c.astype(np.float64)[None, :])[0]) - float(l)))
        if worst > 1e-3:
            print(f"export of {d} disagrees with PyTorch by {worst}", file=sys.stderr)
            return 5, results
        # state_dict in train_phase shape so --prev warm-start keeps working for independent / next shared run
        head_sd = {
            "mean": torch.as_tensor(params["mean"]), "std": torch.as_tensor(params["std"]),
            "obs_layer.weight": torch.as_tensor(params["w_obs"]), "obs_layer.bias": torch.as_tensor(params["b_obs"]),
            "cand_layer.weight": torch.as_tensor(params["w_cand"]),
            "mid.weight": torch.as_tensor(params["w_mid"]), "mid.bias": torch.as_tensor(params["b_mid"]),
            "out.weight": torch.as_tensor(params["w_out"]).reshape(1, -1), "out.bias": torch.as_tensor(params["b_out"]),
        }
        torch.save({"model": head_sd, "schema_hash": schema["hash"], "hidden": hidden, "shared_trunk": True},
                   os.path.join(out, "model.pt"))
        metrics = {"decision": d, "chunks": len(files_by[d]), "export_max_abs_diff": worst, "shared_trunk": True,
                   "history": history, "best_epoch": best_epoch, "best_val_loss": best_val,
                   "constant_val_loss": history[-1]["constant_val_loss"] if history else None, "pool": pool_info,
                   "seconds": round(time.time() - started, 1)}
        with open(os.path.join(out, "train_metrics.json"), "w", encoding="utf-8") as f:
            json.dump(metrics, f, indent=1)
        with open(os.path.join(out, "DONE"), "w", encoding="utf-8") as f:
            f.write("done")
        results[d] = metrics
        print(f"exported {d} (export check {worst:.2e})", flush=True)
    return 0, results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="", help="comma-separated directories with *.NNNNNN.ares chunks")
    parser.add_argument("--packed", default="",
                        help="a pack made by ml/pack_dataset.py --candidates: every trainer reads it instead "
                             "of the padded raw chunks (one disk pass instead of one per run)")
    parser.add_argument("--out", required=True)
    parser.add_argument("--prev", default="")
    parser.add_argument("--freeze-trunk", action="store_true",
                        help="keep the warm-started observation projection fixed and adapt only the heads\n"
                             "(the value calibration of a new opponent is a shift of the label mean; with the trunk\n"
                             "frozen that shift cannot drag the state representation with it)")
    parser.add_argument("--adv-weight", action="store_true",
                        help="weight every policy row by its advantage outcome - V(s), V fitted on a held-out\n"
                             "fraction of the same games (--adv-holdout); the rows of those games are excluded from\n"
                             "the policy update. Night 8 evidence: without a baseline, games the agent loses mostly\n"
                             "teach 'this state is a loss', which blurs the candidate ranking")
    parser.add_argument("--adv-holdout", type=float, default=0.25,
                        help="fraction of games used to fit V (they never enter the policy update)")
    parser.add_argument("--adv-negative", type=float, default=0.1,
                        help="weight of a row whose outcome is below V(s) (0 = drop them, 1 = no advantage weighting)")
    parser.add_argument("--adv-epochs", type=int, default=3)
    parser.add_argument("--adv-hidden", default="256,128")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--steps", type=int, default=0,
                        help="batches between two validations, i.e. one 'epoch' (0 = one pass over the pool); smaller = finer early stopping")
    parser.add_argument("--feed", choices=("mixed", "sequential"), default="mixed",
                        help="mixed: every batch drawn from the whole pool; sequential: the rows of a game in a row (ablation)")
    parser.add_argument("--rows-per-game", type=int, default=32,
                        help="at most this many random rows of each game per decision type go into the pool (0 = all)")
    parser.add_argument("--patience", type=int, default=2,
                        help="stop after this many epochs without a better val_loss; the best epoch is exported")
    parser.add_argument("--hidden", default="256,128")
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-5)
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--independent", action="store_true",
                        help="train each type with train_phase.py (no shared observation trunk)")
    parser.add_argument("--only", default="",
                        help="comma-separated decision types to train (default: every type that has chunks)")
    args = parser.parse_args()

    global MANIFEST
    started = time.time()
    os.makedirs(args.out, exist_ok=True)
    if not args.data and not args.packed:
        print("--data or --packed is required", file=sys.stderr)
        return 2
    if args.packed:
        import ares_data

        MANIFEST = ares_data.open_pack(args.packed)
        available = sorted(d for d, meta in MANIFEST["types"].items() if "candidates" in meta)
        if args.only:
            wanted = [x.strip() for x in args.only.split(",") if x.strip()]
            missing = [w for w in wanted if w not in available]
            if missing:
                print(f"no packed candidates for: {missing}", file=sys.stderr)
                return 2
            decisions = wanted
        else:
            decisions = available
        if not decisions:
            print(f"no decision types with candidates in {args.packed} (pack with --candidates)", file=sys.stderr)
            return 2
        type_files = {d: ({"type": d, "hash": MANIFEST["types"][d]["hash"], "obsSize": MANIFEST["obsSize"],
                           "candidateFeatures": MANIFEST["types"][d]["candidates"]["features"]}, [args.packed])
                      for d in decisions}
    else:
        discovered = discover_decisions(args.data)
        if args.only:
            wanted = [x.strip() for x in args.only.split(",") if x.strip()]
            missing = [w for w in wanted if w not in discovered]
            if missing:
                print(f"no chunks for: {missing}", file=sys.stderr)
                return 2
            decisions = wanted
        else:
            decisions = discovered
        if not decisions:
            print("no finished decision chunks in " + args.data, file=sys.stderr)
            return 2
        type_files = load_type_schemas(args.data, decisions)
        if not type_files:
            print("no usable chunks after schema checks", file=sys.stderr)
            return 2
    print(f"training {len(type_files)} decision types: {', '.join(type_files)}", flush=True)

    if args.independent:
        code, results = train_independent(args, type_files)
    else:
        try:
            import torch  # noqa: F401
        except ImportError:
            print("torch is required for shared-trunk training (or pass --independent)", file=sys.stderr)
            return 2
        code, results = train_shared(args, type_files)

    if code != 0:
        return code

    summary = {"decisions": list(results.keys()), "shared_trunk": not args.independent,
               "seconds": round(time.time() - started, 1), "per_type": results}
    with open(os.path.join(args.out, "train_metrics.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=1)
    with open(os.path.join(args.out, "DONE"), "w", encoding="utf-8") as f:
        f.write("done\n" + "\n".join(results.keys()) + "\n")
    print(f"done: {len(results)} models in {summary['seconds']}s -> {args.out}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
