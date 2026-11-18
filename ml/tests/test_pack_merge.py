"""The merged pack must be row-for-row identical to its sources.

A pack keeps candidates and ids as lazy row views, and every earlier attempt to concatenate packs lost that correspondence
silently (labels pointing at the wrong candidates, 18/11). So the merge is only accepted if, for a sample of rows of every
decision type, the observation bytes, the label, the chosen index, the candidate count and the gathered candidate ids are
exactly what the source pack holds.
"""
import json
import os
import shutil
import sys
import tempfile
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import ares_data  # noqa: E402
import pack_merge  # noqa: E402


class MergeTest(unittest.TestCase):

    def test_merged_rows_match_the_sources(self):
        sources = [pack for pack in (self.source_a, self.source_b) if os.path.isdir(pack)]
        if len(sources) < 2:
            self.skipTest("the two small packs are not on this machine")
        out = os.path.join(self.tmp, "merged")
        pack_merge.merge(sources, out)
        manifest = ares_data.open_pack(out)

        for name in manifest["types"]:
            pieces = []
            for source in sources:
                source_manifest = ares_data.open_pack(source)
                if name not in source_manifest["types"] or not source_manifest["types"][name].get("rows"):
                    continue
                pieces.append(ares_data.open_packed_type(source, name, source_manifest))
            if not pieces:
                self.fail(f"{name} is in the merged pack but in none of the sources")
            merged = ares_data.open_packed_type(out, name, manifest)
            self.assertEqual(sum(len(piece["chosen"]) for piece in pieces), len(merged["chosen"]), name)

            # every sampled row of every source must appear unchanged in the merged pack at the same relative position
            start = 0
            for piece in pieces:
                rows = len(piece["chosen"])
                for local in sorted({0, rows // 2, rows - 1}):
                    absolute = start + local
                    np.testing.assert_array_equal(
                        np.asarray(piece["obs"][local]), np.asarray(merged["obs"][absolute]),
                        err_msg=f"{name}: observation of row {absolute}")
                    for field in ("chosen", "count", "margin", "outcome", "game", "step", "seat", "round", "keep"):
                        if field in piece and field in merged:
                            self.assertEqual(np.asarray(piece[field][local]), np.asarray(merged[field][absolute]),
                                             f"{name}: {field} of row {absolute}")
                    if "cands" in piece:
                        ids_local = np.asarray(piece["ids"][local])
                        ids_merged = np.asarray(merged["ids"][absolute])
                        np.testing.assert_array_equal(
                            ids_local[:int(piece["count"][local])],
                            ids_merged[:int(merged["count"][absolute])],
                            err_msg=f"{name}: candidate ids of row {absolute}")
                start += rows

    def test_the_merged_pack_has_one_trailing_zero_row(self):
        sources = [pack for pack in (self.source_a, self.source_b) if os.path.isdir(pack)]
        if len(sources) < 2:
            self.skipTest("the two small packs are not on this machine")
        out = os.path.join(self.tmp, "merged")
        pack_merge.merge(sources, out)
        manifest = ares_data.open_pack(out)
        for name, meta in manifest["types"].items():
            if "candidates" not in meta:
                continue
            features = meta["candidates"]["features"]
            flat = np.memmap(os.path.join(out, f"{name}.cand.f16"), dtype="<f2", mode="r")
            self.assertEqual(0, int(flat.size) % features, f"{name}: the flat candidate block is not whole rows")
            last = np.asarray(flat[-features:])
            self.assertTrue(np.all(last == 0), f"{name}: the last candidate row must be the zero row")

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="pack_merge_")
        self.source_a = "C:/ares/pack/after_smoke"
        self.source_b = "C:/ares/pack/mull_smoke"

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
