"""Decision-level audit of "finish the game now": when one parameter is all that is left, does the agent take it?

The owner's scenario (22/11): if buying the last ocean (or the last temperature step) ends the game and the player is ahead
even after the opponent sells everything, ending NOW is a certain win and must be chosen. The observation carries both
scores and the three "left" ratios (`oxygen_left`, `temperature_left`, `oceans_left` = left / max), and the STANDARD_ACTION
candidates carry the kind, so this is readable from the recorded decisions - no new games.

    py -3 ml/end_game_audit.py --pack C:/ares/pack/1118-2332_17

Kinds (StandardActionDecisions): 0 STOP, 1 EXCHANGE_ONE_HEAT, 2 PLANT_FOREST, 3 INCREASE_TEMPERATURE, 4/5 INCREASE_INFRA,
6 SP_OCEAN, 7 SP_FOREST, 8 SP_TEMPERATURE, 9/10 SP_INFRA. Only 6 and 8 can be the last step of the game (oxygen is raised
by cards, not by a standard project).
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

NAMES = "D:/ares/tmp/names_now.json"
KINDS = 11
SP_OCEAN, SP_TEMPERATURE = 6, 8


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--tolerance", type=float, default=0.02)
    args = parser.parse_args()

    names = {name: i for i, name in enumerate(json.load(open(NAMES, encoding="utf-8")))}
    manifest = ares_data.open_pack(args.pack)
    if "STANDARD_ACTION" not in manifest["types"]:
        raise SystemExit("no STANDARD_ACTION in this pack")
    data = ares_data.open_packed_type(args.pack, "STANDARD_ACTION", manifest)
    obs, cands, chosen, count, keep = (data["obs"], data["cands"], data["chosen"], data["count"],
                                       np.asarray(data["keep"], dtype=bool))
    rows = len(chosen)
    print(f"{rows} STANDARD_ACTION rows")

    cases = {SP_OCEAN: dict(rows=0, offered=0, taken=0, ahead=0, ahead_taken=0),
             SP_TEMPERATURE: dict(rows=0, offered=0, taken=0, ahead=0, ahead_taken=0)}
    checked = 0
    for i in range(rows):
        if not keep[i]:
            continue
        row = obs[i]
        oxygen_left, temperature_left, oceans_left = float(row[names["oxygen_left"]]), \
            float(row[names["temperature_left"]]), float(row[names["oceans_left"]])
        lefts = [oxygen_left, temperature_left, oceans_left]
        biggest = int(np.argmax(lefts))
        # exactly one parameter is still open, by one step (the ratios are left/max, and the max differs per parameter)
        if sum(1 for value in lefts if value > args.tolerance) != 1 or lefts[biggest] > 0.15:
            continue
        kind = None
        if biggest == 2:
            kind = SP_OCEAN            # lefts = [oxygen, temperature, oceans]
        elif biggest == 1:
            kind = SP_TEMPERATURE
        if kind is None:
            continue                     # oxygen is not raised by a standard project
        base, n, pick = int(cands.offsets[i]), int(count[i]), int(chosen[i])
        kinds = [int(np.argmax(f[:-1])) for f in cands.flat[base:base + n]]
        checked += 1
        case = cases[kind]
        case["rows"] += 1
        if kind in kinds:
            case["offered"] += 1
        taken = pick >= 0 and kinds[pick] == kind
        if taken:
            case["taken"] += 1
        mine, theirs = float(row[names["score:my_winning_points"]]), float(row[names["score:opponent_winning_points"]])
        if mine > theirs and kind in kinds:
            case["ahead"] += 1
            if taken:
                case["ahead_taken"] += 1
        if mine <= theirs and kind in kinds:
            case["behind"] = case.get("behind", 0) + 1
            if taken:
                case["behind_taken"] = case.get("behind_taken", 0) + 1

    print(f"one parameter from the end: {checked} decisions")
    for kind, case in cases.items():
        name = "SP_OCEAN" if kind == SP_OCEAN else "SP_TEMPERATURE"
        print(f"  {name}: {case['rows']} decisions, offered {case['offered']}, taken {case['taken']} "
              f"({100.0 * case['taken'] / max(case['offered'], 1):.0f}% of offered)")
        print(f"      ahead and offered: {case['ahead']} decisions, ended now {case['ahead_taken']} "
              f"({100.0 * case['ahead_taken'] / max(case['ahead'], 1):.0f}%)")
        print(f"      behind and offered: {case.get('behind', 0)} decisions, ended now {case.get('behind_taken', 0)} "
              f"({100.0 * case.get('behind_taken', 0) / max(case.get('behind', 0), 1):.0f}%)")


if __name__ == "__main__":
    main()
