"""The generator survives the rollout memory.

Measured 19/11: with `phasePlayout` and `playout=MULLIGAN` the self-play JVM grows past any heap every ~250 games and exits
with code 6 ("OUT OF MEMORY: ... restart it after the finished chunks"). Two 2000-game runs died at 93 and 252 games, and the
finished chunks are the expensive artifact - so the pipeline continues from them instead of starting over.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402


class ShouldRestartTest(unittest.TestCase):

    def test_an_out_of_memory_exit_continues_from_the_chunks(self):
        self.assertTrue(night_loop.should_restart(6, 250, 2000))

    def test_a_clean_exit_continues_when_games_are_missing(self):
        self.assertTrue(night_loop.should_restart(0, 250, 2000))

    def test_a_real_failure_stops(self):
        self.assertFalse(night_loop.should_restart(4, 250, 2000))   # TOO_MANY_FAILURES

    def test_enough_games_stops_whatever_the_code(self):
        self.assertFalse(night_loop.should_restart(6, 2000, 2000))
        self.assertFalse(night_loop.should_restart(0, 2500, 2000))


if __name__ == "__main__":
    unittest.main()
