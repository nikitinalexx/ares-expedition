"""The fingerprint of an archetype: phase mix + which upgrades the player actually buys.

Why: "is B really a phase-1 fighter?" must be answered by the player's own decisions, not by its score against SMART. The
owner's rule for the specialists (19/11) is exactly this - a phase-1 fighter buys the phase-1 upgrades and plays phase 1; a
phase-3 fighter buys the card-draw upgrade and plays phase 3. The observation already carries the bought upgrades
(`table_phase_N_app_M`), so the fingerprint is read from the corpus.

    py -3 ml/specialist_report.py --pack D:/ares/fixtures/mull7317d
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402
import phase_mix  # noqa: E402

PHASES = 5


def fingerprint(pack, names):
    phases = phase_mix.mix_from_pack(pack)
    counts = {int(v): int((phases == v).sum()) for v in np.unique(phases)}
    total = sum(counts.values()) or 1
    mix = {phase: counts.get(phase, 0) / total for phase in range(1, PHASES + 1)}

    bought = {}
    manifest = ares_data.open_pack(pack)
    if "PICK_PHASE" in manifest["types"]:
        data = ares_data.open_packed_type(pack, "PICK_PHASE", manifest)
        obs = data["obs"]
        games = np.asarray(data["game"])
        for phase in range(1, PHASES + 1):
            for upgrade in (1, 2):
                column = names.index(f"table_phase_{phase}_app_{upgrade}")
                per_game = {}
                for row, game in zip(obs, games.tolist()):
                    if float(row[column]) > 0.5:
                        per_game[game] = True
                bought[(phase, upgrade)] = len(per_game) / max(len(set(games.tolist())), 1)
    return mix, bought


def verdict(mix, bought):
    """Which archetype the corpus says the player is, in the owner's terms."""
    labels = []
    if mix.get(1, 0) >= 0.20 or bought.get((1, 1), 0) + bought.get((1, 2), 0) >= 0.5:
        labels.append("phase-1 fighter (economy: discounts / second green build)")
    if mix.get(3, 0) >= 0.30 or bought.get((3, 2), 0) >= 0.5:
        labels.append("phase-3 fighter (blue actions, draw upgrade)")
    if mix.get(4, 0) >= 0.18 or bought.get((4, 1), 0) + bought.get((4, 2), 0) >= 0.4:
        labels.append("phase-4 fighter (production, double collect)")
    if mix.get(5, 0) >= 0.25:
        labels.append("phase-5 fighter (draft quality)")
    return labels or ["no clear archetype"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    args = parser.parse_args()
    names = json.load(open(args.names, encoding="utf-8"))
    mix, bought = fingerprint(args.pack, names)
    print(f"{args.pack}")
    print("  phases: " + " ".join(f"P{phase}:{100 * share:.0f}%" for phase, share in sorted(mix.items())))
    print("  upgrades bought (share of games):")
    for phase in range(1, PHASES + 1):
        print(f"    phase {phase}: up1 {100 * bought.get((phase, 1), 0):5.1f}%  up2 {100 * bought.get((phase, 2), 0):5.1f}%")
    print("  verdict: " + "; ".join(verdict(mix, bought)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
