"""Read the decision audit: does the value's choice agree with the end-of-game judge, and by how much?

Why this file: the audit (`-Dares.agent.search.judgeAudit`) prints, for every audited decision, the end-of-game margin of each candidate over K worlds
(`mean=`) next to the value's own reading (`v=`). The usable statistic is not a single decision but the aggregate: how often the value's ranking matches
the judge's, and how many points the judge's preferred candidate gains over the value's. That is the first direct measure of "is our value ranking the
phase decision right", as opposed to "does our value fit the final margin" (AUC).

    py -3 ml/judge_report.py --log D:/ares/tmp/audit_champ.log
"""
import argparse
import re


def parse_values(text):
    """'1.0,na,2.5' -> [1.0, None, 2.5]."""
    out = []
    for part in text.split(","):
        part = part.strip()
        out.append(None if part == "na" or part == "" else float(part))
    return out


def parse_line(line):
    """One `[JUDGE]` line into a dict, or None when it does not carry both readings."""
    if "[JUDGE]" not in line:
        return None
    mean = re.search(r"mean=([^\s]+)", line)
    value = re.search(r"v=([^\s]+)", line)
    if not mean or not value:
        return None
    kind = re.search(r"type=(\S+)", line)
    return {
        "type": kind.group(1) if kind else "?",
        "mean": parse_values(mean.group(1)),
        "v": parse_values(value.group(1)),
    }


def argmax(values):
    """Index of the maximum, ignoring None; -1 when nothing is present."""
    best, best_value = -1, None
    for i, value in enumerate(values):
        if value is None:
            continue
        if best_value is None or value > best_value:
            best, best_value = i, value
    return best


def summarise(rows):
    """Agreement and the judge's advantage over the value's choice, averaged over decisions."""
    n = 0
    agree = 0
    advantages = []
    for row in rows:
        if not row:
            continue
        v_best = argmax(row["v"])
        o_best = argmax(row["mean"])
        if v_best < 0 or o_best < 0:
            continue
        n += 1
        if v_best == o_best:
            agree += 1
        advantage = row["mean"][o_best] - row["mean"][v_best]
        advantages.append(advantage)
    if n == 0:
        return {"decisions": 0}
    mean_advantage = sum(advantages) / len(advantages)
    variance = sum((a - mean_advantage) ** 2 for a in advantages) / max(len(advantages) - 1, 1)
    return {
        "decisions": n,
        "agreement": agree / n,
        "mean_advantage": mean_advantage,
        "sd_advantage": variance ** 0.5,
        "se_advantage": (variance / len(advantages)) ** 0.5,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--log", required=True)
    args = parser.parse_args()
    rows = []
    with open(args.log, encoding="utf-8", errors="ignore") as handle:
        for line in handle:
            parsed = parse_line(line)
            if parsed:
                rows.append(parsed)
    by_type = {}
    for row in rows:
        by_type.setdefault(row["type"], []).append(row)
    print(f"{args.log}: {len(rows)} audited decisions")
    for kind, group in sorted(by_type.items()):
        stats = summarise(group)
        if stats["decisions"] == 0:
            continue
        print(f"  {kind}: {stats['decisions']} decisions")
        print(f"    value's choice agrees with the end-of-game judge: {stats['agreement'] * 100:.1f}%")
        print(f"    judge's preferred candidate gains {stats['mean_advantage']:+.2f} points "
              f"(sd {stats['sd_advantage']:.1f}, se {stats['se_advantage']:.2f}) over the value's choice")


if __name__ == "__main__":
    main()
