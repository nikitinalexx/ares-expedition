"""Is an early lead good or bad? Compare the score trajectory with the TRUE final result.

Why: against NETWORK the average score gap is level until round ~14 and then drifts. Reading that as "we lose the late game" is wrong, because the drift is
symmetric: in the games we win, the gap grows +17 by the end, in the games we lose it falls -21. What matters is the SIGN of the early lead. This tool answers
the only question that follows: given that we were ahead at round k, how often did we actually win?

Every fact about the result comes from the engine, not from the trajectory: `RANKING.*.jsonl` carries the `WinPointsService.ScoreBreakdown` of both seats
(terraformingRating, forests, cards, resourceCards, milestones, awards, extra), so the winner is the engine's, and the channels can be compared too. The
corpus records only ONE seat per game (the agent's), so the breakdown is read at that seat's index - get that wrong and the channels look flat and the
question meaningless.
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402
import round_gap  # noqa: E402

CHANNELS = ["TR", "forests", "printed", "resources", "milestones", "awards", "extra"]

# The 25 corporation bits of the V2 table block, in order (CorporationsFeature.FEATURE_NAMES). Arclight owns two: the flag and its animal counter.
CORPORATIONS = ["helion", "ecoline", "mining_guild", "saturn_systems", "phobolog", "inventrix", "unmi_corp",
                "arclight", "arclight_animals", "celestior", "thorgate", "devtechs", "launcstar", "credicor",
                "modpro", "mayni", "sultira", "teractor", "tharsis", "hyperion", "exocorp", "apolloindustries",
                "zetacell", "nebulabs", "interplanetary"]


def game_key(value):
    """The corpus stores unsigned 64-bit ids, the ranking file signed ones."""
    return int(value) & 0xFFFFFFFFFFFFFFFF


def read_breakdowns(path):
    """game -> [mine, opponent] score channels, from the first line of each game.

    `path` may be one file or a directory: a 1 000-game corpus is written in several chunks, each with its OWN `RANKING.00000N.jsonl`. Reading only the
    first one silently measured 250 of 1 000 games - so the directory form is the default, and the caller checks the count against the corpus.
    """
    files = sorted(glob.glob(os.path.join(path, "RANKING.*.jsonl"))) if os.path.isdir(path) else [path]
    out = {}
    for file in files:
        with open(file, encoding="utf-8") as f:
            for line in f:
                obj = json.loads(line)
                if "breakdown" in obj:
                    out[game_key(obj["game"])] = obj["breakdown"]
    return out


def seats(chunk):
    """game -> the seat that was recorded (the corpus records the agent only)."""
    return {int(row["game"]): int(row["seat"]) for row in chunk}


def results(breakdowns, seat_by_game):
    """game -> (our channels, their channels), oriented so that index 0 is always OUR seat."""
    out = {}
    for game, breakdown in breakdowns.items():
        seat = seat_by_game.get(game)
        if seat is None:
            continue
        out[game] = (breakdown[seat], breakdown[1 - seat])
    return out


def won(result):
    """The engine's winner: the seats' totals are equal on a tie, which counts as not won."""
    return sum(result[0]) > sum(result[1])


def lead_table(per_round, outcomes, rounds):
    """For each round: how many times we were ahead, and how often that led to a win."""
    table = []
    for rnd in rounds:
        rows = [(per_round[(game, seat, rnd)][0] - per_round[(game, seat, rnd)][1], game)
                for (game, seat, r) in per_round
                if r == rnd and game in outcomes]
        ahead = [game for gap, game in rows if gap > 0]
        behind = [game for gap, game in rows if gap <= 0]
        if not ahead or not behind:
            continue
        table.append({
            "round": rnd,
            "ahead": len(ahead),
            "win_if_ahead": float(np.mean([outcomes[g] for g in ahead])),
            "win_if_behind": float(np.mean([outcomes[g] for g in behind])),
            "gap_ahead": float(np.mean([g for g, game in rows if g > 0])),
            "gap_behind": float(np.mean([g for g, game in rows if g <= 0])),
        })
    return table


def corporation(obs, index):
    """The agent's corporation from the V2 table block: the first corporation bit that is set (Arclight owns two)."""
    for name in CORPORATIONS:
        if name in index and float(obs[index[name]]) > 0.5:
            return "arclight" if name == "arclight_animals" else name
    return None


def by_corporation(per_round, outcomes, corp_by_game, round_number=5):
    """Per corporation: games, win rate, games ahead at `round_number`, and the win rate among those.

    Why: the early lead turned out to be the losing sign (see the docstring). The corporation is chosen once, from a deal, and the health panel measures that
    the player barely adapts to it - so this is the first cut that asks whether the line is a property of the deal or of the player.
    """
    groups = {}
    for (game, seat, rnd), (mine, theirs) in per_round.items():
        if rnd != round_number or game not in outcomes:
            continue
        corp = corp_by_game.get(game)
        if corp is None:
            continue
        group = groups.setdefault(corp, {"games": 0, "wins": 0, "ahead": 0, "wins_ahead": 0})
        group["games"] += 1
        group["wins"] += 1 if outcomes[game] else 0
        if mine > theirs:
            group["ahead"] += 1
            group["wins_ahead"] += 1 if outcomes[game] else 0
    return [(name, group) for name, group in sorted(groups.items(), key=lambda item: -item[1]["games"])]


def channel_table(outcomes):
    """Mean channels per seat, and the gap in our favour."""
    ours = np.array([channels for channels, _ in outcomes.values()], dtype=float)
    theirs = np.array([other for _, other in outcomes.values()], dtype=float)
    rows = []
    for i, name in enumerate(CHANNELS):
        rows.append((name, float(ours[:, i].mean()), float(theirs[:, i].mean()),
                     float(ours[:, i].mean() - theirs[:, i].mean())))
    rows.append(("total", float(ours.sum(1).mean()), float(theirs.sum(1).mean()),
                 float((ours.sum(1) - theirs.sum(1)).mean())))
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--names", default="D:/ares/tmp/names_now.json")
    parser.add_argument("--chunks", type=int, default=0, help="0 = every chunk (the corpus is small)")
    parser.add_argument("--by-corporation", action="store_true", help="also split the lead question by the agent's corporation")
    parser.add_argument("--round", type=int, default=5, help="the round whose sign is the early lead")
    parser.add_argument("--partial", action="store_true", help="allow fewer ranked games than recorded (default: refuse)")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    mine = names.index("score:my_winning_points")
    theirs = names.index("score:opponent_winning_points")
    index = {name: i for i, name in enumerate(names)}

    ranking = os.path.join(args.data, "RANKING.000001.jsonl")
    breakdowns = read_breakdowns(args.data)

    files = sorted(glob.glob(os.path.join(args.data, "PICK_PHASE.*.ares")))
    if args.chunks:
        files = files[:args.chunks]
    per_round, seat_by_game = {}, {}
    corp_by_game = {}
    for path in files:
        _, chunk = ares_data.read_decisions(path)
        per_round.update(round_gap.round_scores(chunk, mine, theirs))
        seat_by_game.update(seats(chunk))
        for row in chunk:
            game = int(row["game"])
            if corp_by_game.get(game) is None:
                corp_by_game[game] = corporation(row["obs"], index)
    scored = results(breakdowns, seat_by_game)
    outcome = {game: won(result) for game, result in scored.items()}
    if not args.partial and len(outcome) < len(seat_by_game):
        raise SystemExit(f"{len(outcome)} of {len(seat_by_game)} recorded games have a ranking sidecar: the channels would describe a "
                         f"partial sample. Pass --partial only if that is intended.")

    print(f"{args.data}: {len(outcome)} finished games, "
          f"win rate {np.mean(list(outcome.values())) * 100:.1f}%")
    print("  channel          us    them      gap")
    for name, us, them, gap in channel_table(scored):
        print(f"  {name:>10}  {us:6.2f}  {them:6.2f}  {gap:+7.2f}")

    print("  round  ahead  P(win|ahead)  P(win|behind)")
    for row in lead_table(per_round, outcome, range(2, 31)):
        print(f"  {row['round']:>5}  {row['ahead']:>5}  {row['win_if_ahead']:>12.2f}  {row['win_if_behind']:>13.2f}")

    if args.by_corporation:
        print(f"  corporation        games   won   ahead@%d  won|ahead" % args.round)
        for name, group in by_corporation(per_round, outcome, corp_by_game, args.round):
            won_ahead = group["wins_ahead"] / group["ahead"] if group["ahead"] else float("nan")
            print(f"  {name:<18}{group['games']:>6}  {group['wins'] / group['games']:>5.2f}  "
                  f"{group['ahead']:>8}  {won_ahead:>9.2f}")


if __name__ == "__main__":
    main()
