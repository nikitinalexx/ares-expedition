"""Targeted diagnostics of the critic: what is one point worth, one income, and one specific card?

The owner's list (2026-11-17): how much is a victory point worth at the start and at the end of a game, how does the model
trade income against printed points, and how much do the big cards (AntiGravityTechnology, AiCentral, ...) move the chance to
win. These are local questions about one model, answered by perturbing the observation and reading V - no training, no
statistics over games.

    py -3 ml/diagnostics.py --pack C:/ares/pack/<name> --model D:/ares/runs/value/v_x_s1 \
        --names D:/ares/tmp/names_now.json --cards AntiGravityTechnology,AiCentral,MarsUniversity
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
    parser.add_argument("--cards", default="AntiGravityTechnology,AiCentral,MarsUniversity,UnitedPlanetaryAlliance")
    parser.add_argument("--rows", type=int, default=60_000)
    args = parser.parse_args()

    import torch
    from train_policy_ce import load_value_params, value_of

    names = json.load(open(args.names, encoding="utf-8"))
    column = {name: i for i, name in enumerate(names)}
    manifest = ares_data.open_pack(args.pack)

    obs, rounds = [], []
    step = max(1, manifest["types"].get("PICK_PHASE", {}).get("rows", 1) // args.rows)
    for decision in ("PICK_PHASE", "COLLECT_INCOME", "BUILD_CARD"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        rows = len(data["obs"])
        take = np.arange(0, rows, max(1, rows // (args.rows // 3)))
        obs.append(np.asarray(data["obs"][take], dtype=np.float32))
        rounds.append(np.asarray(data["round"][take], dtype=np.int32))
    obs = np.concatenate(obs)
    rounds = np.concatenate(rounds)

    params = load_value_params(args.model)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32) for k, v in params.items()}
    value["clip"] = float(params["clip"])

    def mean_v(matrix):
        return value_of(torch.from_numpy(matrix.astype(np.float32)), value).mean().item()

    base = mean_v(obs)
    print(f"{args.model} on {args.pack}: {len(obs)} rows, mean V {base:.4f}")

    def perturb(name, delta, mask=None):
        index = column[name]
        probe = obs if mask is None else obs[mask]
        changed = probe.copy()
        changed[:, index] += delta
        return mean_v(changed) - mean_v(probe)

    # ---- 1. one victory point, by stage of the game
    print("\n=== one victory point, by round")
    print(f"{'rounds':>10s} {'rows':>7s} {'dV per +1 point':>17s}")
    for low, high in ((1, 4), (5, 8), (9, 12), (13, 16), (17, 99)):
        band = (rounds >= low) & (rounds <= high)
        if band.sum() < 200:
            continue
        print(f"{f'{low}-{high}':>10s} {int(band.sum()):7d} {perturb('score:my_winning_points', 1.0, band):+17.5f}")

    # ---- 2. income against points: the price of one income in points
    print("\n=== one income unit against one printed point")
    point = perturb("score:my_winning_points", 1.0)
    for income in ("mc_income", "steel_income", "titanium_income", "plants_income", "heat_income", "card_income"):
        if income not in column:
            continue
        effect = perturb(income, 1.0)
        price = effect / point if abs(point) > 1e-9 else float("nan")
        print(f"  +1 {income:16s} dV {effect:+.5f}  = {price:5.2f} victory points")

    # ---- 3. the big cards: how much does having it on the table change the chance to win
    print("\n=== a card on the table (from 0 to 1)")
    for card in [c.strip() for c in args.cards.split(",") if c.strip()]:
        name = f"cards:my_blue_{card}"
        if name not in column:
            print(f"  {card:28s} not in the observation (not a blue card of the played expansions?)")
            continue
        index = column[name]
        probe = obs.copy()
        probe[:, index] = 1.0
        print(f"  {card:28s} dV {mean_v(probe) - base:+.5f}")
    if "cards:in_my_hand_AntiGravityTechnology" in column:
        probe = obs.copy()
        probe[:, column["cards:in_my_hand_AntiGravityTechnology"]] = 1.0
        print(f"  {'AntiGravity in HAND':28s} dV {mean_v(probe) - base:+.5f}")


if __name__ == "__main__":
    main()
