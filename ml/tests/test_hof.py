"""The hall-of-fame gates: absolute benchmarks, one head-to-head duel, then the pool.

These are pure functions on numbers, so they are tested without playing a game. A wrong gate is worse than no gate: it either
lets a specialist replace a champion, or blocks every improvement - both look like "the night ran".
"""
import json
import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import hof  # noqa: E402


class GateTest(unittest.TestCase):

    def test_absolute_gate_allows_a_small_dip_and_refuses_a_real_one(self):
        ok, reasons = hof.gate_absolute(0.6393, 0.6393, 0.928, 0.928)
        self.assertTrue(ok, reasons)

        ok, reasons = hof.gate_absolute(0.6250, 0.6393, 0.928, 0.928)      # -1.43 points
        self.assertTrue(ok, reasons)

        ok, reasons = hof.gate_absolute(0.6100, 0.6393, 0.928, 0.928)      # -2.9 points
        self.assertFalse(ok)
        self.assertIn("SMART", reasons[0])

    def test_absolute_gate_refuses_a_random_regression(self):
        ok, reasons = hof.gate_absolute(0.6500, 0.6393, 0.900, 0.928)
        self.assertFalse(ok)
        self.assertIn("RANDOM", reasons[0])

    def test_missing_numbers_fail_closed(self):
        ok, _ = hof.gate_absolute(None, 0.63, 0.92, 0.92)
        self.assertFalse(ok)

    def test_head_to_head_needs_both_the_estimate_and_the_interval(self):
        ok, reasons = hof.gate_head_to_head(0.58, 0.56, 2000)
        self.assertTrue(ok, reasons)

        ok, _ = hof.gate_head_to_head(0.53, 0.51, 2000)      # below the threshold
        self.assertFalse(ok)

        ok, _ = hof.gate_head_to_head(0.60, 0.49, 2000)      # point estimate fine, interval touches 50%
        self.assertFalse(ok)

        ok, _ = hof.gate_head_to_head(0.60, 0.57, 500)       # too few games
        self.assertFalse(ok)

        ok, _ = hof.gate_head_to_head(None, None, 2000)
        self.assertFalse(ok)

    def test_pool_gate_uses_the_recent_champions_and_needs_their_duels(self):
        champions = [{"name": n} for n in ("c1", "c2", "c3", "c4")]
        duels = {
            hof.pair_key("cand", "c2"): {"score": 0.52},
            hof.pair_key("cand", "c3"): {"score": 0.55},
            hof.pair_key("cand", "c4"): {"score": 0.50},
        }
        ok, reasons = hof.gate_pool("cand", champions, duels)
        self.assertTrue(ok, reasons)                            # c2..c4 are recent, c1 is not required

        duels[hof.pair_key("cand", "c4")] = {"score": 0.40}
        ok, reasons = hof.gate_pool("cand", champions, duels)
        self.assertFalse(ok)
        self.assertIn("c4", reasons[0])

        ok, reasons = hof.gate_pool("cand", champions, {})
        self.assertFalse(ok)
        self.assertIn("no pool duel", " ".join(reasons))

    def test_pair_key_is_order_independent(self):
        self.assertEqual(hof.pair_key("a", "b"), hof.pair_key("b", "a"))
        self.assertEqual(hof.pair_key("1118-0058_13", "1117-2001_11"), "1117-2001_11|1118-0058_13")


class PromotionTest(unittest.TestCase):

    def test_champions_are_never_overwritten_and_a_failed_gate_blocks_promotion(self):
        with mock.patch.object(hof, "HOF", os.path.join(self.tmp, "hof.json")):
            state = {"champions": [{"name": "old", "policy": "p", "value": "v", "search": "s",
                                    "measured": {"smart": 0.60, "random": 0.93}}],
                     "duels": {hof.pair_key("new", "old"): {"score": 0.52, "ci_low": 0.50, "games": 2000}}}
            hof.save(state, hof.HOF)

            argv = ["promote", "--name", "new", "--policy", "p2", "--value", "v2", "--smart", "0.61", "--random", "0.93"]
            with mock.patch.object(sys, "argv", ["hof.py"] + argv):
                self.assertEqual(1, hof.main())                 # 0.52 < 0.54 -> blocked

            argv = ["promote", "--name", "new", "--policy", "p2", "--value", "v2", "--smart", "0.61", "--random", "0.93",
                    "--force"]
            with mock.patch.object(sys, "argv", ["hof.py"] + argv):
                self.assertEqual(0, hof.main())                 # forced, and the entry records it
            state = hof.load(hof.HOF)
            self.assertEqual(["old", "new"], [c["name"] for c in state["champions"]])
            self.assertTrue(state["champions"][-1]["forced"])

            argv = ["promote", "--name", "new", "--policy", "p2", "--smart", "0.99", "--random", "0.99", "--force"]
            with mock.patch.object(sys, "argv", ["hof.py"] + argv):
                self.assertEqual(0, hof.main())
            state = hof.load(hof.HOF)
            self.assertEqual(2, len(state["champions"]), "an existing champion must never be duplicated or replaced")

    def setUp(self):
        import tempfile
        self.tmp = tempfile.mkdtemp(prefix="hof_test_")


if __name__ == "__main__":
    unittest.main()
