"""Does the policy know that a build phase can offer two builds - and does passing throw both away?

The rules: in phases 1/2 every player gets one build, and the player who picked the phase gets an extra one (plus a
discount), so a build phase can present two build opportunities in a row. The agent's pass is `skipTurn`, which ends the
whole chain for that player: declining the first opportunity forfeits the second one as well.

This reads the recorded BUILD_CARD rows of a run, groups them by the number of build slots the position reports
(`ctx:my_green_builds`, `ctx:my_blue_red_builds`), and prints the acceptance rate per group - i.e. whether the player
treats "one slot left" and "two slots left" differently. It also cross-tabulates against the round, because the cost of a
build falls with the round (a card built in round r pays 23 - r times).

    py -3 ml/slot_report.py DIR [DIR2 ...]
"""
import json
import os
import sys
from collections import defaultdict

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

STOP = 65535
NAMES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ml", "slot_names.json")


def names_of(path):
    """Feature names of this data: the chunk schema has none, so the current encoder's list is used (it matches)."""
    names = json.load(open(NAMES, encoding="utf-8")) if os.path.exists(NAMES) else None
    return names


def main():
    names = names_of(NAMES)
    if names is None:
        raise SystemExit("ml/slot_names.json is missing: run `--mode names --out ml/slot_names.json` first")
    green = names.index("ctx:my_green_builds")
    blue = names.index("ctx:my_blue_red_builds")
    rows = defaultdict(lambda: {"taken": 0, "declined": 0})
    per_round = defaultdict(lambda: {"taken": 0, "declined": 0})
    total = 0
    for directory in sys.argv[1:]:
        for path in ares_data.chunk_paths(directory, "BUILD_CARD"):
            _, chunk = ares_data.read_decisions(path)
            obs = np.asarray(chunk["obs"], dtype=np.float32)
            chosen_ids = chunk["ids"][np.arange(len(chunk)), chunk["chosen"]]
            accepted = chosen_ids != STOP
            slots = obs[:, green] + obs[:, blue]
            for slot_count, ok, round_no in zip(slots.round().astype(int), accepted, chunk["round"]):
                rows[slot_count]["taken" if ok else "declined"] += 1
                band = "early" if round_no <= 8 else ("mid" if round_no <= 16 else "late")
                per_round[(band, slot_count)]["taken" if ok else "declined"] += 1
                total += 1
    print(f"{'slots':>6} {'rows':>7} {'accept':>7}")
    for slot_count in sorted(rows):
        entry = rows[slot_count]
        n = entry["taken"] + entry["declined"]
        print(f"{slot_count:>6} {n:>7} {entry['taken'] / max(n, 1):>7.3f}")
    print("\nby round band:")
    for key in sorted(per_round):
        band, slot_count = key
        entry = per_round[key]
        n = entry["taken"] + entry["declined"]
        if n < 200:
            continue
        print(f"  {band:>5} slots={slot_count} rows={n:>6} accept={entry['taken'] / n:.3f}")
    print(f"\nrows: {total}")


if __name__ == "__main__":
    main()
