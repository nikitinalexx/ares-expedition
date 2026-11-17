"""Train the critic on the game's own score decomposition instead of one number.

The owner's objection (2026-11-17): a single final margin is dominated by tempo in our games, so a critic trained on it cannot
learn that a card scores - and "printed points" is only one of seven channels (TR, forests, printed card points, points from
resources on cards, milestones, awards, extra). Teaching an auxiliary target per hand-picked feature would not scale; the
game's own `ScoreBreakdown` is exactly the set of channels that define victory, and the margin is its sum:

    sum(breakdown[me]) - sum(breakdown[opp]) == margin        (verified on the packed rows)

So the critic predicts the seven-term difference d = b[me] - b[opp], and the ranking value is V = sum(d). A card that adds two
printed points raises that channel by two in every game, won or lost, so the channel is learnable even where the outcome is
not explained by it.

    py -3 ml/train_breakdown.py --pack C:/ares/pack/<name> --out D:/ares/tmp/bd1 --epochs 4
"""
import argparse
import json
import math
import os
import sys
import time

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

CHANNELS = ("tr", "forests", "cards", "resourceCards", "milestones", "awards", "extra")
SCALE = 20.0


def load(pack, rows_per_game=16, seed=1):
    """Rows with their observation and the seven-term score difference against the opponent of the same game."""
    manifest = ares_data.open_pack(pack)
    obs_parts, target_parts, margin_parts, game_parts = [], [], [], []
    rng = np.random.default_rng(seed)
    for decision in sorted(manifest["types"]):
        if decision == "AFTERSTATE":
            continue
        data = ares_data.open_packed_type(pack, decision, manifest)
        if "chosen" not in data:
            continue
        labels = np.load(os.path.join(pack, f"{decision}.lab.npz"))
        if "breakdown" not in labels.files:
            continue
        breakdown = labels["breakdown"]
        games = np.asarray(data["game"])
        seats = np.asarray(data["seat"])
        margin = np.asarray(data["margin"], dtype=np.float32)
        # the opponent's breakdown for the same game and the other seat
        by_key = {(int(g), int(s)): i for i, (g, s) in enumerate(zip(games.tolist(), seats.tolist()))}
        keep = np.zeros(len(games), dtype=bool)
        for i, (g, s) in enumerate(zip(games.tolist(), seats.tolist())):
            other = by_key.get((g, 1 - s))
            if other is not None:
                keep[i] = True
        position = np.flatnonzero(keep)
        if rows_per_game:
            order = rng.permutation(len(position))
            keep_pos = position[order[:max(rows_per_game, int(len(position) * 0.1))]]
            position = np.sort(keep_pos)
        if not len(position):
            continue
        opponent = np.array([by_key[(int(games[i]), 1 - int(seats[i]))] for i in position])
        obs_parts.append(np.asarray(data["obs"][position], dtype=np.float32))
        target_parts.append((breakdown[position] - breakdown[opponent]).astype(np.float32))
        margin_parts.append(margin[position])
        game_parts.append(games[position])
    obs = np.concatenate(obs_parts)
    target = np.concatenate(target_parts)
    margin = np.concatenate(margin_parts)
    game = np.concatenate(game_parts)
    if np.abs(target.sum(axis=1) - margin).max() > 1e-3:
        raise SystemExit("the breakdown does not sum to the margin: the pack is inconsistent")
    return obs, target, margin, game


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--hidden", default="512,256")
    parser.add_argument("--batch", type=int, default=4096)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--rows-per-game", type=int, default=16)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--export", default="", help="collapse the seven heads into one value model (V = sum) and write it "
                                                     "in the Java format")
    args = parser.parse_args()

    import torch
    from torch import nn

    device = torch.device("cuda" if (args.device == "auto" and torch.cuda.is_available()) else
                          ("cpu" if args.device == "auto" else args.device))
    torch.manual_seed(args.seed)
    obs, target, margin, game = load(args.pack, args.rows_per_game, args.seed)
    print(f"{args.pack}: {len(obs)} rows, {len(np.unique(game))} games, channels {CHANNELS}", flush=True)

    # split by game, so no game sits on both sides
    unique = np.unique(game)
    rng = np.random.default_rng(args.seed)
    held = set(rng.permutation(unique)[:max(1, len(unique) // 10)].tolist())
    validation = np.array([g in held for g in game])
    train_idx, val_idx = np.flatnonzero(~validation), np.flatnonzero(validation)

    # the owner's scale: divide by the 99.9th percentile, no clamp
    sample = obs[np.random.default_rng(args.seed + 11).choice(train_idx, size=min(200_000, len(train_idx)),
                                                              replace=False)]
    std = np.percentile(np.abs(sample), 99.9, axis=0)
    std = np.where(std < 1e-6, 1.0, std).astype(np.float32)
    mean = np.zeros(obs.shape[1], dtype=np.float32)

    class Net(nn.Module):
        def __init__(self):
            super().__init__()
            self.register_buffer("mean", torch.as_tensor(mean))
            self.register_buffer("std", torch.as_tensor(std))
            h1, h2 = [int(x) for x in args.hidden.split(",")]
            self.w1 = nn.Linear(obs.shape[1], h1)
            self.w2 = nn.Linear(h1, h2)
            self.w3 = nn.Linear(h2, len(CHANNELS))

        def forward(self, x):
            z = (x - self.mean) / self.std
            h = torch.relu(self.w1(z))
            h = torch.relu(self.w2(h))
            return self.w3(h)

    model = Net().to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr, weight_decay=1e-5)
    y_all = torch.from_numpy(target / SCALE).to(device)

    def batches(index, batch):
        order = rng.permutation(index)
        for start in range(0, len(order), batch):
            part = order[start:start + batch]
            if len(part) < 2:
                continue
            yield (torch.from_numpy(obs[part]).to(device), y_all[part])

    history = []
    for epoch in range(1, args.epochs + 1):
        model.train()
        running, seen = 0.0, 0
        for x, y in batches(train_idx, args.batch):
            out = model(x)
            loss = nn.functional.mse_loss(out, y)
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            running += float(loss) * len(x)
            seen += len(x)
        model.eval()
        with torch.no_grad():
            predictions = []
            for start in range(0, len(val_idx), args.batch):
                part = val_idx[start:start + args.batch]
                predictions.append(model(torch.from_numpy(obs[part]).to(device)).cpu().numpy())
        predicted = np.concatenate(predictions) * SCALE
        truth = target[val_idx]
        per_channel = np.mean((predicted - truth) ** 2, axis=0)
        margin_error = float(np.mean((predicted.sum(axis=1) - margin[val_idx]) ** 2))
        constant = float(np.mean((truth - truth.mean(axis=0)) ** 2, axis=0).mean())
        row = {"epoch": epoch, "loss": running / max(seen, 1), "margin_mse": margin_error,
               "constant_mse": constant,
               "channels": {c: round(float(v), 3) for c, v in zip(CHANNELS, per_channel)}}
        history.append(row)
        print(json.dumps(row), flush=True)
        os.makedirs(args.out, exist_ok=True)
        json.dump({"history": history, "channels": CHANNELS, "hidden": args.hidden},
                  open(os.path.join(args.out, "history.json"), "w", encoding="utf-8"), indent=1)
        torch.save({"model": model.state_dict(), "mean": mean, "std": std, "hidden": args.hidden},
                   os.path.join(args.out, "model.pt"))
    if args.export:
        # V = sum of the seven channel heads, so the exported model is an ordinary one-output value net: collapse the last
        # layer by summing its rows. That keeps the deployment path untouched (the Java loader sees one logit) while the
        # training signal was per channel.
        state = model.state_dict()
        w3 = state["w3.weight"].detach().cpu().numpy()      # [7, h2]
        b3 = state["w3.bias"].detach().cpu().numpy()        # [7]
        params = {"mean": mean, "std": std,
                  "w1": state["w1.weight"].detach().cpu().numpy(), "b1": state["w1.bias"].detach().cpu().numpy(),
                  "w2": state["w2.weight"].detach().cpu().numpy(), "b2": state["w2.bias"].detach().cpu().numpy(),
                  "w3": w3.sum(axis=0) / SCALE, "b3": b3.sum() / SCALE}
        import hashlib
        import train_value
        from phase_model import model_hash
        # The Java loader compares the model's observation hash with the current encoder's, so it must be the pack's real
        # hash (sha256 of the observation layout text), not a placeholder.
        observation_hash = ares_data.open_pack(args.pack)["observationHash"]
        sample = np.unique(np.linspace(0, len(val_idx) - 1, min(8, len(val_idx))).astype(int))
        rows = obs[val_idx][sample]

        def score(row):
            z = np.clip((row - mean) / std, -1e6, 1e6)
            h1 = np.maximum(params["w1"] @ z + params["b1"], 0.0)
            h2 = np.maximum(params["w2"] @ h1 + params["b2"], 0.0)
            return float(h2 @ params["w3"] + params["b3"])

        reference = {"inputs": rows.astype(np.float32).tolist(),
                     "logits": [score(r) for r in rows.astype(np.float64)]}
        meta = train_value.export_value(args.export, observation_hash, obs.shape[1],
                                        [int(x) for x in args.hidden.split(",")], 1e6, params, reference)
        json.dump({"exported": args.export, "value": "sum of the seven channel heads",
                   "channels": CHANNELS, "weightsSha256": meta["weightsSha256"]},
                  open(os.path.join(args.out, "export.json"), "w", encoding="utf-8"), indent=1)
        print(f"exported {args.export} (V = sum of {len(CHANNELS)} heads)")
    print(f"done: {args.out}")


if __name__ == "__main__":
    main()
