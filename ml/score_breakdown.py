"""Where do the points go? The last observation of every game, agent against opponent.

Win rate says nothing about *how* a player loses. The observation carries both tables' win points, forests, incomes and
card counts, so the last decision of a recorded game is a free scoreboard. Averaging it over a dataset shows whether a
player is behind on terraforming points, on card points, on the engine (income/cards) or on milestones/awards.

    py -3 ml/score_breakdown.py DIR [DIR ...]
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

NAMES_PATH = "D:/ares/tmp/names795.json"
FIELDS = ["win_points", "forests", "mc_income", "card_income", "played_cards_count", "blue_cards_count",
          "green_cards_count", "hand_cards_count"]


def main():
    names = json.load(open(NAMES_PATH, encoding="utf-8"))
    index = {name: i for i, name in enumerate(names)}
    for directory in sys.argv[1:]:
        last = {}
        for decision in ("PICK_PHASE", "BUILD_CARD", "PERFORM_BLUE_ACTION"):
            for path in ares_data.chunk_paths(directory, decision):
                _, rows = ares_data.read_decisions(path)
                for row in rows:
                    game = int(row["game"])
                    step = int(row["step"])
                    if game not in last or step > last[game][0]:
                        last[game] = (step, row["obs"].astype(np.float32))
        if not last:
            print(f"{directory}: no rows")
            continue
        report = {"directory": directory, "games": len(last)}
        mine, theirs = {}, {}
        for name in FIELDS:
            i = index[name]
            j = index.get("op_" + name)
            values = np.array([obs[i] for _, obs in last.values()])
            mine[name] = round(float(values.mean()), 3)
            if j is not None:
                theirs[name] = round(float(np.array([obs[j] for _, obs in last.values()]).mean()), 3)
        report["agent"] = mine
        report["opponent"] = theirs
        report["gap"] = {k: round(mine[k] - theirs[k], 3) for k in mine if k in theirs}
        print(json.dumps(report, indent=1))


if __name__ == "__main__":
    main()
