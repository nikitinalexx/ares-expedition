"""Tests for the decision-audit reader (`ml/judge_report.py`).

Why: the report produces the first direct number on whether our value ranks the phase decision the way the game's own outcome does. An error here (a
mis-parsed `na`, an argmax that counts a missing candidate as zero, an agreement counted per world instead of per decision) would turn a measurement
into a slogan.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import judge_report  # noqa: E402

LINE = ("[JUDGE] type=PICK_PHASE turns=5 cands=4 worlds=2 w0=1.0,2.0,na,4.0 w1=2.0,3.0,na,5.0 "
        "mean=1.5,-1.0,na,4.5 v=0.10,0.30,0.20,0.05")


class ReadLinesTest(unittest.TestCase):

    def test_reads_utf16_and_utf8(self):
        import tempfile
        for encoding in ("utf-16", "utf-8"):
            with tempfile.NamedTemporaryFile("w", suffix=".log", delete=False, encoding=encoding) as handle:
                handle.write(LINE + "\n")
                path = handle.name
            try:
                lines = judge_report.read_lines(path)
                self.assertEqual(len(lines), 1, encoding)
                self.assertIsNotNone(judge_report.parse_line(lines[0]), encoding)
            finally:
                os.unlink(path)


class ParseTest(unittest.TestCase):

    def test_values_with_na(self):
        self.assertEqual(judge_report.parse_values("1.0,na,2.5"), [1.0, None, 2.5])

    def test_parse_line(self):
        row = judge_report.parse_line(LINE)
        self.assertEqual(row["type"], "PICK_PHASE")
        self.assertEqual(row["mean"], [1.5, -1.0, None, 4.5])
        self.assertEqual(row["v"], [0.10, 0.30, 0.20, 0.05])

    def test_line_without_judge_is_ignored(self):
        self.assertIsNone(judge_report.parse_line("[SEARCH] 100 decisions"))


class ArgmaxTest(unittest.TestCase):

    def test_ignores_missing(self):
        self.assertEqual(judge_report.argmax([1.0, None, 3.0]), 2)

    def test_all_missing(self):
        self.assertEqual(judge_report.argmax([None, None]), -1)

    def test_negative_values(self):
        self.assertEqual(judge_report.argmax([-5.0, -2.0]), 1)


class SummariseTest(unittest.TestCase):

    def test_agreement_and_advantage(self):
        # value prefers index 1 (mean -1.0); the judge prefers index 3 (4.5) -> disagrees, advantage 5.5
        rows = [judge_report.parse_line(LINE)]
        stats = judge_report.summarise(rows)
        self.assertEqual(stats["decisions"], 1)
        self.assertAlmostEqual(stats["agreement"], 0.0)
        self.assertAlmostEqual(stats["mean_advantage"], 5.5)

    def test_agreement_when_both_pick_the_same(self):
        row = {"mean": [1.0, 5.0], "v": [0.1, 0.9]}
        stats = judge_report.summarise([row])
        self.assertAlmostEqual(stats["agreement"], 1.0)
        self.assertAlmostEqual(stats["mean_advantage"], 0.0)

    def test_empty(self):
        self.assertEqual(judge_report.summarise([])["decisions"], 0)


if __name__ == "__main__":
    unittest.main()


class DecomposeTest(unittest.TestCase):

    def test_world_and_position_spread(self):
        # two decisions; the first's worlds are 10, 20, the second's 0, 10 -> world sd 5, position sd 5
        rows = [
            {"v": [1.0, 0.0], "mean": [0.0, 0.0], "worlds": [[10.0, 0.0], [20.0, 0.0]]},
            {"v": [1.0, 0.0], "mean": [0.0, 0.0], "worlds": [[0.0, 0.0], [10.0, 0.0]]},
        ]
        stats = judge_report.decompose(rows, split=1)
        self.assertEqual(stats["decisions"], 2)
        self.assertAlmostEqual(stats["world_sd"], 7.0710678, places=4)
        self.assertAlmostEqual(stats["position_sd"], 7.0710678, places=4)

    def test_empty(self):
        self.assertEqual(judge_report.decompose([])["decisions"], 0)
