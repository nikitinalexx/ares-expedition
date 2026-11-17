"""The behavioural census of a deployed bundle: what does the player actually do?

One command, before any tuning (docs/AUDIT.md). It plays games with the EXACT deployment configuration against SMART, packs
the recorded rows, and prints the scorecard, the phase mix and the action census of every decision type. The point is to
look at the player, not at the metrics I chose last time: the phase 5 share once fell 20% -> 2.2% and no instrument in the
loop would have shown it.

    py -3 ml/audit_player.py --policy D:/ares/runs/policy/best_18pct_20261117 \
        --critics D:/ares/runs/value/v_best18_s1,D:/ares/runs/value/v_best18_s2,D:/ares/runs/value/v_best18_s3 \
        --phase-critic D:/ares/runs/value/v_best18_s1
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
JAVA = r"C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"


def classpath():
    with open(os.path.join(ROOT, "target", "cp.txt"), encoding="utf-8") as f:
        return "target/classes;" + f.read().strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--policy", required=True)
    parser.add_argument("--critics", required=True)
    parser.add_argument("--phase-critic", default="")
    parser.add_argument("--games", type=int, default=60)
    parser.add_argument("--types", default="BUILD_CARD,DRAFT_CARDS,PICK_PHASE")
    parser.add_argument("--opponent", default="SMART")
    args = parser.parse_args()

    work = tempfile.mkdtemp(prefix="census_", dir="C:/ares/tmp" if os.path.isdir("C:/ares/tmp") else None)
    search = ["-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
              f"-Dares.agent.search.types={args.types}", "-Dares.agent.search.pairs=true",
              f"-Dares.agent.valueModel={args.critics}"]
    if args.phase_critic:
        search.append(f"-Dares.agent.valueModel.phase={args.phase_critic}")

    try:
        print(f"=== 1. {args.games} games vs {args.opponent} with the deployment configuration")
        play_out = os.path.join(work, "games")
        subprocess.run([JAVA, "-Xmx8g"] + search + ["-cp", classpath(),
                        "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "play", "--out", play_out,
                        "--games", str(args.games), "--threads", "14", "--games-per-chunk", str(args.games),
                        "--matchups", f"AGENT:{args.opponent}:1", "--max-rows-per-game-type", "8",
                        "--models", args.policy],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=3600)

        pack = os.path.join(work, "pack")
        subprocess.run([sys.executable, os.path.join(HERE, "pack_dataset.py"), "--data", play_out, "--out", pack,
                        "--candidates"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=3600)

        print("\n=== 2. the action census (every decision type)")
        subprocess.run([sys.executable, os.path.join(HERE, "action_coverage.py"), "--pack", pack,
                        "--out", os.path.join(work, "coverage.json")], timeout=3600)

        print("\n=== 3. the phase mix")
        subprocess.run([sys.executable, os.path.join(HERE, "phase_mix.py"), pack], timeout=3600)

        print(f"\n=== 4. scorecard against {args.opponent}")
        score_path = os.path.join(work, "score.json")
        subprocess.run([JAVA, "-Xmx8g"] + search + ["-cp", classpath(),
                        "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "scorecard", "--a", "AGENT",
                        "--b", args.opponent, "--games", str(args.games), "--concurrency", "10",
                        "--out", score_path, "--models", args.policy],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=3600)
        try:
            with open(score_path, encoding="utf-8") as f:
                blob = json.load(f)
            ours = sum(blob["aCards"].values())
            theirs = sum(blob["bCards"].values())
            print(f"  points   ours {blob['aAvgPoints']:.1f}  theirs {blob['bAvgPoints']:.1f}  "
                  f"win rate {blob['aWinRate'] * 100:.1f}%")
            print(f"  cards/game ours {ours:.1f}  theirs {theirs:.1f}")
        except (OSError, ValueError, KeyError):
            print(f"  the scorecard did not produce a readable report ({score_path})")

        print("\n=== 5. hold on: the flags above are the tuning backlog")
        print("  docs/AUDIT.md: fix a flag or record why it is benign in ml/gap_allowlist.json before tuning anything.")
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
