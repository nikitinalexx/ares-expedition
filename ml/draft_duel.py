"""Who wins, the player who takes more cards in phase 5? A head-to-head test on mirror games.

The owner's claim (2026-11-17): the player who sees and takes more cards in phase 5 wins. In a self-play corpus BOTH seats are
recorded, so the claim can be tested directly per game: count the draft picks of each seat, and check whether the seat with more
drafts won more often than the seat with fewer. Same for build decisions and for cards sold.

This needs a corpus without `--max-rows-per-game-type` (the cap samples rows, so the counts would saturate).

    py -3 ml/draft_duel.py --pack C:/ares/pack/mirror_allrows
"""
import argparse
import os
import sys
from collections import Counter

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def per_game_seat(manifest, pack, decision):
    if decision not in manifest["types"]:
        return {}
    data = ares_data.open_packed_type(pack, decision, manifest)
    games = np.asarray(data["game"])
    seats = np.asarray(data["seat"])
    counts = Counter(zip(games.tolist(), seats.tolist()))
    return counts


def outcomes(manifest, pack):
    """{(game, seat): outcome} from any type - the label is per decision maker and game."""
    for decision in ("PICK_PHASE", "DRAFT_CARDS", "BUILD_CARD"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(pack, decision, manifest)
        games = np.asarray(data["game"])
        seats = np.asarray(data["seat"])
        outcome = np.asarray(data["outcome"], dtype=np.float32)
        result = {}
        for game, seat, value in zip(games.tolist(), seats.tolist(), outcome.tolist()):
            result[(game, seat)] = value
        return result
    return {}


def duel(name, counts, result):
    wins_high = wins_low = ties = 0
    diffs = []
    for game in {g for g, _ in result}:
        a, b = counts.get((game, 0), 0), counts.get((game, 1), 0)
        oa, ob = result.get((game, 0)), result.get((game, 1))
        if oa is None or ob is None or a == b:
            if oa is not None and ob is not None and a == b:
                ties += 1
            continue
        high_is_a = a > b
        winner_is_a = oa > ob
        if high_is_a == winner_is_a:
            wins_high += 1
        else:
            wins_low += 1
        diffs.append(abs(a - b))
    total = wins_high + wins_low
    if not total:
        print(f"{name:16s} no comparable games")
        return
    share = wins_high / total
    se = (share * (1 - share) / total) ** 0.5
    print(f"{name:16s} the seat with MORE wins {share * 100:5.1f}% of {total} decided games "
          f"(+/- {1.96 * se * 100:.1f}), ties at equal count {ties}, mean gap {np.mean(diffs):.1f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    args = parser.parse_args()
    manifest = ares_data.open_pack(args.pack)
    result = outcomes(manifest, args.pack)
    games = len({g for g, _ in result})
    print(f"{args.pack}: {games} games with both seats")
    for name, decision in (("draft picks", "DRAFT_CARDS"), ("build decisions", "BUILD_CARD"),
                           ("cards sold", "SELL_CARDS_LAST_ROUND"), ("blue actions", "PERFORM_BLUE_ACTION")):
        duel(name, per_game_seat(manifest, args.pack, decision), result)


if __name__ == "__main__":
    main()
