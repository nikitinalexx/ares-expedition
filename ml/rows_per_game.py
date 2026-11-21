"""Rows per game in a netdata CSV: what the recorder actually produced, against the owner's 300-400 states per player.

    py -3 ml/rows_per_game.py D:/ares/data/netd_all300/netdata.csv ...

Why: the corpus size is the product of games and states per game, and the second factor was never checked. The owner
logged a post-state after EVERY step for BOTH players (blue actions, discards in phase 5, passes) and got 300-400 rows per
player per game. A recorder that deduplicates or samples is a different corpus at the same game count.
"""
import collections
import sys


def main(paths):
    for path in paths:
        per_game = collections.Counter()
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                per_game[line.split(",", 2)[0]] += 1
        games = len(per_game)
        rows = sum(per_game.values())
        counts = sorted(per_game.values())
        if not games:
            print(f"{path}: empty")
            continue
        print(f"{path}\n  games={games} rows={rows} rows/game mean={rows / games:.1f} "
              f"median={counts[len(counts) // 2]} min={counts[0]} max={counts[-1]} "
              f"| per player mean={rows / games / 2:.1f} (owner: 300-400)")


if __name__ == "__main__":
    main(sys.argv[1:])
