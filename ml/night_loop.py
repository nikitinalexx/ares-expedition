"""The night: expert iteration that watches itself.

Each iteration plays games with the current policy + critic, packs them, trains the critic (hand identity masked) and the
policy (cross-entropy on the search's choices), then measures. The loop is not allowed to run blind:

* DATA-DRIFT GUARD. The share of "stop" among the BUILD_CARD rows is the canary. In the first manual iterations it fell
  92% -> 63% -> 32% while the player learned to build 24 cards per game (a healthy player builds ~11), i.e. the loop was
  drifting towards "build everything". If the share drops below a floor the next generation adds exploration on
  BUILD_CARD (`-Dares.agent.explore`), so the corpus keeps both outcomes and the critic can see what a decline is worth.
* STAGNATION GUARD. If the best score does not improve for `--patience` iterations, the iteration doubles its games
  (more data for the critic is the only lever that has ever moved value quality) and logs it loudly. Two doublings in a
  row with no gain stops the night early and says so - a night that grinds a plateau is worse than a short honest one.
* DISK GUARD. Corpus on the SSD, raw files deleted as soon as the pack is verified (the pack is the dataset), only the
  last `--keep-packs` packs kept. Models are never deleted.
* Every iteration appends one row to the ledger, so the morning reads one file.

    py -3 ml/night_loop.py --minutes 480 --games 20000
"""
import argparse
import glob
import json
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import action_coverage  # noqa: E402
ROOT = os.path.dirname(HERE)
CP_FILE = os.path.join(ROOT, "target", "cp.txt")
JAVA = r"C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
SSD_DATA = "C:/ares/data"
PACK_ROOT = "C:/ares/pack"
POLICY_ROOT = "D:/ares/runs/policy"
VALUE_ROOT = "D:/ares/runs/value"
LEDGER = "D:/ares/tmp/night_ledger.json"

SEARCH = ["-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
          "-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS", "-Dares.agent.search.pairs=true"]


def search_args(types, critic, phase_critic=""):
    """The search flags for one run. `types` decides which menus the critic ranks - PICK_PHASE included means the critic
    chooses the phase, with `phase_critic` as its own model set (Dares.agent.valueModel.phase)."""
    args = [a for a in SEARCH if not a.startswith("-Dares.agent.search.types=")]
    args.append(f"-Dares.agent.search.types={types}")
    args.append(f"-Dares.agent.valueModel={critic}")
    if phase_critic:
        args.append(f"-Dares.agent.valueModel.phase={phase_critic}")
    return args


def log(message):
    print(f"[{time.strftime('%H:%M:%S')}] {message}", flush=True)


def classpath():
    with open(CP_FILE, encoding="utf-8") as f:
        return "target/classes;" + f.read().strip()


def play(out, games, policy, critic, threads, explore, matchups, types, phase_critic="", timeout=7200):
    os.makedirs(out, exist_ok=True)
    args = [JAVA, "-Xmx10g"] + search_args(types, critic, phase_critic)
    if explore:
        args.append(f"-Dares.agent.explore={explore}")
    args += ["-cp", classpath(), "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "play",
             "--out", out, "--games", str(games), "--threads", str(threads), "--games-per-chunk", "2000",
             "--matchups", matchups, "--max-rows-per-game-type", "8",
             "--models", policy]
    started = time.time()
    log_path = out.rstrip("/\\") + ".play.log"
    with open(log_path, "w", encoding="utf-8") as sink:
        result = subprocess.run(args, stdout=sink, stderr=subprocess.STDOUT, timeout=timeout)
    seconds = time.time() - started
    summary = {}
    try:
        with open(log_path, encoding="utf-8", errors="ignore") as f:
            for line in f:
                if '"gamesPlayed"' in line:
                    summary = json.loads(line[line.index("{"):])
    except (OSError, ValueError):
        pass
    return result.returncode, seconds, summary


def run(cmd, log_path, timeout=7200, quiet=False):
    with open(log_path, "w", encoding="utf-8") as sink:
        result = subprocess.run(cmd, stdout=sink, stderr=subprocess.STDOUT, timeout=timeout)
    return result.returncode


def tail(path, count=6):
    try:
        with open(path, encoding="utf-8", errors="ignore") as f:
            return "".join(f.readlines()[-count:])
    except OSError:
        return ""


def number_in(path, key):
    """First number that follows "key": in the tool output (a JSON blob may be nested or preceded by log lines)."""
    import re
    try:
        with open(path, encoding="utf-8", errors="ignore") as f:
            match = re.search(r'"' + re.escape(key) + r'"\s*:\s*(-?[0-9.]+)', f.read())
        return float(match.group(1)) if match else None
    except OSError:
        return None


def phases_of(path):
    """{"1 build green": 0.11, ...} from the profile log: the phase the player calls, as a share of its picks.

    The owner's hypothesis (2026-11-17): calling phase 4 often hands the opponent production, and rarely calling 5 leaves us
    with a poor hand. Both are visible here, and this is the instrument that has to be checked every iteration.
    """
    import re
    try:
        with open(path, encoding="utf-8", errors="ignore") as f:
            text = f.read()
        block = text[text.index('"phases"'):]
        return {name: float(value) for name, value in re.findall(r'"([1-5] [a-z/ ]+)":\s*([0-9.]+)', block)}
    except (OSError, ValueError):
        return {}


def free_gb(path):
    import ctypes
    free = ctypes.c_ulonglong(0)
    ctypes.windll.kernel32.GetDiskFreeSpaceExW(ctypes.c_wchar_p(path), None, None, ctypes.byref(free))
    return free.value / 2 ** 30


def prune(keep_packs):
    packs = sorted(glob.glob(os.path.join(PACK_ROOT, "*")), key=os.path.getmtime, reverse=True)
    removed = []
    for path in packs[keep_packs:]:
        if os.path.isdir(path):
            shutil.rmtree(path, ignore_errors=True)
            removed.append(os.path.basename(path))
    return removed


def measure(policy, critic, games, out_dir, pack, types, phase_critic=""):
    """The acceptance instruments in order: argmax, profile, and only then the score."""
    argmax = os.path.join(out_dir, "argmax.json")
    stop_share = None
    try:
        with open(argmax, "w", encoding="utf-8") as sink:
            subprocess.run([sys.executable, os.path.join(HERE, "policy_argmax.py"), policy, pack, "BUILD_CARD"],
                           stdout=sink, stderr=subprocess.STDOUT, timeout=1800)
        stop_share = number_in(argmax, "argmax_stop_share")
    except (OSError, subprocess.SubprocessError):
        pass
    profile = os.path.join(out_dir, "profile.log")
    subprocess.run([sys.executable, os.path.join(HERE, "check_player.py"), "--model", policy, "--games", "60",
                    "--opponent", "SMART", "--no-references", "--search", "--value-model", critic],
                   stdout=open(profile, "w", encoding="utf-8"), stderr=subprocess.STDOUT, timeout=3600)
    builds = number_in(profile, "builds_per_game")
    phases = phases_of(profile)
    def versus(opponent, count, label):
        path = os.path.join(out_dir, f"compare_{label}.json")
        args = [JAVA, "-Xmx10g"] + search_args(types, critic, phase_critic) + [
            "-cp", classpath(), "com.terraforming.ares.selfplay.SelfPlayMain", "--mode", "compare", "--a", "AGENT",
            "--b", opponent, "--games", str(count), "--concurrency", "14", "--out", path, "--models", policy]
        with open(os.path.join(out_dir, f"compare_{label}.log"), "w", encoding="utf-8") as sink:
            subprocess.run(args, stdout=sink, stderr=subprocess.STDOUT, timeout=7200)
        try:
            with open(path, encoding="utf-8") as f:
                blob = json.load(f)
            return blob.get("aScore"), [blob.get("aScoreCi95Low"), blob.get("aScoreCi95High")]
        except (OSError, ValueError):
            return None, None

    # The benchmark (SMART) and the floor (RANDOM). The night of 16/11 produced a player at 10.9% vs SMART and 53% vs
    # RANDOM - a specialist that only ever played against a copy of itself - so both numbers are recorded from now on.
    score, ci = versus("SMART", games, "smart")
    random_score, random_ci = versus("RANDOM", max(500, games // 2), "random")
    return {"stop_share": stop_share, "builds_per_game": builds, "phases": phases, "score": score, "ci": ci,
            "score_random": random_score, "ci_random": random_ci, "games": games}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--minutes", type=int, default=480)
    parser.add_argument("--games", type=int, default=20000)
    parser.add_argument("--threads", type=int, default=14)
    parser.add_argument("--epochs-critic", type=int, default=4)
    parser.add_argument("--epochs-policy", type=int, default=3)
    parser.add_argument("--critics", type=int, default=3,
                        help="critics trained per iteration; the search averages them (ensemble_check.py measured 10.28%% "
                             "against 8.63%% for one, 10 000 games, disjoint intervals)")
    parser.add_argument("--patience", type=int, default=3)
    parser.add_argument("--keep-packs", type=int, default=3)
    parser.add_argument("--stop-share-floor", type=float, default=0.18)
    parser.add_argument("--coverage-baseline", default="",
                        help="coverage json to compare the first iteration against (ml/action_coverage.py --out)")
    parser.add_argument("--coverage-floor", type=float, default=0.10)
    parser.add_argument("--regret-every", type=int, default=3,
                        help="every K iterations run the counterfactual regret report on the phase decision (ml/regret_report.py): "
                             "chosen share against best-by-rollout share, i.e. over- and under-use measured by the engine")
    parser.add_argument("--regret-positions", type=int, default=40)
    parser.add_argument("--auto-levers", action="store_true", default=True,
                        help="on a plateau, climb the lever list (docs/AUDIT.md) instead of only enlarging the corpus")
    parser.add_argument("--entropy", type=float, default=0.01,
                        help="entropy bonus on the policy (0 = off): without it the trainer sharpens the policy onto one "
                             "option every iteration, which is how phase 5 fell to 2%")
    parser.add_argument("--adv-weight-clip", default="0.2,3.0")
    parser.add_argument("--coverage-drop", type=float, default=0.05)
    parser.add_argument("--explore", default="BUILD_CARD:0.15")
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--types", default="BUILD_CARD,DRAFT_CARDS",
                        help="menus the critic ranks. Add PICK_PHASE to let the critic choose the phase - the policy alone "
                             "never calls phase 5 (0% of its picks, and no phase-5 row exists in the corpus)")
    parser.add_argument("--phase-critic", action="store_true",
                        help="use the first critic of the ensemble as the phase-pick model (Dares.agent.valueModel.phase)")
    parser.add_argument("--matchups", default="AGENT:AGENT:0.85,AGENT:RANDOM:0.15",
                        help="opponents of the training games. 85%% mirror matches taught a player that beats a copy of "
                             "itself: 10.89%% vs SMART but only 53%% vs RANDOM (measured 2026-11-17)")
    parser.add_argument("--seed-policy", default="D:/ares/runs/policy/boot3")
    parser.add_argument("--seed-critic", default="D:/ares/runs/value/v_boot3_mask")
    parser.add_argument("--smoke", action="store_true", help="one tiny iteration, to check the plumbing")
    args = parser.parse_args()
    if args.smoke:
        args.games, args.minutes, args.patience = 400, 30, 99

    os.makedirs(os.path.dirname(LEDGER), exist_ok=True)
    ledger = []
    if os.path.exists(LEDGER):
        try:
            ledger = json.load(open(LEDGER, encoding="utf-8"))
        except ValueError:
            ledger = []
    started = time.time()
    # Every artifact carries the run tag. Without it, a second run reuses "night01", "night02", ... and overwrites the
    # previous run's models - which happened on 2026-11-17: the 10.89% bundle was replaced by the next run's iteration 2.
    run_tag = time.strftime("%m%d-%H%M")
    policy, critic = args.seed_policy, args.seed_critic
    best = max([row.get("score") or 0 for row in ledger], default=0.0)
    flat = 0
    games = args.games
    log(f"night starts: policy {policy}, critic {critic}, {games} games per iteration, "
        f"budget {args.minutes} min, best so far {best:.3f}")

    prev_name = ""
    def double_corpus():
        nonlocal games
        games *= 2
        return f"corpus {games // 2} -> {games} games per iteration (more data for the critic)"

    def more_entropy():
        args.entropy = min(0.05, (args.entropy or 0.01) * 2)
        return f"policy entropy {args.entropy:.3f} (the trainer must not sharpen onto one option)"

    def more_exploration():
        args.explore = ",".join(part if ":" not in part else part.split(":")[0] + ":0.60"
                                for part in args.explore.split(",") if part)
        return f"exploration {args.explore} (keep the alternatives in the corpus)"

    def search_more_types():
        args.types = args.types + ",STANDARD_ACTION"
        return f"the critic now ranks {args.types} (let value decide one more menu)"

    ladder = [double_corpus, more_entropy, more_exploration, search_more_types]

    while time.time() - started < args.minutes * 60:
        index = len(ledger) + 1
        name = f"{run_tag}_{index:02d}"
        raw = f"{SSD_DATA}/{name}"
        pack = f"{PACK_ROOT}/{name}"
        out_dir = f"D:/ares/tmp/{name}"
        os.makedirs(out_dir, exist_ok=True)
        log(f"=== iteration {index}: {games} games, explore '{args.explore}'")

        if free_gb("C:/") < 45:
            removed = prune(max(1, args.keep_packs // 2))
            log(f"disk low on C: ({free_gb('C:/'):.0f} GB) - pruned packs {removed}")
        if os.path.isdir(raw):
            shutil.rmtree(raw, ignore_errors=True)

        phase_critic = (critic.split(",")[0] if isinstance(critic, str) else critic[0]) if args.phase_critic else ""
        code, seconds, summary = play(raw, games, policy, critic, args.threads, args.explore, args.matchups,
                                      args.types, phase_critic)
        played = summary.get("gamesPlayed", 0)
        failed = summary.get("gamesFailed", 0)
        # The engine throws loudly when a menu exceeds MAX_CANDIDATES ("menu has N candidates; the decision is sized for M").
        # That is a rule-level blocker, not a training problem: it appears when the player's style changes and the caps were
        # measured on an older one. Surface it as a first-class event instead of a line of stderr.
        overflow = []
        try:
            with open(raw.rstrip("/\\") + ".play.log", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    if "candidates" in line and ("sized for" in line or "has " in line and "options" in line):
                        overflow.append(line.strip()[:160])
                        if len(overflow) >= 5:
                            break
        except OSError:
            pass
        if overflow:
            log(f"CAP OVERFLOW - the menu caps must be re-measured (ml/menu_sizes.py): {overflow[0]}")
        log(f"played {played} games in {seconds / 60:.1f} min ({failed} failed, {summary.get('rows', 0)} rows)")
        if code != 0 or played < games * 0.5:
            log(f"self-play unusable (exit {code}); skipping the iteration")
            break

        code = run([sys.executable, os.path.join(HERE, "pack_dataset.py"), "--data", raw, "--out", pack,
                    "--candidates"], os.path.join(out_dir, "pack.log"))
        if code != 0:
            log(f"packing failed (exit {code}); skipping")
            shutil.rmtree(raw, ignore_errors=True)
            continue
        code = run([sys.executable, os.path.join(HERE, "pack_check.py"), "--pack", pack, "--data", raw,
                    "--limit-chunks", "2"], os.path.join(out_dir, "pack_check.log"))
        if code != 0:
            log(f"the pack does not reproduce the raw chunks; skipping (see {out_dir}/pack_check.log)")
            continue
        shutil.rmtree(raw, ignore_errors=True)

        # Coverage: did this corpus still use every decision type's options? A self-training loop writes the next corpus with
        # its own greedy choice, so a type can collapse silently (phase 5 went 20% -> 7% -> 2.2% in two iterations and
        # nothing noticed). Any flagged type gets exploration in the NEXT generation.
        coverage_path = f"D:/ares/tmp/coverage_{name}.json"
        previous_path = args.coverage_baseline if (index == 1 and args.coverage_baseline) else \
            f"D:/ares/tmp/coverage_{prev_name}.json" if prev_name else ""
        try:
            current = action_coverage.coverage(pack)
            previous = {}
            if previous_path and os.path.exists(previous_path):
                with open(previous_path, encoding="utf-8") as f:
                    previous = json.load(f)
            collapsed = action_coverage.flags(current, previous, floor=args.coverage_floor, drop=args.coverage_drop)
            with open(coverage_path, "w", encoding="utf-8") as f:
                json.dump(current, f, indent=1)
            for item in collapsed:
                log(f"COLLAPSE {item['decision']}: lost {item['lost']} gone {item['gone']} "
                    f"entropy {item['entropy_before']} -> {item['entropy_now']}")
            added = sorted({item["decision"] for item in collapsed if item["decision"] not in args.explore})
            if added:
                args.explore = (args.explore + "," if args.explore else "") + \
                               ",".join(f"{decision}:0.60" for decision in added)
                log(f"exploration added for the next generation: {added} -> {args.explore}")
        except Exception as error:  # noqa: BLE001 - the night must not die on a diagnostic
            collapsed = []
            log(f"coverage check failed ({error}); the iteration continues")

        new_critics = [f"{VALUE_ROOT}/v_{name}_s{k}" for k in range(1, args.critics + 1)]
        critic_ok = True
        for k, new_critic in enumerate(new_critics, start=1):
            code = run([sys.executable, os.path.join(HERE, "train_value.py"), "--packed", pack, "--out", new_critic,
                        "--target", "margin", "--epochs", str(args.epochs_critic), "--hidden", "512,256",
                        "--names", args.names, "--drop-prefix", "cards:in_my_hand_", "--seed", str(k)],
                       os.path.join(out_dir, f"critic{k}.log"))
            ok = code == 0 and os.path.exists(os.path.join(new_critic, "weights.bin"))
            critic_ok = critic_ok and ok
            log(f"critic {k}/{len(new_critics)} {'ok' if ok else 'FAILED'}: "
                f"{tail(os.path.join(out_dir, f'critic{k}.log'), 2).strip()}")
        new_critic = ",".join(new_critics) if critic_ok else ""

        new_policy = f"{POLICY_ROOT}/{name}"
        code = run([sys.executable, os.path.join(HERE, "train_policy_ce.py"), "--packed", pack, "--out", new_policy,
                    "--epochs", str(args.epochs_policy), "--entropy", str(args.entropy),
                    "--adv-weight-clip", args.adv_weight_clip], os.path.join(out_dir, "policy.log"))
        policy_ok = code == 0 and len(glob.glob(os.path.join(new_policy, "*", "model.json"))) > 0
        log(f"policy {'ok' if policy_ok else 'FAILED'}: {tail(os.path.join(out_dir, 'policy.log'), 2).strip()}")
        if not (critic_ok and policy_ok):
            log("training failed; keeping the previous pair and continuing")
            continue

        result = measure(new_policy, new_critic, 1000 if not args.smoke else 100, out_dir, pack, args.types,
                         new_critic.split(",")[0] if args.phase_critic else "")

        regret = None
        if args.regret_every and index % args.regret_every == 0:
            regret_path = os.path.join(out_dir, "regret.json")
            code = run([sys.executable, os.path.join(HERE, "regret_report.py"), "--run", "--type", "PICK_PHASE",
                        "--positions", str(args.regret_positions), "--policy", new_policy, "--critics", new_critic,
                        "--phase-critic", new_critic.split(",")[0], "--types", args.types,
                        "--out", regret_path], os.path.join(out_dir, "regret.log"))
            if code == 0:
                regret = {"positions": args.regret_positions, "file": regret_path}
                log(f"regret report: {tail(os.path.join(out_dir, 'regret.log'), 6).strip()}")
        row = {"iteration": index, "name": name, "games": played, "rows": summary.get("rows"),
               "failed": failed, "seconds": round(seconds, 1), "explore": args.explore,
               "critic": new_critic, "critics": len(new_critics), "policy": new_policy, **result,
               "time": time.strftime("%Y-%m-%dT%H:%M:%S")}
        ledger.append(row)
        json.dump(ledger, open(LEDGER, "w", encoding="utf-8"), indent=1)
        mix = " ".join(f"{k.split()[0]}:{v * 100:.0f}%" for k, v in sorted((result.get("phases") or {}).items()))
        log(f"vs SMART {result['score']} {result['ci']} | vs RANDOM {result['score_random']} "
            f"{result['ci_random']} | builds {result['builds_per_game']} | phases {mix} | "
            f"argmax stop {result['stop_share']}")

        prev_name = name
        policy, critic = new_policy, new_critic
        score = result["score"] or 0.0
        floor = result.get("score_random") or 0.0
        if score > best + 0.005:
            best, flat = score, 0
            log(f"new best {best:.3f}")
        else:
            flat += 1
            log(f"no improvement ({flat}/{args.patience})")
        if result["stop_share"] is not None and result["stop_share"] < args.stop_share_floor:
            log(f"the argmax stops on only {result['stop_share']:.1%} of cards: adding exploration to keep declines in the data")
            args.explore = "BUILD_CARD:0.30"
        if flat >= args.patience:
            # A plateau is not an answer. Climb the lever list one step at a time (docs/AUDIT.md), and say which step was
            # taken, so the night produces either a better player or a precise negative result.
            step = ladder.pop(0) if ladder else None
            if step is None:
                log("stagnation and the lever list is exhausted: stopping the night")
                break
            reason = step()
            log(f"STAGNATION -> next lever: {reason}")
            ledger[-1]["ladder"] = reason
            flat = 0

        removed = prune(args.keep_packs)
        if removed:
            log(f"pruned packs {removed}; free on C: {free_gb('C:/'):.0f} GB, D: {free_gb('D:/'):.0f} GB")

    log(f"night finished: {len(ledger)} iterations, best score {best:.3f}, ledger {LEDGER}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
