"""How sensitive is the critic to one feature? Perturb it by a fraction of its own spread and read dV.

The point: a term the value function ignores cannot steer the search, no matter how the decision is framed. The
perturbation is expressed in standard deviations of the feature over the sampled states, because the model standardises
its inputs and clips them: a raw delta that looks small can already be at the clip.

    py -3 ml/value_sensitivity.py VALUE_DIR [DATA_DIR]
"""
import glob
import json
import os
import sys

import numpy as np
import torch

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402
from train_policy_ce import load_value_params, value_of  # noqa: E402

NAME_FILES = ["D:/ares/tmp/names_v5.json", "D:/ares/tmp/names_v4.json", "D:/ares/tmp/names795b.json",
              "D:/ares/tmp/names805.json"]
DATA = "D:/ares/data/r13_self"


def names_for(obs_size):
    for path in NAME_FILES:
        if not os.path.exists(path):
            continue
        names = json.load(open(path, encoding="utf-8"))
        if len(names) == obs_size:
            return names
    raise SystemExit(f"no names file with {obs_size} features")


def main():
    value_dir = sys.argv[1]
    data_dir = sys.argv[2] if len(sys.argv) > 2 else DATA
    files = sorted(glob.glob(os.path.join(data_dir, "*.ares")))
    if not files:
        raise SystemExit(f"no chunks in {data_dir}")
    schema, _ = ares_data.read_decisions(files[0])
    names = {name: i for i, name in enumerate(names_for(schema["obsSize"]))}

    # sample the whole run, not the opening: one chunk out of every ten
    obs = []
    for path in files[::max(1, len(files) // 10)][:10]:
        _, rows = ares_data.read_decisions(path)
        if len(rows):
            obs.append(rows["obs"][::max(1, len(rows) // 400)].astype(np.float32))
    obs = torch.from_numpy(np.concatenate(obs))
    params = load_value_params(value_dir)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32) for k, v in params.items()}
    value["clip"] = float(params["clip"])
    base = value_of(obs, value)
    print(f"{value_dir}: {len(obs)} observations sampled across the run, mean V {base.mean():.4f}")

    def show(label, index, points_per_unit):
        column = obs[:, index]
        spread = float(column.std())
        if spread == 0:
            print(f"  {label:34s} constant in this data")
            return
        for sigmas in (0.1, 0.3, 1.0):
            probe = obs.clone()
            probe[:, index] += sigmas * spread
            moved = value_of(probe, value) - base
            per_point = moved.mean() / (sigmas * spread * points_per_unit)
            print(f"  {label:30s} mean {column.mean():7.2f} sd {spread:6.2f}  +{sigmas:4.1f} sd  "
                  f"dV {moved.mean():+.4f} (median {moved.median():+.4f})  dV per point {per_point:+.5f}")

    # one point of score, however it arrives: these four must agree if the value is uniform in points
    for name in ("score:my_winning_points", "forests"):
        if name in names:
            show(name, names[name], 1.0)          # 1 unit = 1 winning point
    if "score:opponent_winning_points" in names:
        show("score:opponent_winning_points", names["score:opponent_winning_points"], 1.0)
    if "mc_income" in names:
        show("mc_income", names["mc_income"], 0.0)   # money per round, not a point
    for name in ("played_cards_count", "blue_cards_count"):
        if name in names:
            show(name, names[name], 0.0)


if __name__ == "__main__":
    main()
