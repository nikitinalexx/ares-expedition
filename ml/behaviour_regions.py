"""Premise 2 (owner's question, 20/11): does the behaviour space have separate regions, and are they all weak?

Why this decides the plan: coverage is only worth building if strong games occupy more than one behaviour region. If every strong game
looks the same in the game's own scoring shares, then "finding all strategies" is chasing ghosts - there would be one way to win and the
archive would be a museum. The descriptors are the game's own channels (terraforming share, card-points share), not a classification I
invented.

Reads labels only:

    py -3 ml/behaviour_regions.py --packed D:/ares/pack/1119-2204_22 --bins 4
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import round_scores  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--packed", required=True)
    parser.add_argument("--bins", type=int, default=4, help="equal-width bins per share; the cell grid is bins x bins")
    parser.add_argument("--percentile", type=int, default=75, help="what counts as a strong game-seat")
    args = parser.parse_args()

    labels = round_scores.load_labels(args.packed)
    table = round_scores.round_table(labels)
    final = round_scores.final_score(table)

    # one entry per game-seat: its final score and its behaviour shares
    n = len(table["game"])
    last = np.ones(n, dtype=bool)
    last[:-1] = (table["game"][1:] != table["game"][:-1]) | (table["seat"][1:] != table["seat"][:-1])
    key_game = table["game"][last]
    key_seat = table["seat"][last]
    shares = round_scores.descriptor_shares(final)
    scores = final.sum(axis=1)
    # margin from the row's own label: labels are the acting seat's margin, so this is that seat's point of view
    margins = table["margin"][last]

    cells = np.array([round_scores.cell_index(s, args.bins) for s in shares], dtype=np.int64)
    rows = round_scores.summarise_cells(shares, scores, margins, args.bins)
    rows.sort(key=lambda r: -r["n"])

    total = sum(r["n"] for r in rows)
    print(f"pack {args.packed}")
    print(f"game-seats {total}, bins {args.bins} -> {args.bins * args.bins} possible cells, {len(rows)} occupied")
    print()
    print("  tr_bin card_bin      n   share   mean score   mean margin   margin sd")
    for r in rows:
        print(f"  {r['tr_bin']:>6} {r['card_bin']:>8} {r['n']:>6} {r['n'] / total * 100:6.1f}%"
              f" {r['score']:>12.1f} {r['margin']:>13.1f} {r['margin_sd']:>11.1f}")
    print()

    share, strong = round_scores.top_cell_share(scores, cells, args.percentile)
    print(f"strong game-seats (top {100 - args.percentile}% by score): {strong}")
    print(f"  share of them in the single most populated cell: {share * 100:.1f}%")
    strong_cells = sorted({int(c) for c in cells[scores >= np.percentile(scores, args.percentile)] if c >= 0})
    print(f"  distinct cells occupied by strong game-seats: {len(strong_cells)} of {len(rows)}")
    best = max(rows, key=lambda r: r["margin"])
    print(f"  strongest cell by mean margin: tr_bin {best['tr_bin']}, card_bin {best['card_bin']}"
          f" (n {best['n']}, mean margin {best['margin']:.1f})")
    spread = np.std([r["margin"] for r in rows]) if rows else float("nan")
    print(f"  sd of mean margins across cells: {spread:.1f} points")
    print()
    print("reading: if 'share in the most populated cell' is near 100%, strong games are behaviourally identical and coverage has")
    print("nothing to find; if it is far below 100% and the cells differ in mean margin, the space is real and worth covering.")


if __name__ == "__main__":
    main()
