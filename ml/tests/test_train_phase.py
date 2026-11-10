"""Tests of the PICK_PHASE trainer on synthetic data with a known rule. Needs torch (skipped otherwise):
    ml\\.venv\\Scripts\\python.exe -m unittest discover -s ml/tests -v
"""
import json
import os
import struct
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

ML = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ML))

from ares_data import record_dtype  # noqa: E402
from phase_model import load_exported, numpy_forward  # noqa: E402

try:
    import torch  # noqa: F401
    HAVE_TORCH = True
except ImportError:
    HAVE_TORCH = False

OBS = 12


def write_chunk(path, n_games, seed, schema_hash="synthetic0000hash", rows_per_game=6, rule=None, nan=False):
    """Rows whose outcome follows `rule(phase, obs0)` so that the trainer has something to find."""
    schema = {"type": "PICK_PHASE", "obsSize": OBS, "maxCandidates": 5, "candidateFeatures": 6, "obsDtype": "f2", "hash": schema_hash, "observationLayout": "synthetic"}
    dtype = record_dtype(schema)
    n = n_games * rows_per_game
    rng = np.random.default_rng(seed)
    rows = np.zeros(n, dtype=dtype)
    rows["obs"] = rng.random((n, OBS)).astype(np.float16)
    rows["count"] = 5
    rows["ids"][:] = np.arange(1, 6)
    for k in range(5):
        rows["feat"][:, k, k] = 1.0
    rows["chosen"] = rng.integers(0, 5, n)
    rows["game"] = np.repeat(np.arange(seed * 10_000, seed * 10_000 + n_games), rows_per_game)
    phase = rows["chosen"].astype(int) + 1
    outcome = np.array([rule(p, o) for p, o in zip(phase, rows["obs"][:, 0])], dtype=np.float32)
    rows["outcome"] = outcome
    rows["margin"] = np.where(outcome > 0.5, 3.0, np.where(outcome < 0.5, -3.0, 0.0))
    if nan:
        rows["obs"][0, 0] = np.nan
    header = json.dumps(schema).encode()
    with open(path, "wb") as f:
        f.write(b"ARES" + struct.pack("<HI", 2, len(header)) + header + rows.tobytes())


def phase_three_wins(phase, obs0):
    return 1.0 if phase == 3 else 0.0


@unittest.skipUnless(HAVE_TORCH, "torch is not installed")
class TrainPhase(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.tmp = Path(self._tmp.name)
        self.data = self.tmp / "data"
        self.data.mkdir()
        for i in range(3):
            write_chunk(self.data / f"PICK_PHASE.{i + 1:06d}.ares", 400, i + 1, rule=phase_three_wins)

    def tearDown(self):
        self._tmp.cleanup()

    def train(self, out, *extra, data=None):
        cmd = [sys.executable, str(ML / "train_phase.py"), "--data", str(data or self.data), "--out", str(out), "--hidden", "16,8",
               "--batch", "256", "--device", "cpu", "--lr", "3e-3", *extra]
        return subprocess.run(cmd, capture_output=True, text=True)

    def test_it_learns_a_known_rule_and_the_export_is_consistent(self):
        out = self.tmp / "m"
        r = self.train(out, "--epochs", "6")
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        self.assertTrue((out / "DONE").exists())
        metrics = json.loads((out / "train_metrics.json").read_text())
        self.assertTrue(metrics["beats_baseline"], metrics["history"][-1])
        self.assertLess(metrics["export_max_abs_diff"], 1e-4)

        meta, params = load_exported(str(out))  # read back by the independent numpy implementation
        rng = np.random.default_rng(7)
        cands = np.zeros((5, 6))
        cands[np.arange(5), np.arange(5)] = 1.0
        wins = 0
        for _ in range(50):
            logits = numpy_forward(params, meta["clip"], rng.random(OBS), cands)
            wins += int(np.argmax(logits) == 2)
            self.assertGreater(logits[2] - np.delete(logits, 2).max(), 0.5, "phase 3 is the only winning one")
        self.assertEqual(50, wins)

    def test_the_action_effect_tells_whether_the_choice_matters(self):
        out = self.tmp / "m"
        self.assertEqual(0, self.train(out, "--epochs", "6").returncode)
        effect = json.loads((out / "train_metrics.json").read_text())["action_effect"]
        self.assertGreater(effect, 0.1, "the result depends on the phase: swapping it must hurt")

        data = self.tmp / "data_no_action_effect"
        data.mkdir()
        for i in range(3):
            write_chunk(data / f"PICK_PHASE.{i + 1:06d}.ares", 400, i + 1, rule=lambda p, o: 1.0 if float(o) > 0.5 else 0.0)
        out2 = self.tmp / "m_no_effect"
        self.assertEqual(0, self.train(out2, "--epochs", "6", data=data).returncode)
        effect2 = json.loads((out2 / "train_metrics.json").read_text())["action_effect"]
        self.assertLess(abs(effect2), 0.03, "the result does not depend on the phase: swapping it changes nothing")

    def test_it_learns_the_dependence_on_the_observation_too(self):
        data = self.tmp / "data2"
        data.mkdir()
        for i in range(3):
            write_chunk(data / f"PICK_PHASE.{i + 1:06d}.ares", 400, i + 1, rule=lambda p, o: 1.0 if (p == 2) == (float(o) > 0.5) else 0.0)
        out = self.tmp / "m2"
        r = self.train(out, "--epochs", "12", "--hidden", "32,16", data=data)
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        meta, params = load_exported(str(out))
        cands = np.zeros((5, 6))
        cands[np.arange(5), np.arange(5)] = 1.0
        high, low = np.full(OBS, 0.9), np.full(OBS, 0.1)
        self.assertEqual(1, int(np.argmax(numpy_forward(params, meta["clip"], high, cands))), "obs0 high: phase 2 wins")
        self.assertNotEqual(1, int(np.argmax(numpy_forward(params, meta["clip"], low, cands))), "obs0 low: anything but phase 2 wins")

    def test_a_restarted_trainer_continues_from_the_checkpoint(self):
        out = self.tmp / "m"
        self.assertEqual(0, self.train(out, "--epochs", "1").returncode)
        (out / "DONE").unlink()  # as if it died after the first epoch's checkpoint
        r = self.train(out, "--epochs", "3")
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("resuming from the checkpoint after epoch 1", r.stdout)
        epochs = [json.loads(l)["epoch"] for l in r.stdout.splitlines() if l.startswith("{")]
        self.assertEqual([2, 3], epochs, "only the missing epochs are trained")

    def test_warm_start_from_the_previous_model(self):
        first = self.tmp / "m1"
        self.assertEqual(0, self.train(first, "--epochs", "2").returncode)
        r = self.train(self.tmp / "m2", "--epochs", "1", "--prev", str(first))
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("warm start", r.stdout)

    def test_a_previous_model_of_another_architecture_is_not_used(self):
        first = self.tmp / "m1"
        self.assertEqual(0, self.train(first, "--epochs", "1", "--hidden", "8,4").returncode)
        r = self.train(self.tmp / "m2", "--epochs", "1", "--prev", str(first))
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("training from scratch", r.stdout)

    def test_data_of_different_encoders_is_not_mixed(self):
        write_chunk(self.data / "PICK_PHASE.000004.ares", 50, 9, schema_hash="anotherencoder00", rule=phase_three_wins)
        r = self.train(self.tmp / "m", "--epochs", "1")
        self.assertEqual(3, r.returncode)
        self.assertIn("must not be mixed", r.stderr)
        self.assertFalse((self.tmp / "m" / "DONE").exists())

    def test_nan_in_the_data_stops_the_training(self):
        write_chunk(self.data / "PICK_PHASE.000001.ares", 50, 1, rule=phase_three_wins, nan=True)
        r = self.train(self.tmp / "m", "--epochs", "1")
        self.assertEqual(3, r.returncode)
        self.assertFalse((self.tmp / "m" / "DONE").exists())

    def test_no_data_is_an_error(self):
        empty = self.tmp / "empty"
        empty.mkdir()
        r = self.train(self.tmp / "m", "--epochs", "1", data=empty)
        self.assertEqual(2, r.returncode)

    def test_validation_split_is_by_game_and_stable(self):
        from train_phase import is_validation_game
        games = np.arange(100_000, dtype=np.uint64)
        side = is_validation_game(games, 0.1)
        self.assertAlmostEqual(0.1, side.mean(), delta=0.01)
        self.assertTrue(np.array_equal(side, is_validation_game(games, 0.1)), "deterministic")
        rows_of_games = np.repeat(games[:1000], 6)
        row_side = is_validation_game(rows_of_games, 0.1).reshape(1000, 6)
        self.assertTrue((row_side == row_side[:, :1]).all(), "all rows of a game are on the same side")


if __name__ == "__main__":
    unittest.main()
