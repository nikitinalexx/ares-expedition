"""Review of the observation features: which of them are noise?

    python ml/audit_features.py --data DIR --names DIR/feature_names.json [--out docs/ENCODER_REVIEW.md]

Per feature: constant? nearly constant? an exact copy of another feature? outside [0, 1] after the static normalisation? using only a tiny part of
its range (a static maximum that is far too large)? never active (a dead card slot)? any linear relation to the result of the game?
The model standardises every input with statistics of the data, so scale problems do no harm to it; what remains harmful is information that is
WRONG (values out of range = a wrong maximum, a feature that is a copy) or that wastes capacity (constants, copies).
"""
import argparse
import io
import collections
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import chunk_paths, open_memmap  # noqa: E402


def load(data_dir):
    obs, outcome, rounds = [], [], []
    for path in chunk_paths(data_dir, "PICK_PHASE"):
        _, rows = open_memmap(path)
        obs.append(np.asarray(rows["obs"]).astype(np.float32))
        outcome.append(np.asarray(rows["outcome"]))
        rounds.append(np.asarray(rows["round"]))
    return np.concatenate(obs), np.concatenate(outcome), np.concatenate(rounds)


def block_of(name):
    if name.startswith("op_"):
        return "opponent table (op_*)"
    if name.startswith("hand:"):
        rest = name[5:]
        return "hand vector: card identities in the hand / on the table" if rest[:1].isupper() else "hand vector: global & player numbers"
    if name.startswith("extra:"):
        return "extras (round, phases, dummy)"
    if name.startswith(("milestone_", "award_")):
        return "milestones & awards"
    return "table: global / own player"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--names", required=True)
    parser.add_argument("--out", default=None)
    parser.add_argument("--write-mask", default=None, help="write the column mask of the observation without exact copies (resource of the Java builder)")
    parser.add_argument("--base-size", type=int, default=898, help="number of columns produced by the V2 encoder (the extras after them are not masked)")
    args = parser.parse_args()

    names = json.load(open(args.names, encoding="utf-8"))
    X, y, rounds = load(args.data)
    n, d = X.shape
    if d != len(names):
        raise SystemExit(f"{d} values per row but {len(names)} names: the names do not belong to this data")

    mean, std = X.mean(0), X.std(0)
    lo, hi = X.min(0), X.max(0)
    p999 = np.percentile(X, 99.9, axis=0)
    nonzero = (X != 0).mean(0)
    unique = np.array([len(np.unique(X[:, j])) for j in range(d)])

    constant = unique == 1
    nearly = (~constant) & np.array([(np.unique(X[:, j], return_counts=True)[1].max() / n) > 0.999 for j in range(d)])

    # exact copies: identical columns
    groups = collections.defaultdict(list)
    for j in range(d):
        if not constant[j]:
            groups[X[:, j].tobytes()].append(j)
    copies = [g for g in groups.values() if len(g) > 1]
    redundant_exact = sum(len(g) - 1 for g in copies)

    # linear copies: |r| >= 0.999 between different columns that are not exact copies
    live = np.where(~constant & (std > 1e-6))[0]
    Z = (X[:, live] - mean[live]) / std[live]
    corr = (Z.T @ Z) / n
    np.fill_diagonal(corr, 0)
    pairs = np.argwhere(np.triu(np.abs(corr) >= 0.999))
    exact_pairs = {(a, b) for g in copies for a in g for b in g if a < b}
    linear_pairs = [(live[i], live[j], corr[i, j]) for i, j in pairs if (min(live[i], live[j]), max(live[i], live[j])) not in exact_pairs]

    out_of_range = [j for j in range(d) if hi[j] > 1.001 or lo[j] < -0.001]
    binary = unique <= 2
    underscaled = [j for j in range(d) if not constant[j] and not binary[j] and p999[j] < 0.1 and hi[j] < 0.25]
    dead_cards = [j for j in range(d) if constant[j] and (names[j].startswith("hand:") and names[j][5:6].isupper() or names[j].startswith("op_") and names[j][3:4].isupper())]
    ycentered = y - y.mean()
    with np.errstate(invalid="ignore", divide="ignore"):
        relation = np.where(std > 1e-6, np.abs((X - mean).T @ ycentered / n) / (std * ycentered.std()), 0.0)
    no_signal = [j for j in live if relation[j] < 0.003]

    lines = ["# Ревизия признаков наблюдения", "",
             f"Данные: {n} состояний из {len(chunk_paths(args.data, 'PICK_PHASE'))} чанков (самоигра, разные контексты), признаков: {d}. Сгенерировано `ml/audit_features.py`.", "",
             "## Итог", "",
             f"| что | сколько из {d} |", "|---|---|",
             f"| константы (всегда одно значение) | {int(constant.sum())} |",
             f"| почти константы (одно значение в >99,9% состояний) | {int(nearly.sum())} |",
             f"| точные копии другого признака (лишних) | {redundant_exact} в {len(copies)} группах |",
             f"| линейные копии (|r| ≥ 0,999) сверх точных | {len(linear_pairs)} пар |",
             f"| выходят за [0, 1] (неверный максимум нормализации) | {len(out_of_range)} |",
             f"| используют <10% диапазона (максимум нормализации сильно завышен) | {len(underscaled)} |",
             f"| «мёртвые» карточные слоты (карта никогда не встретилась) | {len(dead_cards)} |",
             f"| нет даже линейной связи с исходом (|r| < 0,003) среди меняющихся | {len(no_signal)} из {len(live)} |", "",
             "Модель стандартизирует входы по данным, поэтому масштаб ей не мешает; вред от неверного максимума (значения > 1) и от копий — потеря ёмкости и ложная уверенность в «независимых» признаках."]

    lines += ["", "## По блокам", "", "| блок | признаков | константы | точные копии (лишние) | вне [0,1] | нет связи с исходом |", "|---|---|---|---|---|---|"]
    blocks = collections.OrderedDict()
    for j, name in enumerate(names):
        blocks.setdefault(block_of(name), []).append(j)
    copy_extra = {j for g in copies for j in g[1:]}
    for label, idx in blocks.items():
        lines.append(f"| {label} | {len(idx)} | {sum(constant[j] for j in idx)} | {sum(j in copy_extra for j in idx)} | "
                     f"{sum(j in set(out_of_range) for j in idx)} | {sum(j in set(no_signal) for j in idx)} |")

    def listing(title, idx, extra=None, limit=40):
        lines.extend(["", f"## {title} ({len(idx)})", ""])
        for j in list(idx)[:limit]:
            lines.append(f"- `{names[j]}`" + (f" — {extra(j)}" if extra else ""))
        if len(idx) > limit:
            lines.append(f"- … ещё {len(idx) - limit}")

    listing("Константы", np.where(constant)[0], lambda j: f"всегда {X[0, j]:g}")
    listing("Точные копии", [g[0] for g in copies], lambda j: "то же самое: " + ", ".join(f"`{names[k]}`" for g in copies if g[0] == j for k in g[1:6]), 30)
    listing("Выходят за [0, 1]", sorted(out_of_range, key=lambda j: -hi[j]), lambda j: f"от {lo[j]:g} до {hi[j]:g}")
    listing("Используют <10% диапазона", underscaled, lambda j: f"99,9-й перцентиль {p999[j]:.3f}, максимум {hi[j]:.3f}")
    listing("Сильнее всего связаны с исходом (линейно)", sorted(live, key=lambda j: -relation[j])[:25], lambda j: f"|r| = {relation[j]:.3f}", 25)

    if args.write_mask:
        drop = []
        for g in copies:
            kept = min(g)  # the first column (the table side) stays, its copies go
            drop += [[k, kept] for k in g if k != kept and k < args.base_size]
        drop.sort()
        dropped = {k for k, _ in drop}
        mask = {"baseSize": args.base_size, "keep": [j for j in range(args.base_size) if j not in dropped], "drop": drop}
        os.makedirs(os.path.dirname(os.path.abspath(args.write_mask)), exist_ok=True)
        json.dump(mask, open(args.write_mask, "w"))
        print(f"mask written: keep {len(mask['keep'])} of {args.base_size}, drop {len(drop)} exact copies")

    text = "\n".join(lines) + "\n"
    if args.out:
        os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
        open(args.out, "w", encoding="utf-8").write(text)
    print("\n".join(lines[:22]))


if __name__ == "__main__":
    main()
