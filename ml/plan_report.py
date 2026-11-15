"""Does the latent plan code actually change what the player does?

The latent-plan design (services/agent/PlanCodes.java) puts four private bits of a per-game "plan" into the observation,
so that the policy can play a *different style* under each code and the outcome can select the codes that win. The whole
idea stands or falls on one measurable fact: **different codes must produce different behaviour**. If all 16 codes pick
the same phases, build the same number of cards and spend the same way, the code carries no information and there is
nothing to select from.

    py -3 ml/plan_report.py DIR [DIR2 ...] [--min-games 5]

It reads the recorded decisions of a run (the plan bits are the last four observation columns), splits every row by the
code of the player that made it, and prints:

  * phases picked per code (the style signal: a build player calls 1/2, an engine player calls 3, a draft player 5);
  * cards built per game, build acceptance, drafts per game, blue actions per game, phase-3 standard spends;
  * the win rate and mean margin per code (which codes the outcomes favour);
  * a divergence verdict: chi-square of the phase distribution across codes (does code predict phase at all?) and the
    spread of builds per game.
"""

import argparse
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

STOP = 65535
PLAN_BITS = 4
PHASE_NAMES = {1: "build green", 2: "build blue/red", 3: "blue actions", 4: "income", 5: "draft"}


def code_of(schema, rows):
    """The latent plan of the player that made each decision (the last PLAN_BITS observation columns, LSB first)."""
    obs = np.asarray(rows["obs"], dtype=np.float32)
    code = np.zeros(len(rows), dtype=np.int32)
    for bit in range(PLAN_BITS):
        code |= (obs[:, schema["obsSize"] - PLAN_BITS + bit] > 0.5).astype(np.int32) << bit
    return code


def chi2_sf(stat, dof):
    """Survival function of chi-square, Wilson-Hilferty: good enough to say 'is this structure or noise'."""
    if dof <= 0:
        return 1.0
    z = ((stat / dof) ** (1.0 / 3.0) - (1.0 - 2.0 / (9.0 * dof))) / math.sqrt(2.0 / (9.0 * dof))
    return 0.5 * math.erfc(z / math.sqrt(2.0))


def report(directory, min_games):
    per_code = {}
    games_all = set()

    def bucket(code, game):
        entry = per_code.setdefault(int(code), {"games": set(), "phases": {}, "build_taken": 0, "build_stop": 0,
                                                "drafts": 0, "blue": 0, "standard": {}, "margins": [], "outcomes": []})
        entry["games"].add(int(game))
        return entry

    for path in ares_data.chunk_paths(directory, "PICK_PHASE"):
        schema, rows = ares_data.read_decisions(path)
        codes = code_of(schema, rows)
        for row, code in zip(rows, codes):
            entry = bucket(code, row["game"])
            phase = int(row["ids"][int(row["chosen"])])
            entry["phases"][phase] = entry["phases"].get(phase, 0) + 1
            games_all.add(int(row["game"]))

    for path in ares_data.chunk_paths(directory, "BUILD_CARD"):
        schema, rows = ares_data.read_decisions(path)
        codes = code_of(schema, rows)
        for row, code in zip(rows, codes):
            entry = bucket(code, row["game"])
            games_all.add(int(row["game"]))
            if int(row["ids"][int(row["chosen"])]) == STOP:
                entry["build_stop"] += 1
            else:
                entry["build_taken"] += 1

    for path in ares_data.chunk_paths(directory, "DRAFT_CARDS"):
        schema, rows = ares_data.read_decisions(path)
        codes = code_of(schema, rows)
        for row, code in zip(rows, codes):
            bucket(code, row["game"])["drafts"] += 1
            games_all.add(int(row["game"]))

    for path in ares_data.chunk_paths(directory, "PERFORM_BLUE_ACTION"):
        schema, rows = ares_data.read_decisions(path)
        codes = code_of(schema, rows)
        for row, code in zip(rows, codes):
            entry = bucket(code, row["game"])
            if int(row["ids"][int(row["chosen"])]) != STOP:
                entry["blue"] += 1
            games_all.add(int(row["game"]))

    for path in ares_data.chunk_paths(directory, "STANDARD_ACTION"):
        schema, rows = ares_data.read_decisions(path)
        codes = code_of(schema, rows)
        for row, code in zip(rows, codes):
            entry = bucket(code, row["game"])
            action = int(row["ids"][int(row["chosen"])])
            entry["standard"][action] = entry["standard"].get(action, 0) + 1
            entry["margins"].append(float(row["margin"]))
            entry["outcomes"].append(float(row["outcome"]))
            games_all.add(int(row["game"]))

    # Phase rows also carry the outcome: use every row with an outcome for the per-code strength, not only spends.
    strength = {}
    for type_name in ("PICK_PHASE", "BUILD_CARD"):
        for path in ares_data.chunk_paths(directory, type_name):
            schema, rows = ares_data.read_decisions(path)
            codes = code_of(schema, rows)
            for row, code in zip(rows, codes):
                strength.setdefault(int(code), []).append((float(row["outcome"]), float(row["margin"])))
                games_all.add(int(row["game"]))

    codes = sorted(per_code)
    phases = sorted({p for entry in per_code.values() for p in entry["phases"]})
    lines = []
    lines.append(f"{'code':>4} {'games':>6} {'builds':>7} {'accept':>7} {'drafts':>7} {'blue':>6} "
                 f"{'win':>6} {'margin':>7}  phase shares (1 green, 2 blue/red, 3 actions, 4 income, 5 draft)")
    table = {}
    for code in codes:
        entry = per_code[code]
        taken, stop = entry["build_taken"], entry["build_stop"]
        games = len(entry["games"])
        total_phases = sum(entry["phases"].values())
        share = {p: entry["phases"].get(p, 0) / max(total_phases, 1) for p in phases}
        spent = sum(v for a, v in entry["standard"].items() if a not in (0, 1, 2, 3))
        spent_rows = sum(entry["standard"].values())
        wins = strength.get(code, [])
        win = float(np.mean([w for w, _ in wins])) if wins else float("nan")
        margin = float(np.mean([m for _, m in wins])) if wins else float("nan")
        row = {"games": games, "builds_per_game": taken / max(games, 1),
               "build_acceptance": taken / max(taken + stop, 1), "drafts_per_game": entry["drafts"] / max(games, 1),
               "blue_per_game": entry["blue"] / max(games, 1), "phase_share": {str(p): round(v, 3) for p, v in share.items()},
               "standard_share": {str(a): round(v / max(spent_rows, 1), 3) for a, v in sorted(entry["standard"].items())},
               "phase3_standard_projects": round(spent / max(spent_rows, 1), 3),
               "win_rate": round(win, 4), "mean_margin": round(margin, 2), "rows": total_phases}
        table[str(code)] = row
        if games < min_games:
            continue
        names = " ".join(f"{p}:{share[p]:.2f}" for p in phases if share[p] > 0.02)
        lines.append(f"{code:>4} {games:>6} {row['builds_per_game']:>7.2f} {row['build_acceptance']:>7.2f} "
                     f"{row['drafts_per_game']:>7.2f} {row['blue_per_game']:>6.2f} {win:>6.3f} {margin:>7.1f}  {names}")

    # Divergence: can the code predict the phase? Chi-square over the codes that have enough rows.
    counts = np.zeros((len(codes), len(phases)), dtype=np.float64)
    for i, code in enumerate(codes):
        for j, phase in enumerate(phases):
            counts[i, j] = per_code[code]["phases"].get(phase, 0)
    keep = counts.sum(axis=1) > 0
    counts = counts[keep]
    if counts.shape[0] > 1 and counts.shape[1] > 1:
        expected = counts.sum(axis=1, keepdims=True) * counts.sum(axis=0, keepdims=True) / counts.sum()
        stat = float(((counts - expected) ** 2 / np.maximum(expected, 1e-9)).sum())
        dof = (counts.shape[0] - 1) * (counts.shape[1] - 1)
        p_value = chi2_sf(stat, dof)
    else:
        stat, dof, p_value = 0.0, 0, 1.0
    builds = [table[str(c)]["builds_per_game"] for c in codes if table[str(c)]["games"] >= min_games]
    spread = (max(builds) - min(builds)) if len(builds) > 1 else 0.0
    verdict = []
    if p_value < 1e-3:
        verdict.append(f"the code predicts the phase (chi2={stat:.0f}, dof={dof}, p={p_value:.1e})")
    else:
        verdict.append(f"the code does NOT predict the phase (chi2={stat:.0f}, dof={dof}, p={p_value:.2f})")
    if spread >= 2.0:
        verdict.append(f"builds per game differ by {spread:.1f} between codes")
    else:
        verdict.append(f"builds per game are flat across codes (spread {spread:.1f})")

    return {"directory": directory, "games": len(games_all), "chunk_games": len(games_all),
            "codes": table, "divergence": {"chi2": round(stat, 1), "dof": dof, "p": p_value,
                                           "builds_spread": round(spread, 2), "verdict": verdict},
            "table": lines}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directories", nargs="+")
    parser.add_argument("--min-games", type=int, default=5)
    parser.add_argument("--out", default="")
    args = parser.parse_args()
    reports = []
    for directory in args.directories:
        result = report(directory, args.min_games)
        reports.append(result)
        print("\n".join(result["table"]))
        print("DIVERGENCE: " + "; ".join(result["divergence"]["verdict"]))
        print(f"games recorded: {result['games']}\n")
    if args.out:
        json.dump(reports if len(reports) > 1 else reports[0], open(args.out, "w", encoding="utf-8"), indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main())
