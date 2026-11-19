"""Counting finished chunks: one type's files, not every *.ares.

Why: `glob("*.ares")` counts the TYPE files of one chunk (16), so on 19/11 at 22:03 the loop read 16 chunks after the first one,
computed 4 000 games instead of 250, decided the corpus was complete after 1 250 games, skipped the training and ended the night
with one log line. The corpus (and the night's budget) was lost to an arithmetic mistake, not to a modelling one.
"""
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402

TYPES = ["BUILD_CARD", "PICK_PHASE", "MULLIGAN", "BLUFF"]


class ChunksOnDiskTest(unittest.TestCase):

    def test_one_type_per_chunk_is_the_chunk_count(self):
        with tempfile.TemporaryDirectory() as directory:
            for chunk in (1, 2, 3):
                for kind in TYPES:
                    path = os.path.join(directory, f"{kind}.{chunk:06d}.ares")
                    with open(path, "wb") as handle:
                        handle.write(b"")
            self.assertEqual(night_loop.chunks_on_disk(directory), 3, "three chunks, not twelve files")

    def test_an_unfinished_chunk_is_not_counted(self):
        with tempfile.TemporaryDirectory() as directory:
            for kind in TYPES:
                with open(os.path.join(directory, f"{kind}.000001.ares"), "wb") as handle:
                    handle.write(b"")
                with open(os.path.join(directory, f"{kind}.000002.ares.tmp"), "wb") as handle:
                    handle.write(b"")
            self.assertEqual(night_loop.chunks_on_disk(directory), 1)

    def test_an_empty_directory_is_zero(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(night_loop.chunks_on_disk(directory), 0)


if __name__ == "__main__":
    unittest.main()
