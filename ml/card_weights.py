"""Per-card advantage from recorded games, written as a file the CE trainer can use as row weights.

The number for a card is "how much better did the game go than the critic expected, when this card was built", averaged
over the builds of that card. It removes most of the position confound (the critic already priced the position) and it is
stable across halves of the data (split-half correlation ~+0.6), which the row-by-row advantage is not.

    py -3 ml/card_weights.py DIR [DIR ...] --out FILE [--min-builds 30]

The file is a training input, not part of the player: nothing in the engine reads it.
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directories", nargs="+")
    parser.add_argument("--out", required=True)
    parser.add_argument("--value-model", default="D:/ares/runs/value/v_r17_after_margin")
    parser.add_argument("--min-builds", type=int, default=30)
    args = parser.parse_args()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    params = load_value_params(args.value_model)
    value = {k: torch.as_tensor(v if np.ndim(v) else float(v), dtype=torch.float32, device=device)
             for k, v in params.items()}
    value["clip"] = float(params["clip"])

    totals = collections.defaultdict(float)
    counts = collections.Counter()
    for directory in args.directories:
        for path in sorted(glob.glob(os.path.join(directory, "BUILD_CARD.*.ares"))):
            _, chunk = ares_data.read_decisions(path)
            if not len(chunk):
                continue
            obs = torch.from_numpy(chunk["obs"].astype(np.float32)).to(device)
            with torch.no_grad():
                predicted = value_of(obs, value, raw=True).cpu().numpy() * 20.0
            for index in range(len(chunk)):
                card = int(chunk["ids"][index][int(chunk["chosen"][index])])
                if card == 0xFFFF:
                    continue
                totals[card] += float(chunk["margin"][index]) - float(predicted[index])
                counts[card] += 1

    advantages = {str(card): round(totals[card] / counts[card], 3)
                  for card in counts if counts[card] >= args.min_builds}
    payload = {"cards": advantages, "builds": {str(c): counts[c] for c in counts if counts[c] >= args.min_builds},
               "unit": "points of final margin above what the critic predicted"}
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    json.dump(payload, open(args.out, "w", encoding="utf-8"), indent=1, sort_keys=True)
    values = np.array(list(advantages.values()))
    print(f"{len(advantages)} cards with >= {args.min_builds} builds, "
          f"advantage sd {values.std():.2f}, range {values.min():+.2f} .. {values.max():+.2f} -> {args.out}")


if __name__ == "__main__":
    main()
