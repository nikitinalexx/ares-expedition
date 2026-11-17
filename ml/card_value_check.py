"""Does the critic rank cards by what they actually score? One number for the whole catalogue.

The owner's question (2026-11-17): how much does V value a card with printed points against a card that only gives income? The
targeted probes said one income unit is worth ~75 printed points to the model, where the game pays ~1 point for a point and an
income over the remaining rounds is worth single digits - i.e. the model may be systematically biased towards income.

This measures it across the catalogue: for every blue card, the change in V when its table bit goes from 0 to 1, against its
printed winning points from the Java source. If the correlation is near zero, the critic cannot tell a scoring card from an
income card, and every card choice the search makes inherits that.

    py -3 ml/card_value_check.py --pack C:/ares/pack/<name> --model D:/ares/runs/value/v_x_s1
"""
import argparse
import json
import os
import re
import sys
import glob

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CARDS = os.path.join(ROOT, "src", "main", "java", "com", "terraforming", "ares", "cards")


def printed_points():
    """{class name: printed winning points} from the card sources."""
    result = {}
    for path in glob.glob(os.path.join(CARDS, "**", "*.java"), recursive=True):
        text = open(path, encoding="utf-8", errors="ignore").read()
        name = re.search(r"public\s+(?:abstract\s+)?class\s+(\w+)", text)
        if not name:
            continue
        block = re.search(r"getWinningPoints\(\)\s*\{(.*?)\}", text, re.S)
        if not block:
            result[name.group(1)] = 0
            continue
        numbers = [int(x) for x in re.findall(r"return\s+(\d+)", block.group(1))]
        result[name.group(1)] = numbers[0] if numbers else 0
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--rows", type=int, default=20_000)
    parser.add_argument("--top", type=int, default=12)
    args = parser.parse_args()

    import torch
    from train_policy_ce import load_value_params, value_of

    names = json.load(open(args.names, encoding="utf-8"))
    column = {name: i for i, name in enumerate(names)}
    manifest = ares_data.open_pack(args.pack)
    obs = []
    for decision in ("PICK_PHASE", "COLLECT_INCOME"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = len(data["obs"])
        take = np.arange(0, rows, max(1, rows // (args.rows // 2)))
        obs.append(np.asarray(data["obs"][take], dtype=np.float32))
    obs = np.concatenate(obs)

    params = load_value_params(args.model)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32) for k, v in params.items()}
    value["clip"] = float(params["clip"])
    base = value_of(torch.from_numpy(obs), value).mean().item()

    points = printed_points()
    measured = []
    for name, index in column.items():
        if not name.startswith("cards:my_blue_"):
            continue
        card = name[len("cards:my_blue_"):]
        if card not in points:
            continue
        probe = obs.copy()
        probe[:, index] = 1.0
        dv = value_of(torch.from_numpy(probe), value).mean().item() - base
        measured.append((card, points[card], dv))
    if len(measured) < 10:
        raise SystemExit(f"only {len(measured)} cards matched - the pack or the names file is from another encoder")

    dv = np.array([m[2] for m in measured])
    pts = np.array([m[1] for m in measured], dtype=np.float64)
    correlation = np.corrcoef(pts, dv)[0, 1]
    print(f"{args.model} on {args.pack}: {len(measured)} blue cards, mean V {base:.4f}")
    print(f"correlation(printed points, dV) = {correlation:+.3f}")
    print(f"dV of a card with 0 printed points: {dv[pts == 0].mean():+.5f} (n={int((pts == 0).sum())})")
    if (pts > 0).any():
        print(f"dV of a card with printed points:   {dv[pts > 0].mean():+.5f} (n={int((pts > 0).sum())})")
    print()
    order = np.argsort(dv)
    print("most UNDERVALUED by the critic (printed points first):")
    for i in order[:args.top]:
        print(f"  {measured[i][0]:32s} printed {measured[i][1]:3d}  dV {measured[i][2]:+.5f}")
    print("most OVERVALUED (no or few points, high dV):")
    for i in order[::-1][:args.top]:
        print(f"  {measured[i][0]:32s} printed {measured[i][1]:3d}  dV {measured[i][2]:+.5f}")


if __name__ == "__main__":
    main()
