"""Tests for the round-grid score arithmetic (`ml/round_scores.py`).

Why these tests exist: two premise measurements of 20/11 decide the direction of the project with numbers produced here - when the
points are created, and whether strong games occupy distinct behaviour cells. A silent off-by-one in "the round-end score" or a wrong
grouping in a descriptor would be read as a fact about the game. The owner's rule: if it is written, it is tested.
"""
import json
import os
import sys
import tempfile
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import round_scores  # noqa: E402


def write_pack(directory, rows, keep=None, type_name="PICK_PHASE"):
    """A minimal pack: one type, the label fields the module reads."""
    os.makedirs(directory, exist_ok=True)
    n = len(rows)
    labels = {
        "outcome": np.array([r["outcome"] for r in rows], dtype=np.float32),
        "margin": np.array([r["margin"] for r in rows], dtype=np.float32),
        "seat": np.array([r["seat"] for r in rows], dtype=np.uint8),
        "context": np.array([r.get("context", 0) for r in rows], dtype=np.uint8),
        "round": np.array([r["round"] for r in rows], dtype=np.uint16),
        "step": np.array([r["step"] for r in rows], dtype=np.uint16),
        "game": np.array([r["game"] for r in rows], dtype=np.uint16),
        "breakdown": np.array([r["breakdown"] for r in rows], dtype=np.float32),
        "keep": np.array(keep if keep is not None else [1] * n, dtype=np.uint8),
    }
    np.savez(os.path.join(directory, f"{type_name}.lab.npz"), **labels)
    manifest = {"obsSize": 8, "observationHash": "test", "types": {type_name: {"rows": n}}}
    json.dump(manifest, open(os.path.join(directory, "manifest.json"), "w", encoding="utf-8"))
    return directory


def row(game, seat, rnd, step, tr=0, forests=0, cards=0, resource=0, milestones=0, awards=0, extra=0,
        margin=0.0, outcome=0.5):
    return {"game": game, "seat": seat, "round": rnd, "step": step,
            "breakdown": [tr, forests, cards, resource, milestones, awards, extra],
            "margin": margin, "outcome": outcome}


class LoadAndKeepTest(unittest.TestCase):

    def test_keep_drops_rows(self):
        rows = [row(0, 0, 0, 0, tr=1), row(0, 0, 0, 1, tr=2), row(0, 0, 1, 0, tr=3)]
        with tempfile.TemporaryDirectory() as tmp:
            pack = write_pack(tmp, rows, keep=[1, 0, 1])
            labels = round_scores.load_labels(pack)
        self.assertEqual(len(labels["game"]), 2)
        self.assertEqual(list(labels["step"]), [0, 0])

    def test_afterstate_is_excluded_by_default(self):
        rows = [row(0, 0, 0, 0, tr=1)]
        with tempfile.TemporaryDirectory() as tmp:
            pack = write_pack(tmp, rows, type_name="AFTERSTATE")
            with self.assertRaises(SystemExit):
                round_scores.load_labels(pack)
            labels = round_scores.load_labels(pack, include_afterstates=True)
        self.assertEqual(len(labels["game"]), 1)


class RoundTableTest(unittest.TestCase):

    def test_last_row_of_each_round(self):
        rows = [row(0, 0, 0, 0, tr=1), row(0, 0, 0, 1, tr=2),
                row(0, 0, 1, 0, tr=3), row(0, 0, 1, 1, tr=5, forests=1)]
        with tempfile.TemporaryDirectory() as tmp:
            table = round_scores.round_table(round_scores.load_labels(write_pack(tmp, rows)))
        self.assertEqual(len(table["round"]), 2)
        self.assertEqual(list(table["step"]), [1, 1])
        self.assertEqual(table["breakdown"][0][0], 2.0)   # end of round 0
        self.assertEqual(table["breakdown"][1][0], 5.0)   # end of round 1
        self.assertEqual(table["breakdown"][1][1], 1.0)

    def test_two_seats_and_two_games_do_not_mix(self):
        rows = [row(0, 0, 0, 0, tr=1), row(0, 1, 0, 0, tr=7),
                row(1, 0, 0, 0, tr=2), row(1, 0, 0, 1, tr=4)]
        with tempfile.TemporaryDirectory() as tmp:
            table = round_scores.round_table(round_scores.load_labels(write_pack(tmp, rows)))
        self.assertEqual(len(table["round"]), 3)
        self.assertEqual(sorted(zip(table["game"].tolist(), table["seat"].tolist(),
                                    table["breakdown"][:, 0].tolist())),
                         [(0, 0, 1.0), (0, 1, 7.0), (1, 0, 4.0)])


class DeltaTest(unittest.TestCase):

    def test_first_round_is_its_level_then_differences(self):
        rows = [row(0, 0, 0, 0, tr=2), row(0, 0, 1, 0, tr=5, forests=1), row(0, 0, 2, 0, tr=6, forests=1)]
        with tempfile.TemporaryDirectory() as tmp:
            labels = round_scores.load_labels(write_pack(tmp, rows))
            table = round_scores.round_table(labels)
            deltas = round_scores.round_deltas(table)
        # round 0: everything it ended with; round 1: +3 TR +1 forest; round 2: +1 TR
        self.assertEqual(deltas[0].tolist(), [2, 0, 0, 0, 0, 0, 0])
        self.assertEqual(deltas[1].tolist(), [3, 1, 0, 0, 0, 0, 0])
        self.assertEqual(deltas[2].tolist(), [1, 0, 0, 0, 0, 0, 0])
        # the deltas must reconstruct the final score exactly - this is the invariant a chart or a percentage relies on
        self.assertEqual(deltas.sum(axis=0).tolist(), table["breakdown"][-1].tolist())

    def test_deltas_do_not_cross_games(self):
        rows = [row(0, 0, 0, 0, tr=2), row(0, 0, 1, 0, tr=4),
                row(1, 0, 0, 0, tr=3), row(1, 0, 1, 0, tr=9)]
        with tempfile.TemporaryDirectory() as tmp:
            table = round_scores.round_table(round_scores.load_labels(write_pack(tmp, rows)))
            deltas = round_scores.round_deltas(table)
        self.assertEqual(deltas[:, 0].tolist(), [2.0, 2.0, 3.0, 6.0])

    def test_final_score_takes_the_last_round(self):
        rows = [row(0, 0, 0, 0, tr=2), row(0, 0, 1, 0, tr=4), row(0, 1, 0, 0, tr=1)]
        with tempfile.TemporaryDirectory() as tmp:
            table = round_scores.round_table(round_scores.load_labels(write_pack(tmp, rows)))
        final = round_scores.final_score(table)
        self.assertEqual(sorted(final[:, 0].tolist()), [1.0, 4.0])


class TailShareTest(unittest.TestCase):

    def test_share_of_the_last_rounds(self):
        # rounds: 2 points, then 4, then 4 -> the last two rounds carry 8 of 10
        rnd = np.array([0, 1, 2, 2])
        per = np.zeros((4, 7), dtype=np.float32)
        per[0, 0] = 2
        per[1, 0] = 4
        per[2, 0] = 2
        per[3, 0] = 2
        self.assertAlmostEqual(round_scores._tail_share(rnd, per, 2), 0.8)
        self.assertAlmostEqual(round_scores._tail_share(rnd, per, 1), 0.4)  # round 2 = two rows of 2, summed
        self.assertAlmostEqual(round_scores._tail_share(rnd, per, 3), 1.0)

    def test_empty_or_zero_score_is_nan_not_zero(self):
        rnd = np.array([0, 1])
        per = np.zeros((2, 7), dtype=np.float32)
        self.assertTrue(np.isnan(round_scores._tail_share(rnd, per, 1)))
        self.assertTrue(np.isnan(round_scores.share_created_in_last(np.zeros(3), 1)))


class DescriptorTest(unittest.TestCase):

    def test_shares_sum_to_one(self):
        b = np.array([3, 1, 2, 2, 1, 1, 0], dtype=np.float32)
        shares = round_scores.descriptor_shares(b)
        self.assertAlmostEqual(float(shares.sum()), 1.0, places=6)
        self.assertAlmostEqual(float(shares[0]), 0.4)   # tr + forests = 4 of 10
        self.assertAlmostEqual(float(shares[1]), 0.4)   # cards + resource = 4 of 10

    def test_zero_score_gives_zeros_not_nan(self):
        shares = round_scores.descriptor_shares(np.zeros(7, dtype=np.float32))
        self.assertEqual(shares.tolist(), [0.0, 0.0, 0.0])

    def test_cell_index_bins_and_rejects_nan(self):
        self.assertEqual(round_scores.cell_index([0.0, 0.0, 1.0], bins=4), 0)
        self.assertEqual(round_scores.cell_index([0.99, 0.0, 0.01], bins=4), 12)
        self.assertEqual(round_scores.cell_index([0.1, 0.9, 0.0], bins=4), 0 * 4 + 3)
        self.assertEqual(round_scores.cell_index([float("nan"), 0.5, 0.5], bins=4), -1)


if __name__ == "__main__":
    unittest.main()


class CellSummaryTest(unittest.TestCase):

    def test_occupancy_counts_and_means(self):
        shares = np.array([[0.0, 0.0, 1.0], [0.1, 0.0, 0.9], [0.9, 0.0, 0.1]])
        scores = np.array([10.0, 20.0, 30.0])
        margins = np.array([-5.0, 5.0, 15.0])
        rows = round_scores.summarise_cells(shares, scores, margins, bins=4)
        self.assertEqual(len(rows), 2)
        big = next(r for r in rows if r["n"] == 2)
        self.assertAlmostEqual(big["score"], 15.0)
        self.assertAlmostEqual(big["margin"], 0.0)

    def test_top_cell_share_counts_the_strongest(self):
        # 75th percentile of [100,99,50,49,48,47] is 50.25, so the strong seats are 100 and 99 - in different cells
        cells = np.array([0, 1, 0, 0, 0, 0])
        scores = np.array([100, 99, 50, 49, 48, 47])
        share, strong = round_scores.top_cell_share(scores, cells, percentile=75)
        self.assertEqual(strong, 2)
        self.assertAlmostEqual(share, 0.5)

    def test_unknown_cells_are_ignored(self):
        share, strong = round_scores.top_cell_share(np.array([1.0, 2.0]), np.array([-1, -1]), percentile=50)
        self.assertTrue(np.isnan(share))
        self.assertEqual(strong, 0)


class PointsPerRoundTest(unittest.TestCase):

    def test_points_are_differences_and_the_last_is_the_settlement(self):
        grid = {
            "game": np.array([0, 0]), "seat": np.array([0, 0]), "round": np.array([0, 1]),
            "step": np.array([0, 0]), "live_me": np.array([10.0, 30.0]),
            "final": np.array([[0, 0, 0, 0, 0, 0, 45.0], [0, 0, 0, 0, 0, 0, 45.0]], dtype=np.float32),
        }
        seats = round_scores.points_per_round(grid)
        self.assertEqual(len(seats), 1)
        self.assertEqual(seats[0]["points"].tolist(), [20.0, 15.0])
        self.assertEqual(seats[0]["settlement"], 15.0)
        self.assertEqual(seats[0]["final"], 45.0)

    def test_single_round_settles_everything(self):
        grid = {
            "game": np.array([0]), "seat": np.array([1]), "round": np.array([3]),
            "step": np.array([0]), "live_me": np.array([7.0]),
            "final": np.array([[0, 0, 0, 0, 0, 0, 20.0]], dtype=np.float32),
        }
        seats = round_scores.points_per_round(grid)
        self.assertEqual(len(seats), 1)
        self.assertEqual(seats[0]["points"].tolist(), [13.0])

    def test_two_seats_of_one_game_do_not_mix(self):
        grid = {
            "game": np.array([0, 0, 0, 0]), "seat": np.array([0, 0, 1, 1]),
            "round": np.array([0, 1, 0, 1]), "step": np.array([0, 0, 0, 0]),
            "live_me": np.array([5.0, 9.0, 3.0, 4.0]),
            "final": np.array([[0, 0, 0, 0, 0, 0, 12.0]] * 2
                              + [[0, 0, 0, 0, 0, 0, 6.0]] * 2, dtype=np.float32),
        }
        seats = round_scores.points_per_round(grid)
        self.assertEqual(len(seats), 2)
        by_seat = {s["seat"]: s for s in seats}
        self.assertEqual(by_seat[0]["points"].tolist(), [4.0, 3.0])
        self.assertEqual(by_seat[1]["points"].tolist(), [1.0, 2.0])


class LoadRoundGridTest(unittest.TestCase):

    def test_reads_the_two_score_columns_with_keep(self):
        rows, obs_size = 3, 3
        with tempfile.TemporaryDirectory() as tmp:
            obs = np.arange(rows * obs_size, dtype=np.float16).reshape(rows, obs_size)
            obs.tofile(os.path.join(tmp, "PICK_PHASE.obs.f16"))
            labels = {
                "outcome": np.array([0.0, 1.0, 0.5], dtype=np.float32),
                "margin": np.array([-1.0, 2.0, 0.0], dtype=np.float32),
                "seat": np.array([0, 0, 1], dtype=np.uint8),
                "context": np.array([0, 0, 0], dtype=np.uint8),
                "round": np.array([0, 1, 0], dtype=np.uint16),
                "step": np.array([0, 0, 0], dtype=np.uint16),
                "game": np.array([0, 0, 0], dtype=np.uint16),
                "breakdown": np.zeros((rows, 7), dtype=np.float32),
                "keep": np.array([1, 0, 1], dtype=np.uint8),
            }
            np.savez(os.path.join(tmp, "PICK_PHASE.lab.npz"), **labels)
            json.dump({"obsSize": obs_size, "observationHash": "t",
                       "types": {"PICK_PHASE": {"rows": rows}}},
                      open(os.path.join(tmp, "manifest.json"), "w", encoding="utf-8"))
            names = ["noise", "score:my_winning_points", "score:opponent_winning_points"]
            grid = round_scores.load_round_grid(tmp, names)
        self.assertEqual(len(grid["game"]), 2)          # keep dropped row 1
        self.assertEqual(grid["live_me"].tolist(), [1.0, 7.0])
        self.assertEqual(grid["live_op"].tolist(), [2.0, 8.0])
