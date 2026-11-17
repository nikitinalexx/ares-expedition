"""Does taking more cards in phase 5 predict winning? The owner's claim, measured.

The claim (2026-11-17): the player who sees and takes more cards in phase 5 wins - a wider view of the deck raises the quality
of the hand, the hand raises the money (excess cards sell at 3 MC each round) and the money buys better cards. If it holds,
the quantity to move is "draft picks per game", not the phase-call frequency or the money.

Measured from the pack: the number of DRAFT_CARDS decisions per game (each is one card taken), the number of BUILD_CARD
decisions (build opportunities), the cards sold, and the final margin.

    py -3 ml/draft_report.py --pack C:/ares/pack/<name>
"""
import argparse
import json
import os
import sys
from collections import Counter

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def count_rows(manifest, pack, decision):
    if decision not in manifest["types"]:
        return Counter()
    data = ares_data.open_packed_type(pack, decision, manifest)
    return Counter(np.asarray(data["game"]).tolist())


def margin_of(manifest, pack):
    out = {}
    for decision in ("PICK_PHASE", "COLLECT_INCOME", "BUILD_CARD"):
        if decision not in manifest["types"]:
            continue
        data = ares_data.open_packed_type(pack, decision, manifest)
        games = np.asarray(data["game"])
        margins = np.asarray(data["margin"], dtype=np.float32)
        for game, value in zip(games.tolist(), margins.tolist()):
            out[game] = value
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    args = parser.parse_args()
    manifest = ares_data.open_pack(args.pack)
    margin = margin_of(manifest, args.pack)
    drafts = count_rows(manifest, args.pack, "DRAFT_CARDS")
    builds = count_rows(manifest, args.pack, "BUILD_CARD")
    sells = count_rows(manifest, args.pack, "SELL_CARDS_LAST_ROUND")

    games = [g for g in margin if g in drafts or g in builds]
    draft = np.array([drafts.get(g, 0) for g in games], dtype=np.float32)
    build = np.array([builds.get(g, 0) for g in games], dtype=np.float32)
    sell = np.array([sells.get(g, 0) for g in games], dtype=np.float32)
    m = np.array([margin[g] for g in games], dtype=np.float32)
    print(f"{args.pack}: {len(games)} games")
    print(f"draft picks per game: mean {draft.mean():.1f}  build decisions {build.mean():.1f}  "
          f"sale decisions {sell.mean():.1f}")
    print(f"correlation(draft picks, margin)  = {np.corrcoef(draft, m)[0, 1]:+.3f}")
    print(f"correlation(build decisions, margin) = {np.corrcoef(build, m)[0, 1]:+.3f}")
    print()
    print(f"{'draft picks':>12s} {'games':>7s} {'mean margin':>12s}")
    for low, high in ((0, 2), (3, 5), (6, 8), (9, 12), (13, 99)):
        mask = (draft >= low) & (draft <= high)
        if mask.sum() >= 50:
            print(f"{f'{low}-{high}':>12s} {int(mask.sum()):7d} {m[mask].mean():+12.2f}")
    print()
    print(f"{'builds':>12s} {'games':>7s} {'mean margin':>12s}")
    for low, high in ((0, 3), (4, 6), (7, 9), (10, 14), (15, 99)):
        mask = (build >= low) & (build <= high)
        if mask.sum() >= 50:
            print(f"{f'{low}-{high}':>12s} {int(mask.sum()):7d} {m[mask].mean():+12.2f}")


if __name__ == "__main__":
    main()
