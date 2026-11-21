"""Tests of the exported value model's contract with the engine.

The engine always feeds the FULL observation. So a critic trained with a block zeroed is only deployable if the exported
weights provably ignore that block - the weights of a column that is always zero never receive a gradient and would keep
their random initial values. `mask_features` zeroes them and `train_value.main` refuses to export a model that reacts.
"""
import os
import sys
import unittest

import numpy as np

ML = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ML)

import train_value  # noqa: E402


def random_params(obs_size=24, hidden=(8, 6), seed=3):
    rng = np.random.default_rng(seed)
    h1, h2 = hidden
    return {"mean": np.zeros(obs_size, dtype=np.float32),
            "std": np.ones(obs_size, dtype=np.float32),
            "w1": rng.normal(0, 0.3, (h1, obs_size)).astype(np.float32),
            "b1": rng.normal(0, 0.1, h1).astype(np.float32),
            "w2": rng.normal(0, 0.3, (h2, h1)).astype(np.float32),
            "b2": rng.normal(0, 0.1, h2).astype(np.float32),
            "w3": rng.normal(0, 0.3, h2).astype(np.float32),
            "b3": np.zeros(1, dtype=np.float32)}


class BatchSize(unittest.TestCase):
    """The optimiser step count IS the batch size, and it was never asserted.

    The champion's critics were trained with the default `--batch 4096` on 143 936 rows: 35 updates per epoch, 140 in
    total, 14 seconds of training for the value every later decision was ranked by. The batch is the only knob that sets
    how many times the optimiser sees the data, so a silent default there is a silent cap on the whole model - this test
    pins that a smaller batch really produces more updates on the same rows.
    """

    def fit(self, batch):
        rng = np.random.default_rng(4)
        obs = rng.normal(size=(400, 12)).astype(np.float32)
        outcome = (rng.random(400) > 0.5).astype(np.float32)
        return train_value.fit_value(obs, outcome, hidden=(8, 6), epochs=1, batch=batch, seed=2,
                                     device="cpu", verbose=False)

    def test_a_small_batch_makes_more_updates_on_the_same_rows(self):
        big = self.fit(4096)["history"][0]["batches"]
        small = self.fit(32)["history"][0]["batches"]
        self.assertEqual(big, 1, "400 rows in one 4096-batch is a single update")
        self.assertGreaterEqual(small, 10, "400 rows at batch 32 must be ~12 updates")
        self.assertGreater(small, big * 5)

    def test_the_default_batch_is_not_a_cosmic_one(self):
        # Not a style rule: 4096 is what the champion's critic silently used, and it made 140 updates in total. If the
        # default ever returns there, this fails and the next run has to say so out loud.
        self.assertLessEqual(train_value.DEFAULT_BATCH, 256)


class NestedSubset(unittest.TestCase):
    """The 1 000-game run must be a subset of the 5 000-game run: that is what makes a volume comparison a volume test."""

    def test_smaller_limits_are_nested_and_ascending(self):
        ids = np.repeat(np.arange(100, 0 - 1, -1), 3)  # unsorted ids, several rows per game
        small = train_value.nested_games_subset(ids, 10)
        large = train_value.nested_games_subset(ids, 50)
        np.testing.assert_array_equal(small, np.arange(0, 10))
        self.assertTrue(set(small.tolist()) <= set(large.tolist()), "the small corpus must be inside the large one")
        self.assertEqual(len(large), 50)

    def test_no_limit_keeps_everything(self):
        self.assertIsNone(train_value.nested_games_subset(np.arange(10), 0))


class OptimizerChoice(unittest.TestCase):
    """The owner's recipe is SGD 0.01 + momentum 0.9, and the critic trainer only had Adam.

    Every batch-size claim measured so far was therefore about Adam's update rule, not his. This pins that the optimiser
    is selectable, that it changes the result, and that momentum is passed through.
    """

    def fit(self, optimizer, momentum=0.9):
        rng = np.random.default_rng(4)
        obs = rng.normal(size=(300, 10)).astype(np.float32)
        outcome = (rng.random(300) > 0.5).astype(np.float32)
        return train_value.fit_value(obs, outcome, hidden=(8, 6), epochs=2, batch=32, lr=0.01, seed=2,
                                     device="cpu", verbose=False, optimizer=optimizer, momentum=momentum)

    def test_sgd_trains_and_differs_from_adam(self):
        adam = self.fit("adam")
        sgd = self.fit("sgd")
        self.assertTrue(np.isfinite(sgd["params"]["w1"]).all())
        self.assertFalse(np.allclose(adam["params"]["w1"], sgd["params"]["w1"]),
                         "SGD and Adam produced identical weights: the flag is not wired")

    def test_momentum_changes_the_update(self):
        none = self.fit("sgd", momentum=0.0)
        heavy = self.fit("sgd", momentum=0.9)
        self.assertFalse(np.allclose(none["params"]["w1"], heavy["params"]["w1"]),
                         "momentum 0 and 0.9 gave identical weights: it is not passed to the optimiser")


class MaskedColumns(unittest.TestCase):
    def test_a_masked_model_is_invariant_to_the_block_and_the_plain_one_is_not(self):
        params = random_params()
        columns = [2, 5, 7, 11]
        row = np.random.default_rng(9).normal(size=24)
        bumped = row.copy()
        bumped[columns] = 1.0

        masked = train_value.mask_features(params, columns)
        self.assertAlmostEqual(float(train_value.numpy_forward(masked, 8.0, row)),
                               float(train_value.numpy_forward(masked, 8.0, bumped)), places=9)
        self.assertNotAlmostEqual(float(train_value.numpy_forward(params, 8.0, row)),
                                  float(train_value.numpy_forward(params, 8.0, bumped)), places=6,
                                  msg="without the mask the random weights on those columns do react: the test would pass "
                                      "for the wrong reason")

    def test_masking_does_not_touch_anything_else(self):
        params = random_params()
        columns = [0, 3]
        masked = train_value.mask_features(params, columns)
        keep = [i for i in range(24) if i not in columns]
        np.testing.assert_array_equal(masked["w1"][:, keep], params["w1"][:, keep])
        np.testing.assert_array_equal(masked["mean"][keep], params["mean"][keep])
        np.testing.assert_array_equal(masked["std"][keep], params["std"][keep])
        np.testing.assert_array_equal(masked["w1"][:, columns], np.zeros((8, len(columns))))
        np.testing.assert_array_equal(masked["mean"][columns], np.zeros(len(columns)))
        np.testing.assert_array_equal(masked["std"][columns], np.ones(len(columns)))

    def test_no_columns_is_the_identity(self):
        params = random_params()
        self.assertIs(train_value.mask_features(params, []), params)

    def test_the_exported_layout_matches_the_masked_parameters(self):
        for name, shape in train_value.value_weight_layout(24, (8, 6)):
            self.assertEqual(tuple(shape), train_value.mask_features(random_params(), [1])[name].shape)


if __name__ == "__main__":
    unittest.main()
