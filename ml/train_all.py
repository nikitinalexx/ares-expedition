"""Train one Q-network per decision type that has finished chunks, with a shared observation trunk.

    python ml/train_all.py --data DIR1,DIR2 --out MODEL_DIR [--prev PREV] [--epochs 3] [--hidden 256,128]
    python ml/train_all.py ... --independent   # fallback: call train_phase.py once per type (no shared trunk)

Layout written to MODEL_DIR (Java loads it with -Dares.agent.models=MODEL_DIR):
    MODEL_DIR/<DECISION_TYPE>/{model.json,weights.bin,model.pt,train_metrics.json,DONE}
    MODEL_DIR/DONE
    MODEL_DIR/train_metrics.json

Shared trunk (default): one mean/std and W_obs for every type; each type keeps its own candidate head.
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
from ares_data import chunk_paths, discover_decisions, open_memmap, validate  # noqa: E402
from phase_model import export_from_arrays, load_exported, numpy_forward  # noqa: E402
from train_phase import CLIP, iterate_blocks, list_chunks, standardisation  # noqa: E402


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


def train_independent(args, type_files):
    """One subprocess of train_phase.py per type. No shared weights."""
    results = {}
    for decision, (schema, files) in type_files.items():
        out = os.path.join(args.out, decision)
        os.makedirs(out, exist_ok=True)
        prev = os.path.join(args.prev, decision) if args.prev and os.path.isdir(os.path.join(args.prev, decision)) else ""
        cmd = [sys.executable, os.path.join(os.path.dirname(__file__), "train_phase.py"),
               "--data", args.data, "--out", out, "--decision", decision,
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

    # pooled standardisation over every type's training rows
    all_files = [f for d in decisions for f in files_by[d]]
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
        mean, std, _ = standardisation(all_files, args.val_fraction)

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
            if previous.get("schema_hash") != schemas[d]["hash"] or previous.get("hidden") != hidden:
                continue
            # independent previous export: only the matching head + obs layer if shapes agree
            sd = previous["model"]
            with torch.no_grad():
                if "obs_layer.weight" in sd and sd["obs_layer.weight"].shape == model.obs_layer.weight.shape:
                    model.obs_layer.weight.copy_(sd["obs_layer.weight"])
                    model.obs_layer.bias.copy_(sd["obs_layer.bias"])
                    model.mean.copy_(sd["mean"])
                    model.std.copy_(sd["std"])
                # train_phase / exported shared state_dict keys: cand_layer, mid, out
                if "cand_layer.weight" in sd and sd["cand_layer.weight"].shape == model.heads[d].cand.weight.shape:
                    model.heads[d].cand.weight.copy_(sd["cand_layer.weight"])
                    model.heads[d].mid.weight.copy_(sd["mid.weight"])
                    model.heads[d].mid.bias.copy_(sd["mid.bias"])
                    model.heads[d].out.weight.copy_(sd["out.weight"])
                    model.heads[d].out.bias.copy_(sd["out.bias"])
                    loaded_any = True
        if loaded_any:
            print("warm start of shared trunk / heads from " + args.prev)

    loss_fn = nn.BCEWithLogitsLoss()
    # sampling weights ~ rows so rare types (UNMI) still appear but common types dominate
    type_weights = []
    for d in decisions:
        n = sum(len(open_memmap(f)[1]) for f in files_by[d])
        type_weights.append(max(n, 1))
    type_weights = np.asarray(type_weights, dtype=np.float64)
    type_weights /= type_weights.sum()

    history = []
    for epoch in range(start_epoch, args.epochs):
        model.train()
        running, seen = 0.0, 0
        # one pass: draw types by weight, stream one training block each time until ~all types covered several times
        steps = max(8, len(decisions) * 4)
        for _ in range(steps):
            d = decisions[int(rng.choice(len(decisions), p=type_weights))]
            for obs, cand, outcome in iterate_blocks(files_by[d], False, args.val_fraction, rng, True):
                permutation = rng.permutation(len(obs))
                for s in range(0, len(obs), args.batch):
                    idx = permutation[s:s + args.batch]
                    logits = model(d, torch.from_numpy(obs[idx]).to(device), torch.from_numpy(cand[idx]).to(device))
                    loss = loss_fn(logits, torch.from_numpy(outcome[idx]).to(device))
                    if not torch.isfinite(loss):
                        print(f"loss diverged on {d} in epoch {epoch}", file=sys.stderr)
                        return 4, {}
                    optimizer.zero_grad()
                    loss.backward()
                    nn.utils.clip_grad_norm_(model.parameters(), 5.0)
                    optimizer.step()
                    running += float(loss) * len(idx)
                    seen += len(idx)
                break  # one block per drawn type per step keeps an epoch bounded

        # validation: mean loss over types that have val rows
        model.eval()
        val_parts = []
        with torch.no_grad():
            for d in decisions:
                total, n = 0.0, 0
                for obs, cand, outcome in iterate_blocks(files_by[d], True, args.val_fraction, None, False):
                    logits = model(d, torch.from_numpy(obs).to(device), torch.from_numpy(cand).to(device))
                    total += float(nn.functional.binary_cross_entropy_with_logits(
                        logits, torch.from_numpy(outcome).to(device), reduction="sum"))
                    n += len(outcome)
                if n:
                    val_parts.append(total / n)
        model.train()
        val_loss = float(np.mean(val_parts)) if val_parts else float("nan")
        history.append({"epoch": epoch + 1, "train_loss": running / max(seen, 1), "val_loss": val_loss, "rows_seen": seen})
        print(json.dumps(history[-1]), flush=True)
        tmp = checkpoint_path + ".tmp"
        torch.save({"model": model.state_dict(), "optimizer": optimizer.state_dict(), "epoch": epoch + 1,
                    "obs_size": obs_size, "hidden": hidden, "mean": mean, "std": std,
                    "decisions": decisions}, tmp)
        os.replace(tmp, checkpoint_path)

    # export one Java-readable folder per type (shared trunk baked into each)
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
        for obs, cand, _ in iterate_blocks(files_by[d], True, args.val_fraction, None, False):
            sample_obs, sample_cands = obs[:16], cand[:16]
            break
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
                   "history": history, "seconds": round(time.time() - started, 1)}
        with open(os.path.join(out, "train_metrics.json"), "w", encoding="utf-8") as f:
            json.dump(metrics, f, indent=1)
        with open(os.path.join(out, "DONE"), "w", encoding="utf-8") as f:
            f.write("done")
        results[d] = metrics
        print(f"exported {d} (export check {worst:.2e})", flush=True)
    return 0, results


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
    parser.add_argument("--independent", action="store_true",
                        help="train each type with train_phase.py (no shared observation trunk)")
    parser.add_argument("--only", default="",
                        help="comma-separated decision types to train (default: every type that has chunks)")
    args = parser.parse_args()

    started = time.time()
    os.makedirs(args.out, exist_ok=True)
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
