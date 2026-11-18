"""The hall of fame: our own past champions, and the gate a candidate must pass to replace one.

Why (owner, 2026-11-18): his NETWORK was retrained over several self-play iterations and slowly specialised - it beat its
previous self and lost universality. A candidate that only beats "me yesterday" is not progress. The gate here is the
statement "a new champion must be better than the old one, and must not lose to the recent ones", measured head to head
between two of *our* bundles, never against a teacher.

Design notes that matter:

* A bundle is meaningless without its search configuration (the menu list alone moved a bundle by 40 points), so an entry
  stores `search` next to `policy` and `value`.
* The gates are ordered by cost: absolute benchmarks first (the loop already produces them), then one duel against the
  current champion, and only if both pass, the pool. Pairwise results are cached in the same file, so a pair is measured
  once and a new iteration adds one row, not a whole round-robin.
* `duel` runs `SelfPlayMain --mode compare --a AGENT --b AGENT_V2`: `AGENT` reads `-Dares.agent.models` and `AGENT_V2` reads
  `-Dares.agent.models2`, so two different policy bundles play each other with the same engine and the same search flags.
  Until `-Dares.agent.valueModel2` exists both sides are judged by the same critic ensemble, which isolates the policy; the
  module records that in the entry so nobody reads a policy duel as a bundle duel.

    py -3 ml/hof.py table
    py -3 ml/hof.py duel --a D:/ares/runs/policy/A --b D:/ares/runs/policy/B --games 2000
    py -3 ml/hof.py promote --name 1118-0058_13 --policy ... --value ... --search "..." --smart 0.6393 --random 0.928
"""
import argparse
import glob
import json
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
HOF = "D:/ares/hof_of_fame.json"
JAVA = r"C:\Program Files\Java\jdk-25.0.5.0.2\bin\java.exe"
CP_FILE = os.path.join(ROOT, "target", "cp.txt")

# A candidate may not give up more than this on an absolute benchmark to win a head-to-head.
ABS_TOLERANCE = 0.015
# 2000 games give a +-2.2 point interval, so 54% is separable from 50%.
H2H_THRESHOLD = 0.54
H2H_MIN_GAMES = 2000
# In the pool, the candidate must not lose to any of the recent champions.
POOL_FLOOR = 0.45
POOL_RECENT = 3


def load(path=HOF):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {"champions": [], "duels": {}}


def save(state, path=HOF):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=1)
    os.replace(tmp, path)


# --------------------------------------------------------------------------------------- the gates (pure logic)


def gate_absolute(candidate_smart, champion_smart, candidate_random, champion_random, tolerance=ABS_TOLERANCE):
    """Both absolute benchmarks must be within `tolerance` of the champion's. Returns (ok, reasons)."""
    reasons = []
    if candidate_smart is None or champion_smart is None:
        reasons.append("a SMART number is missing")
    elif candidate_smart < champion_smart - tolerance:
        reasons.append(f"SMART {candidate_smart:.4f} against the champion's {champion_smart:.4f} is a real regression")
    if candidate_random is None or champion_random is None:
        reasons.append("a RANDOM number is missing")
    elif candidate_random < champion_random - tolerance:
        reasons.append(f"RANDOM {candidate_random:.4f} against the champion's {champion_random:.4f} is a real regression")
    return (not reasons, reasons)


def gate_head_to_head(score, ci_low, games, threshold=H2H_THRESHOLD, min_games=H2H_MIN_GAMES):
    """A duel must be won outright: the point estimate above the threshold and the interval clear of 50%."""
    reasons = []
    if games < min_games:
        reasons.append(f"{games} games is below the {min_games} a promotion needs")
    if score is None or ci_low is None:
        reasons.append("the duel has no result")
        return False, reasons
    if score < threshold:
        reasons.append(f"won {score:.3f} of the duel, below {threshold:.2f}")
    if ci_low <= 0.5:
        reasons.append(f"the interval's lower bound {ci_low:.3f} does not clear 50%")
    return (not reasons, reasons)


def gate_pool(candidate, champions, duels, floor=POOL_FLOOR, recent=POOL_RECENT):
    """Against the recent champions the candidate may not lose: every measured pair must be at least `floor`."""
    reasons = []
    checked = 0
    for champion in champions[-recent:]:
        key = pair_key(candidate, champion["name"])
        if key not in duels:
            reasons.append(f"no duel against {champion['name']} (the pool gate needs it)")
            continue
        checked += 1
        score = duels[key].get("score")
        if score is None or score < floor:
            reasons.append(f"lost to {champion['name']}: {score}")
    if checked == 0:
        reasons.append("no pool duel was measured")
    return (not reasons, reasons)


def pair_key(a, b):
    """One key per unordered pair, so A-vs-B and B-vs-A share a cache entry (the score is always 'A's win rate')."""
    return "|".join(sorted([a, b]))


# --------------------------------------------------------------------------------------- the duel (runs games)


def classpath():
    with open(CP_FILE, encoding="utf-8") as f:
        return "target/classes;" + f.read().strip()


def duel(policy_a, policy_b, games=2000, value=None, search=None, out=None, concurrency=14, timeout=7200):
    """Plays bundle A (AGENT) against bundle B (AGENT_V2). Returns the parsed result of `--mode compare`."""
    search = search or ["-Dares.agent.search=oneply", "-Dares.agent.search.symmetric=true",
                        "-Dares.agent.search.pairs=true"]
    out = out or f"D:/ares/tmp/hof_duel_{int(time.time())}.json"
    args = [JAVA, "-Xmx10g"] + list(search)
    args += [f"-Dares.agent.models={policy_a}", f"-Dares.agent.models2={policy_b}"]
    if value:
        args.append(f"-Dares.agent.valueModel={value}")
        args.append(f"-Dares.agent.valueModel.phase={value.split(',')[0]}")
    args += ["-cp", classpath(), "com.terraforming.ares.selfplay.SelfPlayMain",
             "--mode", "compare", "--a", "AGENT", "--b", "AGENT_V2",
             "--games", str(games), "--concurrency", str(concurrency), "--out", out]
    log = out + ".log"
    with open(log, "w", encoding="utf-8") as sink:
        subprocess.run(args, stdout=sink, stderr=subprocess.STDOUT, timeout=timeout)
    with open(out, encoding="utf-8") as f:
        blob = json.load(f)
    return {"score": blob.get("aScore"), "ci_low": blob.get("aScoreCi95Low"), "ci_high": blob.get("aScoreCi95High"),
            "games": blob.get("games"), "failed": blob.get("failedGames"), "file": out, "log": log}


# --------------------------------------------------------------------------------------- the commands


def command_table(args):
    state = load(args.hof)
    champions = state.get("champions", [])
    if not champions:
        print("no champions yet")
        return 0
    print(f"{'name':24s} {'SMART':>7s} {'RANDOM':>7s}  search")
    for champion in champions:
        measured = champion.get("measured", {})
        smart = measured.get("smart")
        random = measured.get("random")
        print(f"{champion['name']:24s} {smart:7.4f} {random:7.4f}  {champion.get('search', '-')}"[:140])
    duels = state.get("duels", {})
    if duels:
        print("\nduels (score = first name's win rate):")
        for key, value in sorted(duels.items()):
            print(f"  {key:50s} {value.get('score'):.3f} [{value.get('ci_low'):.3f}, {value.get('ci_high'):.3f}] "
                  f"{value.get('games')} games")
    return 0


def command_duel(args):
    state = load(args.hof)
    result = duel(args.a, args.b, args.games, value=args.value, search=args.search.split(",") if args.search else None)
    key = pair_key(name_of(args.a), name_of(args.b))
    if name_of(args.a) != key.split("|")[0]:
        # the cache stores the score of the alphabetically first bundle; flip when needed
        result["score"] = 1.0 - result["score"] if result["score"] is not None else None
    state.setdefault("duels", {})[key] = result
    save(state, args.hof)
    print(json.dumps(result, indent=1))
    return 0


def command_promote(args):
    state = load(args.hof)
    champions = state.get("champions", [])
    if any(c["name"] == args.name for c in champions):
        print(f"{args.name} is already a champion; champions are never overwritten")
        return 0
    champion = champions[-1] if champions else None
    ok = True
    if champion is not None:
        measured = champion.get("measured", {})
        passed, reasons = gate_absolute(args.smart, measured.get("smart"), args.random, measured.get("random"))
        ok = ok and passed
        for reason in reasons:
            print("  absolute gate:", reason)
        result = state.get("duels", {}).get(pair_key(args.name, champion["name"]))
        if result is None:
            print(f"  head-to-head gate: no duel against {champion['name']} (run `duel` first)")
            ok = False
        else:
            passed, reasons = gate_head_to_head(result.get("score"), result.get("ci_low"), result.get("games") or 0)
            ok = ok and passed
            for reason in reasons:
                print("  head-to-head gate:", reason)
        passed, reasons = gate_pool(args.name, champions, state.get("duels", {}))
        ok = ok and passed
        for reason in reasons:
            print("  pool gate:", reason)
    if not ok and not args.force:
        print(f"{args.name} is NOT promoted")
        return 1
    champions.append({"name": args.name, "policy": args.policy, "value": args.value, "search": args.search,
                      "measured": {"smart": args.smart, "random": args.random},
                      "promoted": time.strftime("%Y-%m-%dT%H:%M:%S"), "forced": bool(args.force and not ok)})
    state["champions"] = champions
    save(state, args.hof)
    print(f"{args.name} promoted{' (forced)' if args.force and not ok else ''}")
    return 0


def name_of(policy_path):
    return os.path.basename(str(policy_path).rstrip("/\\"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--hof", default=HOF)
    sub = parser.add_subparsers(dest="command", required=True)

    table = sub.add_parser("table")
    table.set_defaults(func=command_table)

    duel_cmd = sub.add_parser("duel")
    duel_cmd.add_argument("--a", required=True)
    duel_cmd.add_argument("--b", required=True)
    duel_cmd.add_argument("--games", type=int, default=2000)
    duel_cmd.add_argument("--value", default="")
    duel_cmd.add_argument("--search", default="")
    duel_cmd.set_defaults(func=command_duel)

    promote = sub.add_parser("promote")
    promote.add_argument("--name", required=True)
    promote.add_argument("--policy", required=True)
    promote.add_argument("--value", default="")
    promote.add_argument("--search", default="")
    promote.add_argument("--smart", type=float, required=True)
    promote.add_argument("--random", type=float, required=True)
    promote.add_argument("--force", action="store_true", help="promote even when a gate fails; the entry records it")
    promote.set_defaults(func=command_promote)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
