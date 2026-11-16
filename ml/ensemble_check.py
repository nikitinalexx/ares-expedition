"""One critic or three? The ensemble is the only lever that has ever moved the player by more than a point.

The search averages the readings of every net in `-Dares.agent.valueModel` when it is given a comma-separated list, so the
comparison is exactly one flag. Same pack, same policy, same games: the only difference is how many critics vote.

    py -3 ml/ensemble_check.py --pack C:/ares/pack/night12 --policy D:/ares/runs/policy/night12 --games 3000
"""
import argparse
import json
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = r"C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
SEARCH = ["-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
          "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS", "-Dares.agent.search.pairs=true"]


def classpath():
    with open(os.path.join(ROOT, "target", "cp.txt"), encoding="utf-8") as f:
        return "target/classes;" + f.read().strip()


def critic(pack, out, names, seed):
    started = time.time()
    with open(out + ".log", "w", encoding="utf-8") as sink:
        code = subprocess.run([sys.executable, os.path.join(HERE, "train_value.py"), "--packed", pack,
                               "--out", out, "--target", "margin", "--epochs", "4", "--hidden", "512,256",
                               "--names", names, "--drop-prefix", "cards:in_my_hand_", "--seed", str(seed)],
                              stdout=sink, stderr=subprocess.STDOUT).returncode
    auc = None
    try:
        with open(os.path.join(out, "train_metrics.json"), encoding="utf-8") as f:
            auc = json.load(f).get("auc")
    except (OSError, ValueError):
        pass
    return code == 0, auc, time.time() - started


def compare(policy, critics, games, out):
    started = time.time()
    args = [JAVA, "-Xmx10g"] + SEARCH + [f"-Dares.agent.valueModel={','.join(critics)}", "-cp", classpath(),
            "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "compare", "--a", "AGENT", "--b", "SMART",
            "--games", str(games), "--concurrency", "14", "--out", out, "--models", policy]
    with open(out + ".log", "w", encoding="utf-8") as sink:
        code = subprocess.run(args, stdout=sink, stderr=subprocess.STDOUT).returncode
    score, ci = None, None
    try:
        with open(out, encoding="utf-8") as f:
            blob = json.load(f)
        score, ci = blob.get("aScore"), [blob.get("aScoreCi95Low"), blob.get("aScoreCi95High")]
    except (OSError, ValueError):
        pass
    return code, score, ci, time.time() - started


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--policy", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--seeds", default="1,2,3")
    parser.add_argument("--games", type=int, default=3000)
    parser.add_argument("--out", default="")
    args = parser.parse_args()
    name = os.path.basename(args.pack.rstrip("/\\"))
    out_dir = args.out or f"D:/ares/tmp/ensemble_{name}"
    os.makedirs(out_dir, exist_ok=True)

    critics = []
    for seed in [int(s) for s in args.seeds.split(",")]:
        path = f"D:/ares/runs/value/v_{name}_s{seed}"
        ok, auc, seconds = critic(args.pack, path, args.names, seed)
        print(json.dumps({"seed": seed, "critic": path, "ok": ok, "auc": auc, "seconds": round(seconds, 1)}),
              flush=True)
        if ok:
            critics.append(path)
    if not critics:
        raise SystemExit("no critic trained")

    rows = []
    for label, nets in (("single", critics[:1]), ("ensemble", critics)):
        code, score, ci, seconds = compare(args.policy, nets, args.games, os.path.join(out_dir, label + ".json"))
        row = {"label": label, "nets": len(nets), "score": score, "ci": ci, "seconds": round(seconds, 1),
               "exit": code}
        print(json.dumps(row), flush=True)
        rows.append(row)
    json.dump(rows, open(os.path.join(out_dir, "result.json"), "w", encoding="utf-8"), indent=1)
    if len(rows) == 2 and rows[0]["score"] is not None and rows[1]["score"] is not None:
        print(f"single {rows[0]['score'] * 100:.2f}% vs ensemble {rows[1]['score'] * 100:.2f}% "
              f"({args.games} games each)")


if __name__ == "__main__":
    main()
