"""Which player generated each pack? Label mean + margin mean + label files, per pack.

    py -3 ml/pack_who.py C:/ares/pack

Why: "more games" only means something if they are games of the player being trained. A corpus mixed from a random
policy, a colour arm and the champion is not 22 750 champion games - and the value is trained on the winner's side, so
the label mean of a pack says which side its games were recorded from. The margin mean says how one-sided they were.
"""
import glob
import json
import os
import sys

import numpy as np


def stats(pack):
    out = {}
    for path in glob.glob(os.path.join(pack, "*.lab.npz")):
        decision = os.path.basename(path)[:-len(".lab.npz")]
        if decision == "AFTERSTATE":
            continue
        with np.load(path) as z:
            keys = set(z.files)
            outcome = z["outcome"] if "outcome" in keys else None
            margin = z["margin"] if "margin" in keys else None
            rows = len(z["game"]) if "game" in keys else 0
        out[decision] = {"rows": rows,
                         "outcome": float(outcome.mean()) if outcome is not None and rows else None,
                         "margin": float(margin.mean()) if margin is not None and rows else None}
    return out


def main(roots):
    for root in roots:
        for pack in sorted(glob.glob(os.path.join(root, "*"))):
            manifest = os.path.join(pack, "manifest.json")
            if not os.path.exists(manifest):
                continue
            with open(manifest, encoding="utf-8") as f:
                m = json.load(f)
            per = stats(pack)
            rows = sum(v["rows"] for v in per.values())
            # the decision type closest to "a game outcome", weighted by rows
            pairs = [(v["outcome"], v["rows"]) for v in per.values() if v["outcome"] is not None]
            outcome = sum(o * r for o, r in pairs) / sum(r for _, r in pairs) if pairs else None
            mpairs = [(v["margin"], v["rows"]) for v in per.values() if v["margin"] is not None]
            margin = sum(o * r for o, r in mpairs) / sum(r for _, r in mpairs) if mpairs else None
            print(f"{os.path.basename(pack):16s} games~{rows // 64:5d} rows {rows:8d} "
                  f"outcome {outcome if outcome is None else round(outcome, 4)} "
                  f"margin {margin if margin is None else round(margin, 2)} "
                  f"source {m.get('source', '')}")


if __name__ == "__main__":
    main(sys.argv[1:] or ["C:/ares/pack"])
