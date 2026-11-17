"""Over-use and under-use of every option, measured by the engine itself.

The owner's question (2026-11-17): how can the agent learn on its own that it calls phase 4 too often and phase 5 too rarely?
The frequency alone cannot answer that - what is needed is the frequency *and* the share of positions where the option is the
best one by an actual playout. `--mode rollouttest` replays a position once per candidate and reports which candidate was best
and which one the player chose, with the candidate ids. This tool turns that into a per-option table:

  chosen%   how often the player takes the option
  best%     how often it is the best option by rollout
  regret    mean final margin lost by taking the player's choice instead of the best one

An option with chosen% >> best% is over-used; chosen% << best% is under-used. That is a measurement, not a band: no number is
declared healthy, the table says where the player's frequencies disagree with the game's own outcomes.

    py -3 ml/regret_report.py --report D:/ares/tmp/rollout_phase.json [--report ...]
    py -3 ml/regret_report.py --run --type PICK_PHASE --positions 60 --policy DIR --critics A,B
"""
import argparse
import json
import os
import subprocess
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = r"C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"


def classpath():
    with open(os.path.join(ROOT, "target", "cp.txt"), encoding="utf-8") as f:
        return "target/classes;" + f.read().strip()


def run_rollout(decision, positions, policy, critics, phase_critic, types, out):
    args = [JAVA, "-Xmx10g", "-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
            f"-Dares.agent.search.types={types}", "-Dares.agent.search.pairs=true",
            f"-Dares.agent.valueModel={critics}"]
    if phase_critic:
        args.append(f"-Dares.agent.valueModel.phase={phase_critic}")
    args += ["-cp", classpath(), "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "rollouttest",
             "--type", decision, "--games", str(positions), "--out", out, "--models", policy]
    subprocess.run(args, check=False)


def table(report):
    samples = report.get("samples") or []
    if not samples:
        return
    chosen = Counter()
    best = Counter()
    regret = Counter()
    count = Counter()
    for sample in samples:
        chosen[sample.get("policyId")] += 1
        best[sample.get("bestId")] += 1
        regret[sample.get("policyId")] += sample.get("difference", 0.0)
        count[sample.get("policyId")] += 1
    total = len(samples)
    print(f"\n{report.get('type')}: {total} positions, policy {report.get('policyMargin', 0):+.2f} "
          f"vs best-by-rollout {report.get('bestMargin', 0):+.2f}, difference "
          f"{report.get('meanDifference', 0):+.2f} +/- {1.96 * (report.get('sd', 0) / max(total, 1) ** 0.5):.2f}, "
          f"agreement {report.get('agree', 0)}/{total}")
    print(f"{'option':>8s} {'chosen%':>8s} {'best%':>7s} {'over(+)/under(-)':>17s} {'regret on it':>13s}")
    for option in sorted(set(chosen) | set(best)):
        chosen_share = 100.0 * chosen[option] / max(total, 1)
        best_share = 100.0 * best[option] / max(total, 1)
        mean_regret = regret[option] / max(count[option], 1)
        print(f"{option:>8d} {chosen_share:7.1f}% {best_share:6.1f}% {chosen_share - best_share:+16.1f}% "
              f"{mean_regret:+13.2f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="append", default=[])
    parser.add_argument("--run", action="store_true", help="run the rollout test first")
    parser.add_argument("--type", default="PICK_PHASE")
    parser.add_argument("--positions", type=int, default=60)
    parser.add_argument("--policy", default="")
    parser.add_argument("--critics", default="")
    parser.add_argument("--phase-critic", default="")
    parser.add_argument("--types", default="BUILD_CARD,DRAFT_CARDS,PICK_PHASE")
    parser.add_argument("--out", default="D:/ares/tmp/rollout_report.json")
    args = parser.parse_args()

    reports = list(args.report)
    if args.run:
        if not (args.policy and args.critics):
            raise SystemExit("--run needs --policy and --critics")
        run_rollout(args.type, args.positions, args.policy, args.critics, args.phase_critic, args.types, args.out)
        reports.append(args.out)
    for path in reports:
        with open(path, encoding="utf-8") as f:
            table(json.load(f))


if __name__ == "__main__":
    main()
