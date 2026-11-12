"""Tests for train_all (multi-type + shared trunk). Needs torch."""
import json
import struct
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

ML = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ML))

from ares_data import discover_decisions, record_dtype  # noqa: E402
from phase_model import load_exported, numpy_forward  # noqa: E402

try:
    import torch  # noqa: F401
    HAVE_TORCH = True
except ImportError:
    HAVE_TORCH = False

OBS = 12


def write_typed_chunk(path, decision, cand_features, n_games, seed, schema_hash="synthetic0000hash", rows_per_game=4):
    schema = {"type": decision, "obsSize": OBS, "maxCandidates": 5, "candidateFeatures": cand_features,
              "obsDtype": "f2", "candDtype": "f2", "hash": schema_hash, "observationLayout": "synthetic"}
    dtype = record_dtype(schema, 4)
    n = n_games * rows_per_game
    rng = np.random.default_rng(seed)
    rows = np.zeros(n, dtype=dtype)
    rows["obs"] = rng.random((n, OBS)).astype(np.float16)
    # first obs feature correlates with the winning candidate index for a learnable signal
    rows["obs"][:, 0] = (rng.integers(0, 5, n) / 5.0).astype(np.float16)
    rows["count"] = 5
    rows["ids"][:] = np.arange(1, 6)
    for k in range(5):
        rows["feat"][:, k, 0] = 1.0 if k < cand_features else 0.0
        if cand_features > 1:
            rows["feat"][:, k, min(k, cand_features - 1)] = 1.0
    rows["chosen"] = (rows["obs"][:, 0].astype(np.float32) * 5).astype(np.uint8).clip(0, 4)
    rows["game"] = np.repeat(np.arange(seed * 10_000, seed * 10_000 + n_games), rows_per_game)
    rows["outcome"] = np.where(rows["chosen"] == (rows["obs"][:, 0].astype(np.float32) * 5).astype(int).clip(0, 4), 1.0, 0.0).astype(np.float32)
    rows["margin"] = np.where(rows["outcome"] > 0.5, 2.0, -2.0)
    header = json.dumps(schema).encode()
    with open(path, "wb") as f:
        f.write(b"ARES" + struct.pack("<HI", 4, len(header)) + header + rows.tobytes())


class Discover(unittest.TestCase):
    def test_discovers_prefixes(self):
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            root = Path(tmp)
            write_typed_chunk(root / "PICK_PHASE.000001.ares", "PICK_PHASE", 6, 2, 1)
            write_typed_chunk(root / "BUILD_CARD.000001.ares", "BUILD_CARD", 8, 2, 2)
            (root / "BUILD_CARD.000002.ares.tmp").write_bytes(b"nope")
            self.assertEqual(["BUILD_CARD", "PICK_PHASE"], discover_decisions(str(root)))


class Pool(unittest.TestCase):
    def test_cap_keeps_at_most_k_random_rows_of_every_game(self):
        from train_all import cap_per_game
        games = np.repeat(np.arange(50), 30)  # the rows of a game come in a row, as the writer stores them
        rng = np.random.default_rng(0)
        kept = cap_per_game(games, 5, rng)
        self.assertTrue(np.all(np.diff(kept) > 0), "sorted: the memmap is read forward")
        self.assertEqual([5] * 50, np.bincount(games[kept]).tolist())
        self.assertNotEqual(kept.tolist(), cap_per_game(games, 5, rng).tolist(), "a random subset, not the first k rows")
        self.assertEqual(list(range(len(games))), cap_per_game(games, 100, rng).tolist())

    def test_pool_holds_every_training_game_of_every_file_capped(self):
        from train_all import build_pools
        from train_phase import is_validation_game
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            files = []
            for i in range(3):
                path = Path(tmp) / f"PICK_PHASE.{i + 1:06d}.ares"
                write_typed_chunk(path, "PICK_PHASE", 6, 40, i + 1, rows_per_game=20)
                files.append(str(path))
            all_games = np.concatenate([np.repeat(np.arange(s * 10_000, s * 10_000 + 40), 20) for s in (1, 2, 3)])
            expected = set(np.unique(all_games[~is_validation_game(all_games, 0.2)]).tolist())
            # one call builds both pools in one pass over the files (the fix for the HDD-bound double read)
            train, val = build_pools(files, 0.2, np.random.default_rng(1), 3)
            obs, cand, outcome, game = train
            self.assertEqual(expected, set(np.unique(game).tolist()))
            self.assertLessEqual(np.bincount(game - game.min()).max(), 3)
            self.assertEqual(len(obs), len(cand))
            self.assertEqual(np.float16, obs.dtype)
            self.assertFalse(set(np.unique(val[3]).tolist()) & expected, "validation games never appear in training")


@unittest.skipUnless(HAVE_TORCH, "torch is not installed")
class TrainAllShared(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.tmp = Path(self._tmp.name)
        self.data = self.tmp / "data"
        self.data.mkdir()
        for i in range(2):
            # Different schema hashes per type (as in real DecisionFile: candidate layout is part of the hash).
            write_typed_chunk(self.data / f"PICK_PHASE.{i + 1:06d}.ares", "PICK_PHASE", 6, 80, i + 1,
                              schema_hash="phasehash00000000")
            write_typed_chunk(self.data / f"MULLIGAN.{i + 1:06d}.ares", "MULLIGAN", 10, 80, i + 10,
                              schema_hash="mulliganhash00000")

    def tearDown(self):
        self._tmp.cleanup()

    def test_shared_trunk_exports_one_folder_per_type(self):
        out = self.tmp / "model"
        cmd = [sys.executable, str(ML / "train_all.py"), "--data", str(self.data), "--out", str(out),
               "--epochs", "2", "--hidden", "16,8", "--batch", "128", "--device", "cpu", "--lr", "3e-3"]
        r = subprocess.run(cmd, capture_output=True, text=True)
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        self.assertTrue((out / "DONE").exists())
        for decision in ("PICK_PHASE", "MULLIGAN"):
            folder = out / decision
            self.assertTrue((folder / "DONE").exists(), decision)
            self.assertTrue((folder / "model.json").exists(), decision)
            meta, params = load_exported(folder)
            self.assertEqual(decision, meta["decisionType"])
            # shared mean must match across types
            self.assertTrue(np.allclose(params["mean"], load_exported(out / "PICK_PHASE")[1]["mean"]))
            logits = numpy_forward(params, meta["clip"], np.zeros(OBS), np.eye(meta["candFeatures"], dtype=np.float64)[:1])
            self.assertEqual(1, len(logits))

    def test_exports_the_epoch_with_the_best_val_loss_and_stops_early(self):
        out = self.tmp / "early"
        cmd = [sys.executable, str(ML / "train_all.py"), "--data", str(self.data), "--out", str(out),
               "--epochs", "12", "--patience", "1", "--hidden", "64,32", "--batch", "16", "--device", "cpu", "--lr", "3e-2"]
        r = subprocess.run(cmd, capture_output=True, text=True)
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        metrics = json.loads((out / "PICK_PHASE" / "train_metrics.json").read_text(encoding="utf-8"))
        history = metrics["history"]
        losses = [h["val_loss"] for h in history]
        self.assertEqual(1 + int(np.argmin(losses)), metrics["best_epoch"])
        self.assertAlmostEqual(min(losses), metrics["best_val_loss"])
        self.assertLessEqual(len(history), metrics["best_epoch"] + 1)
        self.assertIn(f"exporting epoch {metrics['best_epoch']}", r.stdout)

    def test_sequential_feed_ablation_trains_and_is_recorded(self):
        out = self.tmp / "seq"
        cmd = [sys.executable, str(ML / "train_all.py"), "--data", str(self.data), "--out", str(out), "--feed", "sequential",
               "--epochs", "2", "--hidden", "16,8", "--batch", "64", "--device", "cpu"]
        r = subprocess.run(cmd, capture_output=True, text=True)
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        metrics = json.loads((out / "PICK_PHASE" / "train_metrics.json").read_text(encoding="utf-8"))
        self.assertEqual("sequential", metrics["pool"]["feed"])
        self.assertGreater(metrics["pool"]["train_games"], 0)

    def test_independent_mode(self):
        out = self.tmp / "ind"
        cmd = [sys.executable, str(ML / "train_all.py"), "--data", str(self.data), "--out", str(out),
               "--independent", "--epochs", "1", "--hidden", "16,8", "--batch", "128", "--device", "cpu"]
        r = subprocess.run(cmd, capture_output=True, text=True)
        self.assertEqual(0, r.returncode, r.stderr + r.stdout)
        self.assertTrue((out / "PICK_PHASE" / "DONE").exists())
        self.assertTrue((out / "MULLIGAN" / "DONE").exists())
        self.assertTrue((out / "DONE").exists())


class JavaModelArgs(unittest.TestCase):
    def test_prefers_models_bundle(self):
        sys.path.insert(0, str(ML))
        import nightly
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            root = Path(tmp)
            (root / "PICK_PHASE").mkdir()
            (root / "PICK_PHASE" / "model.json").write_text("{}")
            args = nightly.java_model_args(root, 0.5)
            self.assertEqual(["--models", str(root), "--temperature", "0.5"], args)
            single = Path(tmp) / "one"
            single.mkdir()
            (single / "model.json").write_text("{}")
            self.assertEqual(["--model", str(single), "--temperature", "0"], nightly.java_model_args(single, 0))
            missing = nightly.java_model_args("D:/missing", 0.3)
            self.assertEqual(["--models", "--temperature", "0.3"], [missing[0], missing[2], missing[3]])
            self.assertEqual(Path("D:/missing"), Path(missing[1]))


if __name__ == "__main__":
    unittest.main()
