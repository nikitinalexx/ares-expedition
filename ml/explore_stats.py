"""Which behaviour actually pays: measured from mirror games, with no opinion about what is good.

Both seats of a recorded self-play game are played by the same player, so the only difference between them is the
exploration noise; the seat's margin (`myPoints - opponentPoints`) is then a measurement of the line that seat played.
This script relates the margin to everything the observation says about how the seat played - which phases it called,
the income it finished with, tags, card colours, milestones and awards - and prints the effects sorted by size, so the
data decides which strategies are worth learning instead of us assuming it.

    py -3 ml/explore_stats.py DIR [DIR ...]

Correlation is reported with its 95% interval, and for every axis the mean margin of the top and bottom third, which is
easier to read than a coefficient.
"""
import collections
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

NAMES = next((p for p in ("D:/ares/tmp/names805.json", "D:/ares/tmp/names795.json")
               if os.path.exists(p)), "D:/ares/tmp/names795.json")
PHASE_NAMES = {1: "phase 1 build green", 2: "phase 2 build blue/red", 3: "phase 3 actions",
               4: "phase 4 income", 5: "phase 5 draft"}

# Everything the observation says about *how* a seat played. Nothing here is a target: the script only measures.
AXES = [
    "mc_income", "steel_income", "titanium_income", "plants_income", "heat_income", "card_income",
    "played_cards_count", "blue_cards_count", "green_cards_count", "hand_cards_count",
    "table_played_space", "table_played_earth", "table_played_event", "table_played_science",
    "table_played_plant", "table_played_energy", "table_played_building", "table_played_animal",
    "table_played_jupiter", "table_played_microbe",
    "max_mc_per_card", "max_heat_per_card", "max_plants_per_card", "max_cards_per_card",
    "win_points", "forests", "mc", "plants", "heat",
]


def load_games(directory):
    games = {}
    phase_picks = collections.Counter()
    for path in glob.glob(os.path.join(directory, "*.ares")):
        kind = os.path.basename(path).split(".")[0]
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            key = (int(row["game"]), int(row["seat"]))
            game = games.setdefault(key, {"phase": collections.Counter(), "obs": None, "step": -1,
                                          "margin": 0.0, "outcome": 0.0})
            game["margin"] = float(row["margin"])
            game["outcome"] = float(row["outcome"])
            if kind == "PICK_PHASE":
                ids = row["ids"][: int(row["count"])]
                phase = int(ids[int(row["chosen"])])
                game["phase"][phase] += 1
                phase_picks[phase] += 1
            if int(row["step"]) >= game["step"]:
                game["step"] = int(row["step"])
                game["obs"] = row["obs"].astype(np.float32)
    return games, phase_picks


def report(directory, index):
    games, phase_picks = load_games(directory)
    if not games:
        print(f"{directory}: nothing recorded")
        return
    seats = [g for g in games.values() if g["obs"] is not None]
    margin = np.array([g["margin"] for g in seats])
    total_phase = max(sum(phase_picks.values()), 1)
    print(f"\n{directory}: {len(seats)} seats, mean margin {margin.mean():+.2f} "
          f"(sd {margin.std(ddof=1):.1f})")
    print("phase picks:", {PHASE_NAMES[p]: round(c / total_phase, 3) for p, c in sorted(phase_picks.items())})
    print(f"decisive games: win {np.mean([g['outcome'] == 1 for g in seats]):.3f}, "
          f"loss {np.mean([g['outcome'] == 0 for g in seats]):.3f}")

    rows = []
    for phase, name in PHASE_NAMES.items():
        values = np.array([g["phase"][phase] for g in seats], dtype=float)
        rows.append((f"calls of {name}", effect(values, margin)))
    for name in AXES:
        i = index.get(name)
        if i is None:
            continue
        values = np.array([g["obs"][i] for g in seats], dtype=float)
        if values.std() == 0:
            continue
        rows.append((name, effect(values, margin)))

    rows.sort(key=lambda item: -abs(item[1][0]))
    print("\n  how a seat played -> margin (top third vs bottom third of that dimension)")
    print(f"  {'dimension':26s} {'corr':>6s} {'+/-':>5s}   {'low third':>9s} {'high third':>10s}")
    for name, (corr, error, low, high) in rows:
        print(f"  {name:26s} {corr:+6.2f} {1.96 * error:5.2f}   {low:+9.2f} {high:+10.2f}")


def effect(values, margin):
    """Pearson correlation with its standard error, and the mean margin of the low and the high third."""
    if values.std() == 0:
        return 0.0, 0.0, 0.0, 0.0
    corr = float(np.corrcoef(values, margin)[0, 1])
    error = float(np.sqrt(max(1e-12, (1 - corr ** 2) / max(len(values) - 3, 1))))
    low_cut, high_cut = np.quantile(values, [1 / 3, 2 / 3])
    low = float(margin[values <= low_cut].mean()) if (values <= low_cut).any() else 0.0
    high = float(margin[values >= high_cut].mean()) if (values >= high_cut).any() else 0.0
    return corr, error, low, high


def main():
    index = {name: i for i, name in enumerate(json.load(open(NAMES, encoding="utf-8")))}
    for directory in sys.argv[1:]:
        report(directory, index)


if __name__ == "__main__":
    main()
