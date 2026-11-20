"""Tests for the corpus audit screens (`ml/corpus_audit.py`).

Why: the two numbers produced here decide where the next hour of compute goes - whether the critic's loss is dominated by the decision-rich
style, and whether a specialist per corporation is worth building. A silent error (a wrong grouping, an eta-squared computed on the wrong
denominator) would be read as a fact about the game.
"""
import os
import sys
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import corpus_audit  # noqa: E402


def labels(rows):
    """rows of (game, seat, round)."""
    return {
        "game": np.array([r[0] for r in rows], dtype=np.int64),
        "seat": np.array([r[1] for r in rows], dtype=np.int64),
        "round": np.array([r[2] for r in rows], dtype=np.int64),
    }


class RowsPerRoundTest(unittest.TestCase):

    def test_counts_per_group(self):
        data = labels([(0, 0, 0), (0, 0, 0), (0, 0, 1), (0, 1, 0), (1, 0, 0), (1, 0, 0), (1, 0, 0)])
        counts = corpus_audit.rows_per_round(data)
        self.assertEqual(sorted(counts.tolist()), [1, 1, 2, 3])

    def test_empty(self):
        self.assertEqual(corpus_audit.rows_per_round(labels([])).tolist(), [])

    def test_summary_concentration(self):
        # eight quiet rounds of one row and two heavy rounds of ninety-one
        counts = np.array([1] * 8 + [91] * 2, dtype=np.int64)
        summary = corpus_audit.weighting_summary(counts)
        self.assertEqual(summary["rounds"], 10)
        self.assertAlmostEqual(summary["share_of_rows_in_top_decile_of_rounds"], 91 / 190)
        self.assertAlmostEqual(summary["ratio_p90_p10"], 91 / 1)


class CorpStatsTest(unittest.TestCase):

    def test_eta_squared_is_between_over_total(self):
        # corp 1: 10, 12 (mean 11); corp 2: 20, 22 (mean 21); grand 16
        pairs = [(1, 10), (1, 12), (2, 20), (2, 22)]
        stats = corpus_audit.corp_stats(pairs)
        self.assertEqual(stats["corps"], 2)
        # between: 2*(11-16)^2 + 2*(21-16)^2 = 50 + 50 = 100; total: 36+16+16+36 = 104
        self.assertAlmostEqual(stats["eta2"], 100 / 104)
        means = {row["corp"]: row["margin"] for row in stats["rows"]}
        self.assertAlmostEqual(means[1], 11.0)
        self.assertAlmostEqual(means[2], 21.0)

    def test_one_corp_explains_nothing(self):
        # eta squared is BETWEEN over TOTAL: with a single group nothing is explained by the grouping (0/0 is avoided, 0 is returned)
        stats = corpus_audit.corp_stats([(7, 1), (7, 3)])
        self.assertAlmostEqual(stats["eta2"], 0.0)

    def test_empty(self):
        stats = corpus_audit.corp_stats([])
        self.assertEqual(stats["corps"], 0)
        self.assertTrue(np.isnan(stats["eta2"]))


if __name__ == "__main__":
    unittest.main()
