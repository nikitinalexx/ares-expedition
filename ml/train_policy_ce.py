"""Train the policy the way it is USED: pick one of the offered candidates (softmax over the candidate set).

Why this file exists. The old trainer (`train_all.py`) feeds the model the features of the **taken** candidate only and
trains a binary classifier "will I win this game if I take it" against the game outcome. The other candidates of the same
decision are never scored, although at play time the agent compares their logits. That objective has no term that can
distinguish the better of two offered cards, and its only "signal" is a game-level outcome attached to one action - which
is exactly the drift this project kept measuring (every retrain worse than its parent, three nights in a row).

Here the label is the CHOSEN INDEX inside the candidate set and the loss is cross-entropy over the candidates, which is
the same function the engine computes when it picks argmax. Optional outcome weighting is available but OFF by default.

    py -3 ml/train_policy_ce.py --data DIR --out BUNDLE [--prev BUNDLE] [--epochs 3] [--rows-per-game 8]
                               [--max-chunks-per-type 10] [--outcome-weight 0.0]
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
CATEGORICAL = ["BLUE_INPUT", "BUILD_CARD", "BUILD_INPUT", "BUILD_PAYMENT", "DISCARD_CARDS", "DRAFT_CARDS",
               "MULLIGAN", "PERFORM_BLUE_ACTION", "PICK_CORPORATION", "PICK_PHASE", "SELL_CARDS",
               "SELL_CARDS_LAST_ROUND", "STANDARD_ACTION", "UNMI_RT"]


class Head(nn.Module):
    """Linear(feat)->h1 over every candidate, shared observation projection, then the same per-candidate tail."""

    def __init__(self, obs_size, cand_features, hidden, mean=None, std=None):
        super().__init__()
        h1, h2 = hidden
        self.obs_layer = nn.Linear(obs_size, h1)
        self.cand_layer = nn.Linear(cand_features, h1, bias=False)
        self.mid = nn.Linear(h1, h2)
        self.out = nn.Linear(h2, 1)
        self.register_buffer("mean", torch.zeros(obs_size) if mean is None else torch.as_tensor(mean, dtype=torch.float32))
        self.register_buffer("std", torch.ones(obs_size) if std is None else torch.as_tensor(std, dtype=torch.float32))

    def forward(self, obs, cands):
        z = torch.clamp((obs - self.mean) / self.std, -CLIP, CLIP)
        a = self.obs_layer(z).unsqueeze(1)
        h1 = torch.relu(a + self.cand_layer(cands))
        h2 = torch.relu(self.mid(h1))
        return self.out(h2).squeeze(-1)


def cap_per_game(games, k, rng):
    order = np.lexsort((rng.random(len(games)), games))
    g = games[order]
    first = np.r_[True, g[1:] != g[:-1]]
    group_start = np.maximum.accumulate(np.where(first, np.arange(len(g)), 0))
    return np.sort(order[(np.arange(len(g)) - group_start) < k])


def load_type(directory, decision, rows_per_game, max_chunks, seed):
    """obs [N,obs], cands [N,maxCand,feat], chosen [N], count [N], outcome [N], game [N] for one decision type."""
    rng = np.random.default_rng(seed)
    files = ares_data.chunk_paths(directory, decision)
    if max_chunks:
        files = files[:max_chunks]
    obs, cands, chosen, count, outcome, game = [], [], [], [], [], []
    for path in files:
        schema, rows = ares_data.read_decisions(path)
        if not len(rows):
            continue
        if rows_per_game:
            keep = cap_per_game(rows["game"].astype(np.uint64), rows_per_game, rng)
            rows = rows[keep]
            if not len(rows):
                continue
        obs.append(rows["obs"].astype(np.float32))
        cands.append(rows["feat"].astype(np.float32))
        chosen.append(rows["chosen"].astype(np.int64))
        count.append(rows["count"].astype(np.int64))
        outcome.append(rows["outcome"].astype(np.float32))
        game.append(rows["game"].astype(np.uint64))
    if not obs:
        return None
    data = {"obs": np.concatenate(obs), "cands": np.concatenate(cands), "chosen": np.concatenate(chosen),
            "count": np.concatenate(count), "outcome": np.concatenate(outcome), "game": np.concatenate(game),
            "obsSize": schema["obsSize"], "candFeatures": schema["candidateFeatures"], "schemaHash": schema["hash"]}
    # rows with a single candidate teach nothing about ranking
    keep = data["count"] >= 2
    for key in ("obs", "cands", "chosen", "count", "outcome", "game"):
        data[key] = data[key][keep]
    return data


def warm_start(model, bundle, decision):
    """The parent bundle's head for this type, with the observation projection padded if the encoder grew."""
    path = os.path.join(bundle, decision)
    if not os.path.isdir(path):
        return False
    meta, params = load_exported(path)
    if params["w_cand"].shape != model.cand_layer.weight.shape or params["w_mid"].shape != model.mid.weight.shape:
        return False
    old = params["mean"].shape[0]
    new = model.obs_layer.in_features
    if old > new:
        return False
    with torch.no_grad():
        model.obs_layer.weight.zero_()
        model.obs_layer.weight[:, :old].copy_(torch.as_tensor(params["w_obs"]))
        model.obs_layer.bias.copy_(torch.as_tensor(params["b_obs"]))
        model.mean.zero_()
        model.mean[:old].copy_(torch.as_tensor(params["mean"]))
        model.std.fill_(1.0)
        model.std[:old].copy_(torch.as_tensor(params["std"]))
        model.cand_layer.weight.copy_(torch.as_tensor(params["w_cand"]))
        model.mid.weight.copy_(torch.as_tensor(params["w_mid"]))
        model.mid.bias.copy_(torch.as_tensor(params["b_mid"]))
        model.out.weight.copy_(torch.as_tensor(params["w_out"]).reshape(1, -1))
        model.out.bias.copy_(torch.as_tensor(params["b_out"]).reshape(-1)[:1])
    return True


def load_value_params(directory):
    """The exported value model (mean, std, w1, b1, w2, b2, w3, b3) as numpy arrays, for the advantage baseline."""
    meta = json.load(open(os.path.join(directory, "model.json"), encoding="utf-8"))
    raw = np.fromfile(os.path.join(directory, "weights.bin"), dtype="<f4")
    obs, (h1, h2), clip = meta["obsSize"], meta["hidden"], float(meta["clip"])
    pos = 0

    def take(n):
        nonlocal pos
        chunk = raw[pos:pos + n]
        pos += n
        return chunk

    return {"mean": take(obs), "std": take(obs), "w1": take(h1 * obs).reshape(h1, obs), "b1": take(h1),
            "w2": take(h2 * h1).reshape(h2, h1), "b2": take(h2), "w3": take(h2), "b3": take(1), "clip": clip}


def value_of(obs, value):
    """V(s) of a batch of observations, torch tensors, same numerics as the Java inference."""
    z = torch.clamp((obs - value["mean"]) / value["std"], -value["clip"], value["clip"])
    h1 = torch.relu(z @ value["w1"].T + value["b1"])
    h2 = torch.relu(h1 @ value["w2"].T + value["b2"])
    return torch.sigmoid(h2 @ value["w3"] + value["b3"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--prev", default="")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--rows-per-game", type=int, default=8)
    parser.add_argument("--max-chunks-per-type", type=int, default=10,
                        help="a subset of the chunks keeps the candidate tensors (N x K x features) in RAM")
    parser.add_argument("--outcome-weight", type=float, default=0.0,
                        help="0 = pure candidate cross-entropy; >0 also weights each row by the game outcome")
    parser.add_argument("--objective", default="ce", choices=["ce", "pg-uniform", "pg"],
                        help="ce: imitate the chosen candidate. pg: the rows were generated by the policy of --prev with "
                             "probability (1-q) and by a uniformly random pick with probability q (the exploration "
                             "generator), so the behaviour probability is b(a|s) = (1-q)*1[a=argmax pi_prev] + q/K and the "
                             "importance ratio pi_new/b is exact; the loss is -E[(pi/b) A grad log pi], A = outcome - V(s). "
                             "Needs --adv-value-model and --prev")
    parser.add_argument("--behaviour", type=float, default=1.0,
                        help="q of the pg objective: probability that the generator picked a candidate uniformly at random "
                             "(1.0 = the whole type was explored, 0.35 = a bounded share so the games stay coherent)")
    parser.add_argument("--adv-normalize", action="store_true",
                        help="normalise the advantage per batch (mean 0, std 1): the pg step")
    parser.add_argument("--max-ratio", type=float, default=20.0,
                        help="cap of the importance ratio, so one confident wrong row cannot dominate a batch")
    parser.add_argument("--min-outcome", type=float, default=0.0,
                        help="train only on rows of games with at least this outcome (0.5 = wins only): behaviour cloning "
                             "from successes instead of from the average of all games")
    parser.add_argument("--adv-value-model", default="",
                        help="value model folder: weight every row by (outcome - V(s)), i.e. a policy-gradient step on "
                             "the real objective instead of pure imitation")
    parser.add_argument("--hidden", default="256,128")
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--batch", type=int, default=512)
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--types", default="")
    parser.add_argument("--device", default="auto")
    args = parser.parse_args()

    started = time.time()
    torch.manual_seed(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    decisions = [t for t in (args.types.split(",") if args.types else CATEGORICAL) if t]
    hidden = [int(x) for x in args.hidden.split(",")]

    pools, val_pools = {}, {}
    for decision in decisions:
        data = load_type(args.data, decision, args.rows_per_game, args.max_chunks_per_type, args.seed)
        if data is None or len(data["obs"]) < 100:
            print(f"  {decision:22s} skipped (no usable rows)")
            continue
        if args.min_outcome > 0:
            keep = data["outcome"] >= args.min_outcome
            for key in ("obs", "cands", "chosen", "count", "outcome", "game"):
                data[key] = data[key][keep]
            if len(data["obs"]) < 100:
                print(f"  {decision:22s} skipped ({int(keep.sum())} rows above outcome {args.min_outcome})")
                continue
            print(f"  {decision:22s} filtered to {int(keep.sum())} rows of games with outcome >= {args.min_outcome}")
        split_key = (data["game"] * np.uint64(2654435761)) % np.uint64(1000)
        val = split_key < int(1000 * args.val_fraction)
        pools[decision] = {k: (v[~val] if isinstance(v, np.ndarray) and len(v) == len(val) else v)
                           for k, v in data.items()}
        val_pools[decision] = {k: (v[val] if isinstance(v, np.ndarray) and len(v) == len(val) else v)
                               for k, v in data.items()}
        print(f"  {decision:22s} train {len(pools[decision]['obs']):7d}  val {len(val_pools[decision]['obs']):6d}  "
              f"candidates {data['cands'].shape[1]}")
    if not pools:
        raise SystemExit("no usable decision types")

    obs_size = next(iter(pools.values()))["obsSize"]
    models = {}
    parents = {}
    for decision, data in pools.items():
        model = Head(obs_size, data["candFeatures"], hidden).to(device)
        if args.prev and warm_start(model, args.prev, decision):
            print(f"  {decision:22s} warm start from the parent")
        models[decision] = model
        if args.objective in ("pg", "pg-uniform"):
            parent = Head(obs_size, data["candFeatures"], hidden).to(device)
            if not (args.prev and warm_start(parent, args.prev, decision)):
                raise SystemExit(f"{decision}: the pg objective needs the generating model in --prev")
            parent.eval()
            for parameter in parent.parameters():
                parameter.requires_grad = False
            parents[decision] = parent

    optimizers = {d: torch.optim.Adam(models[d].parameters(), lr=args.lr) for d in models}
    rng = np.random.default_rng(args.seed)

    advantage = None
    if args.adv_value_model:
        params = load_value_params(args.adv_value_model)
        advantage = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32, device=device)
                     for k, v in params.items()}
        advantage["clip"] = float(params["clip"])
        print(f"advantage weighting: rows weighted by outcome - V(s) of {args.adv_value_model}")

    def batches(data, indices, batch):
        for start in range(0, len(indices), batch):
            idx = indices[start:start + batch]
            if len(idx) < 2:
                continue
            yield (torch.from_numpy(data["obs"][idx]).to(device),
                   torch.from_numpy(data["cands"][idx]).to(device),
                   torch.from_numpy(data["chosen"][idx]).to(device),
                   torch.from_numpy(data["count"][idx]).to(device),
                   torch.from_numpy(data["outcome"][idx]).to(device))

    def loss_of(model, old, obs, cands, chosen, count, outcome):
        logits = model(obs, cands)                                     # [B, K]
        mask = torch.arange(cands.shape[1], device=device)[None, :] < count[:, None]
        logits = logits.masked_fill(~mask, -1e9)
        if args.objective in ("pg-uniform", "pg"):
            # The generator drew the taken candidate from b(a|s) = (1-q) * 1[a = argmax pi_prev] + q / K: with probability
            # q it picked uniformly at random (-Dares.agent.explore), otherwise the parent's deterministic choice. Both
            # parts are known, so pi(a|s)/b(a|s) is exact and this is REINFORCE with a baseline, not an approximation:
            #     loss = -E[ (pi/b) * A * log pi ],  A = outcome - V(s)
            if advantage is None:
                raise SystemExit("--objective pg needs --adv-value-model (the baseline)")
            if old is None:
                raise SystemExit("--objective pg needs --prev (the model that generated the data)")
            log_pi = torch.log_softmax(logits, dim=1)
            taken = log_pi.gather(1, chosen.unsqueeze(1)).squeeze(1)
            with torch.no_grad():
                old_logits = old(obs, cands).masked_fill(~mask, -1e9)
                greedy = old_logits.argmax(dim=1)
                q = args.behaviour
                k = count.float()
                behaviour = torch.where(chosen == greedy, (1.0 - q) + q / k, torch.full_like(k, q) / k)
                weight = outcome - value_of(obs, advantage)
                if args.adv_normalize:
                    weight = (weight - weight.mean()) / (weight.std() + 1e-6)
            ratio = (torch.exp(taken) / behaviour).clamp(max=args.max_ratio)
            return -(weight * ratio * taken).mean()
        ce = nn.functional.cross_entropy(logits, chosen.clamp(max=cands.shape[1] - 1), reduction="none")
        if advantage is not None:
            # Policy gradient with the value as the baseline: rows where the game went better than V promised push the
            # taken action up, rows where it went worse push it down. Signed, so the sign of the weight matters.
            weight = outcome - value_of(obs, advantage)
            return (ce * weight).mean()
        if args.outcome_weight > 0:
            weight = 1.0 + args.outcome_weight * (2.0 * outcome - 1.0)   # win 1+w, loss 1-w
            ce = ce * weight
        return ce.mean()

    history = []
    for epoch in range(1, args.epochs + 1):
        for decision, model in models.items():
            model.train()
            data = pools[decision]
            order = rng.permutation(len(data["obs"]))
            running, seen = 0.0, 0
            for obs, cands, chosen, count, outcome in batches(data, order, args.batch):
                loss = loss_of(model, parents.get(decision), obs, cands, chosen, count, outcome)
                if not torch.isfinite(loss):
                    raise SystemExit(f"{decision}: the loss is {float(loss)} in epoch {epoch}")
                optimizers[decision].zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(model.parameters(), 5.0)
                optimizers[decision].step()
                running += float(loss) * len(obs)
                seen += len(obs)
            model.eval()
            with torch.no_grad():
                correct, total, val_loss, top3 = 0, 0, 0.0, 0
                val = val_pools[decision]
                for obs, cands, chosen, count, outcome in batches(val, np.arange(len(val["obs"])), 1024):
                    logits = model(obs, cands)
                    mask = torch.arange(cands.shape[1], device=device)[None, :] < count[:, None]
                    logits = logits.masked_fill(~mask, -1e9)
                    val_loss += float(nn.functional.cross_entropy(logits, chosen.clamp(max=cands.shape[1] - 1))) * len(obs)
                    ranked = torch.topk(logits, k=min(3, logits.shape[1]), dim=1).indices
                    correct += int((ranked[:, 0] == chosen).sum())
                    top3 += int((ranked == chosen.unsqueeze(1)).any(dim=1).sum())
                    total += len(obs)
            history.append({"epoch": epoch, "type": decision, "train_loss": running / max(seen, 1),
                            "val_loss": val_loss / max(total, 1), "top1": correct / max(total, 1),
                            "top3": top3 / max(total, 1)})
            print(json.dumps({k: (round(v, 4) if isinstance(v, float) else v) for k, v in history[-1].items()}),
                  flush=True)

    # export: one bundle, a folder per decision type (plus the parent's untouched types)
    os.makedirs(args.out, exist_ok=True)
    if args.prev:
        import shutil
        for name in sorted(os.listdir(args.prev)):
            source = os.path.join(args.prev, name)
            if os.path.isdir(source) and name not in models:
                target = os.path.join(args.out, name)
                if not os.path.exists(target):
                    shutil.copytree(source, target)
            elif name in ("DONE", "MODEL_INFO.json"):
                shutil.copy2(source, os.path.join(args.out, name))
    for decision, model in models.items():
        state = model.state_dict()
        params = {"mean": state["mean"].cpu().numpy(), "std": state["std"].cpu().numpy(),
                  "w_obs": state["obs_layer.weight"].cpu().numpy(), "b_obs": state["obs_layer.bias"].cpu().numpy(),
                  "w_cand": state["cand_layer.weight"].cpu().numpy(),
                  "w_mid": state["mid.weight"].cpu().numpy(), "b_mid": state["mid.bias"].cpu().numpy(),
                  "w_out": state["out.weight"].cpu().numpy().reshape(-1), "b_out": state["out.bias"].cpu().numpy()}
        data = pools[decision]
        with torch.no_grad():
            val = val_pools[decision]
            sample = np.arange(min(8, len(val["obs"])))
            logits = model(torch.from_numpy(val["obs"][sample]).to(device),
                           torch.from_numpy(val["cands"][sample]).to(device)).cpu().numpy()
        reference = {"inputs": val["obs"][sample].tolist(), "candidates": val["cands"][sample].tolist(),
                     "logits": logits.tolist()}
        export_from_arrays(os.path.join(args.out, decision), data["schemaHash"], obs_size, data["candFeatures"],
                           hidden, CLIP, params, reference, decision_type=decision)
    metrics = {"data": args.data, "prev": args.prev, "types": list(models), "epochs": args.epochs,
               "rows_per_game": args.rows_per_game, "max_chunks_per_type": args.max_chunks_per_type,
               "outcome_weight": args.outcome_weight, "history": history,
               "adv_value_model": args.adv_value_model,
               "seconds": round(time.time() - started, 1)}
    json.dump(metrics, open(os.path.join(args.out, "ce_metrics.json"), "w", encoding="utf-8"), indent=1)
    # The nightly pipeline accepts a model folder only when the trainer says it is complete.
    with open(os.path.join(args.out, "DONE"), "w", encoding="utf-8") as f:
        f.write(json.dumps({"time": time.strftime("%Y-%m-%dT%H:%M:%S"), "trainer": "train_policy_ce"}))
    json.dump({"trainer": "train_policy_ce", "target": "candidate cross-entropy", "outcome_weight": args.outcome_weight,
               "epochs": args.epochs, "rows_per_game": args.rows_per_game, "data": args.data, "prev": args.prev},
              open(os.path.join(args.out, "MODEL_INFO.json"), "w", encoding="utf-8"), indent=1)
    print(json.dumps({k: v for k, v in metrics.items() if k != "history"}, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
