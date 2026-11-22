"""Offline top-k recall of the policy against the search's own choice, per decision type.

Why: the owner allows pruning candidate menus by the policy prior IF the prior shows a result (22/11). The first result is
not a play test but this: on held-out games, how often is the action the SEARCH chose inside the policy's top-k, and how big
are the menus? Pruning is only defensible where top-k recall is high and k << menu size.

    py -3 ml/policy_recall.py --pack C:/ares/pack/1118-2332_18 --policy D:/ares/runs/policy/1118-2332_17

The pack must be one the policy did not train on (the default policy trained on 1118-2332_17; use 16 or 18).
The bundle is a directory of per-decision models: <policy>/<DECISION>/model.json + weights.bin.
"""
import argparse
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402
from phase_model import load_exported, numpy_forward  # noqa: E402
from train_policy_ce import load_packed_type  # noqa: E402

WIDE = ["SELL_CARDS", "SELL_CARDS_LAST_ROUND", "DISCARD_CARDS", "BLUE_INPUT", "BUILD_INPUT",
        "BUILD_CARD", "PICK_PHASE", "MULLIGAN", "DRAFT_CARDS", "PERFORM_BLUE_ACTION",
        "PICK_CORPORATION", "STANDARD_ACTION"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--policy", required=True)
    parser.add_argument("--decisions", default=",".join(WIDE))
    parser.add_argument("--limit", type=int, default=15000, help="rows sampled per type")
    parser.add_argument("--out", default="")
    args = parser.parse_args()

    manifest = ares_data.open_pack(args.pack)
    rng = np.random.default_rng(12345)
    report = {"pack": args.pack, "policy": args.policy, "types": {}}

    for decision in [d.strip() for d in args.decisions.split(",") if d.strip()]:
        model_dir = os.path.join(args.policy, decision)
        if not os.path.exists(os.path.join(model_dir, "model.json")):
            print(f"  {decision:24s} no model in the bundle")
            continue
        meta, params = load_exported(model_dir)
        clip = float(meta.get("clip", 5.0))
        data = load_packed_type(args.pack, decision, manifest, 0, 0, 1, -1)
        if data is None or len(data["obs"]) == 0:
            print(f"  {decision:24s} no rows")
            continue
        if int(meta["obsSize"]) != int(data["obsSize"]) or int(meta["candFeatures"]) != int(data["candFeatures"]):
            print(f"  {decision:24s} schema mismatch: policy {meta['obsSize']}/{meta['candFeatures']} "
                  f"pack {data['obsSize']}/{data['candFeatures']}")
            continue
        n = len(data["obs"])
        take = np.arange(n) if n <= args.limit else rng.choice(n, args.limit, replace=False)
        top1 = top4 = top8 = 0
        ranks, sizes, missed = [], [], 0
        for i in take:
            count = int(data["count"][i])
            if count < 2:
                continue
            chosen = int(data["chosen"][i])
            if chosen >= count:
                missed += 1
                continue
            cands = np.asarray(data["cands"][i])[:count]
            logits = numpy_forward(params, clip, np.asarray(data["obs"][i], dtype=np.float64), cands.astype(np.float64))
            rank = int(np.sum(logits > logits[chosen]))
            ranks.append(rank)
            sizes.append(count)
            top1 += rank == 0
            top4 += rank < 4
            top8 += rank < 8
        total = len(ranks)
        if total == 0:
            print(f"  {decision:24s} no usable rows")
            continue
        entry = {
            "rows": total,
            "menu_mean": round(float(np.mean(sizes)), 2),
            "menu_max": int(max(sizes)),
            "top1": round(top1 / total, 4),
            "top4": round(top4 / total, 4),
            "top8": round(top8 / total, 4),
            "mean_rank": round(float(np.mean(ranks)), 3),
            "missed_chosen_index": missed,
        }
        report["types"][decision] = entry
        print(f"  {decision:24s} rows {total:6d} menu {entry['menu_mean']:5.2f} (max {entry['menu_max']:2d}) "
              f"top1 {entry['top1']:.3f} top4 {entry['top4']:.3f} top8 {entry['top8']:.3f}  "
              f"mean rank {entry['mean_rank']:.2f}")

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=1)
        print(f"wrote {args.out}")
    print("\n=== pruning gate (top4 recall and menu size)")
    for name, e in sorted(report["types"].items(), key=lambda kv: kv[1]["menu_mean"], reverse=True):
        print(f"  {name:24s} menu {e['menu_mean']:5.2f}  top4 {e['top4']:.3f}  top8 {e['top8']:.3f}")


if __name__ == "__main__":
    main()
