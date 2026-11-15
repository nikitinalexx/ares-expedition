"""Read a macro audit report: does committing to the engine plan pay, and where?

    py -3 ml/macro_report.py FILE.json
"""
import json
import sys

import numpy as np

path = sys.argv[1]
report = json.load(open(path, encoding="utf-8"))
samples = report["samples"]
print(f"{path}: {report['positions']} positions, plan forced for {report['turnsBudget']} turns")
print(f"  normal play {report['normalMargin']:+.1f}, with the plan {report['planMargin']:+.1f}, "
      f"difference {report['meanDifference']:+.1f} +/- {1.96 * report['sd'] / max(1, report['positions']) ** 0.5:.1f}")
print(f"  better with the plan in {report['betterWithPlan']} positions, better without in {report['betterWithout']}")

samples = sorted(samples, key=lambda s: -s["blueCards"])
print("\n  blue cards -> normal, plan, difference")
for sample in samples:
    print(f"  {int(sample['blueCards']):3d}   {sample['normalMargin']:+7.1f} {sample['planMargin']:+7.1f} "
          f"{sample['difference']:+7.1f}")

with_blue = [s["difference"] for s in samples if s["blueCards"] >= 1]
without = [s["difference"] for s in samples if s["blueCards"] < 1]
for label, values in (("positions with a blue card", with_blue), ("positions without", without)):
    if values:
        values = np.array(values)
        print(f"\n  {label}: {len(values)} positions, mean difference {values.mean():+.1f} "
              f"+/- {1.96 * values.std(ddof=1) / max(1, len(values)) ** 0.5 if len(values) > 1 else 0:.1f}")
