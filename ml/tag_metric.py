"""How much microbe play a corpus actually contains, read from the observation.

Why a separate module: the microbe experiment is a supplement, not a strategy, so the only useful number is "how many microbe tags reached the table",
per game and averaged. The observation already carries it (`table_played_microbe`, one of the 11 tag counters, stored as count/5), so the metric needs no
new recording - just the column.

    py -3 ml/tag_metric.py --data D:/ares/data/1120_arm_microbe --names D:/ares/tmp/names_now.json --tag microbe
"""
import argparse
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

SCALE = 5.0  # ObservationBuilder writes each tag counter as count / 5

# The microbe block of `TableResourcesFeature`: nine counters of the microbe resource held on cards, written RAW (no scaling).
MICROBE_COLUMNS = [
    "anaerobic_microbe_count",
    "decomposers_microbe_count",
    "decomposing_fungus_microbe_count",
    "ghg_microbe_count",
    "nitrite_microbe_count",
    "regolith_microbe_count",
    "self_replicating_microbe_count",
    "tardigrades_microbe_count",
    "bacterial_aggregates_microbe_count",
]


def microbe_held(rows, columns):
    """Microbe resources held on cards at each seat's last recorded round, summed over the nine counters."""
    per = {}
    for row in rows:
        key = (int(row["game"]), int(row["seat"]))
        round_no = int(row["round"])
        if key not in per or round_no > per[key][0]:
            per[key] = (round_no, sum(float(row["obs"][column]) for column in columns))
    return {key: value for key, (_, value) in per.items()}


def last_per_seat(rows, column):
    """The value at the LAST recorded round of each (game, seat): the table at the end of the game."""
    per = {}
    for row in rows:
        key = (int(row["game"]), int(row["seat"]))
        round_no = int(row["round"])
        if key not in per or round_no > per[key][0]:
            per[key] = (round_no, float(row["obs"][column]))
    return {key: value for key, (_, value) in per.items()}


def mean(values):
    values = list(values)
    return sum(values) / len(values) if values else float("nan")


def share_at_least(values, threshold):
    values = list(values)
    return sum(1 for v in values if v >= threshold) / len(values) if values else float("nan")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--tag", default="microbe")
    parser.add_argument("--chunks", type=int, default=10)
    parser.add_argument("--threshold", type=float, default=1.0,
                        help="a game-seat counts as 'with the tag' when its end-of-game counter reaches this many cards")
    parser.add_argument("--microbes", action="store_true",
                        help="use the nine microbe-resource counters (the cards that hold microbes) instead of the tag counter")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    rows = []
    for path in sorted(glob.glob(os.path.join(args.data, "PICK_PHASE.*.ares")))[:args.chunks]:
        _, chunk = ares_data.read_decisions(path)
        rows.append(chunk)
    all_rows = [row for chunk in rows for row in chunk]
    if args.microbes:
        columns = [names.index(name) for name in MICROBE_COLUMNS]
        per_seat = microbe_held(all_rows, columns)
        values = list(per_seat.values())
        print(f"{args.data}: {len(values)} game-seats, microbe RESOURCES held on cards")
        print(f"  mean per game-seat at the end: {mean(values):.2f}")
        print(f"  game-seats with at least {args.threshold:g}: {share_at_least(values, args.threshold) * 100:.1f}%")
        return

    column = names.index(f"table_played_{args.tag}")
    rows = []
    for path in sorted(glob.glob(os.path.join(args.data, "PICK_PHASE.*.ares")))[:args.chunks]:
        _, chunk = ares_data.read_decisions(path)
        rows.append(chunk)
    all_rows = [row for chunk in rows for row in chunk]
    per_seat = last_per_seat(all_rows, column)
    values = [value * SCALE for value in per_seat.values()]
    print(f"{args.data}: {len(values)} game-seats, tag '{args.tag}'")
    print(f"  mean tags on the table at the end: {mean(values):.2f}")
    print(f"  game-seats with at least {args.threshold:g}: {share_at_least(values, args.threshold) * 100:.1f}%")


if __name__ == "__main__":
    main()
