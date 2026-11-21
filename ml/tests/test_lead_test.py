"""Tests for `ml/lead_test.py`.

Why: the report claims "an early lead predicts LOSING to NETWORK". That claim is only true if the winner is the ENGINE's and the breakdown is read at the
agent's seat. Both are easy to get wrong in opposite directions: the ids are unsigned in the corpus and signed in the ranking file, and the agent plays seat 0
in fewer than half the games. A silent mix-up would flatten every channel and turn the lead question into noise - which is exactly what the first, wrong run
showed.
"""
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import lead_test  # noqa: E402


def breakdown(tr, forests, cards, resources, milestones, awards, extra=0):
    return [tr, forests, cards, resources, milestones, awards, extra]


class KeyTest(unittest.TestCase):

    def test_signed_and_unsigned_agree(self):
        self.assertEqual(lead_test.game_key(-2056914255290040320), lead_test.game_key(16389829818419511296))

    def test_positive_unchanged(self):
        self.assertEqual(lead_test.game_key(42), 42)


class RankingTest(unittest.TestCase):

    def test_decision_lines_are_ignored(self):
        lines = [
            {"game": 7, "breakdown": [breakdown(20, 1, 2, 3, 0, 0), breakdown(21, 0, 0, 20, 0, 0)]},
            {"game": 7, "step": 6, "round": 1, "type": "BUILD_CARD", "chosen": 1},
        ]
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "RANKING.000001.jsonl")
            with open(path, "w", encoding="utf-8") as f:
                for line in lines:
                    f.write(json.dumps(line) + "\n")
            parsed = lead_test.read_breakdowns(path)
        self.assertEqual(list(parsed), [7])
        self.assertEqual(parsed[7][0], breakdown(20, 1, 2, 3, 0, 0))

    def test_every_chunk_is_read(self):
        # a 1 000-game corpus writes RANKING.00000N.jsonl per chunk; reading only the first measured 250 of 1 000
        with tempfile.TemporaryDirectory() as tmp:
            for chunk in (1, 2):
                with open(os.path.join(tmp, f"RANKING.{chunk:06d}.jsonl"), "w", encoding="utf-8") as f:
                    f.write(json.dumps({"game": chunk, "breakdown": [breakdown(1, 0, 0, 0, 0, 0), breakdown(0, 0, 0, 0, 0, 0)]}) + "\n")
            parsed = lead_test.read_breakdowns(tmp)
        self.assertEqual(sorted(parsed), [1, 2])


class OrientationTest(unittest.TestCase):

    def test_breakdown_is_read_at_the_agents_seat(self):
        # the agent was seat 1, so the SECOND row is ours
        parsed = {5: [breakdown(10, 0, 0, 0, 0, 0), breakdown(30, 1, 2, 3, 0, 0)]}
        seats = {5: 1}
        ours, theirs = lead_test.results(parsed, seats)[5]
        self.assertEqual(sum(ours), 36)
        self.assertEqual(sum(theirs), 10)

    def test_games_without_a_recorded_seat_are_dropped(self):
        parsed = {5: [breakdown(1, 0, 0, 0, 0, 0), breakdown(2, 0, 0, 0, 0, 0)]}
        self.assertEqual(lead_test.results(parsed, {}), {})

    def test_tie_is_not_a_win(self):
        self.assertFalse(lead_test.won((breakdown(10, 0, 0, 0, 0, 0), breakdown(10, 0, 0, 0, 0, 0))))
        self.assertTrue(lead_test.won((breakdown(11, 0, 0, 0, 0, 0), breakdown(10, 0, 0, 0, 0, 0))))


class LeadTableTest(unittest.TestCase):

    def test_win_rate_conditional_on_being_ahead(self):
        per_round = {
            (0, 0, 5): (10.0, 4.0),   # ahead, wins below
            (1, 0, 5): (10.0, 4.0),   # ahead, loses below
            (2, 0, 5): (3.0, 4.0),    # behind, wins below
            (3, 0, 5): (3.0, 4.0),    # behind, loses below
        }
        outcomes = {0: True, 1: False, 2: True, 3: False}
        row = lead_test.lead_table(per_round, outcomes, [5])[0]
        self.assertEqual(row["ahead"], 2)
        self.assertAlmostEqual(row["win_if_ahead"], 0.5)
        self.assertAlmostEqual(row["win_if_behind"], 0.5)
        self.assertAlmostEqual(row["gap_ahead"], 6.0)
        self.assertAlmostEqual(row["gap_behind"], -1.0)

    def test_unfinished_games_are_skipped(self):
        per_round = {(0, 0, 5): (10.0, 4.0), (1, 0, 5): (30.0, 4.0)}
        self.assertEqual(lead_test.lead_table(per_round, {0: True}, [5]), [])


class ChannelTableTest(unittest.TestCase):

    def test_mean_gap(self):
        outcomes = {0: (breakdown(20, 0, 0, 0, 0, 0), breakdown(10, 0, 0, 0, 0, 0))}
        rows = dict((name, (us, them, gap)) for name, us, them, gap in lead_test.channel_table(outcomes))
        self.assertAlmostEqual(rows["TR"][2], 10.0)
        self.assertAlmostEqual(rows["total"][2], 10.0)


class CorporationTest(unittest.TestCase):

    def index_for(self, *names):
        return {name: i for i, name in enumerate(names)}

    def test_first_set_bit(self):
        obs = [0.0, 1.0, 0.0]
        self.assertEqual(lead_test.corporation(obs, self.index_for("helion", "ecoline", "mining_guild")), "ecoline")

    def test_arclight_animal_bit_is_arclight(self):
        obs = [1.0, 0.0]
        self.assertEqual(lead_test.corporation(obs, self.index_for("arclight_animals", "helion")), "arclight")

    def test_no_corporation(self):
        self.assertIsNone(lead_test.corporation([0.0, 0.0], self.index_for("helion", "ecoline")))

    def test_group_counts(self):
        # three games: helion wins ahead, helion loses ahead, ecoline loses behind
        per_round = {(0, 0, 5): (6.0, 4.0), (1, 0, 5): (9.0, 4.0), (2, 0, 5): (3.0, 4.0)}
        outcomes = {0: True, 1: False, 2: False}
        corp = {0: "helion", 1: "helion", 2: "ecoline"}
        table = dict(lead_test.by_corporation(per_round, outcomes, corp, 5))
        self.assertEqual(table["helion"]["games"], 2)
        self.assertEqual(table["helion"]["wins"], 1)
        self.assertEqual(table["helion"]["ahead"], 2)
        self.assertEqual(table["helion"]["wins_ahead"], 1)
        self.assertEqual(table["ecoline"]["ahead"], 0)


if __name__ == "__main__":
    unittest.main()
