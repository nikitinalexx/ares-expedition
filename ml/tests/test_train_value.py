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
