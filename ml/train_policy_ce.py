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
    if max_chunks and len(files) > max_chunks:
        # Sample the chunks instead of taking the first N: with a wide iteration (tens of thousands of games) the first N
        # thousand games are one part of the run, not a sample of it.
        picked = np.random.default_rng(seed).choice(len(files), max_chunks, replace=False)
        files = [files[i] for i in sorted(picked)]
    obs, cands, chosen, ids, count, outcome, game, margin = [], [], [], [], [], [], [], []
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
        ids.append(rows["ids"].astype(np.int64))
        count.append(rows["count"].astype(np.int64))
        outcome.append(rows["outcome"].astype(np.float32))
        margin.append(rows["margin"].astype(np.float32))
        game.append(rows["game"].astype(np.uint64))
    if not obs:
        return None
    data = {"obs": np.concatenate(obs), "cands": np.concatenate(cands), "chosen": np.concatenate(chosen),
            "ids": np.concatenate(ids),
            "count": np.concatenate(count), "outcome": np.concatenate(outcome), "game": np.concatenate(game),
            "margin": np.concatenate(margin),
            "obsSize": schema["obsSize"], "candFeatures": schema["candidateFeatures"], "schemaHash": schema["hash"]}
    # rows with a single candidate teach nothing about ranking
    keep = data["count"] >= 2
    for key in ("obs", "cands", "chosen", "ids", "count", "outcome", "game", "margin"):
        data[key] = data[key][keep]
    return data


def load_packed_type(directory, decision, manifest, rows_per_game=0, max_chunks=0, seed=1):
    """The same dict as load_type, from a pack: real candidates only, no padding to the menu maximum.

    The raw loader allocates [rows, maxCand, features] (for BUILD_INPUT that is 12 x 269 for a row that usually has 7),
    which is why it needed a chunk subset to fit in RAM. The packed view gathers [batch, kmax, features] for the batch
    being trained on, so a full corpus costs the real candidate bytes and nothing else.

    The cross-type de-duplication mask is NOT used here: the same state recorded for two different decision types is a
    legitimate row for each of those heads, and dropping it would silently shrink a policy corpus. The mask exists for the
    value pool, where one state must count once.
    """
    data = ares_data.open_packed_type(directory, decision, manifest)
    if "cands" not in data:
        return None
    keep = np.asarray(data["count"]) >= 2
    if rows_per_game:
        # Same meaning as in the raw loader: a memory guard on how many states of one game are kept. cap_per_game
        # returns POSITIONS, so it has to be turned into a row mask before the two filters are combined.
        rng = np.random.default_rng(seed)
        picked = cap_per_game(np.asarray(data["game"]).astype(np.uint64), rows_per_game, rng)
        row_keep = np.zeros(len(keep), dtype=bool)
        row_keep[picked] = True
        keep = keep & row_keep
    if max_chunks:
        print(f"  {decision:22s} note: --max-chunks-per-type is applied when packing (ml/pack_dataset.py), not here")
    if not keep.any():
        return None
    for key in ("obs", "cands", "ids", "chosen", "count", "outcome", "game", "margin"):
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


def value_of(obs, value, raw=False):
    """V(s) of a batch of observations, torch tensors, same numerics as the Java inference.

    `raw=True` returns the output before the sigmoid: for a margin-trained critic that number IS the predicted margin/20,
    which is the scale a "how many points is this worth" advantage has to be compared on.
    """
    z = torch.clamp((obs - value["mean"]) / value["std"], -value["clip"], value["clip"])
    h1 = torch.relu(z @ value["w1"].T + value["b1"])
    h2 = torch.relu(h1 @ value["w2"].T + value["b2"])
    logit = h2 @ value["w3"] + value["b3"]
    return logit if raw else torch.sigmoid(logit)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="")
    parser.add_argument("--out", required=True)
    parser.add_argument("--prev", default="")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--rows-per-game", type=int, default=8)
    parser.add_argument("--max-chunks-per-type", type=int, default=10,
                        help="a subset of the chunks keeps the candidate tensors (N x K x features) in RAM")
    parser.add_argument("--anchor", default="",
                        help="bundle to hold the model close to (the parent). With --anchor-weight > 0 the loss adds a "
                             "distillation term to the anchor's distribution, so a latent-plan run cannot drift the base "
                             "style it was supposed to keep: measured, imitating nudged games without it cost 7 points "
                             "against SMART while the same budget without nudges cost nothing")
    parser.add_argument("--anchor-weight", type=float, default=0.0,
                        help="weight of the anchor term (0 = off). It only binds where the anchor is confident, so a "
                             "small value lets the code-conditioned rows deviate while the rest of the state space stays")
    parser.add_argument("--anchor-code", type=int, default=-1,
                        help="apply the anchor only to rows whose plan code equals this value (-1 = all rows): the "
                             "deployed style is usually code 0")
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
    parser.add_argument("--card-weights", default="",
                        help="JSON of per-card advantages (ml/card_weights.py). For BUILD_CARD rows the loss weight is "
                             "scaled by (1 + alpha * advantage): the card-level estimate is stable across halves of the "
                             "data, where the row-level advantage is noise")
    parser.add_argument("--card-weight-alpha", type=float, default=0.15)
    parser.add_argument("--relabel", default="",
                        help="`card-advantage`: for BUILD_CARD rows replace the label with the best candidate by its "
                             "per-card advantage (ml/card_weights.py). This is offline policy improvement on the states we "
                             "actually visited: the head learns to prefer cards that went better than the critic expected, "
                             "instead of imitating the card the old policy happened to take")
    parser.add_argument("--adv-target", default="outcome", choices=["outcome", "margin"],
                        help="what the advantage is measured in: the win (win - P(win)) or the score (margin/20 - V's "
                             "prediction), which is the scale the printed card points live on")
    parser.add_argument("--adv-normalize", action="store_true",
                        help="normalise the advantage per batch (mean 0, std 1): the pg step")
    parser.add_argument("--adv-weight-clip", default="0.2,3.0",
                        help="clip of the advantage weight that scales the cross-entropy. Signed, unbounded weights make "
                             "the objective unbounded below (measured 2026-11-16: PICK_PHASE val loss 860, train loss -20), "
                             "because driving a taken action's probability to zero lowers the loss without limit. A strictly "
                             "positive clipped weight turns the step into reweighting: rows whose game went better than the "
                             "critic promised count more, rows that went worse count less, and the loss stays a bounded "
                             "cross-entropy. `0,0` restores the old unbounded behaviour (not recommended)")
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
    parser.add_argument("--packed", default="",
                        help="a directory made by ml/pack_dataset.py --candidates: read the packed candidate "
                             "block (real candidates only) instead of the padded raw chunks")
    parser.add_argument("--device", default="auto")
    args = parser.parse_args()

    started = time.time()
    torch.manual_seed(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    decisions = [t for t in (args.types.split(",") if args.types else CATEGORICAL) if t]
    hidden = [int(x) for x in args.hidden.split(",")]
    low_weight, high_weight = (float(x) for x in args.adv_weight_clip.split(","))
    if low_weight > 0 and high_weight <= low_weight:
        raise SystemExit(f"--adv-weight-clip needs low < high, got {args.adv_weight_clip}")

    pools, val_pools = {}, {}
    card_weights = {}
    if args.card_weights:
        payload = json.load(open(args.card_weights, encoding="utf-8"))
        card_weights = {int(card): float(value) for card, value in payload.get("cards", {}).items()}
        print(f"card weights: {len(card_weights)} cards from {args.card_weights}, alpha {args.card_weight_alpha}")

    manifest = ares_data.open_pack(args.packed) if args.packed else None
    for decision in decisions:
        if args.packed:
            data = load_packed_type(args.packed, decision, manifest, args.rows_per_game,
                                    args.max_chunks_per_type, args.seed)
        else:
            data = load_type(args.data, decision, args.rows_per_game, args.max_chunks_per_type, args.seed)
        if data is None or len(data["obs"]) < 100:
            print(f"  {decision:22s} skipped (no usable rows)")
            continue
        if args.min_outcome > 0:
            keep = data["outcome"] >= args.min_outcome
            for key in ("obs", "cands", "chosen", "ids", "count", "outcome", "game", "margin"):
                data[key] = data[key][keep]
            if len(data["obs"]) < 100:
                print(f"  {decision:22s} skipped ({int(keep.sum())} rows above outcome {args.min_outcome})")
                continue
            print(f"  {decision:22s} filtered to {int(keep.sum())} rows of games with outcome >= {args.min_outcome}")
        if args.relabel == "card-advantage" and decision == "BUILD_CARD" and card_weights:
            relabelled = 0
            for row in range(len(data["chosen"])):
                count = int(data["count"][row])
                ids = data["ids"][row][:count]
                best, best_value = -1, -1e9
                for index in range(count):
                    card = int(ids[index])
                    if card == 0xFFFF or card not in card_weights:
                        continue
                    if card_weights[card] > best_value:
                        best_value = card_weights[card]
                        best = index
                if best >= 0:
                    data["chosen"][row] = best
                    relabelled += 1
            print(f"  {decision:22s} relabelled {relabelled} rows by the per-card advantage")

        split_key = (data["game"] * np.uint64(2654435761)) % np.uint64(1000)
        val = split_key < int(1000 * args.val_fraction)

        def take(value, mask):
            # Row-aligned values are split; anything else (widths, schema hashes, lazy views) is passed through. A packed
            # view is not an ndarray, but it has a shape and supports a boolean mask, so a full corpus can be split
            # without materialising the padded candidate tensor.
            aligned = getattr(value, "shape", None) is not None and len(value) == len(mask)
            return value[mask] if aligned else value

        pools[decision] = {k: take(v, ~val) for k, v in data.items()}
        val_pools[decision] = {k: take(v, val) for k, v in data.items()}
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

    anchors = {}
    if args.anchor_weight > 0:
        if not args.anchor:
            raise SystemExit("--anchor-weight needs --anchor <bundle>")
        for decision, data in pools.items():
            anchor = Head(obs_size, data["candFeatures"], hidden).to(device)
            if not warm_start(anchor, args.anchor, decision):
                raise SystemExit(f"{decision}: the anchor bundle has no usable head")
            anchor.eval()
            for parameter in anchor.parameters():
                parameter.requires_grad = False
            anchors[decision] = anchor
        where = "all rows" if args.anchor_code < 0 else f"plan code {args.anchor_code} only"
        print(f"  anchor {args.anchor} with weight {args.anchor_weight} on {where}")

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
            chosen = torch.from_numpy(data["chosen"][idx]).to(device)
            ids = torch.from_numpy(data["ids"][idx]).to(device)
            yield (torch.from_numpy(data["obs"][idx]).to(device),
                   torch.from_numpy(data["cands"][idx]).to(device),
                   chosen,
                   torch.from_numpy(data["count"][idx]).to(device),
                   torch.from_numpy(data["outcome"][idx]).to(device),
                   torch.from_numpy(data["margin"][idx]).to(device),
                   ids.gather(1, chosen.unsqueeze(1)).squeeze(1))

    def advantage_of(obs, outcome, margin):
        """A(s,a): how much better this game went than the critic promised.

        `outcome`: win minus P(win) - the classic baseline. `margin`: (final margin/20) minus the critic's raw output,
        which is on the same scale as the margin model's prediction, so the advantage is in POINTS. Points are what the
        printed card values are, and a win/loss baseline cannot see a 3-point card at all (measured: dV 0.008 for three
        points against 0.19 for ten milestone points).
        """
        if args.adv_target == "margin":
            return margin / 20.0 - value_of(obs, advantage, raw=True)
        return outcome - value_of(obs, advantage)

    def anchor_term(anchor, model, obs, cands, mask, logits):
        """Distillation to the anchor: -sum_a p_anchor(a|s) log p_model(a|s) on the rows the anchor guards.

        It is a KL up to a constant, and it is the trust region of this loop: the code has to explain a *change* in
        behaviour, and every change it does not explain is paid for here. Without it the shared trunk follows the nudged
        games and the baseline style drifts (measured: 37.8% -> 30.8% against SMART).
        """
        with torch.no_grad():
            anchor_logits = anchor(obs, cands).masked_fill(~mask, -1e9)
            p = torch.softmax(anchor_logits, dim=1)
        guard = torch.ones(obs.shape[0], dtype=torch.bool, device=device)
        if args.anchor_code >= 0:
            bits = torch.clamp((obs[:, -4:] > 0.5).to(torch.int64), 0, 1)
            code = (bits * torch.tensor([1, 2, 4, 8], dtype=torch.int64, device=device)).sum(dim=1)
            guard = code == args.anchor_code
        if not bool(guard.any()):
            return None
        log_p = torch.log_softmax(logits, dim=1)
        return -(p * log_p).sum(dim=1)[guard].mean()

    def loss_of(model, old, obs, cands, chosen, count, outcome, margin, decision, chosen_ids):
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
                weight = advantage_of(obs, outcome, margin)
                if args.adv_normalize:
                    weight = (weight - weight.mean()) / (weight.std() + 1e-6)
                weight = weight.clamp(low_weight, high_weight)
            ratio = (torch.exp(taken) / behaviour).clamp(max=args.max_ratio)
            return -(weight * ratio * taken).mean()
        ce = nn.functional.cross_entropy(logits, chosen.clamp(max=cands.shape[1] - 1), reduction="none")
        if advantage is not None:
            # Policy gradient with the value as the baseline: rows where the game went better than V promised count more,
            # rows that went worse count less. The weight is CLIPPED to stay positive: a signed, unbounded weight makes the
            # objective unbounded below (measured 2026-11-16: PICK_PHASE val loss 860, train loss -20), because driving a
            # taken action's probability to zero lowers the loss without limit.
            weight = advantage_of(obs, outcome, margin)
            if args.adv_normalize:
                weight = (weight - weight.mean()) / (weight.std() + 1e-6)
            weight = weight.clamp(low_weight, high_weight)
            return (ce * weight).mean()
        if card_weights:
            table = torch.tensor([card_weights.get(int(card), 0.0) for card in chosen_ids.tolist()],
                                 dtype=torch.float32, device=device)
            ce = ce * (1.0 + args.card_weight_alpha * table).clamp(0.2, 3.0)
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
            for obs, cands, chosen, count, outcome, margin, chosen_ids in batches(data, order, args.batch):
                loss = loss_of(model, parents.get(decision), obs, cands, chosen, count, outcome, margin, decision,
                               chosen_ids)
                anchor = anchors.get(decision)
                if anchor is not None:
                    with torch.no_grad():
                        pass
                    mask = torch.arange(cands.shape[1], device=device)[None, :] < count[:, None]
                    logits = model(obs, cands).masked_fill(~mask, -1e9)
                    term = anchor_term(anchor, model, obs, cands, mask, logits)
                    if term is not None:
                        loss = loss + args.anchor_weight * term
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
                for obs, cands, chosen, count, outcome, margin, _ in batches(val, np.arange(len(val["obs"])), 1024):
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
    metrics = {"data": args.packed or args.data, "prev": args.prev, "types": list(models), "epochs": args.epochs,
               "rows_per_game": args.rows_per_game, "max_chunks_per_type": args.max_chunks_per_type,
               "outcome_weight": args.outcome_weight, "history": history,
               "adv_value_model": args.adv_value_model,
               "seconds": round(time.time() - started, 1)}
    json.dump(metrics, open(os.path.join(args.out, "ce_metrics.json"), "w", encoding="utf-8"), indent=1)
    # The nightly pipeline accepts a model folder only when the trainer says it is complete.
    with open(os.path.join(args.out, "DONE"), "w", encoding="utf-8") as f:
        f.write(json.dumps({"time": time.strftime("%Y-%m-%dT%H:%M:%S"), "trainer": "train_policy_ce"}))
    json.dump({"trainer": "train_policy_ce", "target": "candidate cross-entropy", "outcome_weight": args.outcome_weight,
               "epochs": args.epochs, "rows_per_game": args.rows_per_game, "data": args.packed or args.data, "prev": args.prev},
              open(os.path.join(args.out, "MODEL_INFO.json"), "w", encoding="utf-8"), indent=1)
    print(json.dumps({k: v for k, v in metrics.items() if k != "history"}, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
