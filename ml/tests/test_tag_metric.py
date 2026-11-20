"""Tests for the tag metric (`ml/tag_metric.py`).

Why: the microbe experiment is judged by this number, and both of its steps are easy to get subtly wrong - taking the first round instead of the last,
or mixing two seats of one game. A wrong number here would make a corpus look microbe-rich when it is not.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import tag_metric  # noqa: E402


def row(game, seat, rnd, value, column=3):
    obs = [0.0] * 6
    obs[column] = value
    return {"game": game, "seat": seat, "round": rnd, "obs": obs}


class LastPerSeatTest(unittest.TestCase):

    def test_takes_the_last_round_of_each_seat(self):
        rows = [row(0, 0, 1, 0.2), row(0, 0, 5, 0.8), row(0, 0, 3, 0.4),
                row(0, 1, 1, 0.0), row(0, 1, 4, 0.2)]
        per = tag_metric.last_per_seat(rows, 3)
        self.assertEqual(len(per), 2)
        self.assertAlmostEqual(per[(0, 0)], 0.8)
        self.assertAlmostEqual(per[(0, 1)], 0.2)

    def test_seats_of_one_game_do_not_mix(self):
        rows = [row(7, 0, 2, 1.0), row(7, 1, 2, 0.0)]
        per = tag_metric.last_per_seat(rows, 3)
        self.assertAlmostEqual(per[(7, 0)], 1.0)
        self.assertAlmostEqual(per[(7, 1)], 0.0)

    def test_empty(self):
        self.assertEqual(tag_metric.last_per_seat([], 3), {})


class MicrobeHeldTest(unittest.TestCase):

    def test_sums_the_counters_of_the_last_round(self):
        obs_early = [0.0] * 8
        obs_early[5] = 2.0
        obs_late = [0.0] * 8
        obs_late[3] = 5.0
        obs_late[5] = 1.0
        rows = [{"game": 0, "seat": 0, "round": 2, "obs": obs_early},
                {"game": 0, "seat": 0, "round": 6, "obs": obs_late}]
        per = tag_metric.microbe_held(rows, [3, 5])
        self.assertAlmostEqual(per[(0, 0)], 6.0)

    def test_a_seat_without_microbes_is_zero_not_missing(self):
        rows = [{"game": 1, "seat": 1, "round": 3, "obs": [0.0] * 8}]
        per = tag_metric.microbe_held(rows, [3, 5])
        self.assertAlmostEqual(per[(1, 1)], 0.0)


class MeanAndShareTest(unittest.TestCase):
    def test_mean_of_scaled_values(self):
        # the observation stores count/5, so 0.4 means two cards
        self.assertAlmostEqual(tag_metric.mean([0.4 * 5, 0.0]), 1.0)

    def test_share_at_least(self):
        self.assertAlmostEqual(tag_metric.share_at_least([0.0, 1.0, 2.0, 0.5], 1.0), 0.5)

    def test_empty_is_nan_not_zero(self):
        import math
        self.assertTrue(math.isnan(tag_metric.mean([])))
        self.assertTrue(math.isnan(tag_metric.share_at_least([], 1.0)))


if __name__ == "__main__":
    unittest.main()
