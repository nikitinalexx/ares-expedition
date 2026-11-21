"""Is the value calibrated? Predicted win probability against the actual outcome, per round.

Why: the owner's observation (21/11) is that the agent "shows 26%" in a position it has already lost 89-132, and that against strong players it
systematically overrates its own position. That is a calibration claim, and it is measurable without a new game: every recorded observation is a state,
every game has an outcome, and the critic is a pure function of the observation.

The forward pass mirrors `MlpValueModel.logit` exactly (mean, std, clip, two ReLU layers, a linear head). The `reference.json` that the trainer writes
next to the weights is used as the test: if this file's arithmetic disagreed with PyTorch, the test would fail instead of producing a plausible curve.

    py -3 ml/value_calibration.py --data D:/ares/data/1120_vs_network_1000 \
        --models D:/ares/runs/value/v_1118-2332_17_s1,D:/ares/runs/value/v_1118-2332_17_s2
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def load(model_dir):
    """The forward pass of one exported value model, as (mean, std, clip, w1, b1, w2, b2, w3, b3)."""
    with open(os.path.join(model_dir, "model.json"), encoding="utf-8") as f:
        meta = json.load(f)
    if meta.get("kind") != "value_mlp":
        raise ValueError(f"{model_dir} is not a value_mlp model")
    obs = int(meta["obsSize"])
    h1, h2 = int(meta["hidden"][0]), int(meta["hidden"][1])
    clip = float(meta["clip"])
    raw = np.fromfile(os.path.join(model_dir, "weights.bin"), dtype="<f4")
    expected = obs + obs + h1 * obs + h1 + h2 * h1 + h2 + h2 + 1
    if raw.size != expected:
        raise ValueError(f"{model_dir}: {raw.size} weights, expected {expected}")
    at = 0
    mean = raw[at:at + obs]; at += obs
    std = raw[at:at + obs]; at += obs
    w1 = raw[at:at + h1 * obs].reshape(h1, obs); at += h1 * obs
    b1 = raw[at:at + h1]; at += h1
    w2 = raw[at:at + h2 * h1].reshape(h2, h1); at += h2 * h1
    b2 = raw[at:at + h2]; at += h2
    w3 = raw[at:at + h2]; at += h2
    b3 = float(raw[at])
    return mean, std, clip, w1, b1, w2, b2, w3, b3


def logits(model, observations):
    """The model's logit for every row, in float64 (the corpus stores float16)."""
    mean, std, clip, w1, b1, w2, b2, w3, b3 = model
    x = observations.astype(np.float64)
    z = np.clip((x - mean) / std, -clip, clip)
    a1 = np.maximum(z @ w1.T + b1, 0.0)
    a2 = np.maximum(a1 @ w2.T + b2, 0.0)
    return a2 @ w3 + b3


def ensemble_logits(models, observations):
    return np.mean([logits(model, observations) for model in models], axis=0)


def sigmoid(x):
    return 1.0 / (1.0 + np.exp(-x))


def calibration(predicted, won, bins=5, edges=None):
    """(bin centre, games, mean predicted, actual win rate) - the honesty curve."""
    if edges is None:
        edges = np.linspace(0.0, 1.0, bins + 1)
    rows = []
    for lo, hi in zip(edges[:-1], edges[1:]):
        sel = (predicted >= lo) & (predicted < hi if hi < 1.0 else predicted <= hi)
        if sel.sum() == 0:
            continue
        rows.append(((lo + hi) / 2, int(sel.sum()), float(predicted[sel].mean()), float(won[sel].mean())))
    return rows


def margin_calibration(predicted, actual, bins=6):
    """The same honesty curve in MARGIN units: (bin centre, games, mean predicted, mean actual).

    Why a second table: the champion's critics are trained with `--target margin`, so their output is the predicted score difference divided by
    `margin_scale`; a sigmoid of it is not a probability and would make the model look absurd. The margin table is the one that answers "does it
    overrate our position", and it is comparable to the engine's own `ScoreBreakdown`.
    """
    if len(predicted) == 0:
        return []
    edges = np.quantile(predicted, np.linspace(0.0, 1.0, bins + 1))
    edges[-1] += 1e-6
    rows = []
    for lo, hi in zip(edges[:-1], edges[1:]):
        sel = (predicted >= lo) & (predicted < hi)
        if sel.sum() == 0:
            continue
        rows.append(((lo + hi) / 2, int(sel.sum()), float(predicted[sel].mean()), float(actual[sel].mean())))
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--models", required=True, help="comma-separated model directories (an ensemble)")
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--type", default="PICK_PHASE")
    parser.add_argument("--rounds", default="1,5,10,15,20,25")
    parser.add_argument("--chunks", type=int, default=0)
    parser.add_argument("--margin-scale", type=float, default=0.0,
                        help="the trainer's margin_scale: when >0 the output is a MARGIN, not a logit (the champion's critics use 20)")
    args = parser.parse_args()

    models = [load(d.strip()) for d in args.models.split(",") if d.strip()]
    rounds = {int(r) for r in args.rounds.split(",") if r.strip()}

    files = sorted(glob.glob(os.path.join(args.data, f"{args.type}.*.ares")))
    if args.chunks:
        files = files[:args.chunks]
    obs, won, rnd, margins = [], [], [], []
    for path in files:
        _, chunk = ares_data.read_decisions(path)
        obs.append(np.asarray(chunk["obs"], dtype=np.float32))
        won.append(np.asarray(chunk["outcome"], dtype=np.float64))
        rnd.append(np.asarray(chunk["round"], dtype=int))
        margins.append(np.asarray(chunk["margin"], dtype=np.float64))
    if not obs:
        raise SystemExit(f"no {args.type} chunks in {args.data}")
    obs = np.concatenate(obs)
    won = np.concatenate(won)
    rnd = np.concatenate(rnd)
    margin = np.concatenate(margins)

    predicted = sigmoid(ensemble_logits(models, obs))
    if args.margin_scale > 0:
        raw = ensemble_logits(models, obs) * args.margin_scale
        print(f"{args.data}: {len(raw)} {args.type} rows, "
              f"mean predicted margin {raw.mean():+.1f}, mean actual margin {margin.mean():+.2f}")
        print("  predicted margin   rows   mean predicted   mean actual")
        for centre, n, mean_pred, mean_actual in margin_calibration(raw, margin):
            print(f"  {centre:>16.1f}  {n:>5}  {mean_pred:>14.1f}  {mean_actual:>11.1f}")
        print("  round   rows   mean predicted   mean actual")
        for r in sorted(rounds):
            sel = rnd == r
            if sel.sum() == 0:
                continue
            print(f"  {r:>5}  {int(sel.sum()):>5}  {raw[sel].mean():>14.1f}  {margin[sel].mean():>11.1f}")
        return

    print(f"{args.data}: {len(predicted)} {args.type} rows, actual win rate {won.mean() * 100:.1f}%, "
          f"mean predicted {predicted.mean() * 100:.1f}%")
    print("  predicted bin   rows   mean predicted   actual")
    for centre, n, mean_pred, actual in calibration(predicted, won):
        print(f"  {centre:>13.2f}  {n:>5}  {mean_pred * 100:>14.1f}%  {actual * 100:>6.1f}%")

    print("  round   rows   mean predicted   actual")
    for r in sorted(rounds):
        sel = rnd == r
        if sel.sum() == 0:
            continue
        print(f"  {r:>5}  {int(sel.sum()):>5}  {predicted[sel].mean() * 100:>14.1f}%  {won[sel].mean() * 100:>6.1f}%")


if __name__ == "__main__":
    main()
