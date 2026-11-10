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
              "obsDtype": "f2", "hash": schema_hash, "observationLayout": "synthetic"}
    dtype = record_dtype(schema)
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
        f.write(b"ARES" + struct.pack("<HI", 2, len(header)) + header + rows.tobytes())


class Discover(unittest.TestCase):
    def test_discovers_prefixes(self):
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            root = Path(tmp)
            write_typed_chunk(root / "PICK_PHASE.000001.ares", "PICK_PHASE", 6, 2, 1)
            write_typed_chunk(root / "BUILD_CARD.000001.ares", "BUILD_CARD", 8, 2, 2)
            (root / "BUILD_CARD.000002.ares.tmp").write_bytes(b"nope")
            self.assertEqual(["BUILD_CARD", "PICK_PHASE"], discover_decisions(str(root)))


@unittest.skipUnless(HAVE_TORCH, "torch is not installed")
class TrainAllShared(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.tmp = Path(self._tmp.name)
        self.data = self.tmp / "data"
        self.data.mkdir()
        for i in range(2):
            write_typed_chunk(self.data / f"PICK_PHASE.{i + 1:06d}.ares", "PICK_PHASE", 6, 80, i + 1)
            write_typed_chunk(self.data / f"MULLIGAN.{i + 1:06d}.ares", "MULLIGAN", 10, 80, i + 10)

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
