"""Which option of which decision type is the player systematically wrong about? All 16 types, one pass.

The owner's point (2026-11-17): the printed point is just the one parameter that is easy to see. The same blindness can sit in
the blue-card choice, the blue action, the build input, the standard action, the corporation - anywhere. This scans every
decision type and every option it actually uses, and reports the mean final margin of the games in which that option was
chosen, against the mean for the type. An option that is taken often and sits far below its type's average is a candidate blind
spot; the causal follow-up for it is the engine's rollout test (`--mode rollouttest --type <type>`).

Cheap and observational: the option is chosen by the current player, so a low margin can mean "bad option" or "bad position".
It is a *screening* tool that says where to spend the expensive counterfactual, not a verdict.

    py -3 ml/option_report.py --pack C:/ares/pack/<name> [--type BLUE_INPUT] [--top 8]
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
    parser.add_argument("--type", default="", help="one decision type; default: every type in the pack")
    parser.add_argument("--min-rows", type=int, default=200)
    parser.add_argument("--top", type=int, default=6)
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    types = [args.type] if args.type else sorted(manifest["types"])
    for decision in types:
        if decision not in manifest["types"] or decision == "AFTERSTATE":
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        if "chosen" not in data:
            continue
        chosen = np.asarray(data["chosen"])
        margin = np.asarray(data["margin"], dtype=np.float32)
        outcome = np.asarray(data["outcome"], dtype=np.float32)
        ids = data["ids"]
        option = np.empty(len(chosen), dtype=np.int64)
        for start in range(0, len(chosen), 4096):
            block = np.arange(start, min(start + 4096, len(chosen)))
            rows = ids[block]
            option[block] = rows[np.arange(len(block)), chosen[block]]
        if len(chosen) < args.min_rows:
            continue
        overall = float(margin.mean())
        values, counts = np.unique(option, return_counts=True)
        rows = []
        for value, count in zip(values.tolist(), counts.tolist()):
            if count < 20:
                continue
            mask = option == value
            rows.append((value, count, float(margin[mask].mean()), float(outcome[mask].mean())))
        rows.sort(key=lambda r: r[2])
        print(f"\n=== {decision}: {len(chosen)} rows, mean margin {overall:+.2f}, "
              f"{len(rows)} options with 20+ picks")
        print(f"{'option':>10s} {'picks':>8s} {'share':>7s} {'mean margin':>12s} {'win rate':>9s} {'vs type':>8s}")
        for value, count, mean, win in rows[:args.top]:
            print(f"{value:>10d} {count:8d} {count / len(chosen) * 100:6.1f}% {mean:+12.2f} {win:9.3f} "
                  f"{mean - overall:+8.2f}")
        if len(rows) > args.top:
            best = rows[-1]
            print(f"{best[0]:>10d} {best[1]:8d} {best[1] / len(chosen) * 100:6.1f}% {best[2]:+12.2f} {best[3]:9.3f} "
                  f"{best[2] - overall:+8.2f}   <- best of the type")


if __name__ == "__main__":
    main()
