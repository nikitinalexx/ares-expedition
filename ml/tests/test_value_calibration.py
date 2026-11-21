"""Tests for `ml/value_calibration.py`.

Why: the tool turns a recorded corpus into a calibration curve, and the whole curve is only as good as the forward pass agreeing with the trainer's. The
trainer writes `reference.json` (8 inputs and their PyTorch logits) next to the weights; the Java loader refuses a model that disagrees with it, and this
test does the same for the Python path. A wrong layer order or a sigmoid applied twice would produce a plausible-looking curve that means nothing.
"""
import json
import os
import sys
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import value_calibration  # noqa: E402

MODEL = "D:/ares/runs/value/v_1118-2332_17_s1"


class ForwardPassTest(unittest.TestCase):

    @unittest.skipUnless(os.path.exists(os.path.join(MODEL, "reference.json")), "no exported critic on this machine")
    def test_matches_the_trainers_reference(self):
        model = value_calibration.load(MODEL)
        with open(os.path.join(MODEL, "reference.json"), encoding="utf-8") as f:
            reference = json.load(f)
        inputs = np.array(reference["inputs"], dtype=np.float32)
        expected = np.array(reference["logits"], dtype=np.float64)
        got = value_calibration.logits(model, inputs)
        self.assertLess(float(np.max(np.abs(got - expected))), 1e-3)

    @unittest.skipUnless(os.path.exists(os.path.join(MODEL, "reference.json")), "no exported critic on this machine")
    def test_ensemble_of_one_is_the_same_model(self):
        model = value_calibration.load(MODEL)
        inputs = np.random.default_rng(1).normal(size=(4, 1256)).astype(np.float32)
        np.testing.assert_allclose(value_calibration.ensemble_logits([model], inputs),
                                   value_calibration.logits(model, inputs), rtol=1e-9, atol=1e-9)


class CalibrationTest(unittest.TestCase):

    def test_bins_report_mean_and_actual(self):
        predicted = np.array([0.05, 0.05, 0.55, 0.55, 0.95])
        won = np.array([0.0, 1.0, 0.0, 1.0, 1.0])
        rows = value_calibration.calibration(predicted, won, bins=5)
        self.assertEqual(len(rows), 3)
        self.assertEqual(rows[0][1], 2)
        self.assertAlmostEqual(rows[0][2], 0.05)
        self.assertAlmostEqual(rows[0][3], 0.5)

    def test_sigmoid_is_the_inverse_logit(self):
        x = np.array([-2.0, 0.0, 2.0])
        np.testing.assert_allclose(value_calibration.sigmoid(x), [0.11920292, 0.5, 0.88079708], rtol=1e-6)


if __name__ == "__main__":
    unittest.main()
