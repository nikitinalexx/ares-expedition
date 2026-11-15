"""Coverage check: which game/player accessors do the RULES read that the ENCODER never reads?

The completeness test is a hand-written list of facts, so it systematically misses **rule predicates** - things the engine
reads to decide what is still legal (e.g. `planetAtTheStartOfThePhase`, which gates `canIncreaseTemperature` and every
`onTemperatureChangedEffect`). This script is the mechanical version: rule accessors minus encoder accessors.

    py -3 ml/rule_coverage.py                     # print the gap
    py -3 ml/rule_coverage.py --update-allowlist  # accept the current gap (writes the JSON below)
    py -3 ml/rule_coverage.py --check             # exit 1 if an accessor outside the allowlist appears

The allowlist is not "these are fine": every entry carries the reason it is not encoded, so the diff stays small and the
next reviewer sees exactly what is intentionally out of the observation.
"""
import argparse
import collections
import io
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "java", "com", "terraforming", "ares")
ALLOWLIST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rule_coverage_allowlist.json")

RULE_SOURCES = [
    "states", "validation", "processors", "services/TerraformingService.java", "services/DiscountService.java",
    "services/TurnTypeService.java", "services/CardValidationService.java", "services/SpecialEffectsService.java",
    "services/WinPointsService.java", "services/DraftCardsService.java", "services/CardService.java",
]
ENCODER_SOURCES = ["services/ai/advanced", "services/agent/ObservationBuilder.java"]

ACCESSOR = re.compile(r"\.(get[A-Z]\w*|is[A-Z]\w*)\s*\(")

# Why an accessor may stay out of the observation. Anything not listed here must be justified in the allowlist.
REASONS = {
    "plumbing": "identity / turn bookkeeping, not part of the position",
    "upstream": "enforced before the decision is offered (the candidate list already excludes it)",
    "candidate": "carried by the candidate features of the decision, not by the global state",
    "crysis": "co-op crisis mode, out of agent scope",
}


def _files(entry):
    path = os.path.join(ROOT, entry)
    if os.path.isfile(path):
        return [path]
    result = []
    for base, _, names in os.walk(path):
        for name in names:
            if name.endswith(".java"):
                result.append(os.path.join(base, name))
    return result


def _accessors(paths):
    used = collections.Counter()
    for path in paths:
        text = io.open(path, encoding="utf-8", errors="ignore").read()
        for name in ACCESSOR.findall(text):
            used[name] += 1
    return used


def gap():
    rules = _accessors([p for entry in RULE_SOURCES for p in _files(entry)])
    encoder = _accessors([p for entry in ENCODER_SOURCES for p in _files(entry)])
    return collections.Counter({name: count for name, count in rules.items() if name not in encoder})


def default_reason(name):
    if name in ("getId", "getPlayerUuid", "getNextTurn", "getTurnTypeService", "getPhaseId", "getProjectId",
                "getInputParams", "getChoiceOptions", "getProjectType", "getPayments", "getInput", "getCardsToSee",
                "getAlreadyDrafted", "getCardsToTake", "getCorporationCardId", "getWinPointsInfo", "getFirst",
                "isIntermediate", "getSpecialEffects", "getTerraformingService", "getPaymentValidationService",
                "getActiveCardAction"):
        return "plumbing"
    if name.startswith("getCrysis") or name.startswith("isCrysis") or name in (
            "getOpenedCards", "getCrysisCard", "getCrysisCards", "getCardToTokensCount", "getCrisisVp",
            "getSpecialCrysisGoal", "getOceanDetrimentSize"):
        return "crysis"
    if name in ("getOxygenRequirement", "getTemperatureRequirement", "getInfrastructureRequirement", "getPrice",
                "getProjectPrice", "getDiscount", "getCollectableResource", "getTags", "getCardTags", "isValidOxygen",
                "isValidTemperatute", "isValidInfrastructure", "getResources", "getPoints"):
        return "candidate"
    if name in ("isOxygenMin", "isTemperatureMin", "getInfrastructureColor", "getExpansions", "isMulligan",
                "isActiveCard", "isPhaseUpgraded", "getForbiddenPhases", "getExtraPoints",
                "getSelectedCorporationCard", "getDoubleCollectCardId", "isHasUnmiAction", "isUnmiCorporation",
                "isBuiltSpecialDesignLastTurn"):
        return "upstream"
    if name == "getProjectsDeck":
        return "derivable"
    return ""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--update-allowlist", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    current = gap()
    if args.update_allowlist:
        data = {name: {"count": count, "reason": default_reason(name)} for name, count in sorted(current.items())}
        io.open(ALLOWLIST, "w", encoding="utf-8").write(json.dumps(data, indent=1) + "\n")
        print(f"allowlist written: {len(data)} accessors -> {ALLOWLIST}")
        return 0

    allowed = json.load(io.open(ALLOWLIST, encoding="utf-8")) if os.path.isfile(ALLOWLIST) else {}
    unknown = {name: count for name, count in current.items() if name not in allowed}
    unused = [name for name in allowed if name not in current]

    print(f"{len(current)} rule accessors the encoder never reads ({len(allowed)} in the allowlist)")
    for name, count in sorted(current.items(), key=lambda item: -item[1]):
        mark = " " if name in allowed else "!"
        print(f"  {mark} {count:4d}x {name}" + ("" if name in allowed else "   <-- new"))
    if unused:
        print("\nallowlist entries that no longer exist (remove them): " + ", ".join(sorted(unused)))
    if args.check and unknown:
        print("\nFAIL: new rule accessors that the observation may not carry: " + ", ".join(sorted(unknown)))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
