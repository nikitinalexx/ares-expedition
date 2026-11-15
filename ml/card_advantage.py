"""Which cards are worth points, after the position they were built in is taken into account?

For every build, the advantage of the game (final margin minus what the critic expected at the decision) is attributed to
the card that was built. Averaging that per card removes most of "a strong position builds strong cards": the critic
already predicted the strong position, so what is left is how much better the game went than predicted.

Split-half stability is reported: if the two halves of the games agree on the ranking, the signal is real and a candidate
feature (or a CE reweighting) can carry it; if they do not, the numbers are noise and no training will help.

    py -3 ml/card_advantage.py DIR [DIR ...] [--value-model DIR]
"""
import argparse
import collections
import glob
import json
import os
import sys

import numpy as np
import torch

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ares_data  # noqa: E402
from train_policy_ce import load_value_params, value_of  # noqa: E402


def load(directory, value, device):
    rows = []
    for path in sorted(glob.glob(os.path.join(directory, "BUILD_CARD.*.ares"))):
        _, chunk = ares_data.read_decisions(path)
        if not len(chunk):
            continue
        obs = torch.from_numpy(chunk["obs"].astype(np.float32)).to(device)
        with torch.no_grad():
            predicted = value_of(obs, value, raw=True).cpu().numpy() * 20.0  # the critic predicts margin/20
        for index in range(len(chunk)):
            chosen = int(chunk["ids"][index][int(chunk["chosen"][index])])
            if chosen == 0xFFFF:
                continue
            rows.append((int(chunk["game"][index]), chosen, float(chunk["margin"][index]) - float(predicted[index])))
    return rows


def ranking(rows):
    totals = collections.defaultdict(float)
    counts = collections.Counter()
    for _, card, advantage in rows:
        totals[card] += advantage
        counts[card] += 1
    return {card: totals[card] / counts[card] for card in counts if counts[card] >= 40}, counts


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directories", nargs="+")
    parser.add_argument("--value-model", default="D:/ares/runs/value/v_r17_after_margin")
    args = parser.parse_args()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    params = load_value_params(args.value_model)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32, device=device)
             for k, v in params.items()}
    value["clip"] = float(params["clip"])

    rows = []
    for directory in args.directories:
        rows.extend(load(directory, value, device))
    print(f"{sum(1 for _ in rows)} builds over {len({g for g, _, _ in rows})} games")

    even = [row for row in rows if row[0] % 2 == 0]
    odd = [row for row in rows if row[0] % 2 == 1]
    first, counts = ranking(even)
    second, _ = ranking(odd)
    common = sorted(set(first) & set(second))
    print(f"cards with >=40 builds in each half: {len(common)}")
    if len(common) >= 10:
        a = np.array([first[card] for card in common])
        b = np.array([second[card] for card in common])
        print(f"split-half correlation of the card advantage: {np.corrcoef(a, b)[0, 1]:+.3f}")
        print(f"spread: sd {a.std():.2f} points")

    known = ranking(rows)[0]
    ordered = sorted(known.items(), key=lambda item: -item[1])
    print("\ncards with the best average advantage (built >=40 times):")
    for card, advantage in ordered[:12]:
        print(f"  {card:5d}  {advantage:+6.2f}  ({counts[card]} builds)")
    print("cards with the worst average advantage:")
    for card, advantage in ordered[-8:]:
        print(f"  {card:5d}  {advantage:+6.2f}  ({counts[card]} builds)")

    if len(common) >= 10:
        stable = [(card, known[card]) for card in common if np.sign(first[card]) == np.sign(second[card])]
        print(f"\ncards whose advantage keeps its sign in both halves: {len(stable)} of {len(common)}")


if __name__ == "__main__":
    main()
