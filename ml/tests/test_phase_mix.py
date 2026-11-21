"""Tests for `ml/phase_mix.py`.

Why: the tool's output is used to say what the benchmark DOES ("NETWORK picks phase 1 in 32.5% of rounds"). A round has exactly ONE pick per player (verified on
the corpus: every revealed vector sums to 1), so a silent error - counting the unrevealed first round, or averaging the flags across rows instead of taking each
row's own pick - would fake that number.
"""
import os
import sys
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import phase_mix  # noqa: E402


def row(round_number, mine, theirs):
    # obs holds the two blocks back to back: positions 0..4 = ours, 5..9 = theirs
    obs = list(mine) + list(theirs)
    return {"round": round_number, "obs": obs}


class MixTest(unittest.TestCase):

    def test_one_pick_per_row(self):
        rows = [row(2, [1, 0, 0, 0, 0], [0, 0, 1, 0, 0])]
        ours, theirs, used = phase_mix.mix(rows, [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertEqual(used, 1)
        self.assertAlmostEqual(ours[0], 1.0)
        self.assertAlmostEqual(theirs[2], 1.0)
        self.assertAlmostEqual(float(np.sum(ours)), 1.0)

    def test_first_round_is_skipped(self):
        # no picks are revealed before round 2; an empty vector must not count as a row
        rows = [row(1, [0, 0, 0, 0, 0], [0, 0, 0, 0, 0]), row(2, [1, 0, 0, 0, 0], [1, 0, 0, 0, 0])]
        _, _, used = phase_mix.mix(rows, [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertEqual(used, 1)

    def test_rows_are_averaged(self):
        rows = [row(2, [1, 0, 0, 0, 0], [1, 0, 0, 0, 0]), row(3, [0, 1, 0, 0, 0], [0, 0, 0, 1, 0])]
        ours, theirs, used = phase_mix.mix(rows, [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertEqual(used, 2)
        self.assertAlmostEqual(ours[0], 0.5)
        self.assertAlmostEqual(ours[1], 0.5)
        self.assertAlmostEqual(theirs[3], 0.5)

    def test_multi_flag_vector_is_normalised_not_summed_raw(self):
        # not produced by the current encoder (one pick per round), but the tool must not turn a changed encoding into a fake 150% mix
        rows = [row(2, [1, 1, 0, 0, 0], [0, 0, 1, 0, 0])]
        ours, _, _ = phase_mix.mix(rows, [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertAlmostEqual(ours[0], 0.5)
        self.assertAlmostEqual(float(np.sum(ours)), 1.0)

    def test_a_row_without_picks_for_one_seat_is_skipped(self):
        rows = [row(2, [1, 0, 0, 0, 0], [0, 0, 0, 0, 0])]
        _, _, used = phase_mix.mix(rows, [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertEqual(used, 0)

    def test_empty(self):
        ours, theirs, used = phase_mix.mix([], [0, 1, 2, 3, 4], [5, 6, 7, 8, 9])
        self.assertEqual(used, 0)
        self.assertAlmostEqual(float(np.sum(ours)), 0.0)


if __name__ == "__main__":
    unittest.main()
