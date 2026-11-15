"""Which observation values ever fire? The bit audit the owner asked for.

Reads recorded games and reports, for every one of the 780 values (and every candidate feature), how often it is non-zero,
so a column that NEVER fires stands out. A dead column is one of two things: a card or a rule from an expansion we never
play (junk that dilutes the vector) or a broken feature (a bug - the value is computed but never reaches the vector).

    py -3 ml/feature_audit.py DIR [--names FILE] [--min-share 0.0005]
"""
import argparse
import glob
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402

NAME_FILES = ["D:/ares/tmp/names_v5.json", "D:/ares/tmp/names_v4.json", "D:/ares/tmp/names795b.json"]


def names_for(size):
    for path in NAME_FILES:
        if os.path.exists(path):
            names = json.load(open(path, encoding="utf-8"))
            if len(names) == size:
                return names
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory")
    parser.add_argument("--names", default="")
    parser.add_argument("--min-share", type=float, default=0.0005,
                        help="a value seen in fewer than this share of the rows is reported as suspicious")
    parser.add_argument("--max-rows", type=int, default=400000)
    parser.add_argument("--cards", default="", help="cards-by-expansion file (SelfPlayMain --mode cards) to classify "
                                                    "dead bits: junk from an expansion we never play, or a broken feature")
    args = parser.parse_args()

    files = sorted(glob.glob(os.path.join(args.directory, "*.ares")))
    if not files:
        raise SystemExit(f"no chunks in {args.directory}")

    # one observation shape for the whole directory; sample rows from every type
    per_type = {}
    obs_all = []
    cand_all = {}
    total_rows = 0
    seen = 0
    rng = np.random.default_rng(1)
    for path in files:
        kind = os.path.basename(path).split(".")[0]
        schema, rows = ares_data.read_decisions(path)
        if not len(rows):
            continue
        take = min(len(rows), 800)
        idx = np.sort(rng.choice(len(rows), take, replace=False))
        obs_all.append(rows["obs"][idx].astype(np.float32))
        count = rows["count"].astype(np.int64)
        cands = rows["feat"][idx].astype(np.float32)
        counts = count[idx]
        mask = np.arange(cands.shape[1])[None, :] < counts[:, None]
        cand_all.setdefault(kind, []).append((cands, mask))
        per_type[kind] = per_type.get(kind, 0) + len(rows)
        total_rows += len(rows)
        seen += take
        if seen >= args.max_rows:
            break

    obs = np.concatenate(obs_all)
    names = json.load(open(args.names, encoding="utf-8")) if args.names else names_for(obs.shape[1])
    if names is None or len(names) != obs.shape[1]:
        names = [f"#{i}" for i in range(obs.shape[1])]

    share = (obs != 0).mean(axis=0)
    dead = [(share[i], names[i]) for i in range(len(names)) if share[i] < args.min_share]
    print(f"{args.directory}: {total_rows} rows, {seen} sampled, {len(files)} chunks")
    print("decision types:", ", ".join(f"{k}={v}" for k, v in sorted(per_type.items(), key=lambda kv: -kv[1])))
    expansion_of = {}
    if args.cards and os.path.exists(args.cards):
        for expansion, classes in json.load(open(args.cards, encoding="utf-8")).items():
            for name in classes:
                expansion_of.setdefault(name, expansion)
    playable = {"BASE", "DISCOVERY", "BUFFED_CORPORATION"}

    def classify(name):
        base = name.split(":")[-1]
        if base.startswith(("op_", "table_", "hand_")):
            base = base[3:]
        for expansion, expansion_name in expansion_of.items():
            if expansion == base or expansion_name == base:
                return "junk: " + expansion if expansion not in playable else "played card, never built"
        return "not a card class"

    print(f"\nobservation values that fire in fewer than {args.min_share:.4%} of the rows: {len(dead)}")
    for rate, name in sorted(dead):
        print(f"  {rate:9.5%}  {name:55s} {classify(name)}")

    constant = []
    for i in range(len(names)):
        column = obs[:, i]
        if column.min() == column.max() and share[i] > args.min_share:
            constant.append(names[i])
    print(f"\nvalues that fire but never CHANGE (constant in this sample): {len(constant)}")
    for name in constant[:20]:
        print(f"           {name}")

    print("\nthe values that fire least but still fire (bottom 15):")
    order = np.argsort(share)
    shown = 0
    for i in order:
        if share[i] >= args.min_share:
            print(f"  {share[i]:9.3%}  {names[i]}")
            shown += 1
            if shown == 15:
                break

    # candidate features: a position that never differs across candidates is unlearnable
    print("\ncandidate features that are constant over the candidates of every row (per decision type, bottom 8):")
    for kind, chunks in sorted(cand_all.items()):
        values = np.concatenate([c for c, _ in chunks])
        masks = np.concatenate([m for _, m in chunks])
        spread = []
        for f in range(values.shape[2]):
            column = values[:, :, f][masks]
            spread.append(0.0 if column.size == 0 else float(column.std()))
        spread = np.array(spread)
        worst = np.argsort(spread)[:8]
        joined = ", ".join(f"#{i}(sd {spread[i]:.3f})" for i in worst)
        print(f"  {kind:22s} {joined}")


if __name__ == "__main__":
    main()
