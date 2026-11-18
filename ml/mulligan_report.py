"""Does the player ever mulligan?

The corpus says the mulligan was declined in 100% of the recorded rows, and `rankOnly` keeps the policy's stop, so the
menu is a no-op today. This is the sensor for "did it learn to throw the junk away": per game, how many cards it discarded
in the mulligan, and the share of games that discarded at least one - plus the same for the end-of-round sale, which has the
identical collapse.

    py -3 ml/mulligan_report.py --pack C:/ares/pack/<name>
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def behaviour(pack, decision, stop_id=0xFFFF):
    manifest = ares_data.open_pack(pack)
    if decision not in manifest["types"]:
        return None
    data = ares_data.open_packed_type(pack, decision, manifest)
    chosen = np.asarray(data["chosen"], dtype=np.int64)
    game = np.asarray(data["game"])
    ids = data["ids"]
    stopped = np.array([ids[i][chosen[i]] == stop_id for i in range(len(chosen))], dtype=bool)
    per_game = {}
    for value, game_id in zip(stopped.tolist(), game.tolist()):
        entry = per_game.setdefault(int(game_id), [0, 0])
        entry[0] += 1
        entry[1] += 0 if value else 1
    discarded = np.array([v[1] for v in per_game.values()], dtype=np.float64)
    # The whole point of a "discard the junk" rule is that the COUNT follows the hand: a fixed number would show up as one
    # spike in this distribution, while a hand-dependent rule spreads over several values.
    histogram = {int(k): int(v) for k, v in zip(*np.unique(discarded, return_counts=True))}
    return {
        "rows": len(chosen),
        "games": len(per_game),
        "stop_share": float(stopped.mean()),
        "games_with_a_discard": float((discarded > 0).mean()),
        "mean_discards_per_game": float(discarded.mean()),
        "max_discards_in_a_game": int(discarded.max()) if len(discarded) else 0,
        "by_count": histogram,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    args = parser.parse_args()
    print(f"{args.pack}")
    for decision, stop in (("MULLIGAN", 0xFFFF), ("SELL_CARDS", 0xFFFF), ("SELL_CARDS_LAST_ROUND", 0xFFFF),
                           ("SELL_CARDS", 0)):
        if decision == "SELL_CARDS" and stop == 0:
            continue
        found = behaviour(args.pack, decision, stop)
        if found is None:
            print(f"  {decision:22s} not in this pack")
            continue
        print(f"  {decision:22s} rows {found['rows']:7d} games {found['games']:6d} "
              f"stop {100 * found['stop_share']:5.1f}% "
              f"games that discarded {100 * found['games_with_a_discard']:5.1f}% "
              f"mean {found['mean_discards_per_game']:.2f} max {found['max_discards_in_a_game']}")
        spread = " ".join(f"{count}:{100 * games / found['games']:.0f}%"
                          for count, games in sorted(found["by_count"].items()))
        print(f"  {'':22s} per game: {spread}")


if __name__ == "__main__":
    main()
