"""Does calling phase 5 instead of phase 4 actually correlate with a better final margin? Measured on our own games.

The owner's theory (2026-11-17): phase 4 should be rare (~5%) and phase 5 common (~40%), because calling production mostly
gives the opponent their production, and a player without a good hand cannot win. This tool tests the premise before anything
is changed: for every PICK_PHASE row it takes the round and the phase that was actually called, and reports the mean FINAL
margin of the player who called it, per round band. Confounded (the phase is chosen by the current policy, so it reflects
what the policy does in different positions) but it is the game's own data, not an opinion.

    py -3 ml/phase_value.py --pack C:/ares/pack/night01
"""
import argparse
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--rounds", default="1-3,4-6,7-9,10-12,13-99")
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    if "PICK_PHASE" not in manifest["types"]:
        raise SystemExit(f"{args.pack}: no PICK_PHASE")
    data = ares_data.open_packed_type(args.pack, "PICK_PHASE", manifest)
    chosen = np.asarray(data["chosen"])
    margin = np.asarray(data["margin"], dtype=np.float32)
    outcome = np.asarray(data["outcome"], dtype=np.float32)
    round_no = np.asarray(data["round"], dtype=np.int32)
    ids = data["ids"]

    phase = np.full(len(chosen), -1, dtype=np.int32)
    for start in range(0, len(chosen), 4096):
        block = np.arange(start, min(start + 4096, len(chosen)))
        ids_block = ids[block]
        phase[block] = ids_block[np.arange(len(block)), chosen[block]]
    known = phase >= 0
    print(f"{args.pack}: {known.sum()} PICK_PHASE rows, {len(np.unique(np.asarray(data['game'])))} games")
    print(f"phase share: {[f'{p}:{(phase == p).mean() * 100:.0f}%' for p in range(5) if (phase == p).any()]}")
    print()
    bands = []
    for spec in args.rounds.split(","):
        low, high = (int(x) for x in spec.split("-"))
        bands.append((spec, (round_no >= low) & (round_no <= high)))
    header = f"{'rounds':>8s} {'rows':>7s}"
    for p in range(5):
        header += f" {'ph' + str(p + 1) + ' margin':>14s}"
    print(header)
    print('-' * len(header))
    for label, band in bands:
        rows = band & known
        line = f"{label:>8s} {int(rows.sum()):7d}"
        for p in range(5):
            values = margin[rows & (phase == p)]
            line += f" {(f'{values.mean():7.2f} (n={len(values)})' if len(values) >= 20 else '        -      '):>14s}"
        print(line)
    print()
    print('win rate by phase (all rows):')
    for p in range(5):
        rows = known & (phase == p)
        if rows.sum() >= 20:
            print(f"  phase {p + 1}: n={int(rows.sum()):6d}  mean margin {margin[rows].mean():7.2f}  "
                  f"win rate {outcome[rows].mean():.3f}")


if __name__ == "__main__":
    main()
