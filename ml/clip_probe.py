"""Does V distinguish 50 MC from 200 MC? Ask the exported models directly.

The input clamp is mean +/- clip * std. With clip = 5 and the encoder's mc/100, everything above ~0.63 (about 63 MC) enters
the first layer as the same number, so two positions that differ only in money become indistinguishable. This sets the mc
column to a series of values on real rows and prints the mean V for each model, which shows exactly where the response
flattens.

    py -3 ml/clip_probe.py --pack C:/ares/pack/mirror_allrows --models D:/ares/tmp/clip5 D:/ares/tmp/clip20 \
        --feature mc --values 0.1,0.2,0.4,0.63,0.8,1.2,2.0
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402
from train_policy_ce import load_value_params, value_of  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--models", nargs="+", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--feature", default="mc")
    parser.add_argument("--values", default="0.1,0.2,0.4,0.63,0.8,1.2,2.0")
    parser.add_argument("--rows", type=int, default=4000)
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    column = names.index(args.feature)
    manifest = ares_data.open_pack(args.pack)
    frames = []
    for decision in sorted(manifest["types"]):
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = min(len(data["obs"]), args.rows // len(manifest["types"]) + 1)
        if rows:
            frames.append(np.asarray(data["obs"][np.arange(rows)], dtype=np.float32))
    obs = np.concatenate(frames)
    print(f"{args.pack}: {len(obs)} rows, feature '{args.feature}' (column {column}), "
          f"observed {obs[:, column].min():.2f}..{obs[:, column].max():.2f}")
    import torch
    for model_dir in args.models:
        params = load_value_params(model_dir)
        value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32) for k, v in params.items()}
        value["clip"] = float(params["clip"])
        mean, std = params["mean"][column], params["std"][column]
        clip_at = mean + float(params["clip"]) * std
        print(f"\n{model_dir}  (clip {params['clip']:.0f}, column mean {mean:.3f}, std {std:.4f}, "
              f"clip point {clip_at:.3f})")
        print(f"{'value':>8s} {'encoded z':>10s} {'mean V':>9s} {'dV vs previous':>15s}")
        previous = None
        for text in args.values.split(","):
            probe = obs.copy()
            probe[:, column] = float(text)
            result = float(value_of(torch.from_numpy(probe), value).mean())
            delta = "" if previous is None else f"{result - previous:+.5f}"
            print(f"{float(text):8.2f} {(float(text) - mean) / std:10.2f} {result:9.5f} {delta:>15s}")
            previous = result


if __name__ == "__main__":
    main()
