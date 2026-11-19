"""Which cards does a bundle actually build, by name - and does it take the phase-1 upgrade at all?

Reads BUILD_CARD rows of a packed corpus (the chosen candidate's id per row), counts them per game, and joins the ids with
the card table. The owner's question (19/11): the player picks phase 1 in only 8.7% of turns, and the phase-1 upgrade that
lets it build a phase-2 card under 12 is one of the development paths - so first: is that upgrade built at all?

    py -3 ml/card_builds.py --pack D:/ares/fixtures/mull7317d --top 25
"""
import argparse
import collections
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def card_names(path):
    if not path or not os.path.exists(path):
        return {}
    blob = json.load(open(path, encoding="utf-8"))
    names = {}
    if isinstance(blob, dict):
        for key, value in blob.items():
            entries = value if isinstance(value, list) else value.get("cards", []) if isinstance(value, dict) else []
            for entry in entries:
                if isinstance(entry, dict) and "id" in entry:
                    names[int(entry["id"])] = entry.get("name") or entry.get("className") or str(entry["id"])
                elif isinstance(entry, int):
                    names[entry] = str(entry)
    return names


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--cards", default="D:/ares/tmp/cards_by_expansion.json")
    parser.add_argument("--top", type=int, default=25)
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    if "BUILD_CARD" not in manifest["types"]:
        print(f"{args.pack}: no BUILD_CARD")
        return 2
    data = ares_data.open_packed_type(args.pack, "BUILD_CARD", manifest)
    chosen = np.asarray(data["chosen"], dtype=np.int64)
    games = np.asarray(data["game"])
    ids = data["ids"]
    names = card_names(args.cards)
    counts = collections.Counter()
    for index, game in zip(chosen.tolist(), games.tolist()):
        if 0 <= index < len(ids[0]):
            counts[int(ids[0][index])] += 1
    games_total = len(set(games.tolist()))
    print(f"{args.pack}: {games_total} games, {len(chosen)} build decisions, {len(counts)} distinct cards built")
    print(f"{'card':44s} {'built':>7s} {'per game':>9s}")
    for card_id, count in counts.most_common(args.top):
        print(f"{names.get(card_id, 'id ' + str(card_id))[:44]:44s} {count:7d} {count / max(games_total, 1):9.2f}")
    print()
    upgrades = [(card_id, count) for card_id, count in counts.items()
                if "phase" in names.get(card_id, "").lower() or "upgrade" in names.get(card_id, "").lower()]
    print(f"phase-upgrade cards built: {len(upgrades)} distinct")
    for card_id, count in sorted(upgrades, key=lambda row: -row[1])[:10]:
        print(f"  {names.get(card_id, card_id)[:50]:50s} {count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
