"""Distil the search's ranking into one decision head (the only policy-side signal that is not game-outcome noise).

Data: the `RANKING.<chunk>.jsonl` files written by `DecisionLogs` during self-play with the search on. Each line is the
search's value for EVERY candidate of a decision, joinable to the recorded row by (game, step). Instead of imitating the
one action that was taken, the head learns the whole ordering - which is exactly the improvement operator the search is.

    py -3 ml/distill.py --data DIR --parent MODEL_DIR --type BUILD_CARD --out DIR [--temperature 1.0]

The result is written as a full model bundle (the parent with its <type> head replaced), so the engine needs nothing new.
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
import ares_data  # noqa: E402
from phase_model import export_from_arrays, load_exported  # noqa: E402

CLIP = 5.0


class Head(nn.Module):
    """The same function as `phase_model.numpy_forward`: a shared observation projection, then one per-candidate head."""

    def __init__(self, obs_size, cand_features, hidden, mean, std):
        super().__init__()
        self.register_buffer("mean", torch.as_tensor(mean, dtype=torch.float32))
        self.register_buffer("std", torch.as_tensor(std, dtype=torch.float32))
        h1, h2 = hidden
        self.obs_layer = nn.Linear(obs_size, h1)
        self.cand_layer = nn.Linear(cand_features, h1, bias=False)
        self.mid = nn.Linear(h1, h2)
        self.out = nn.Linear(h2, 1)

    def forward(self, obs, cands):
        z = torch.clamp((obs - self.mean) / self.std, -CLIP, CLIP)
        a = self.obs_layer(z)
        h1 = torch.relu(a.unsqueeze(1) + self.cand_layer(cands))
        h2 = torch.relu(self.mid(h1))
        return self.out(h2).squeeze(-1)


def read_rankings(directory):
    """(game, step) -> (ids, scores) for every decision the search scored."""
    result = {}
    for name in sorted(os.listdir(directory)):
        if not name.startswith("RANKING.") or not name.endswith(".jsonl"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8") as f:
            for line in f:
                try:
                    row = json.loads(line)
                except json.JSONDecodeError:
                    continue
                result[(int(row["game"]) & 0xFFFFFFFFFFFFFFFF, int(row["step"]))] = (row["ids"], row["scores"])
    return result


def read_rows(directory, decision, rankings, rows_per_game=0, seed=1):
    """The recorded rows of `decision` that have a search ranking attached."""
    rng = np.random.default_rng(seed)
    obs, cands, target, games = [], [], [], []
    for path in ares_data.chunk_paths(directory, decision):
        schema, rows = ares_data.read_decisions(path)
        if not len(rows):
            continue
        keep = []
        for i, row in enumerate(rows):
            key = (int(row["game"]), int(row["step"]))
            if key not in rankings:
                continue
            ids, scores = rankings[key]
            if len(ids) != row["count"] or list(ids) != [int(x) for x in row["ids"][:row["count"]]]:
                continue  # the candidate order changed: not the same menu
            keep.append(i)
        if not keep:
            continue
        keep = np.asarray(keep)
        if rows_per_game:
            games_col = rows["game"][keep].astype(np.uint64)
            order = np.lexsort((rng.random(len(games_col)), games_col))
            g = games_col[order]
            first = np.r_[True, g[1:] != g[:-1]]
            group_start = np.maximum.accumulate(np.where(first, np.arange(len(g)), 0))
            keep = keep[order[(np.arange(len(g)) - group_start) < rows_per_game]]
        obs.append(rows["obs"][keep].astype(np.float32))
        cands.append(rows["feat"][keep].astype(np.float32))
        games.append(rows["game"][keep].astype(np.uint64))
        for i in keep:
            ids, scores = rankings[(int(rows["game"][i]), int(rows["step"][i]))]
            target.append(np.asarray(scores, dtype=np.float32))
    if not obs:
        raise SystemExit(f"no {decision} rows have a search ranking in {directory}")
    count = max(len(t) for t in target)
    width = cands[0].shape[1]  # the chunk pads candidates to the decision's maxCandidates
    padded = np.full((len(target), width), -1e9, dtype=np.float32)
    mask = np.zeros((len(target), width), dtype=bool)
    for i, t in enumerate(target):
        padded[i, :len(t)] = t
        mask[i, :len(t)] = True
    return (np.concatenate(obs), np.concatenate(cands), padded, mask, np.concatenate(games))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="directory with RANKING.*.jsonl and the decision chunks")
    parser.add_argument("--parent", required=True, help="model bundle whose <type> head is refined")
    parser.add_argument("--type", default="BUILD_CARD")
    parser.add_argument("--out", required=True, help="output model bundle")
    parser.add_argument("--temperature", type=float, default=1.0)
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--batch", type=int, default=256)
    parser.add_argument("--rows-per-game", type=int, default=32)
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--freeze-trunk", action="store_true",
                        help="keep the parent's observation projection, train only the candidate head")
    args = parser.parse_args()

    started = time.time()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    rankings = read_rankings(args.data)
    print(f"{len(rankings)} ranked decisions in {args.data}")
    obs, cands, teacher, mask, games = read_rows(args.data, args.type, rankings, args.rows_per_game, args.seed)
    print(f"{len(obs)} {args.type} rows with a ranking, {int(mask.sum(axis=1).mean())} candidates each")

    # split by game
    split_key = (games.astype(np.uint64) * np.uint64(2654435761)) % np.uint64(1000)
    val = split_key < int(1000 * args.val_fraction)
    if val.sum() < 50:
        val[:] = False
        val[:max(50, len(val) // 10)] = True
    train_idx, val_idx = np.flatnonzero(~val), np.flatnonzero(val)
    print(f"train {len(train_idx)} / validation {len(val_idx)} rows (by game)")

    meta, params = load_exported(os.path.join(args.parent, args.type))
    hidden = meta["hidden"]
    model = Head(meta["obsSize"], meta["candFeatures"], hidden, params["mean"], params["std"]).to(device)
    with torch.no_grad():
        model.obs_layer.weight.copy_(torch.as_tensor(params["w_obs"]))
        model.obs_layer.bias.copy_(torch.as_tensor(params["b_obs"]))
        model.cand_layer.weight.copy_(torch.as_tensor(params["w_cand"]))
        model.mid.weight.copy_(torch.as_tensor(params["w_mid"]))
        model.mid.bias.copy_(torch.as_tensor(params["b_mid"]))
        model.out.weight.copy_(torch.as_tensor(params["w_out"]).reshape(1, -1))
        model.out.bias.copy_(torch.as_tensor(params["b_out"]).reshape(-1)[:1])
    if args.freeze_trunk:
        for p in model.obs_layer.parameters():
            p.requires_grad = False
        for p in model.cand_layer.parameters():
            p.requires_grad = False

    optimizer = torch.optim.Adam([p for p in model.parameters() if p.requires_grad], lr=args.lr)
    temperature = max(args.temperature, 1e-3)

    def batch(indices):
        return (torch.from_numpy(obs[indices]).to(device),
                torch.from_numpy(cands[indices]).to(device),
                torch.from_numpy(teacher[indices]).to(device),
                torch.from_numpy(mask[indices]).to(device))

    def evaluate(indices):
        model.eval()
        with torch.no_grad():
            correct, total, top3, loss = 0, 0, 0, 0.0
            for start in range(0, len(indices), 512):
                chunk = indices[start:start + 512]
                x, c, t, m = batch(chunk)
                logits = model(x, c)
                logits = logits.masked_fill(~m, -1e9)
                probs = torch.log_softmax(logits / temperature, dim=1)
                teacher_probs = torch.softmax(t / temperature, dim=1)
                loss += float(-(teacher_probs * probs).sum(dim=1).mean()) * len(chunk)
                ranked = torch.topk(logits, k=min(3, logits.shape[1]), dim=1).indices
                best = t.argmax(dim=1)
                correct += int((ranked[:, 0] == best).sum())
                top3 += int((ranked == best.unsqueeze(1)).any(dim=1).sum())
                total += len(chunk)
        model.train()
        return loss / max(total, 1), correct / max(total, 1), top3 / max(total, 1)

    rng = np.random.default_rng(args.seed)
    history, best = [], {"loss": float("inf"), "state": None, "epoch": 0}
    for epoch in range(1, args.epochs + 1):
        model.train()
        order = rng.permutation(train_idx)
        running, seen = 0.0, 0
        for start in range(0, len(order), args.batch):
            chunk = order[start:start + args.batch]
            if len(chunk) < 2:
                continue
            x, c, t, m = batch(chunk)
            logits = model(x, c).masked_fill(~m, -1e9)
            log_probs = torch.log_softmax(logits / temperature, dim=1)
            teacher_probs = torch.softmax(t / temperature, dim=1)
            loss = -(teacher_probs * log_probs).sum(dim=1).mean()
            if not torch.isfinite(loss):
                raise SystemExit(f"the distillation loss is {float(loss)} in epoch {epoch}")
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_([p for p in model.parameters() if p.requires_grad], 5.0)
            optimizer.step()
            running += float(loss) * len(chunk)
            seen += len(chunk)
        val_loss, top1, top3 = evaluate(val_idx)
        row = {"epoch": epoch, "train_loss": running / max(seen, 1), "val_loss": val_loss,
               "agree_top1": top1, "agree_top3": top3}
        history.append(row)
        print(json.dumps({k: (round(v, 4) if isinstance(v, float) else v) for k, v in row.items()}), flush=True)
        if val_loss < best["loss"]:
            best = {"loss": val_loss, "state": {k: v.detach().clone() for k, v in model.state_dict().items()},
                    "epoch": epoch}
    model.load_state_dict(best["state"])
    model.eval()

    # Export: copy the parent bundle, replace this head. Same files the engine already reads.
    import shutil
    if os.path.exists(args.out):
        shutil.rmtree(args.out)
    os.makedirs(args.out, exist_ok=True)
    for name in sorted(os.listdir(args.parent)):
        source = os.path.join(args.parent, name)
        target = os.path.join(args.out, name)
        if os.path.isdir(source):
            if name == args.type:
                continue
            shutil.copytree(source, target)
        elif name in ("DONE", "MODEL_INFO.json"):
            shutil.copy2(source, target)

    state = model.state_dict()
    export_params = {
        "mean": state["mean"].cpu().numpy(), "std": state["std"].cpu().numpy(),
        "w_obs": state["obs_layer.weight"].cpu().numpy(), "b_obs": state["obs_layer.bias"].cpu().numpy(),
        "w_cand": state["cand_layer.weight"].cpu().numpy(),
        "w_mid": state["mid.weight"].cpu().numpy(), "b_mid": state["mid.bias"].cpu().numpy(),
        "w_out": state["out.weight"].cpu().numpy().reshape(-1), "b_out": state["out.bias"].cpu().numpy(),
    }
    with torch.no_grad():
        sample = np.flatnonzero(val_idx)[:8]
        x, c, _, m = batch(sample)
        reference_logits = model(x, c).cpu().numpy()
    reference = {"inputs": obs[sample].tolist(), "candidates": cands[sample].tolist(),
                 "logits": reference_logits.tolist()}
    decision_hash = hashlib.sha256((args.type + "|distilled").encode("utf-8")).hexdigest()[:16]
    export_from_arrays(os.path.join(args.out, args.type), meta["schemaHash"], meta["obsSize"], meta["candFeatures"],
                       hidden, CLIP, export_params, reference, decision_type=args.type)
    metrics = {"type": args.type, "rows_train": int(len(train_idx)), "rows_val": int(len(val_idx)),
               "epochs": args.epochs, "temperature": temperature, "best_epoch": best["epoch"],
               "val_loss": round(best["loss"], 4), "history": history, "freeze_trunk": args.freeze_trunk,
               "seconds": round(time.time() - started, 1), "out": args.out, "data": args.data, "parent": args.parent}
    json.dump(metrics, open(os.path.join(args.out, f"distill_{args.type}.json"), "w", encoding="utf-8"), indent=1)
    print(json.dumps({k: v for k, v in metrics.items() if k != "history"}, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
