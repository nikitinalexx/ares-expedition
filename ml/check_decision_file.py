"""Prints a summary of a decision file and runs the sanity checks. Exit code 1 if something is wrong.

    python ml/check_decision_file.py run1/PICK_PHASE.ares
"""
import sys

import numpy as np

from ares_data import read_decisions, validate


def main(path):
    schema, rows = read_decisions(path)
    problems = validate(schema, rows)
    print(f"rows={len(rows)}")
    print(f"games={len(np.unique(rows['game']))}")
    print(f"obs_size={schema['obsSize']}")
    print(f"hash={schema['hash']}")
    print(f"mean_outcome={float(rows['outcome'].mean()):.4f}")
    print(f"sum_outcome={float(rows['outcome'].sum()):.1f}")
    print(f"sum_margin={float(rows['margin'].sum()):.1f}")
    print(f"problems={problems}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
