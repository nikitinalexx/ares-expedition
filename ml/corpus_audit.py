"""Corpus audit: two screens that decide how to spend the next hour of compute.

Screen 1 (owner's observation): the number of decision rows a game produces depends on what the player DOES - a blue-action player has ~500
decisions, a green-card player ~100 - so an unweighted critic loss over-weights the decision-rich style, and a per-row target weights the terminal
outcome by how many decisions the plan happened to make. Measured here: rows per (game, seat, round), and how concentrated they are.

Screen 2: how much does the final margin vary by corporation in the existing corpus? It is a screen, not a proof - the corporation was CHOSEN by
our player, so it is endogenous - but if the spread is negligible, a specialist per corporation is wasted work.

    py -3 ml/corpus_audit.py --packed D:/ares/pack/1120_probe8 --raw D:/ares/data/1120_probe8
"""
import argparse
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import round_scores  # noqa: E402


def rows_per_round(labels):
    """Rows per (game, seat, round), as a flat array of counts.

    The unit of the question: one round of one seat is the natural "sample" of a position. If one round produces 20 rows and another 2, the loss
    sees the first ten times more often.
    """
    game = np.asarray(labels["game"]).astype(np.int64)
    seat = np.asarray(labels["seat"]).astype(np.int64)
    rnd = np.asarray(labels["round"]).astype(np.int64)
    if len(game) == 0:
        return np.zeros(0, dtype=np.int64)
    order = np.lexsort((rnd, seat, game))
    game, seat, rnd = game[order], seat[order], rnd[order]
    same = np.zeros(len(game), dtype=bool)
    same[1:] = (game[1:] == game[:-1]) & (seat[1:] == seat[:-1]) & (rnd[1:] == rnd[:-1])
    starts = np.flatnonzero(~same)
    counts = np.diff(np.append(starts, len(game)))
    return counts


def weighting_summary(counts):
    """The distribution of rows per round and the concentration of the loss."""
    counts = np.asarray(counts, dtype=np.float64)
    if len(counts) == 0:
        return {}
    total = counts.sum()
    order = np.sort(counts)
    top_share = order[int(len(order) * 0.9):].sum() / total if len(order) >= 10 else float("nan")
    return {
        "rounds": int(len(counts)),
        "rows": int(total),
        "mean": float(counts.mean()),
        "p10": float(np.percentile(counts, 10)),
        "median": float(np.median(counts)),
        "p90": float(np.percentile(counts, 90)),
        "max": float(counts.max()),
        "ratio_p90_p10": float(np.percentile(counts, 90) / max(np.percentile(counts, 10), 1)),
        "share_of_rows_in_top_decile_of_rounds": float(top_share),
    }


def corp_stats(pairs):
    """Mean margin per corporation and the share of the margin's variance it explains (eta squared).

    `pairs` is an iterable of (corporation id, margin). The screen answers "is the corporation worth a specialist each?"; with an endogenous
    choice it cannot answer "which corporation is best".
    """
    by = {}
    for corp, margin in pairs:
        by.setdefault(int(corp), []).append(float(margin))
    if not by:
        return {"corps": 0, "eta2": float("nan"), "rows": []}
    all_values = np.array([v for values in by.values() for v in values], dtype=np.float64)
    grand = all_values.mean()
    total_ss = ((all_values - grand) ** 2).sum()
    between_ss = 0.0
    rows = []
    for corp, values in sorted(by.items(), key=lambda kv: -len(kv[1])):
        arr = np.array(values, dtype=np.float64)
        between_ss += len(arr) * (arr.mean() - grand) ** 2
        rows.append({"corp": corp, "n": int(len(arr)), "margin": float(arr.mean()),
                     "sd": float(arr.std()) if len(arr) > 1 else 0.0})
    return {"corps": len(by), "eta2": float(between_ss / total_ss) if total_ss > 0 else float("nan"),
            "rows": rows}


def corp_pairs_from_raw(directory):
    """(corporation, that seat's margin) from the raw PICK_CORPORATION decisions.

    The PICK_CORPORATION row carries the game's margin for the acting seat, so no join with the labels is needed.
    """
    import ares_data
    pairs = []
    for path in ares_data.chunk_paths(directory, "PICK_CORPORATION"):
        _, data = ares_data.read_decisions(path)
        for row in data:
            chosen = int(row["chosen"])
            count = int(row["count"])
            if 0 <= chosen < count:
                pairs.append((int(row["ids"][chosen]), float(row["margin"])))
    return pairs


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--packed", default="", help="pack directory, for the row-weighting screen")
    parser.add_argument("--raw", default="", help="raw corpus directory, for the corporation screen")
    parser.add_argument("--type", default="", help="restrict the weighting screen to one decision type")
    args = parser.parse_args()

    if args.packed:
        types = [args.type] if args.type else None
        labels = round_scores.load_labels(args.packed, types=types)
        counts = rows_per_round(labels)
        summary = weighting_summary(counts)
        print(f"row weighting: {args.packed} ({'type ' + args.type if args.type else 'all types'})")
        for key, value in summary.items():
            print(f"  {key:>44}: {value:,.2f}" if isinstance(value, float) else f"  {key:>44}: {value:,}")
        print()

    if args.raw:
        pairs = corp_pairs_from_raw(args.raw)
        stats = corp_stats(pairs)
        print(f"corporation screen: {args.raw}")
        print(f"  corps {stats['corps']}, seats {len(pairs)}, variance of the margin explained by the corporation: "
              f"{stats['eta2'] * 100:.1f}%")
        print("  corp  seats  mean margin   sd")
        for row in stats["rows"][:12]:
            print(f"  {row['corp']:>4} {row['n']:>6} {row['margin']:>12.1f} {row['sd']:>6.1f}")


if __name__ == "__main__":
    main()
