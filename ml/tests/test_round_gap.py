"""Tests for `ml/round_gap.py`.

Why: the report says WHEN the gap against a benchmark opens, and that number will choose the next direction (tempo, conversion or plan). A silent error -
taking the first row of a round instead of the last, mixing two seats - would point the project at the wrong half of the game.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import round_gap  # noqa: E402


def row(game, seat, rnd, step, mine, theirs):
    obs = [0.0] * 4
    obs[1] = mine
    obs[2] = theirs
    return {"game": game, "seat": seat, "round": rnd, "step": step, "obs": obs}


class RoundScoresTest(unittest.TestCase):

    def test_takes_the_last_row_of_a_round(self):
        rows = [row(0, 0, 3, 0, 10.0, 4.0), row(0, 0, 3, 7, 14.0, 9.0)]
        per = round_gap.round_scores(rows, 1, 2)
        self.assertEqual(per[(0, 0, 3)], (14.0, 9.0))

    def test_seats_and_games_do_not_mix(self):
        rows = [row(0, 0, 1, 0, 3.0, 1.0), row(0, 1, 1, 0, 5.0, 2.0), row(1, 0, 1, 0, 7.0, 7.0)]
        per = round_gap.round_scores(rows, 1, 2)
        self.assertEqual(per[(0, 0, 1)], (3.0, 1.0))
        self.assertEqual(per[(0, 1, 1)], (5.0, 2.0))
        self.assertEqual(per[(1, 0, 1)], (7.0, 7.0))


class GapTest(unittest.TestCase):

    def test_mean_gap_per_round(self):
        per = {(0, 0, 1): (10.0, 4.0), (1, 0, 1): (8.0, 4.0), (0, 0, 2): (12.0, 10.0)}
        table = round_gap.gap_by_round(per)
        self.assertEqual(table[0][0], 1)
        self.assertAlmostEqual(table[0][1], 5.0)   # (6 + 4) / 2
        self.assertAlmostEqual(table[1][1], 2.0)

    def test_empty(self):
        self.assertEqual(round_gap.gap_by_round({}), [])


class UnitsTest(unittest.TestCase):

    def test_parses_the_java_vector(self):
        text = "NORMALIZATION_VECTOR = new float[]{ 52.0f, 100.0f, 1.0f };"
        self.assertEqual(round_gap.normalisation_vector(text), [52.0, 100.0, 1.0])

    def test_missing_vector_fails_loudly(self):
        with self.assertRaises(ValueError):
            round_gap.normalisation_vector("public static final int X = 1;")

    def test_divisors_follow_the_mask(self):
        # final column 0 <- base 2, final column 1 <- base 0, final column 2 is past the V2 block
        self.assertEqual(round_gap.divisors(["a", "b", "ctx"], [2, 0], [52.0, 7.0, 100.0]), [100.0, 52.0, 1.0])

    def test_scaling_turns_a_normalised_gap_into_units(self):
        per = {(0, 0, 1): (0.10, 0.05)}
        tables = round_gap.multi_gap({"money": per}, [("money", 100.0)])
        self.assertAlmostEqual(tables[0][1][0][1], 5.0)

    def test_column_spec(self):
        names = ["win_points", "op_win_points", "mc"]
        self.assertEqual(round_gap.parse_columns("win_points=op_win_points=score", names),
                         [(0, 1, "score")])


if __name__ == "__main__":
    unittest.main()
