"""The strategy-diversity sensor: the plan mix, not the action mix.

The guard sees collapsed options; this sees a player that uses every menu and still plays one plan. The tests pin the two
properties that make the report readable: a profile label is what it says, and a corpus dominated by one plan reports a
large top share with a small effective number of profiles - with no clustering artefact (k-means with k groups always
partitions into k groups, which is why it was rejected).
"""
import os
import sys
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import strategy_mix  # noqa: E402


def row(phase, channel_index, activity=(0, 0, 0, 0, 0)):
    shares = np.zeros(len(strategy_mix.CHANNELS))
    shares[channel_index] = 1.0
    return np.concatenate([shares, [phase / 5.0], np.asarray(activity, dtype=float)])


class ProfileKeyTest(unittest.TestCase):

    def test_the_label_says_phase_and_channel(self):
        self.assertEqual("phase3:resourceCards", strategy_mix.profile_key(row(3, 3)))
        self.assertEqual("phase1:forests", strategy_mix.profile_key(row(1, 1)))
        self.assertEqual("phase5:cards", strategy_mix.profile_key(row(5, 2)))

    def test_an_unknown_phase_is_labelled_rather_than_guessed(self):
        self.assertEqual("phase?:tr", strategy_mix.profile_key(row(0, 0)))


class SummaryTest(unittest.TestCase):

    def test_a_dominated_corpus_reports_a_big_top_share(self):
        matrix = np.array([row(3, 3) for _ in range(80)] + [row(1, 1) for _ in range(20)])
        report = strategy_mix.summarise(matrix)
        self.assertEqual("phase3:resourceCards", report["top_profile"])
        self.assertAlmostEqual(0.80, report["top_share"], places=3)
        self.assertEqual(2, report["profiles"])
        self.assertAlmostEqual(1.65, report["effective_profiles"], places=2)  # exp(-0.8ln0.8 - 0.2ln0.2) = 1.649

    def test_a_mixed_corpus_reports_more_effective_profiles(self):
        matrix = np.array([row(phase, channel) for phase in (1, 2, 3, 4, 5) for channel in (0, 2, 3, 5)])
        report = strategy_mix.summarise(matrix)
        self.assertEqual(20, report["profiles"])
        self.assertAlmostEqual(0.05, report["top_share"], places=3)          # every profile holds 1/20
        self.assertAlmostEqual(20.0, report["effective_profiles"], places=1)

    def test_an_empty_matrix_does_not_crash(self):
        self.assertEqual({"player_games": 0}, strategy_mix.summarise(np.zeros((0, 13))))


class ProfilesTest(unittest.TestCase):

    def test_profiles_are_distributions_over_the_score_channels(self):
        pack = "D:/ares/pack/bdcheck"
        if not os.path.isdir(pack):
            self.skipTest("the 18.7k pack is not on this machine")
        matrix = strategy_mix.profiles(pack, limit=500)
        self.assertGreater(len(matrix), 100)
        channels = matrix[:, :len(strategy_mix.CHANNELS)]
        np.testing.assert_allclose(channels.sum(axis=1), 1.0, atol=1e-6)
        # a channel can be NEGATIVE: the game has cards with negative printed points (-3 in this pack)
        self.assertTrue(np.all(channels >= -0.25), channels.min())
        phases = matrix[:, len(strategy_mix.CHANNELS)]
        self.assertTrue(np.all(phases >= 0.0) and np.all(phases <= 1.05))


if __name__ == "__main__":
    unittest.main()
