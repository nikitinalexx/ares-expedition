"""Health panel for a player bundle: strength is only accepted if it is not bought with degeneration.

Owner's requirement (2026-11-20): we may keep training, but the hand must stay on the pulse - a gain that comes from collapsing into one line is not a
gain. So every candidate bundle is described by five families of numbers and promoted only if none of them collapses relative to the reference:

1. **profile** - builds per game, build acceptance, standard projects (the existing anti-degeneracy gate);
2. **phase diversity** - the normalised entropy of each game's own phase mix, averaged over games (a player whose every game looks the same has a low
   value here even if its *average* mix is varied);
3. **card diversity** - the colour shares, the distinct cards per game and the card concentration (HHI over played cards);
4. **adaptation to the deal** - how far the phase mix of each corporation deviates from the global mix (0 = the deal is ignored);
5. **score composition** - the spread of the game's own channels across games.

The thresholds are RELATIVE to a reference bundle, never absolute: there is no "healthy profile" in this project, only "this candidate did not
collapse relative to the one we have" (AGENTS.md).

    py -3 ml/health.py --data D:/ares/data/1120_clean5000 --reference D:/ares/data/1120_probe8 --chunks 10
"""
import argparse
import collections
import glob
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402

PHASES = 5


def shannon(counts):
    """Entropy in nats of a count vector (0 when empty)."""
    values = np.asarray([c for c in counts if c > 0], dtype=np.float64)
    if values.size == 0:
        return 0.0
    p = values / values.sum()
    return float(-(p * np.log(p)).sum())


def normalised_entropy(counts, categories):
    """Entropy divided by its maximum for `categories` outcomes: 0 = one outcome, 1 = uniform."""
    if categories <= 1:
        return 0.0
    return shannon(counts) / float(np.log(categories))


def hhi(ids):
    """Herfindahl index of card ids: 1 = a single card, ~0 = many equally used cards."""
    counts = collections.Counter(ids)
    if not counts:
        return 1.0
    total = sum(counts.values())
    return float(sum((c / total) ** 2 for c in counts.values()))


def adaptation_distance(per_seat, min_games=30):
    """How much the phase mix moves with the corporation, in L1 distance from the global mix.

    0 means "the deal is ignored": every corporation gets the same plan. It is computed only for corporations with enough games to mean something, and
    it is a distance, not a norm - a player may adapt a little or a lot.
    """
    global_counts = np.zeros(PHASES)
    by_corp = collections.defaultdict(lambda: np.zeros(PHASES))
    for entry in per_seat.values():
        global_counts += entry["phases"]
        if entry["corp"] is not None:
            by_corp[entry["corp"]] += entry["phases"]
    if global_counts.sum() == 0:
        return 0.0
    global_mix = global_counts / global_counts.sum()
    total, weighted = 0.0, 0.0
    for corp, counts in by_corp.items():
        n = counts.sum()
        if n < min_games:
            continue
        mix = counts / n
        weighted += n * float(np.abs(mix - global_mix).sum()) / 2.0
        total += n
    return float(weighted / total) if total else 0.0


def read_bundle(directory, chunks=0):
    """Per (game, seat) aggregation of the decisions a corpus records."""
    def files(kind):
        found = sorted(glob.glob(os.path.join(directory, f"{kind}.*.ares")))
        return found[:chunks] if chunks else found

    ids = json.load(open("D:/ares/tmp/cards_by_expansion.ids.json", encoding="utf-8")) if os.path.exists(
        "D:/ares/tmp/cards_by_expansion.ids.json") else {}
    by_id = {int(k): v for k, v in ids.items()}
    root = os.path.join("src", "main", "java", "com", "terraforming", "ares", "cards")
    color_of = {}
    if os.path.isdir(root):
        for color in ("green", "blue", "red"):
            folder = os.path.join(root, color)
            if os.path.isdir(folder):
                for name in os.listdir(folder):
                    if name.endswith(".java"):
                        color_of[name[:-5]] = color

    per_seat = collections.defaultdict(lambda: {"phases": np.zeros(PHASES), "cards": [], "colors": collections.Counter(),
                                               "corp": None, "builds": 0, "declines": 0, "standards": 0})

    for path in files("PICK_PHASE"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            chosen = int(row["chosen"])
            count = int(row["count"])
            if 0 <= chosen < count:
                key = (int(row["game"]), int(row["seat"]))
                per_seat[key]["phases"][int(row["ids"][chosen]) - 1] += 1

    for path in files("BUILD_CARD"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            key = (int(row["game"]), int(row["seat"]))
            chosen = int(row["chosen"])
            count = int(row["count"])
            if not (0 <= chosen < count):
                continue
            card = int(row["ids"][chosen])
            if card == 0xFFFF or card <= 0:   # the stop candidate (CardFeatures.STOP_ID)
                per_seat[key]["declines"] += 1
                continue
            per_seat[key]["cards"].append(card)
            per_seat[key]["builds"] += 1
            color = color_of.get(by_id.get(card, ""))
            if color:
                per_seat[key]["colors"][color] += 1

    for path in files("PICK_CORPORATION"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            chosen = int(row["chosen"])
            count = int(row["count"])
            if 0 <= chosen < count:
                per_seat[(int(row["game"]), int(row["seat"]))]["corp"] = int(row["ids"][chosen])

    for path in files("STANDARD_ACTION"):
        _, rows = ares_data.read_decisions(path)
        for row in rows:
            chosen = int(row["chosen"])
            count = int(row["count"])
            if 0 <= chosen < count and int(row["ids"][chosen]) != 0:
                per_seat[(int(row["game"]), int(row["seat"]))]["standards"] += 1
    return per_seat


def panel(per_seat):
    """The five families of numbers, from the per-seat aggregation."""
    if not per_seat:
        raise SystemExit("no game-seats in the corpus")
    entries = list(per_seat.values())
    entropies = [normalised_entropy(e["phases"], PHASES) for e in entries if e["phases"].sum() > 0]
    color_totals = collections.Counter()
    for e in entries:
        color_totals.update(e["colors"])
    total_cards = sum(color_totals.values()) or 1
    distinct = [len(set(e["cards"])) for e in entries]
    builds = np.array([e["builds"] for e in entries], dtype=np.float64)
    declines = np.array([e["declines"] for e in entries], dtype=np.float64)
    standards = np.array([e["standards"] for e in entries], dtype=np.float64)
    return {
        "game_seats": len(entries),
        "builds_per_game": float(builds.mean()),
        "build_acceptance": float(builds.sum() / max(builds.sum() + declines.sum(), 1)),
        "standard_projects_per_game": float(standards.mean()),
        "phase_entropy": float(np.mean(entropies)) if entropies else 0.0,
        "green_share": color_totals["green"] / total_cards,
        "blue_share": color_totals["blue"] / total_cards,
        "red_share": color_totals["red"] / total_cards,
        "color_entropy": normalised_entropy([color_totals["green"], color_totals["blue"], color_totals["red"]], 3),
        "distinct_cards_per_game": float(np.mean(distinct)) if distinct else 0.0,
        "card_hhi": float(np.mean([hhi(e["cards"]) for e in entries])),
        "adaptation_to_corp": adaptation_distance(per_seat),
    }


TOLERANCE = {
    "builds_per_game": 0.20,
    "build_acceptance": 0.10,
    "standard_projects_per_game": 0.30,
    "phase_entropy": 0.20,
    "color_entropy": 0.15,
    "distinct_cards_per_game": 0.20,
    "adaptation_to_corp": 0.30,
}


def collapse(reference, candidate):
    """Metrics of `candidate` that fell below the reference by more than the tolerance (relative)."""
    dropped = []
    for name, tolerance in TOLERANCE.items():
        ref, cand = float(reference[name]), float(candidate[name])
        if ref <= 0:
            continue
        if cand < ref * (1.0 - tolerance):
            dropped.append((name, ref, cand, 1.0 - cand / ref))
    return dropped


def show(label, values, reference=None):
    print(label)
    for name, value in values.items():
        if reference and name in TOLERANCE:
            ref = float(reference[name])
            delta = (value - ref) / ref * 100 if ref else float("nan")
            print(f"  {name:28s} {value:10.3f}   reference {ref:10.3f}   {delta:+6.1f}%")
        else:
            print(f"  {name:28s} {value:10.3f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--reference", default="")
    parser.add_argument("--chunks", type=int, default=10, help="per decision type (0 = all)")
    args = parser.parse_args()
    candidate = panel(read_bundle(args.data, args.chunks))
    reference = panel(read_bundle(args.reference, args.chunks)) if args.reference else None
    show(f"candidate: {args.data}", candidate)
    if reference:
        print()
        show(f"reference: {args.reference}", reference)
        print()
        dropped = collapse(reference, candidate)
        if dropped:
            print("COLLAPSE - the candidate is worse than the reference beyond tolerance:")
            for name, ref, cand, loss in dropped:
                print(f"  {name}: {cand:.3f} against {ref:.3f} (-{loss * 100:.0f}%)")
        else:
            print("no collapse relative to the reference")


if __name__ == "__main__":
    main()
