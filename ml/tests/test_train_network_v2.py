"""Tests for `ml/train_network_v2.py` and its torch twin.

Why: two bugs here cost hours of generation and training. The game id was parsed as float16, which rounds every id above 2048 (50 000 games became
6 682, the train/validation split leaked and the model played at 4.85% against RANDOM), and the label was read from the wrong column when the id column
was present. Both are silent - the losses stay finite - so they are pinned here instead of being caught by a failed run.
"""
import os
import struct
import sys
import tempfile
import unittest
import zipfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import train_network_v2 as t  # noqa: E402


def line_unlabelled(label, features):
    return ",".join([repr(float(label))] + [repr(float(v)) for v in features])


def line_with_id(game_id, label, features):
    return ",".join([str(game_id), repr(float(label))] + [repr(float(v)) for v in features])


class ReadCsvTest(unittest.TestCase):

    def read(self, lines):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "netdata.csv")
            with open(path, "w", encoding="utf-8") as f:
                f.write("\n".join(lines) + "\n")
            return t.read_csv(path)

    def test_game_id_above_2048_is_exact(self):
        # 5001 is NOT representable in float16 (it rounds to 5000); the id must survive exactly.
        features = [0.0] * (t.TABLE + t.HAND)
        label, x, ids = self.read([line_with_id(5001, 1.0, features)])
        self.assertEqual(ids[0], 5001)
        self.assertAlmostEqual(float(label[0]), 1.0)

    def test_old_format_without_id(self):
        features = [0.5] * (t.TABLE + t.HAND)
        label, x, ids = self.read([line_unlabelled(0.0, features)])
        self.assertEqual(ids[0], -1)
        self.assertAlmostEqual(float(label[0]), 0.0)
        self.assertAlmostEqual(float(x[0, 0]), 0.5)

    def test_label_outside_unit_interval_is_refused(self):
        features = [0.0] * (t.TABLE + t.HAND)
        with self.assertRaises(ValueError):
            self.read([line_with_id(1, 2.0, features)])

    def test_wrong_field_count_is_refused(self):
        with self.assertRaises(ValueError):
            self.read(["1,1.0,0.5,0.5"])


class BinaryTest(unittest.TestCase):
    """The binary corpus is the default now (tens of millions of states do not fit in text), so its layout is pinned.

    The CSV float16 id bug is the reason the id is int32 here, and the reason this test uses an id above 2048.
    """

    def write(self, tmp, records, dimension=t.TABLE + t.HAND, truncate=0):
        path = os.path.join(tmp, "netdata.bin")
        with open(path, "wb") as f:
            f.write(t.MAGIC)
            f.write(struct.pack("<H", dimension))
            for game_id, label, values in records:
                f.write(struct.pack("<i", game_id))
                f.write(struct.pack("<e", label))
                f.write(struct.pack(f"<{len(values)}e", *values))
        if truncate:
            with open(path, "r+b") as f:
                f.truncate(os.path.getsize(path) - truncate)
        return path

    def test_round_trip_keeps_the_game_id_above_2048_and_the_labels(self):
        features = [0.0] * (t.TABLE + t.HAND)
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [(5001, 1.0, features), (5001, 0.0, features), (2, 0.5, features)])
            label, x, ids = t.read_dataset(path, in_ram=True)
        self.assertEqual(ids.tolist(), [5001, 5001, 2])
        self.assertEqual(label.tolist(), [1.0, 0.0, 0.5])
        self.assertEqual(x.shape, (3, t.TABLE + t.HAND))

    def test_the_memmap_path_reads_the_same_rows_and_releases_the_file(self):
        # The default path is a memmap (a 40k-game corpus is 30 GB). On Windows an open map blocks the directory cleanup,
        # so the test releases it explicitly - that is also what a caller that re-reads a corpus must do.
        import gc
        features = [0.25] * (t.TABLE + t.HAND)
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [(5001, 1.0, features)])
            label, x, ids = t.read_dataset(path)
            self.assertEqual(ids.tolist(), [5001])
            self.assertAlmostEqual(float(x[0, 0]), 0.25, places=3)
            del label, x, ids
            gc.collect()

    def test_a_truncated_file_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [(1, 1.0, [0.0] * (t.TABLE + t.HAND))], truncate=100)
            with self.assertRaises(ValueError):
                t.read_dataset(path)

    def test_a_wrong_dimension_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [], dimension=123)
            with self.assertRaises(ValueError):
                t.read_dataset(path)

    def test_a_csv_is_still_detected_by_content(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "netdata.csv")
            with open(path, "w", encoding="utf-8") as f:
                f.write(line_with_id(3, 1.0, [0.0] * (t.TABLE + t.HAND)) + "\n")
            label, x, ids = t.read_dataset(path)
        self.assertEqual(ids.tolist(), [3])


class WarmStartTest(unittest.TestCase):

    def test_an_exported_model_loads_back_into_an_identical_forward(self):
        # The incremental recipe depends on this: train on one chunk, then continue from the exported file on the next.
        import torch
        import train_network_v2_torch as tt

        model = tt.Towers()
        torch.manual_seed(11)
        for parameter in model.parameters():
            with torch.no_grad():
                parameter.copy_(torch.randn_like(parameter) * 0.02)
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "model.zip")
            tt.export(model, path)
            state = tt.load_exported(path)
        resumed = tt.Towers()
        resumed.load_state_dict(state)
        rows = torch.randn(8, t.TABLE + t.HAND)
        with torch.no_grad():
            before = model(rows[:, :t.TABLE], rows[:, t.TABLE:])
            after = resumed(rows[:, :t.TABLE], rows[:, t.TABLE:])
        np.testing.assert_allclose(before.numpy(), after.numpy(), rtol=1e-6, atol=1e-6)


class ExportTest(unittest.TestCase):

    def test_the_export_round_trips_the_layout_the_loader_reads(self):
        import torch  # noqa: F401
        import train_network_v2_torch as tt

        model = tt.Towers()
        torch.manual_seed(7)
        for parameter in model.parameters():
            with torch.no_grad():
                parameter.copy_(torch.randn_like(parameter) * 0.01)
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "model.zip")
            tt.export(model, path)
            params = tt.read_params(path)
        self.assertEqual(params.size, 1511425)
        at = 0
        w1 = params[at:at + t.H1 * t.TABLE].reshape(t.H1, t.TABLE)
        at += t.H1 * t.TABLE
        b1 = params[at:at + t.H1]
        at += t.H1
        w2 = params[at:at + t.H2 * t.HAND].reshape(t.H2, t.HAND)
        at += t.H2 * t.HAND
        b2 = params[at:at + t.H2]
        at += t.H2
        w3 = params[at:at + t.H3 * (t.H1 + t.H2)].reshape(t.H3, t.H1 + t.H2)
        at += t.H3 * (t.H1 + t.H2)
        b3 = params[at:at + t.H3]
        at += t.H3
        w4 = params[at:at + t.H3]
        at += t.H3
        b4 = params[at]
        # The file stores out-major (out * in + in): the Java loader transposes it. The torch model already stores (out, in).
        np.testing.assert_allclose(w1, model.table.weight.detach().numpy(), rtol=1e-5, atol=1e-6)
        np.testing.assert_allclose(b1, model.table.bias.detach().numpy(), rtol=1e-5, atol=1e-6)
        np.testing.assert_allclose(w2, model.hand.weight.detach().numpy(), rtol=1e-5, atol=1e-6)
        np.testing.assert_allclose(w3, model.merge.weight.detach().numpy(), rtol=1e-5, atol=1e-6)
        np.testing.assert_allclose(w4, model.out.weight.detach().numpy().reshape(-1), rtol=1e-5, atol=1e-6)
        self.assertAlmostEqual(float(b4), float(model.out.bias.detach().numpy()[0]), places=6)


if __name__ == "__main__":
    unittest.main()
