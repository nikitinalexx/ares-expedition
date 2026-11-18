"""How many different ways to play does a corpus actually contain?

The action-coverage guard watches *options*: did a decision type collapse onto one choice. It cannot see the failure the owner
described for his NETWORK - a player that uses every menu and still plays one plan. This measures the plans.

A player-game is described by where its points came from (the game's own seven score channels), which phase it leaned on, and
how its activity was split between builds, standard projects, blue actions, sales and drafts. The report groups player-games
into an interpretable profile - "phase 3, points from resource cards" - and prints how much of the corpus the top profile
holds plus the effective number of profiles (exp of the entropy).

    py -3 ml/strategy_mix.py --pack C:/ares/pack/<name>

Why buckets and not k-means: clustering with k groups always partitions the corpus into k groups, so "the largest cluster's
share" says more about k than about the player (measured: an 80/20 corpus reported 29% with k=4 because k-means split the big
blob three ways). A discrete profile has no such artefact, needs no seed, and its numbers mean exactly what they say.

There is no known-good value on purpose (owner, 2026-11-17: a measured count is not a norm). The report is read as a
*trend across iterations*, and as a flag when one profile starts to take over.
"""
import argparse
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

# The game's own seven score channels. A channel may be negative (cards with negative printed points exist), which is why a
# share can be slightly below zero - it is data, not a bug.
CHANNELS = ("tr", "forests", "cards", "resourceCards", "milestones", "awards", "extra")
ACTIVITY = ("BUILD_CARD", "STANDARD_ACTION", "PERFORM_BLUE_ACTION", "SELL_CARDS", "DRAFT_CARDS")
PHASES = 5


def profiles(pack, limit=6000):
    """One row per (game, seat): [7 point shares, phase/5, 5 activity shares]."""
    manifest = ares_data.open_pack(pack)
    width = len(CHANNELS) + 1 + len(ACTIVITY)
    if "PICK_PHASE" not in manifest["types"]:
        return np.zeros((0, width), dtype=np.float64)

    per_game = {}
    data = ares_data.open_packed_type(pack, "PICK_PHASE", manifest)
    games = np.asarray(data["game"])
    seats = np.asarray(data["seat"])
    chosen = np.asarray(data["chosen"], dtype=np.int64)
    ids = data["ids"]
    with np.load(os.path.join(pack, "PICK_PHASE.lab.npz")) as labels:
        breakdown = np.asarray(labels["breakdown"], dtype=np.float64)
    for row in range(len(chosen)):
        key = (int(games[row]), int(seats[row]))
        entry = per_game.get(key)
        if entry is None:
            entry = {"phases": {}, "activity": {}, "channels": breakdown[row]}
            per_game[key] = entry
        row_ids = np.asarray(ids[row])
        if len(row_ids) > int(chosen[row]):
            phase = int(row_ids[int(chosen[row])])
            entry["phases"][phase] = entry["phases"].get(phase, 0) + 1
        if len(per_game) >= limit * 3:
            break

    for decision in ACTIVITY:
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(pack, decision, manifest)
        for game, seat in zip(np.asarray(data["game"]).tolist(), np.asarray(data["seat"]).tolist()):
            key = (int(game), int(seat))
            if key not in per_game:
                continue
            per_game[key]["activity"][decision] = per_game[key]["activity"].get(decision, 0) + 1

    rows = []
    for key in sorted(per_game)[:limit]:
        entry = per_game[key]
        channels = entry["channels"]
        total = float(channels.sum())
        shares = (channels / total) if total > 0 else np.zeros(len(CHANNELS))
        phases = entry["phases"]
        phase = max(phases, key=phases.get) if phases else 0
        phase = phase if 1 <= phase <= PHASES else 0
        activity = entry["activity"]
        count = float(sum(activity.values())) or 1.0
        mix = [activity.get(name, 0) / count for name in ACTIVITY]
        rows.append(np.concatenate([shares, [phase / float(PHASES)], mix]))
    return np.array(rows, dtype=np.float64)


def profile_key(row):
    """The interpretable label: which phase the points came with, and where they came from."""
    phase = int(round(row[len(CHANNELS)] * PHASES))
    channel = CHANNELS[int(np.argmax(row[:len(CHANNELS)]))]
    return f"phase{phase or '?'}:{channel}"


def summarise(matrix, top=5):
    if not len(matrix):
        return {"player_games": 0}
    keys = [profile_key(row) for row in matrix]
    counts = {}
    for key in keys:
        counts[key] = counts.get(key, 0) + 1
    total = float(len(keys))
    shares = {key: value / total for key, value in counts.items()}
    ordered = sorted(shares.items(), key=lambda item: -item[1])
    entropy = -sum(share * math.log(share) for share in shares.values() if share > 0)
    return {"player_games": int(total),
            "profiles": len(shares),
            "top_profile": ordered[0][0],
            "top_share": round(ordered[0][1], 4),
            "profiles_above_5pct": sum(1 for _, share in ordered if share > 0.05),
            "effective_profiles": round(math.exp(entropy), 2),
            "top": [[key, round(share, 4)] for key, share in ordered[:top]]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--limit", type=int, default=6000)
    parser.add_argument("--out", default="")
    args = parser.parse_args()
    matrix = profiles(args.pack, args.limit)
    report = summarise(matrix)
    report["pack"] = args.pack
    print(json.dumps(report, indent=1))
    print()
    means = matrix.mean(axis=0) if len(matrix) else np.zeros(len(CHANNELS) + 1 + len(ACTIVITY))
    print("mean profile: " + "  ".join(f"{name} {value:.2f}" for name, value in zip(CHANNELS, means[:len(CHANNELS)])))
    print("             phase " + f"{means[len(CHANNELS)] * PHASES:.1f}" + "  " +
          "  ".join(f"{name} {value:.2f}" for name, value in zip(ACTIVITY, means[len(CHANNELS) + 1:])))
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=1)


if __name__ == "__main__":
    main()
