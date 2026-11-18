"""Dropping the exploration flag of a menu that produced a collapsed corpus.

Why: without it the gate can loop - a blocked iteration trains nothing, the coverage detector sees the same collapse, adds the
same exploration, and the night repeats the block until the budget is gone. That is grinding without learning.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402


class DropExploreTest(unittest.TestCase):

    def test_one_menu_is_removed_and_the_rest_kept(self):
        self.assertEqual(night_loop.drop_explore("BUILD_CARD:0.15,MULLIGAN:0.9", {"MULLIGAN"}),
                         "BUILD_CARD:0.15")

    def test_nothing_to_drop_leaves_the_spec_alone(self):
        self.assertEqual(night_loop.drop_explore("BUILD_CARD:0.15,MULLIGAN:0.9", set()),
                         "BUILD_CARD:0.15,MULLIGAN:0.9")

    def test_dropping_everything_is_an_empty_spec_not_none(self):
        self.assertEqual(night_loop.drop_explore("MULLIGAN:0.9", {"MULLIGAN"}), "")

    def test_an_empty_spec_stays_empty(self):
        self.assertEqual(night_loop.drop_explore("", {"MULLIGAN"}), "")


if __name__ == "__main__":
    unittest.main()
