"""The loop's stopping guards: the rules that decide when a night is wasting itself.

Why these tests: on 18/11 a broken configuration scored 43.1% against a best of 60.1% and the loop logged "REGRESSION 1/2"
and started another identical iteration - the rollback rule was designed for noise and did not cover a collapse. A guard is
only a guard if a test says when it fires.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402


class CatastrophicTest(unittest.TestCase):

    def test_a_collapse_stops_the_night(self):
        # the measured case: 0.431 against the best 0.601
        self.assertTrue(night_loop.catastrophic(0.601, 0.431, 0.06))

    def test_ordinary_noise_does_not(self):
        # the iteration-to-iteration spread is +-3 points
        self.assertFalse(night_loop.catastrophic(0.601, 0.575, 0.06))
        self.assertFalse(night_loop.catastrophic(0.601, 0.601, 0.06))

    def test_a_drop_exactly_at_the_threshold_does_not_fire(self):
        self.assertFalse(night_loop.catastrophic(0.601, 0.541, 0.06))
        self.assertTrue(night_loop.catastrophic(0.601, 0.540, 0.06))

    def test_missing_numbers_never_stop_a_run(self):
        self.assertFalse(night_loop.catastrophic(None, 0.4, 0.06))
        self.assertFalse(night_loop.catastrophic(0.6, None, 0.06))

    def test_a_rise_is_not_catastrophic(self):
        self.assertFalse(night_loop.catastrophic(0.60, 0.66, 0.06))


class ConfigSummaryTest(unittest.TestCase):

    def test_the_summary_names_every_knob_that_changes_a_score(self):
        class Args:
            types = "A,B,C,D"
            generation_symmetric = False
            record_afterstates = True
            critic_afterstates = True
            replay_packs = 2
            policy_replay = False
            policy_adv = True
            explore = "BUILD_CARD:0.15"
            rows_per_type = 12
            phase_critic_model = "D:/ares/runs/value/v_1118-0058_13_s1"
            phase_critic = True
            phase_playout = True

        summary = night_loop.config_summary(Args())
        for piece in ("types=4 menus", "sym_gen=False", "afterstates=True/True", "replay=2",
                      "adv=True", "explore='BUILD_CARD:0.15'", "rows=12",
                      "phase=D:/ares/runs/value/v_1118-0058_13_s1", "+rollout"):
            self.assertIn(piece, summary)


if __name__ == "__main__":
    unittest.main()
