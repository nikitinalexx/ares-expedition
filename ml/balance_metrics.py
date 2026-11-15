"""Two balance metrics for a player, straight from recorded decisions: which phases it picks, how often it builds.

The owner's test after playing AGENT_V2: "he plays phase 3 and buys standard projects, 0-2 cards per game". This measures
exactly that on the decision chunks of two runs (the old parent vs a trained policy), so a degenerate player is visible
without playing a game by hand.

    py -3 ml/balance_metrics.py DIR1 [DIR2 ...]
"""
import json
import os
import sys
from collections import Counter

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

STOP = 65535


def metrics(directory):
    result = {"directory": directory}
    phase_files = ares_data.chunk_paths(directory, "PICK_PHASE")
    phase = Counter()
    games = set()
    for path in phase_files:
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            games.add(int(row["game"]))
            phase[int(row["ids"][int(row["chosen"])])] += 1
    total = sum(phase.values())
    result["games"] = len(games)
    result["phases"] = {p: round(c / max(total, 1), 4) for p, c in sorted(phase.items())}

    build_taken = build_stop = 0
    draft_taken = 0
    for path in ares_data.chunk_paths(directory, "BUILD_CARD"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            if int(row["ids"][int(row["chosen"])]) == STOP:
                build_stop += 1
            else:
                build_taken += 1
    for path in ares_data.chunk_paths(directory, "DRAFT_CARDS"):
        _, rows = ares_data.read_decisions(path)
        draft_taken += len(rows)
    # a build row is written only when the menu had a card; the share of refusals is the "does not build" signal
    result["build_rows_taken"] = build_taken
    result["build_rows_declined"] = build_stop
    result["build_acceptance"] = round(build_taken / max(build_taken + build_stop, 1), 4)
    result["builds_per_game"] = round(build_taken / max(len(games), 1), 2)
    result["drafts_per_game"] = round(draft_taken / max(len(games), 1), 2)

    # Phase-3 spends: how often the agent buys a standard project instead of passing. This is the decision the owner
    # caught by hand ("he pays 28 for two temperatures while cheap cards would do the same").
    action = Counter()
    for path in ares_data.chunk_paths(directory, "STANDARD_ACTION"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            action[int(row["ids"][int(row["chosen"])])] += 1
    total_actions = sum(action.values())
    result["standard_actions"] = {str(k): round(c / max(total_actions, 1), 4) for k, c in sorted(action.items())}
    result["standard_action_rows"] = total_actions
    return result


if __name__ == "__main__":
    for directory in sys.argv[1:]:
        print(json.dumps(metrics(directory), indent=1))
