"""When does the gap against a benchmark open, and in which channel?

Why: against NETWORK we sit at ~42% and the one-step audits say the decisions are not the cause. The next cheapest fact is the SHAPE of the loss: if the gap
opens in the first rounds it is tempo/economy, if it opens at the end it is conversion, and if it opens steadily it is the plan. Nothing here guesses - it
reads the columns the observation already carries at every decision.

The observation stores the V2 block NORMALISED (each column divided by its maximum in `AiConstants.NORMALIZATION_VECTOR`, then masked by
`observation/v3_mask.json`). A gap of 0.1 therefore means different things for money (52) and for cards (6). `--units` maps a final column back to its
divisor, so the table prints in game units; without it the numbers are the raw normalised values.

    py -3 ml/round_gap.py --data D:/ares/data/1120_vs_network_full --chunks 0
    py -3 ml/round_gap.py --data D:/ares/data/1120_vs_network_full --chunks 0 --units \
        --columns "win_points:op_win_points:score,played_cards_count:op_played_cards_count:cards"
"""
import argparse
import glob
import json
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONSTANTS = os.path.join(REPO, "src", "main", "java", "com", "terraforming", "ares", "services", "ai", "AiConstants.java")
MASK = os.path.join(REPO, "src", "main", "resources", "observation", "v3_mask.json")


def normalisation_vector(text):
    """The floats of `NORMALIZATION_VECTOR` in AiConstants.java, in order."""
    match = re.search(r"NORMALIZATION_VECTOR\s*=\s*(?:new float\[\])?\s*\{(.*?)\}", text, re.S)
    if not match:
        raise ValueError("NORMALIZATION_VECTOR not found")
    return [float(value.rstrip("fF")) for value in re.findall(r"-?\d+(?:\.\d+)?[fF]?", match.group(1))]


def divisors(names, keep, normalisation):
    """For every final observation column, the number its normalised value was divided by (1.0 past the V2 block)."""
    out = []
    for i in range(len(names)):
        if i < len(keep):
            out.append(normalisation[keep[i]])
        else:
            out.append(1.0)
    return out


def round_scores(rows, mine_column, theirs_column):
    """Per (game, seat, round): the last recorded values of that round for both seats."""
    per = {}
    for row in rows:
        key = (int(row["game"]), int(row["seat"]), int(row["round"]))
        step = int(row["step"])
        if key not in per or step > per[key][0]:
            per[key] = (step, float(row["obs"][mine_column]), float(row["obs"][theirs_column]))
    return {key: (mine, theirs) for key, (_, mine, theirs) in per.items()}


def gap_by_round(per_round):
    """Mean (mine - theirs) at the end of each round, and the number of games that reached it."""
    if not per_round:
        return []
    by_round = {}
    for (game, seat, rnd), (mine, theirs) in per_round.items():
        by_round.setdefault(rnd, []).append(mine - theirs)
    return [(rnd, float(np.mean(values)), len(values)) for rnd, values in sorted(by_round.items())]


def multi_gap(channels, scales):
    """One gap table per channel. `channels` is label -> per-round pairs, `scales` is a list of (label, divisor)."""
    out = []
    for label, divisor in scales:
        scaled = {key: (mine * divisor, theirs * divisor) for key, (mine, theirs) in channels[label].items()}
        out.append((label, gap_by_round(scaled)))
    return out


def parse_columns(spec, names):
    """`"mine=theirs=label,mine=theirs=label"` -> indices. '=' because column names contain ':'."""
    out = []
    for item in spec.split(","):
        mine, theirs, label = item.split("=")
        out.append((names.index(mine), names.index(theirs), label))
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--chunks", type=int, default=10, help="0 = every chunk")
    parser.add_argument("--mine-column", default="score:my_winning_points")
    parser.add_argument("--theirs-column", default="score:opponent_winning_points")
    parser.add_argument("--label", default="score gap (us - them)")
    parser.add_argument("--columns", default="", help='comma separated "mine:theirs:label"')
    parser.add_argument("--units", action="store_true", help="divide out each column's normalisation")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    scale = [1.0] * len(names)
    if args.units:
        with open(CONSTANTS, encoding="utf-8") as f:
            norm = normalisation_vector(f.read())
        keep = json.load(open(MASK, encoding="utf-8"))["keep"]
        if len(norm) != len(keep) + 210:
            raise ValueError(f"normalisation has {len(norm)} values, expected {len(keep) + 210}")
        scale = divisors(names, keep, norm)

    files = sorted(glob.glob(os.path.join(args.data, "PICK_PHASE.*.ares")))
    if args.chunks:
        files = files[:args.chunks]
    specs = parse_columns(args.columns, names) if args.columns else [
        (names.index(args.mine_column), names.index(args.theirs_column), args.label)]
    channels = {label: {} for _, _, label in specs}
    for path in files:
        _, chunk = ares_data.read_decisions(path)
        for mine, theirs, label in specs:
            channels[label].update(round_scores(chunk, mine, theirs))

    per_round = channels[specs[0][2]]
    print(f"{args.data}: {len(per_round)} (game, seat, round) points")
    if not args.columns:
        mine_column, theirs_column, _ = specs[0]
        table = gap_by_round({key: (mine * scale[mine_column], theirs * scale[theirs_column])
                              for key, (mine, theirs) in per_round.items()})
        print(f"  round   {args.label}   n")
        for rnd, gap, n in table:
            print(f"  {rnd:>5}   {gap:>+20.2f}   {n}")
        return

    tables = multi_gap(channels, [(label, scale[mine]) for mine, _, label in specs])
    rounds = sorted({rnd for _, table in tables for rnd, _, _ in table})
    print("  round  " + "  ".join(f"{label:>12}" for label, _ in tables))
    by_label = {label: {rnd: (gap, n) for rnd, gap, n in table} for label, table in tables}
    for rnd in rounds:
        cells = []
        for label, _ in tables:
            gap, n = by_label[label].get(rnd, (float("nan"), 0))
            cells.append(f"{gap:>+12.2f}")
        print(f"  {rnd:>5}  " + "  ".join(cells))


if __name__ == "__main__":
    main()
