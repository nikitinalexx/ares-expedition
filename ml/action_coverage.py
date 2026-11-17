"""General gap detector: every decision type, every option, every iteration.

The problem it solves (owner, 2026-11-17): a self-training loop writes its next corpus with its own greedy choice, so an
action can stop being used and nothing notices - phase 5 went 20% -> 7% -> 2.2% silently, the mulligan is declined in 100%
of rows, cards are never sold. My earlier guard watched one action (the stop of BUILD_CARD). This one watches all of them.

For every decision type it reports, from the pack (real candidates only):

  * `coverage` = H(chosen) / mean(ln menu size) - how much of the menu the player actually uses. 1.0 = uniform over the
    menu it was offered, 0 = one option always. Menu-normalised, so a 2-option type is not punished against a 40-option one.
  * the per-option share, and the change against a previous corpus;
  * `NEVER USED` - options that exist in the catalogue but were chosen zero times (opt-in: pass --catalogue);
  * `FLAGS` - a type is flagged when coverage < --min-coverage (the player ignores most of the menu), when a heavy option
    fell below --drop, or when an option disappeared entirely.

The loop (`ml/night_loop.py`) consumes the same functions and turns a flag into exploration for that type in the next
generation, so the gap is closed by the process rather than by a human pointing at it.

    py -3 ml/action_coverage.py --pack C:/ares/pack/night03 --previous D:/ares/tmp/coverage_night02.json
"""
import argparse
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402

# A gap with a recorded reason is not a gap: the detector must be teachable, or it cries wolf every iteration and the
# exploration it injects is wasted. The reason is checked by the owner, the entry lives under version control.
ALLOWLIST = os.path.join(HERE, "gap_allowlist.json")


def chosen_distribution(pack, decision, manifest=None):
    """({candidate id: share}, mean menu size, mean ln menu size) of the choices made in one corpus."""
    manifest = ares_data.open_pack(pack) if manifest is None else manifest
    if decision not in manifest["types"]:
        return None
    data = ares_data.open_packed_type(pack, decision, manifest)
    if "chosen" not in data:
        raise SystemExit(f"{pack}: {decision} was packed without candidates; the census needs "
                         f"`pack_dataset.py --candidates` (it reads what was chosen)")
    chosen = np.asarray(data["chosen"])
    if not len(chosen):
        return None
    ids = data["ids"]
    values = np.empty(len(chosen), dtype=np.int64)
    for start in range(0, len(chosen), 4096):
        block = np.arange(start, min(start + 4096, len(chosen)))
        rows = ids[block]
        values[block] = rows[np.arange(len(block)), chosen[block]]
    unique, counts = np.unique(values, return_counts=True)
    distribution = {int(k): float(v) / len(values) for k, v in zip(unique, counts)}
    counts_per_row = np.asarray(data["count"], dtype=np.float64) if "count" in data else np.ones(len(values))
    counts_per_row = np.maximum(counts_per_row, 1.0)
    return distribution, float(counts_per_row.mean()), float(np.mean(np.log(counts_per_row)))


def entropy(distribution):
    return -sum(p * math.log(p) for p in distribution.values() if p > 0)


def coverage(pack):
    """Per decision type: rows, option count, menu size, coverage ratio and the option shares."""
    manifest = ares_data.open_pack(pack)
    result = {}
    for decision in sorted(manifest["types"]):
        found = chosen_distribution(pack, decision, manifest)
        if found is None:
            continue
        distribution, mean_menu, mean_log_menu = found
        used = entropy(distribution)
        result[decision] = {"rows": int(manifest["types"][decision]["rows"]),
                            "values": len(distribution),
                            "meanCandidates": round(mean_menu, 2),
                            "entropy": round(used, 4),
                            "coverage": round(used / mean_log_menu, 4) if mean_log_menu > 0 else 1.0,
                            "distribution": {str(k): round(v, 4) for k, v in sorted(distribution.items())}}
    return result


def thin_options(info, min_share=0.05, max_values=12, min_menu=2.0):
    """Options that exist and are used, but almost never.

    The slot-based coverage cannot see this: a phase menu offers the same five ids every time, so "which slot" varies even
    when the player picks phase 4 in 42% of the rows and phase 5 in 2%. This sensor is for types whose option set is small
    and stable (phases, input kinds, standard actions), not for card menus whose ids differ row to row.
    """
    if info["values"] < 3 or info["meanCandidates"] < min_menu or info["values"] > max_values:
        return []
    return sorted(k for k, v in info["distribution"].items() if v < min_share)


def allowlisted():
    try:
        with open(ALLOWLIST, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {}


def flags(current, previous, min_coverage=0.45, floor=0.10, drop=0.02):
    """Every way a type can silently stop using its options, minus the ones with a recorded reason to be narrow."""
    flagged = []
    benign = allowlisted()
    for decision, now in current.items():
        if decision in benign:
            continue
        # The json form of a corpus keeps the option ids as strings; everything below compares ids, so normalise once.
        now = dict(now, distribution={int(k): v for k, v in now["distribution"].items()})
        best = max(now["distribution"].values()) if now["distribution"] else 0.0
        # A type can be broken in more than one way at once (always one slot AND an option that disappeared); reporting only
        # the first would hide the rest.
        if now["meanCandidates"] > 2.0 and now["coverage"] < min_coverage:
            flagged.append({"decision": decision, "kind": "narrow", "coverage": now["coverage"],
                            "top": best, "menu": now["meanCandidates"], "lost": [], "gone": []})
        thin = thin_options(now)
        if thin:
            flagged.append({"decision": decision, "kind": "thin", "coverage": now["coverage"],
                            "top": best, "menu": now["meanCandidates"], "thin": thin,
                            "shares": {k: now["distribution"][k] for k in thin}, "lost": [], "gone": []})
        before = previous.get(decision)
        if not before:
            continue
        seen = {int(k): v for k, v in before["distribution"].items()}
        heavy = {k for k, v in seen.items() if v >= floor}
        lost = sorted(k for k in heavy if now["distribution"].get(k, 0.0) < drop)
        gone = sorted(k for k in seen if k not in now["distribution"])
        if lost or gone:
            flagged.append({"decision": decision, "kind": "shrunk", "coverage": now["coverage"],
                            "top": best, "menu": now["meanCandidates"], "lost": lost[:10], "gone": gone[:10],
                            "coverage_before": before["coverage"]})
    return flagged


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--previous", default="", help="coverage json of an earlier corpus")
    parser.add_argument("--out", default="")
    parser.add_argument("--top", type=int, default=6)
    parser.add_argument("--min-coverage", type=float, default=0.45)
    args = parser.parse_args()

    current = coverage(args.pack)
    previous = {}
    if args.previous and os.path.exists(args.previous):
        previous = json.load(open(args.previous, encoding="utf-8"))
    print(f"{'type':22s} {'rows':>8s} {'menu':>5s} {'opts':>5s} {'cover':>6s}  top choices")
    print('-' * 96)
    for decision, info in sorted(current.items(), key=lambda kv: kv[1]["coverage"]):
        top = sorted(info["distribution"].items(), key=lambda kv: -kv[1])[:args.top]
        shown = " ".join(f"{k}:{v * 100:.0f}%" for k, v in top)
        print(f"{decision:22s} {info['rows']:8d} {info['meanCandidates']:5.1f} {info['values']:5d} "
              f"{info['coverage']:6.2f}  {shown}")
    found = flags(current, previous, min_coverage=args.min_coverage)
    benign = allowlisted()
    print()
    if found:
        print("FLAGS (the process must act on these, not a human):")
        for item in found:
            if item["kind"] == "narrow":
                print(f"  {item['decision']:22s} NARROW: uses {item['coverage']:.2f} of its menu, "
                      f"one option takes {item['top'] * 100:.0f}% of {item['menu']:.1f} candidates")
            elif item["kind"] == "thin":
                detail = " ".join(f"{k}:{v * 100:.0f}%" for k, v in item["shares"].items())
                print(f"  {item['decision']:22s} THIN: options used under 5% - {detail}")
            else:
                print(f"  {item['decision']:22s} SHRUNK: lost {item['lost']} gone {item['gone']} "
                      f"coverage {item.get('coverage_before')} -> {item['coverage']}")
    else:
        print("no flag: every type uses a reasonable share of the menu it is offered")
    if benign:
        print()
        print("KNOWN BENIGN (recorded reason, no action taken):")
        for decision, reason in sorted(benign.items()):
            print(f"  {decision:22s} {reason}")
    if args.out:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        json.dump(current, open(args.out, "w", encoding="utf-8"), indent=1)


if __name__ == "__main__":
    main()
