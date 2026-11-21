"""Summary of every pack under C:/ares/pack (and any given dir): rows, games, encoder hash, source.

    py -3 ml/pack_summary.py [C:/ares/pack ...]

Why: the corpus is games x states. Before generating more, the packs that already exist must be counted, and only packs
with the SAME observation hash can be concatenated (train_value refuses a mixed encoder).
"""
import json
import os
import sys


def games_of(pack):
    for name in os.listdir(pack):
        if name.endswith(".lab.npz"):
            import numpy as np
            with np.load(os.path.join(pack, name)) as z:
                if "game" in z:
                    return len(np.unique(z["game"]))
    return 0


def main(roots):
    rows = []
    for root in roots:
        for name in sorted(os.listdir(root)):
            pack = os.path.join(root, name)
            manifest = os.path.join(pack, "manifest.json")
            if not os.path.isdir(pack) or not os.path.exists(manifest):
                continue
            with open(manifest, encoding="utf-8") as f:
                m = json.load(f)
            games = games_of(pack)
            total = sum(t.get("rows", 0) for t in m.get("types", {}).values())
            rows.append((name, m.get("observationHash"), games, total,
                         round(total / games, 1) if games else 0, m.get("source", "")))
    print(f"{'pack':18s} {'hash':18s} {'games':>6s} {'rows':>9s} {'rows/g':>7s}  source")
    for name, h, games, total, per, source in rows:
        print(f"{name:18s} {str(h):18s} {games:6d} {total:9d} {per:7.1f}  {source}")


if __name__ == "__main__":
    main(sys.argv[1:] or ["C:/ares/pack"])
