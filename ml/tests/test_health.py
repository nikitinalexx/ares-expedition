"""Tests for the health panel (`ml/health.py`).

Why: the owner's rule is that strength must not be bought with degeneration, so this panel decides whether a bundle is promoted. A silent error here
(an entropy that divides by the wrong maximum, an adaptation that counts a corporation with three games, a collapse test with the sign reversed) would
either block good work or wave a degenerate player through.
"""
import os
import sys
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import health  # noqa: E402


class EntropyTest(unittest.TestCase):

    def test_one_outcome_is_zero(self):
        self.assertEqual(health.shannon([5, 0, 0]), 0.0)
        self.assertEqual(health.normalised_entropy([5, 0, 0], 3), 0.0)

    def test_uniform_is_one(self):
        self.assertAlmostEqual(health.normalised_entropy([1, 1, 1], 3), 1.0)
        self.assertAlmostEqual(health.normalised_entropy([2, 2], 2), 1.0)

    def test_intermediate(self):
        value = health.normalised_entropy([3, 1], 2)
        self.assertGreater(value, 0.0)
        self.assertLess(value, 1.0)

    def test_empty(self):
        self.assertEqual(health.shannon([]), 0.0)


class HhiTest(unittest.TestCase):

    def test_single_card_is_one(self):
        self.assertAlmostEqual(health.hhi([7, 7, 7]), 1.0)

    def test_many_equal_cards_approach_zero(self):
        self.assertAlmostEqual(health.hhi([1, 2, 3, 4]), 0.25)

    def test_empty_is_one(self):
        self.assertEqual(health.hhi([]), 1.0)


class AdaptationTest(unittest.TestCase):

    def test_ignoring_the_deal_is_zero(self):
        mix = np.array([3, 3, 0, 0, 0])
        per_seat = {}
        for i in range(40):
            per_seat[(i, 0)] = {"phases": mix, "corp": 1 if i % 2 else 2}
        self.assertAlmostEqual(health.adaptation_distance(per_seat), 0.0)

    def test_adapting_to_the_deal_is_positive(self):
        per_seat = {}
        for i in range(40):
            per_seat[(i, 0)] = {"phases": np.array([6, 0, 0, 0, 0]) if i % 2 else np.array([0, 0, 6, 0, 0]),
                                "corp": 1 if i % 2 else 2}
        # each corporation has half the games and a disjoint phase: L1 distance 1.0, halved by the metric = 0.5
        self.assertAlmostEqual(health.adaptation_distance(per_seat), 0.5)

    def test_corporations_below_min_games_are_ignored(self):
        per_seat = {(0, 0): {"phases": np.array([9, 0, 0, 0, 0]), "corp": 1},
                    (1, 0): {"phases": np.array([0, 9, 0, 0, 0]), "corp": 2}}
        self.assertEqual(health.adaptation_distance(per_seat, min_games=30), 0.0)


class PanelTest(unittest.TestCase):

    def seat(self, phases, cards, corp=1, declines=0, standards=0):
        colors = {"green": 0, "blue": 0, "red": 0}
        for card in cards:
            colors["green" if card < 10 else "blue"] += 1
        return {"phases": np.array(phases, dtype=float), "cards": list(cards), "colors": colors,
                "corp": corp, "builds": len(cards), "declines": declines, "standards": standards}

    def test_panel_aggregates(self):
        per_seat = {(0, 0): self.seat([5, 0, 0, 0, 0], [1, 2, 10]),
                    (1, 0): self.seat([0, 0, 5, 0, 0], [3, 11], declines=1, standards=2)}
        values = health.panel(per_seat)
        self.assertEqual(values["game_seats"], 2)
        self.assertAlmostEqual(values["builds_per_game"], 2.5)
        self.assertAlmostEqual(values["build_acceptance"], 5 / 6)
        self.assertAlmostEqual(values["standard_projects_per_game"], 1.0)
        self.assertAlmostEqual(values["phase_entropy"], 0.0)          # each game has one phase
        self.assertAlmostEqual(values["distinct_cards_per_game"], 2.5)
        self.assertAlmostEqual(values["green_share"], 3 / 5)

    def test_collapse_detects_a_drop_beyond_tolerance(self):
        reference = {"builds_per_game": 10.0, "build_acceptance": 0.8, "standard_projects_per_game": 1.0,
                     "phase_entropy": 0.9, "color_entropy": 0.9, "distinct_cards_per_game": 20.0,
                     "adaptation_to_corp": 0.2}
        candidate = dict(reference)
        candidate["builds_per_game"] = 6.0        # -40% -> collapse
        candidate["phase_entropy"] = 0.85         # -5.5% -> fine
        dropped = health.collapse(reference, candidate)
        self.assertEqual([name for name, *_ in dropped], ["builds_per_game"])

    def test_collapse_is_quiet_inside_tolerance(self):
        reference = {"builds_per_game": 10.0, "build_acceptance": 0.8, "standard_projects_per_game": 1.0,
                     "phase_entropy": 0.9, "color_entropy": 0.9, "distinct_cards_per_game": 20.0,
                     "adaptation_to_corp": 0.2}
        candidate = dict(reference)
        candidate["builds_per_game"] = 8.5        # -15% inside the 20% tolerance
        self.assertEqual(health.collapse(reference, candidate), [])


if __name__ == "__main__":
    unittest.main()
