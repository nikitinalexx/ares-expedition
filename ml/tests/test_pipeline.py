"""Tests of the pipeline that decides what to do next: the census, the phase mix, the pack, the report.

Why: every instrument here was written in a hurry to answer a question, and two of them were wrong in ways that changed the
conclusions (phase ids were counted 0..4 while the data uses 1..5; the coverage metric normalised card menus by the wrong
quantity). A wrong instrument is worse than no instrument, because it produces confident nonsense. These tests pin the
contract of each one on synthetic data with a known answer.
"""
import io
import json
import os
import struct
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
from _tmp import temp_dir  # noqa: E402

import numpy as np  # noqa: E402

ML = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ML))

import action_coverage  # noqa: E402
import ares_data  # noqa: E402
import pack_dataset  # noqa: E402
import phase_mix  # noqa: E402
import regret_report  # noqa: E402

OBS = 8
FEATURES = 3


def write_chunk(path, decision, ids_by_row, chosen_by_row, obs_seed=1):
    """One synthetic chunk: the ids each row was offered and the index it chose."""
    candidate_count = max(len(row) for row in ids_by_row)
    schema = {"type": decision, "obsSize": OBS, "maxCandidates": candidate_count,
              "candidateFeatures": FEATURES, "obsDtype": "f2", "candDtype": "f2", "hash": "synthetic",
              "observationLayout": "synthetic"}
    dtype = ares_data.record_dtype(schema, 4)
    rows = np.zeros(len(ids_by_row), dtype=dtype)
    rng = np.random.default_rng(obs_seed)
    rows["obs"] = rng.random((len(ids_by_row), OBS)).astype(np.float16)
    for i, (ids, chosen) in enumerate(zip(ids_by_row, chosen_by_row)):
        rows["count"][i] = len(ids)
        rows["ids"][i, :len(ids)] = ids
        rows["chosen"][i] = chosen
        rows["game"][i] = i
        rows["outcome"][i] = 1.0
        rows["margin"][i] = 4.0
    header = json.dumps(schema).encode()
    with open(path, "wb") as f:
        f.write(b"ARES" + struct.pack("<HI", 4, len(header)) + header + rows.tobytes())


def corpus(root, decision, ids_by_row, chosen_by_row):
    root.mkdir(parents=True, exist_ok=True)
    write_chunk(root / f"{decision}.000001.ares", decision, ids_by_row, chosen_by_row)
    return root


class PhaseMix(unittest.TestCase):
    def test_ids_are_read_from_the_data_not_assumed_to_start_at_zero(self):
        # The bug this pins: the first version looped over range(5) while the ids are 1..5, so phase 5 was never counted
        # and every column was shifted by one - it reported 0% on phase 5 when the truth was 20%.
        rows, chosen = [], []
        for phase in (1, 2, 3, 4, 5):
            for _ in range(20):
                rows.append([1, 2, 3, 4, 5])
                chosen.append(phase - 1)
        with temp_dir() as tmp:
            root = corpus(Path(tmp) / "raw", "PICK_PHASE", rows, chosen)
            pack_dataset.pack(str(root), str(Path(tmp) / "pack"), candidates=True, verbose=False)
            mix = phase_mix.mix_from_pack(str(Path(tmp) / "pack"))
        values, counts = np.unique(mix, return_counts=True)
        self.assertEqual([1, 2, 3, 4, 5], values.tolist())
        self.assertEqual([20, 20, 20, 20, 20], counts.tolist())


class Coverage(unittest.TestCase):
    def test_a_type_that_always_takes_one_slot_is_flagged_narrow(self):
        rows = [[10, 11, 12, 13, 14] for _ in range(50)]
        chosen = [0] * 50  # always the first slot of a five-way menu
        with temp_dir() as tmp:
            root = corpus(Path(tmp) / "raw", "BUILD_CARD", rows, chosen)
            pack_dataset.pack(str(root), str(Path(tmp) / "pack"), candidates=True, verbose=False)
            report = action_coverage.coverage(str(Path(tmp) / "pack"))
            found = action_coverage.flags(report, {})
        self.assertEqual(1, len(found))
        self.assertEqual("BUILD_CARD", found[0]["decision"])
        self.assertEqual("narrow", found[0]["kind"])

    def test_a_rare_option_in_a_small_stable_menu_is_flagged_thin(self):
        rows, chosen = [], []
        for index in range(100):
            rows.append([1, 2, 3, 4, 5])
            # four phases in normal use, phase 5 exactly once: the type is not narrow, but one option is starving
            chosen.append(index % 4 if index else 4)
        with temp_dir() as tmp:
            root = corpus(Path(tmp) / "raw", "PICK_PHASE", rows, chosen)
            pack_dataset.pack(str(root), str(Path(tmp) / "pack"), candidates=True, verbose=False)
            report = action_coverage.coverage(str(Path(tmp) / "pack"))
            found = action_coverage.flags(report, {})
        kinds = {item["kind"] for item in found}
        self.assertIn("thin", kinds, f"got {found}")
        thin = next(item for item in found if item["kind"] == "thin")
        self.assertEqual([5], thin["thin"])

    def test_an_option_that_disappears_is_flagged_against_the_previous_corpus(self):
        before_rows = [[1, 2, 3, 4, 5] for _ in range(100)]
        before_chosen = [0] * 50 + [4] * 50            # phases 1 and 5 used equally
        after_rows = [[1, 2, 3, 4, 5] for _ in range(100)]
        after_chosen = [0] * 100                        # phase 5 is gone
        with temp_dir() as tmp:
            before = corpus(Path(tmp) / "before", "PICK_PHASE", before_rows, before_chosen)
            after = corpus(Path(tmp) / "after", "PICK_PHASE", after_rows, after_chosen)
            pack_dataset.pack(str(before), str(Path(tmp) / "pack_before"), candidates=True, verbose=False)
            pack_dataset.pack(str(after), str(Path(tmp) / "pack_after"), candidates=True, verbose=False)
            previous = action_coverage.coverage(str(Path(tmp) / "pack_before"))
            current = action_coverage.coverage(str(Path(tmp) / "pack_after"))
            found = action_coverage.flags(current, previous)
        gone = [item for item in found if item["kind"] == "shrunk"]
        self.assertTrue(gone, f"expected a shrunk flag, got {found}")
        self.assertIn(5, gone[0]["gone"] or gone[0]["lost"])

    def test_an_allowlisted_type_is_not_flagged(self):
        rows = [[10, 11, 12] for _ in range(50)]
        chosen = [0] * 50
        with temp_dir() as tmp:
            allow = Path(tmp) / "allow.json"
            allow.write_text(json.dumps({"SELL_CARDS": "the game sells automatically"}), encoding="utf-8")
            root = corpus(Path(tmp) / "raw", "SELL_CARDS", rows, chosen)
            pack_dataset.pack(str(root), str(Path(tmp) / "pack"), candidates=True, verbose=False)
            with mock.patch.object(action_coverage, "ALLOWLIST", str(allow)):
                report = action_coverage.coverage(str(Path(tmp) / "pack"))
                found = action_coverage.flags(report, {})
                benign = action_coverage.allowlisted()
        self.assertEqual([], found)
        self.assertIn("SELL_CARDS", benign)


class PackRoundtrip(unittest.TestCase):
    def test_the_pack_reproduces_the_raw_rows(self):
        rows = [[7, 8, 9], [7, 8, 9], [7, 8, 9]]
        chosen = [0, 2, 1]
        with temp_dir() as tmp:
            root = corpus(Path(tmp) / "raw", "BUILD_CARD", rows, chosen)
            pack_dataset.pack(str(root), str(Path(tmp) / "pack"), candidates=True, verbose=False)
            manifest = ares_data.open_pack(str(Path(tmp) / "pack"))
            packed = ares_data.open_packed_type(str(Path(tmp) / "pack"), "BUILD_CARD", manifest)
            raw = ares_data.read_decisions(root / "BUILD_CARD.000001.ares")[1]
            np.testing.assert_array_equal(np.asarray(raw["chosen"]), np.asarray(packed["chosen"]))
            ids = packed["cands"].gather_ids(np.arange(len(chosen)))
            for index in range(len(chosen)):
                np.testing.assert_array_equal(np.asarray(raw["ids"][index][:3]), ids[index][:3])
            np.testing.assert_array_equal(np.asarray(raw["obs"], dtype=np.float16),
                                          np.asarray(packed["obs"][np.arange(len(chosen))], dtype=np.float16))


class RegretTable(unittest.TestCase):
    def test_over_and_under_use_are_signed_against_the_best_by_rollout(self):
        report = {"type": "PICK_PHASE", "positions": 4, "policyMargin": 0.0, "bestMargin": 1.0,
                  "meanDifference": 1.0, "sd": 2.0, "agree": 2,
                  "samples": [{"policyId": 4, "bestId": 1, "difference": 2.0},
                              {"policyId": 4, "bestId": 1, "difference": 2.0},
                              {"policyId": 4, "bestId": 5, "difference": 1.0},
                              {"policyId": 1, "bestId": 1, "difference": 0.0}]}
        buffer = io.StringIO()
        with redirect_stdout(buffer):
            regret_report.table(report)
        text = buffer.getvalue()
        # sampled: chosen 4,4,4,1; best 1,1,5,1 -> option 4 chosen 75% and never best (+75.0),
        # option 1 chosen 25% and best 75% (-50.0), option 5 chosen 0% and best 25% (-25.0)
        self.assertIn("+75.0%", text)
        self.assertIn("-50.0%", text)
        self.assertIn("-25.0%", text)


class LoopHelpers(unittest.TestCase):
    def test_search_args_carry_the_types_and_the_phase_model(self):
        import night_loop
        args = night_loop.search_args("BUILD_CARD,PICK_PHASE", "a,b", "a")
        self.assertIn("-Dares.agent.search.types=BUILD_CARD,PICK_PHASE", args)
        self.assertIn("-Dares.agent.valueModel=a,b", args)
        self.assertIn("-Dares.agent.valueModel.phase=a", args)
        self.assertEqual(1, sum(1 for a in args if a.startswith("-Dares.agent.search.types=")))

    def test_number_in_reads_the_first_value_of_a_key(self):
        import night_loop
        with temp_dir() as tmp:
            path = Path(tmp) / "out.log"
            path.write_text('noise\n{"builds_per_game": 12.5, "builds_per_game_extra": 99}\n', encoding="utf-8")
            self.assertEqual(12.5, night_loop.number_in(str(path), "builds_per_game"))

    def test_phases_of_parses_the_profile_block(self):
        import night_loop
        with temp_dir() as tmp:
            path = Path(tmp) / "profile.log"
            path.write_text('{"phases": {"1 build green": 0.4, "5 draft": 0.1}}\n', encoding="utf-8")
            self.assertEqual({"1 build green": 0.4, "5 draft": 0.1}, night_loop.phases_of(str(path)))


if __name__ == "__main__":
    unittest.main()
