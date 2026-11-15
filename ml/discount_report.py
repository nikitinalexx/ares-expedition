"""Does the player act on the discount that steel/titanium incomes give? (owner's "effective build" question)

Rules, as the engine implements them in `services/DiscountService`: a card with the BUILDING tag costs 2 MC less per
steel income (3 with Advanced Alloys), a SPACE-tag card 3 MC less per titanium income (+1 per Advanced Alloys/Phobolog).
The income is a permanent discount, not a stock.

This reads the recorded BUILD_CARD rows (candidate features carry the tags and the price, the observation carries the two
incomes), computes for every candidate the *effective* price the engine would charge, and answers:

  * is the card the player actually takes the cheapest one by effective price, more often than by nominal price?
  * does the acceptance rate rise when the cheapest affordable card is cheap?

    py -3 ml/discount_report.py DIR [DIR2 ...]
"""
import json
import os
import sys
from collections import defaultdict

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

STOP = 65535
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NAMES = os.path.join(ROOT, "ml", "slot_names.json")
# CardFeatures layout: signature 64, colour 4, tags 11, price/50, active, stop
SIGNATURE, COLOURS, TAGS = 64, 4, 11
TAG_START = SIGNATURE + COLOURS
PRICE = TAG_START + TAGS
BUILDING, SPACE = 6, 0  # Tag enum order: SPACE, EARTH, EVENT, SCIENCE, PLANT, ENERGY, BUILDING, ANIMAL, JUPITER, MICROBE, DYNAMIC


def main():
    names = json.load(open(NAMES, encoding="utf-8"))
    steel = names.index("steel_income")
    titanium = names.index("titanium_income")
    print(f"steel_income at {names[steel]}, titanium_income at {names[titanium]}")

    chosen_is_cheapest_nominal = 0
    chosen_is_cheapest_effective = 0
    differing = 0
    steel_sum = 0.0
    titanium_sum = 0.0
    accepted = 0
    buckets = defaultdict(lambda: {"taken": 0, "declined": 0})
    rows_total = 0
    for directory in sys.argv[1:]:
        for path in ares_data.chunk_paths(directory, "BUILD_CARD"):
            schema, chunk = ares_data.read_decisions(path)
            obs = np.asarray(chunk["obs"], dtype=np.float32)
            feat = np.asarray(chunk["feat"], dtype=np.float32)
            my_steel = obs[:, steel] * 10.0
            my_titanium = obs[:, titanium] * 10.0
            steel_sum += float(my_steel.sum())
            titanium_sum += float(my_titanium.sum())
            for row in range(len(chunk)):
                count = int(chunk["count"][row])
                ids = chunk["ids"][row][:count]
                rows_total += 1
                best_nominal, best_effective = None, None
                for index in range(count):
                    if int(ids[index]) == STOP:
                        continue
                    features = feat[row, index]
                    price = float(features[PRICE]) * 50.0
                    discount = 0.0
                    if features[TAG_START + BUILDING] > 0:
                        discount += 2.0 * my_steel[row]
                    if features[TAG_START + SPACE] > 0:
                        discount += 3.0 * my_titanium[row]
                    effective = max(0.0, price - discount)
                    if best_nominal is None or price < best_nominal[0]:
                        best_nominal = (price, index)
                    if best_effective is None or effective < best_effective[0]:
                        best_effective = (effective, index)
                chosen = int(chunk["chosen"][row])
                bucket = min(6, int((best_effective[0] if best_effective else 0) // 6))
                if int(ids[chosen]) == STOP:
                    buckets[bucket]["declined"] += 1
                    continue
                accepted += 1
                if best_nominal is not None and chosen == best_nominal[1]:
                    chosen_is_cheapest_nominal += 1
                if best_effective is not None:
                    if chosen == best_effective[1]:
                        chosen_is_cheapest_effective += 1
                    if best_nominal is not None and best_nominal[1] != best_effective[1]:
                        differing += 1
                buckets[bucket]["taken"] += 1
    print(f"rows {rows_total}, accepted {accepted}")
    print(f"chosen == cheapest by nominal price:   {chosen_is_cheapest_nominal / max(accepted,1):.3f}")
    print(f"chosen == cheapest by effective price: {chosen_is_cheapest_effective / max(accepted,1):.3f}")
    per_row = max(rows_total, 1)
    print(f"mean steel income per row {steel_sum / per_row:.2f}, titanium {titanium_sum / per_row:.2f}")
    print(f"rows where the discount changes which card is cheapest: {differing / per_row:.3f}")
    print("\nacceptance by the cheapest effective price (6-MC buckets of the best available card):")
    for bucket in sorted(buckets):
        entry = buckets[bucket]
        n = entry["taken"] + entry["declined"]
        if n < 50:
            continue
        print(f"  best <= {6*(bucket+1):>3} MC: rows {n:>6} accept {entry['taken']/n:.3f}")


if __name__ == "__main__":
    main()
