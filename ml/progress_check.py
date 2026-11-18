"""Does the critic price progress toward a milestone or an award?

The owner's question (2026-11-18): the observation carries 40 milestone columns (isInGame, myProgress, opponentProgress,
isAustellarPick) and 18 award columns, but does the value net *use* them? Milestones pay 3 VP automatically to every player
who reaches the threshold (`WinPointsService.milestonesWinPoints`), awards pay 5/4/2 by standing, and no menu can "claim"
either - so the only thing a player can do about them is reach the threshold, and the only signal that this matters is
dV per unit of progress.

Measured the same way as `ml/card_value_check.py`: read rows, raise one column by one unit, look at the change in V.

    py -3 ml/progress_check.py --pack C:/ares/pack/bdcheck --model D:/ares/runs/value/v_1118-0058_13_s1
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--rows", type=int, default=8000)
    parser.add_argument("--step", type=float, default=0.1,
                        help="how much progress to add; the columns are fractions of the threshold, so 0.1 is a tenth")
    args = parser.parse_args()

    import torch
    from train_policy_ce import load_value_params, value_of

    names = json.load(open(args.names, encoding="utf-8"))
    column = {name: i for i, name in enumerate(names)}
    manifest = ares_data.open_pack(args.pack)
    parts = []
    for decision in ("PICK_PHASE", "COLLECT_INCOME", "BUILD_CARD"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = len(data["obs"])
        take = np.arange(0, rows, max(1, rows // (args.rows // 3)))
        parts.append(np.asarray(data["obs"][take], dtype=np.float32))
    obs = np.concatenate(parts)
    print(f"{len(obs)} rows from {args.pack}", flush=True)

    params = load_value_params(args.model)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32) for k, v in params.items()}
    value["clip"] = float(params["clip"])

    def mean_v(matrix):
        total = 0.0
        for start in range(0, len(matrix), 4096):
            total += float(value_of(torch.from_numpy(matrix[start:start + 4096]), value).sum())
        return total / len(matrix)

    base = mean_v(obs)
    print(f"mean V {base:.4f}", flush=True)

    def probe(index, step):
        changed = obs.copy()
        changed[:, index] += step
        return (mean_v(changed) - base) / step * 1.0

    rows = []
    for name, index in column.items():
        if name.startswith("milestone_") and name.endswith("_myProgress"):
            rows.append(("milestone", name, probe(index, args.step)))
        elif name.startswith("milestone_") and name.endswith("_opponentProgress"):
            rows.append(("milestone (opponent)", name, probe(index, args.step)))
        elif name.startswith("award_") and name.endswith("_myProgress_log"):
            rows.append(("award", name, probe(index, args.step)))
        elif name.startswith("award_") and name.endswith("_opponentProgress_log"):
            rows.append(("award (opponent)", name, probe(index, args.step)))

    # the reference: what one unit of income is worth to the same net
    for name in ("mc_income", "steel_income", "titanium_income", "plants_income", "heat_income"):
        if name in column:
            rows.append(("income, for scale", name, probe(column[name], 1.0)))
    for name in ("tr" if "tr" in column else "score:my_winning_points",):
        if name in column:
            rows.append(("points, for scale", name, probe(column[name], 1.0)))

    print()
    print(f"{'group':22s} {'feature':44s} dV per unit")
    for group, name, delta in sorted(rows, key=lambda r: (r[0], r[1])):
        print(f"{group:22s} {name:44s} {delta:+.5f}")

    groups = {}
    for group, name, delta in rows:
        groups.setdefault(group, []).append(delta)
    print()
    for group in ("milestone", "milestone (opponent)", "award", "award (opponent)"):
        if group in groups:
            mean = float(np.mean(groups[group]))
            print(f"{group:22s} mean dV per unit of progress {mean:+.5f} over {len(groups[group])} features")


if __name__ == "__main__":
    main()
