"""Decision-level audit: when the agent places an animal where several holders are offered, does it take the point?

The owner's sanity check (22/11): Fish pays 1 point per animal; EcologicalZone, FilterFeeders, Herbivores, SmallAnimals and
Arclight pay 0.5 (two animals per point). So with one animal to place and every counter even, Fish is the ONLY card that
pays now: choosing anything else gives zero. This reads the recorded decisions (packed BLUE_INPUT/BUILD_INPUT rows), recovers
the animal counters from the observation, works out the marginal points of each candidate and checks the taken one. No game
outcome is used - the question is the choice, not who won.

    py -3 ml/animal_placement_audit.py --pack C:/ares/pack/1118-2332_17

The candidate identity is the one-hot in the candidate features (position in `PlayableCards.ALL`), the same mapping
ml/card_points_data.py uses. Counters are read from the observation by name and are encoded /10.
"""
import argparse
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAYABLE = os.path.join(ROOT, "src", "main", "java", "com", "terraforming", "ares", "services", "agent",
                        "PlayableCards.java")
NAMES = "D:/ares/tmp/names_now.json"

# points per animal, and the observation column that counts animals on that card
HOLDERS = {
    "Fish": (1.0, "fish_animal_count"),
    "Herbivores": (0.5, "herbivores_animal_count"),
    "EcologicalZone": (0.5, "ecologicalzone_animal_count"),
    "FilterFeeders": (0.5, "filterfeeders_animal_count"),
    "SmallAnimals": (0.5, "smallanimals_animal_count"),
    "Livestock": (0.5, "livestock_animal_count"),
    "Birds": (0.5, "birds_animal_count"),
    "Zoos": (0.5, "zoos_animal_count"),
}


def card_order():
    text = open(PLAYABLE, encoding="utf-8").read()
    block = text.split("List<Class<? extends Card>> ALL = List.of(", 1)[1].split(");", 1)[0]
    return re.findall(r"(\w+)\.class", block)


def marginal(per_animal, count):
    """Points gained by placing the next animal: floor((n+1)*rate) - floor(n*rate)."""
    return int(np.floor((count + 1) * per_animal) - np.floor(count * per_animal))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", required=True)
    parser.add_argument("--types", default="BLUE_INPUT,BUILD_INPUT")
    parser.add_argument("--chunk", type=int, default=50000)
    args = parser.parse_args()

    names = open(NAMES, encoding="utf-8").read()
    import json
    name_index = {name: i for i, name in enumerate(json.loads(names))}
    order = card_order()
    manifest = ares_data.open_pack(args.pack)

    total = chosen_optimal = 0
    special = special_optimal = 0          # every counter even, Fish offered -> Fish is the only point
    chosen_counts = {}
    for decision in args.types.split(","):
        if decision not in manifest["types"] or "candidates" not in manifest["types"][decision]:
            continue
        data = ares_data.open_packed_type(args.pack, decision, manifest)
        cands, chosen, count = data["cands"], data["chosen"], data["count"]
        obs, keep = data["obs"], np.asarray(data["keep"], dtype=bool)
        rows = len(chosen)
        for start in range(0, rows, args.chunk):
            stop = min(start + args.chunk, rows)
            for i in range(start, stop):
                if not keep[i] or count[i] < 2 or chosen[i] < 0 or chosen[i] >= count[i]:
                    continue
                base = int(cands.offsets[i])
                features = cands.flat[base:base + int(count[i])]
                identity = [order[int(np.argmax(features[k, :-1]))] for k in range(len(features))]
                offered = [(k, identity[k]) for k in range(len(identity)) if identity[k] in HOLDERS]
                if len(offered) < 2:
                    continue
                row = obs[i]
                counts = {name: int(round(float(row[name_index[column]]) * 10)) for name, (_, column) in HOLDERS.items()}
                gains = [(k, name, marginal(HOLDERS[name][0], counts.get(name, 0))) for k, name in offered]
                best = max(g for _, _, g in gains)
                picked_index = int(chosen[i])
                picked = next((name for k, name, _ in gains if k == picked_index), None)
                if picked is None:
                    continue
                gain = next(g for k, _, g in gains if k == picked_index)
                total += 1
                chosen_counts[picked] = chosen_counts.get(picked, 0) + 1
                if gain == best:
                    chosen_optimal += 1
                # the owner's exact case: no holder has an odd count yet, so only Fish pays
                even = all(counts.get(name, 0) % 2 == 0 for _, name in offered)
                if even and any(name == "Fish" for _, name in offered):
                    special += 1
                    if picked == "Fish":
                        special_optimal += 1
        print(f"{decision}: {total} animal placements with a real choice so far", flush=True)

    print()
    print(f"placements with >=2 animal holders offered: {total}")
    print(f"  took a card with the maximal marginal points: {100.0 * chosen_optimal / max(total, 1):.1f}%")
    print("  taken card: " + ", ".join(f"{name} {n}" for name, n in sorted(chosen_counts.items(), key=lambda x: -x[1])))
    print(f"owner's case (every counter even, Fish offered, so Fish is the only +1): {special} placements, "
          f"Fish taken {100.0 * special_optimal / max(special, 1):.1f}%")


if __name__ == "__main__":
    main()
