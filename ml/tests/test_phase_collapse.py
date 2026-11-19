"""The phase-share collapse: the plan, not a menu.

Why: the owner looked at the player and said "you collapsed into a phase-3 fighter" (19/11). The loop could not see it -
`action_coverage` checks the options inside a decision, and a player that picks phase 3 in 40% of turns and phase 1 in 9% has
perfectly covered menus. A plan that is 40% one phase and 9% another has lost its alternative.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402


class PhaseFlagsTest(unittest.TestCase):

    def test_the_measured_phase_3_fighter_is_flagged(self):
        # the 73% bundle: 1:9%, 2:26%, 3:40%, 4:9%, 5:16% - no phase above 55%, but the plan has lost its alternative
        shares = {1: 9, 2: 26, 3: 40, 4: 9, 5: 16}
        reasons = night_loop.phase_flags(shares, 0.92, 0.10)
        self.assertTrue(reasons, "a concentrated plan must be flagged")
        self.assertTrue(any("concentrated" in reason for reason in reasons))
        self.assertTrue(any("PHASE 1" in reason for reason in reasons))

    def test_the_champion_mix_passes(self):
        # 26/21/23/15/15 - the mix the owner called healthy
        shares = {1: 26, 2: 21, 3: 23, 4: 15, 5: 15}
        self.assertEqual(night_loop.phase_flags(shares, 0.92, 0.10), [])

    def test_a_balanced_mix_passes(self):
        shares = {1: 20, 2: 20, 3: 20, 4: 20, 5: 20}
        self.assertEqual(night_loop.phase_flags(shares, 0.92, 0.10), [])

    def test_a_phase_that_disappears_is_named(self):
        shares = {1: 2, 2: 30, 3: 25, 4: 22, 5: 21}
        reasons = night_loop.phase_flags(shares, 0.92, 0.10)
        self.assertTrue(any("PHASE 1" in reason and "almost never" in reason for reason in reasons))

    def test_a_missing_phase_counts_as_almost_never(self):
        shares = {1: 26, 2: 25, 3: 25, 4: 24}
        reasons = night_loop.phase_flags(shares, 0.92, 0.10)
        self.assertTrue(any("PHASE 5" in reason for reason in reasons))

    def test_one_phase_only_is_the_clearest_collapse(self):
        self.assertTrue(night_loop.phase_flags({3: 50}, 0.92, 0.08))

    def test_an_empty_mix_is_not_a_collapse(self):
        self.assertEqual(night_loop.phase_flags({}, 0.92, 0.08), [])


if __name__ == "__main__":
    unittest.main()
