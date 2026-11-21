"""Rows per game actually stored in a packed corpus, against what `train_value.py --rows-per-game` uses.

    py -3 ml/pack_density.py C:/ares/pack/1118-2332_17 ...

Why: the champion's critics trained on 143 936 rows from 10 000 games = 14.4 rows per game, which is exactly the trainer's
`--rows-per-game 16` default, not what the pack holds. The owner's corpus is ~1000 states per game, so the cap is a silent
corpus-size multiplier: the same games can give several times the rows at this cap alone.
"""
import collections
import os
import sys

HALF = 2  # every pack stores observation blocks as float16


def rows_of_type(pack, type_name):
    path = os.path.join(pack, f"{type_name}.obs.f16")
    if not os.path.exists(path):
        return None
    with open(os.path.join(pack, "manifest.json"), "r", encoding="utf-8") as f:
        import json
        manifest = json.load(f)
    dimension = None
    for key, value in manifest.items():
        if key.endswith("observationSize") or key == "obsSize":
            dimension = int(value)
    if dimension is None:
        dimension = manifest.get("dimension")
    if not dimension:
        return None
    return os.path.getsize(path) // (dimension * HALF)


def main(packs):
    for pack in packs:
        per_type = collections.OrderedDict()
        total = 0
        for name in sorted(os.listdir(pack)):
            if name.endswith(".obs.f16"):
                rows = rows_of_type(pack, name[:-len(".obs.f16")])
                if rows:
                    per_type[name[:-len(".obs.f16")]] = rows
                    total += rows
        games = 0
        labels = os.path.join(pack, "outcome.lab.npz")
        if os.path.exists(labels):
            import numpy as np
            games = len(np.load(labels)["game"]) if "game" in np.load(labels) else 0
        print(f"{pack}: {len(per_type)} types, {total} rows", end="")
        if games:
            print(f", {games} games -> {total / games:.1f} rows/game", end="")
        print()
        for name, rows in per_type.items():
            print(f"  {name:22s} {rows:>10d}" + (f"  {rows / games:6.1f}/game" if games else ""))


if __name__ == "__main__":
    main(sys.argv[1:])
