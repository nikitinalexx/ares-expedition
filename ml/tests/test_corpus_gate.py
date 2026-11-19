"""The corpus gate: a menu that answers the same thing in every game must not be trained on.

Why: on 18/11 the mulligan was declined in 100% of the corpus and nothing noticed, and a 30% sale exploration made the
generator sell in 89.7% of games and collapsed the iteration to 43.1%. Both are visible in the pack before a single model
trains, so the loop has to look.
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import night_loop  # noqa: E402


class MenuVerdictTest(unittest.TestCase):

    def test_a_healthy_corpus_passes(self):
        menus = {"mulligan": 4.6, "mulligan_spread": 7, "mulligan_stop": 0.16, "sale": 0.03}
        self.assertEqual(night_loop.menu_verdict(menus, "BUILD_CARD:0.15,MULLIGAN:0.9"), [])

    def test_a_mulligan_flag_the_generator_ignored_is_caught(self):
        # the measured champion corpus: 0 discards in every game, flag requested
        menus = {"mulligan": 0.0, "mulligan_spread": 1, "mulligan_stop": 1.0, "sale": 0.0}
        reasons = night_loop.menu_verdict(menus, "MULLIGAN:0.9")
        self.assertTrue(any("did not reach the rows" in reason for reason in reasons))

    def test_a_fixed_count_is_caught_even_when_it_discards(self):
        # "always 5" is the failure the owner named: one spike in the per-game distribution
        menus = {"mulligan": 5.0, "mulligan_spread": 1, "mulligan_stop": 0.0, "sale": 0.0}
        reasons = night_loop.menu_verdict(menus, "MULLIGAN:0.9")
        self.assertTrue(any("same number of cards" in reason for reason in reasons))

    def test_the_sale_collapse_is_caught(self):
        menus = {"mulligan": 4.6, "mulligan_spread": 7, "mulligan_stop": 0.16, "sale": 0.897}
        reasons = night_loop.menu_verdict(menus, "SELL_CARDS:0.30")
        self.assertTrue(any("sells in" in reason for reason in reasons))

    def test_a_rare_sale_is_not_a_problem(self):
        menus = {"mulligan": 4.6, "mulligan_spread": 7, "mulligan_stop": 0.16, "sale": 0.30}
        self.assertEqual(night_loop.menu_verdict(menus, "BUILD_CARD:0.15"), [])

    def test_no_sensor_never_blocks_a_run(self):
        self.assertEqual(night_loop.menu_verdict(None, "MULLIGAN:0.9"), [])

    def test_a_pack_without_the_type_is_not_condemned(self):
        # a pack that has no MULLIGAN rows at all gives None: the gate must not read that as "declined every time"
        menus = {"mulligan": None, "mulligan_spread": 0, "mulligan_stop": None, "sale": None}
        self.assertEqual(night_loop.menu_verdict(menus, "MULLIGAN:0.9"), [])


class GeneratorCallTest(unittest.TestCase):

    def test_the_generator_call_binds_rows_per_type_once(self):
        # 18/11: play() grew a parameter, the call site still passed phase_critic positionally, and the whole night died on
        # "play() got multiple values for argument 'rows_per_type'" after one log line. The binding is the contract.
        import inspect
        signature = inspect.signature(night_loop.play)
        bound = signature.bind("/tmp/raw", 10, "policy", "critic", 8, "", "AGENT:AGENT:1", "T1,T2",
                               phase_critic="phase", generation_symmetric=True, afterstates=True, rows_per_type=12)
        self.assertEqual(bound.arguments["rows_per_type"], 12)
        self.assertEqual(bound.arguments["phase_critic"], "phase")
        self.assertEqual(bound.arguments["types"], "T1,T2")


class ExploreSpecTest(unittest.TestCase):

    def test_the_spec_is_parsed_and_written_back(self):
        found = night_loop.parse_explore("BUILD_CARD:0.15,MULLIGAN:0.9")
        self.assertEqual(found, {"BUILD_CARD": 0.15, "MULLIGAN": 0.9})
        self.assertEqual(night_loop.spec_of(found), "BUILD_CARD:0.15,MULLIGAN:0.9")

    def test_an_empty_spec_is_empty(self):
        self.assertEqual(night_loop.parse_explore(""), {})
        self.assertEqual(night_loop.spec_of({}), "")

    def test_the_injected_type_overrides_only_itself(self):
        # 19/11: the injection was cumulative, so COLLECT_INCOME:0.60 stayed in every later corpus and took the night from
        # 63.5% to 53.0%. The merge is per type, and the auto set is rebuilt every iteration from that iteration's flags.
        base = night_loop.parse_explore("BUILD_CARD:0.15,MULLIGAN:0.9")
        auto = {"COLLECT_INCOME": 0.25}
        self.assertEqual(night_loop.spec_of({**base, **auto}),
                         "BUILD_CARD:0.15,MULLIGAN:0.9,COLLECT_INCOME:0.25")
        self.assertEqual(night_loop.spec_of({**base, **{}}), "BUILD_CARD:0.15,MULLIGAN:0.9")

    def test_a_zero_rate_is_written_out_of_the_spec(self):
        self.assertEqual(night_loop.spec_of({"COLLECT_INCOME": 0.0, "BUILD_CARD": 0.15}), "BUILD_CARD:0.15")


if __name__ == "__main__":
    unittest.main()
