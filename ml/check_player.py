"""Acceptance check for a player: what does it actually DO, not only how often it wins.

Owner's rule (2026-11-14): every new network is first measured on two numbers - **cards built per game** and **which phases
it picks**. A player that wins against RANDOM by buying standard projects in phase 3 and never building cards is a weak
player with a scoring trick, and the win rate alone hides it.

    py -3 ml/check_player.py --model DIR [--games 300] [--opponent MIX] [--search] [--keep DIR]

It plays the games itself (self-play, decisions recorded), then reports:

  * phases picked (share per phase) - a real player mixes 1/2/5 and calls 4 only when it pays;
  * cards built per game and how often a build offer is accepted;
  * what it does in phase 3: pass, convert heat/plants, or buy standard projects for money;
  * win rate against the reference opponents it played against;
  * a verdict line that fails when the profile is degenerate.

The recorded data is deleted afterwards unless --keep is given.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

SEARCH_FLAGS = [
    "-Dares.agent.search=oneply",
    "-Dares.agent.search.symmetric=true",
    "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS",
]
# The phase pick is not a card ranking: every candidate is a different round, so the search decides (full comparison).
PHASE_SEARCH_FLAGS = [
    "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS,PICK_PHASE",
    "-Dares.agent.search.fullTypes=PICK_PHASE",
]
PHASE_NAMES = {1: "1 build green", 2: "2 build blue/red", 3: "3 blue actions", 4: "4 income", 5: "5 draft"}


def run(cmd, log_path):
    with open(log_path, "a", encoding="utf-8") as log:
        log.write("\n$ " + " ".join(cmd) + "\n")
        log.flush()
        return subprocess.run(cmd, stdout=log, stderr=subprocess.STDOUT, cwd=ROOT).returncode


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, help="model bundle to check")
    parser.add_argument("--games", type=int, default=300)
    parser.add_argument("--opponent", default="SMART", help="SMART, RANDOM or a matchup list AGENT:SMART:0.7,AGENT:RANDOM:0.3")
    parser.add_argument("--search", action="store_true", help="enable the 1-ply search during these games")
    parser.add_argument("--phase-search", action="store_true",
                        help="also let the search pick the phase (full comparison on PICK_PHASE): the strategic decision")
    parser.add_argument("--value-model", default="D:/ares/runs/value/v_r3_outcome")
    parser.add_argument("--phase-value-model", default="",
                        help="separate critic for the phase pick (-Dares.agent.valueModel.phase): entering a phase is a "
                             "different question from ranking a card")
    parser.add_argument("--no-references", action="store_true",
                        help="skip the win-rate references: only the profile (strength is measured separately)")
    parser.add_argument("--must-build", action="store_true",
                        help="the search decides build-or-stop: build the best available card instead of letting the policy "
                             "decline (the measurement that our player hoards credits while SMART converts them into cards)")
    parser.add_argument("--phase-playout", action="store_true",
                        help="score a phase pick by playing the round out (until the next pick), then V, instead of V of "
                             "the start of the phase")
    parser.add_argument("--horizon", type=int, default=0,
                        help="generations to play out before the leaf value is read (0 = score the immediate afterstate)")
    parser.add_argument("--samples", type=int, default=1, help="determinized playouts per candidate")
    parser.add_argument("--keep", default="", help="directory to keep the recorded games in")
    parser.add_argument("--out", default="", help="write the JSON report here as well")
    args = parser.parse_args()

    directory = args.keep or tempfile.mkdtemp(prefix="check_player_")
    os.makedirs(directory, exist_ok=True)
    java = shutil.which("java") or "java"
    classpath = os.path.join(ROOT, "target", "classes") + os.pathsep + open(os.path.join(ROOT, "target", "cp.txt")).read().strip()
    log = os.path.join(directory, "check.log")

    def play(matchups, out_dir):
        cmd = [java, "-Xmx8g"]
        if args.search:
            cmd += SEARCH_FLAGS + [f"-Dares.agent.valueModel={args.value_model}"]
            if args.horizon:
                cmd += [f"-Dares.agent.search.horizon={args.horizon}", f"-Dares.agent.search.samples={args.samples}"]
            if args.phase_value_model:
                cmd += [f"-Dares.agent.valueModel.phase={args.phase_value_model}"]
            if args.phase_playout:
                cmd += ["-Dares.agent.search.phasePlayout=true"]
            if args.must_build:
                cmd += ["-Dares.agent.search.mustBuild=true"]
            if args.phase_search:
                cmd += PHASE_SEARCH_FLAGS
        cmd += ["-cp", classpath, "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "play",
                "--out", out_dir, "--games", str(args.games), "--threads", str(min(16, os.cpu_count() or 4)),
                "--games-per-chunk", str(max(50, args.games)), "--matchups", matchups,
                "--max-rows-per-game-type", "0", "--models", args.model]
        return run(cmd, log)

    # 1) what it does, against the opponent it is supposed to beat
    behaviour_dir = os.path.join(directory, "behaviour")
    shutil.rmtree(behaviour_dir, ignore_errors=True)
    matchups = args.opponent if ":" in args.opponent else f"AGENT:{args.opponent}:1"
    play(matchups, behaviour_dir)
    sys.path.insert(0, HERE)
    import balance_metrics  # noqa: E402
    behaviour = balance_metrics.metrics(behaviour_dir)

    # 2) how it scores against both references
    results = {}
    for reference in (() if args.no_references else ("SMART", "RANDOM")):
        out = os.path.join(directory, f"vs_{reference}")
        shutil.rmtree(out, ignore_errors=True)
        play(f"AGENT:{reference}:1", out)
    # simulate_compare is the fair way to get a win rate; the recorded games are only for the behaviour metrics
    for reference in (() if args.no_references else ("SMART", "RANDOM")):
        json_path = os.path.join(directory, f"compare_{reference}.json")
        cmd = [java, "-Xmx8g"]
        if args.search:
            cmd += SEARCH_FLAGS + [f"-Dares.agent.valueModel={args.value_model}"]
            if args.horizon:
                cmd += [f"-Dares.agent.search.horizon={args.horizon}", f"-Dares.agent.search.samples={args.samples}"]
            if args.phase_value_model:
                cmd += [f"-Dares.agent.valueModel.phase={args.phase_value_model}"]
            if args.phase_playout:
                cmd += ["-Dares.agent.search.phasePlayout=true"]
            if args.must_build:
                cmd += ["-Dares.agent.search.mustBuild=true"]
            if args.phase_search:
                cmd += PHASE_SEARCH_FLAGS
        cmd += ["-cp", classpath, "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "compare",
                "--a", "AGENT", "--b", reference, "--games", str(args.games), "--concurrency", "16",
                "--out", json_path, "--models", args.model]
        run(cmd, log)
        if os.path.exists(json_path):
            results[reference] = json.load(open(json_path, encoding="utf-8"))

    phases = {PHASE_NAMES.get(int(k), k): v for k, v in behaviour["phases"].items()}
    builds = behaviour["builds_per_game"]
    acceptance = behaviour["build_acceptance"]
    standard = {int(k): v for k, v in behaviour["standard_actions"].items()}
    spends = sum(v for k, v in standard.items() if k not in (0, 1, 2, 3))  # standard projects bought for money
    passes = standard.get(0, 0.0)

    warnings = []
    if builds < 3.0:
        warnings.append(f"builds only {builds:.1f} cards per game")
    if spends > 0.5:
        warnings.append(f"buys a standard project in {spends:.0%} of the phase-3 decisions")
    if passes < 0.1:
        warnings.append(f"almost never passes in phase 3 ({passes:.0%})")
    if phases and max(phases.values()) > 0.8:
        dominant = max(phases, key=phases.get)
        warnings.append(f"{max(phases.values()):.0%} of the phase picks are {dominant}")

    report = {"model": args.model, "games": args.games, "opponent": args.opponent, "search": args.search,
              "phases": phases, "builds_per_game": builds, "build_acceptance": acceptance,
              "phase3": {"pass": passes, "standard_projects": round(spends, 3)},
              "win_rate": {k: round(v["aScore"], 4) for k, v in results.items()},
              "warnings": warnings}
    print(json.dumps(report, indent=1))
    if args.out:
        json.dump(report, open(args.out, "w", encoding="utf-8"), indent=1)
    if not args.keep:
        shutil.rmtree(directory, ignore_errors=True)
    if warnings:
        print("\nPROFILE WARNING: " + "; ".join(warnings))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
