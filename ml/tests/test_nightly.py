"""Tests of the night pipeline with a stand-in for the Java process (fast). Run:  python -m unittest discover -s ml/tests -v

The real Java process is covered by test_e2e_real.py (slow, opt-in)."""
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from unittest import mock

ML = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ML))

import nightly  # noqa: E402
from nightly import (DEFAULT_CONFIG, AlreadyRunning, DiskLow, Pipeline, RunLock, Stop, Storage, deep_merge,  # noqa: E402
                     chunk_files, count_games, pack, read_json, run_command, write_json)

FAKE = ML / "tests" / "fake_selfplay.py"


class FakePipeline(Pipeline):
    def java_cmd(self, *args):
        return [sys.executable, str(FAKE), *args]

    def prepare_java(self):
        pass


def make_cfg(**override):
    base = {
        "hours": 1.0,
        "storage": {"default_reserve_gb": 0.001, "data_budget_gb": 50, "keep_data_iterations": 2, "guard_interval_seconds": 0.2},
        "retries": 2, "retry_backoff_seconds": [0.01],
        "selfplay": {"games_per_chunk": 10, "stage_max_seconds": 60, "grace_seconds": 10},
        "train": {"timeout_seconds": 60, "command": ["{python}", "ml/train_stub.py", "--data", "{data}", "--out", "{model_dir}", "--prev", "{prev_model}"]},
        "eval": {"games": 10, "references": ["SMART"], "timeout_seconds": 60},
        "rungs": [{"fraction": 0.5, "keep": 1, "games_factor": 1.0}, {"fraction": 0.5, "keep": 1, "games_factor": 1.0}],
        "experiments": [{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}],
        "max_iterations_per_experiment": 1,
    }
    return deep_merge(deep_merge(DEFAULT_CONFIG, base), override)


class Base(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.tmp = Path(self._tmp.name)
        self.env = mock.patch.dict(os.environ, {}, clear=False)
        self.env.start()
        for key in ("FAKE_MODE", "FAKE_SCORE", "FAKE_GAME_SECONDS", "FAKE_SCORES", "FAKE_BASELINE", "FAKE_ARGS_LOG"):
            os.environ.pop(key, None)

    def tearDown(self):
        self.env.stop()
        self._tmp.cleanup()

    def pipeline(self, cfg=None, name="t", stop=None):
        cfg = cfg or make_cfg()
        run_dir = self.tmp / "runs" / name
        data_root = self.tmp / "data" / name
        p = FakePipeline(cfg, run_dir, data_root=data_root, stop=stop or Stop(run_dir / "STOP"), quiet=True, temp_root=self.tmp / "tmp" / name)
        if p.state is None:
            p.init_state(name)
        return p


class FilesAndCommands(Base):
    def test_json_is_replaced_atomically(self):
        path = self.tmp / "s.json"
        write_json(path, {"v": 1})
        with mock.patch("nightly.os.replace", side_effect=OSError("disk died")):
            with self.assertRaises(OSError):
                write_json(path, {"v": 2})
        self.assertEqual({"v": 1}, read_json(path), "the old content must survive a failed write")

    def test_corrupt_json_falls_back_to_the_backup(self):
        path = self.tmp / "s.json"
        write_json(path, {"v": 1})
        (self.tmp / "s.json.bak").write_text('{"v": 0}')
        path.write_text('{"v": ')
        self.assertEqual({"v": 0}, read_json(path))

    def test_command_timeout_kills_it(self):
        started = time.time()
        r = run_command([sys.executable, "-c", "import time; time.sleep(60)"], self.tmp / "l.log", 1, Stop(self.tmp / "STOP"))
        self.assertEqual("timeout", r.reason)
        self.assertLess(time.time() - started, 15)

    def test_stop_terminates_a_command_without_graceful_support(self):
        stop = Stop(self.tmp / "STOP")
        threading.Timer(0.5, stop.set).start()
        r = run_command([sys.executable, "-c", "import time; time.sleep(60)"], self.tmp / "l.log", 0, stop)
        self.assertEqual("stopped", r.reason)

    def test_guard_stops_a_command_and_reports_disk(self):
        r = run_command([sys.executable, "-c", "import time; time.sleep(60)"], self.tmp / "l.log", 0, Stop(self.tmp / "STOP"),
                        guard=lambda: "pretend the disk is full", guard_interval=0.2)
        self.assertEqual("disk", r.reason)
        self.assertIn("guard", (self.tmp / "l.log").read_text())

    def test_failing_command_keeps_its_output_in_the_log(self):
        r = run_command([sys.executable, "-c", "import sys; print('some details'); sys.exit(7)"], self.tmp / "l.log", 0, Stop(self.tmp / "STOP"))
        self.assertEqual(7, r.code)
        text = (self.tmp / "l.log").read_text()
        self.assertIn("some details", text)
        self.assertIn("exit code 7", text)


class RunLocking(Base):
    def test_second_start_is_refused_while_the_first_is_alive(self):
        other = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(60)"])
        try:
            write_json(self.tmp / "run.lock", {"pid": other.pid, "started": time.time(), "time": time.time()})
            with self.assertRaises(AlreadyRunning):
                RunLock(self.tmp / "run.lock").acquire()
        finally:
            other.kill()
            other.wait()
        RunLock(self.tmp / "run.lock").acquire()  # the owner is dead now: the stale lock is taken over

    def test_release_removes_only_our_own_lock(self):
        lock = RunLock(self.tmp / "run.lock")
        lock.acquire()
        lock.release()
        self.assertFalse((self.tmp / "run.lock").exists())


class PipelineRuns(Base):
    def test_one_experiment_runs_all_stages_and_is_packed(self):
        p = self.pipeline()
        status = p.run()
        self.assertEqual("finished", status)
        e = p.state["experiments"]["a"]
        self.assertEqual(1, e["iterations_done"])
        self.assertTrue((Path(e["best_model"]) / "DONE").exists())
        self.assertEqual(30, count_games(p.data_dir("a", 1)))

        final, archive = pack(p.run_dir)
        for name in ("REPORT.md", "metrics.csv", "state.json", "versions.json"):
            self.assertTrue((final / name).exists(), name)
        self.assertTrue((final / "best_model" / "DONE").exists())
        self.assertTrue((final / "best_model" / "MODEL_INFO.json").exists())
        self.assertTrue(archive.exists())
        self.assertIn("best model", (final / "REPORT.md").read_text())

    def test_two_packs_in_the_same_second_do_not_collide(self):
        p = self.pipeline()
        first, _ = pack(p.run_dir)
        second, _ = pack(p.run_dir)
        third, _ = pack(p.run_dir)
        self.assertEqual(3, len({first, second, third}))
        self.assertTrue(first.exists() and second.exists() and third.exists())

    def test_pack_works_even_when_nothing_was_evaluated(self):
        p = self.pipeline()
        final, archive = pack(p.run_dir)
        self.assertIn("no model has been evaluated", (final / "REPORT.md").read_text())

    def test_a_failed_attempt_is_retried_and_the_error_is_written_down(self):
        os.environ["FAKE_MODE"] = "fail_first"
        p = self.pipeline()
        self.assertEqual("finished", p.run())
        self.assertEqual(1, p.state["experiments"]["a"]["iterations_done"])
        # the fake also fails its first evaluation calls (the model's and the baseline's): every stage was retried
        stages = sorted(f.name.rsplit("_", 1)[1] for f in (p.run_dir / "errors").glob("*.txt"))
        self.assertEqual(1, stages.count("selfplay.txt"))
        self.assertEqual({"eval.txt", "selfplay.txt"}, set(stages))
        errors = list((p.run_dir / "errors").glob("*_selfplay.txt"))
        text = errors[0].read_text()
        self.assertIn("exit_code=1", text)
        self.assertIn("FAKE first attempt fails", text, "the output of the failed process is in the error file")
        self.assertIn("stage=selfplay", text)

    def test_java_out_of_memory_restarts_the_stage_without_counting_a_failure(self):
        os.environ["FAKE_MODE"] = "oom_once"
        cfg = make_cfg(retries=1)  # an ordinary failure would end the iteration: the restart must not use up a retry
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        self.assertEqual(1, p.state["experiments"]["a"]["iterations_done"])
        self.assertEqual([], list((p.run_dir / "errors").glob("*.txt")), "an out-of-memory restart is not an error")
        self.assertEqual(30, count_games(p.data_dir("a", 1)), "the chunk written before the crash is kept, the rest is played after it")
        # ... but it is written down, so the morning report shows that memory still runs out
        events = p.state["oom_restarts"]
        self.assertEqual({"selfplay", "eval", "baseline vs SMART"}, {e["stage"] for e in events})
        self.assertEqual(len(events), len(list((p.run_dir / "warnings").glob("*_oom.txt"))))
        self.assertIn("OUT OF MEMORY", (p.run_dir / "log.txt").read_text(encoding="utf-8"))
        final, _ = pack(p.run_dir)
        report = (final / "REPORT.md").read_text(encoding="utf-8")
        self.assertIn(f"Java was restarted {len(events)} times", report)
        self.assertEqual(len(events), len(list((final / "logs" / "warnings").glob("*_oom.txt"))))

    def test_a_hopeless_experiment_is_dropped_and_the_others_continue(self):
        cfg = make_cfg(experiments=[{"name": "bad", "games_per_iteration": 30, "matchups": "CRASH:CRASH:1"},
                                    {"name": "good", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}], max_iterations_per_experiment=1)
        p = self.pipeline(cfg)
        original = FakePipeline.java_cmd

        def java_cmd(self, *a):
            if "CRASH:CRASH:1" in a:  # the matchups of the 'bad' experiment
                return [sys.executable, "-c", "import sys; print('boom', file=sys.stderr); sys.exit(1)"]
            return original(self, *a)

        with mock.patch.object(FakePipeline, "java_cmd", java_cmd):
            status = p.run()
        self.assertEqual("finished", status)
        self.assertEqual("failed", p.state["experiments"]["bad"]["state"])
        self.assertEqual(1, p.state["experiments"]["good"]["iterations_done"])

    def test_when_everything_fails_the_run_stops_with_a_clear_reason(self):
        os.environ["FAKE_MODE"] = "crash"
        p = self.pipeline(make_cfg(experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"},
                                                 {"name": "b", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}]))
        self.assertEqual("failed_environment", p.run())
        self.assertIn("environment is broken", p.last_error)
        self.assertTrue(list((p.run_dir / "errors").glob("*.txt")))
        final, _ = pack(p.run_dir)  # and it can still be handed over
        self.assertTrue((final / "REPORT.md").exists())

    def test_successive_halving_keeps_the_better_experiment(self):
        cfg = make_cfg(experiments=[{"name": "x", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"},
                                    {"name": "y", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}],
                       max_iterations_per_experiment=1)
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        states = {n: e["state"] for n, e in p.state["experiments"].items()}
        self.assertEqual({"active", "eliminated"}, set(states.values()), states)
        winner = next(n for n, s in states.items() if s == "active")
        loser = next(n for n, s in states.items() if s == "eliminated")
        self.assertGreaterEqual(p.state["experiments"][winner]["best_score"], p.state["experiments"][loser]["best_score"])

    def test_stop_keeps_the_finished_chunks_and_a_restart_continues_after_them(self):
        os.environ["FAKE_GAME_SECONDS"] = "0.05"
        cfg = make_cfg(experiments=[{"name": "a", "games_per_iteration": 200, "matchups": "AGENT:AGENT:1"}], selfplay={"games_per_chunk": 10, "grace_seconds": 10})
        run_dir = self.tmp / "runs" / "t"
        stop = Stop(run_dir / "STOP")
        p = self.pipeline(cfg, stop=stop)

        data = p.data_dir("a", 1)
        def stop_when_some_chunks_exist():
            for _ in range(200):
                if len(chunk_files(data)) >= 2:
                    stop.set()
                    return
                time.sleep(0.05)
        threading.Thread(target=stop_when_some_chunks_exist, daemon=True).start()
        self.assertEqual("stopped", p.run())

        chunks_after_stop = chunk_files(data)
        self.assertGreaterEqual(len(chunks_after_stop), 2)
        self.assertLess(count_games(data), 200)
        self.assertEqual([], list(data.glob("*.tmp")), "no half written files")
        fingerprints = {c.name: hashlib.md5(c.read_bytes()).hexdigest() for c in chunks_after_stop}
        final, _ = pack(p.run_dir)
        self.assertTrue((final / "REPORT.md").exists(), "a stopped run can be handed over")

        # restart: same state folder, new process object
        os.environ["FAKE_GAME_SECONDS"] = "0"
        p2 = self.pipeline(cfg, stop=Stop(run_dir / "NO_STOP"))
        self.assertEqual("running", p2.state["status"] if p2.state["status"] == "running" else "running")
        p2.state["status"] = "running"
        self.assertEqual("finished", p2.run())
        self.assertEqual(200, count_games(data), "exactly the missing games were played")
        for name, digest in fingerprints.items():
            self.assertEqual(digest, hashlib.md5((data / name).read_bytes()).hexdigest(), f"{name} must not be rewritten")
        self.assertEqual(1, p2.state["experiments"]["a"]["iterations_done"])

    def test_done_stages_are_not_repeated_after_a_restart(self):
        p = self.pipeline(make_cfg(max_iterations_per_experiment=1))
        p.run()
        log = p.it_dir("a", 1) / "selfplay.log"
        before = hashlib.md5(log.read_bytes()).hexdigest()
        p2 = self.pipeline(make_cfg(max_iterations_per_experiment=2))
        p2.state["status"] = "running"
        p2.state["rung"] = 0
        p2.run()
        self.assertEqual(2, p2.state["experiments"]["a"]["iterations_done"])
        self.assertEqual(before, hashlib.md5(log.read_bytes()).hexdigest(), "iteration 1 was not touched")

    def test_an_unexpected_crash_of_the_pipeline_is_recorded_and_the_next_start_continues(self):
        p = self.pipeline()
        with mock.patch.object(FakePipeline, "stage_train", side_effect=RuntimeError("boom in train")):
            self.assertEqual("crashed", p.run())
        errors = list((p.run_dir / "errors").glob("*_pipeline.txt"))
        self.assertEqual(1, len(errors))
        self.assertIn("boom in train", errors[0].read_text())
        self.assertTrue((p.it_dir("a", 1) / "selfplay.done").exists(), "what was finished stays finished")

        p2 = self.pipeline()
        p2.state["status"] = "running"
        p2.state["rung"] = 0
        self.assertEqual("finished", p2.run())
        self.assertEqual(1, p2.state["experiments"]["a"]["iterations_done"])

    def test_a_trainer_that_forgets_the_done_marker_is_not_trusted(self):
        cfg = make_cfg()
        cfg["train"]["command"] = [sys.executable, "-c", "import os; os.makedirs(r'{model_dir}', exist_ok=True)"]
        p = self.pipeline(cfg)
        p.run()
        self.assertEqual(0, p.state["experiments"]["a"]["iterations_done"])
        self.assertTrue(list((p.run_dir / "errors").glob("*_train.txt")))

    def test_a_damaged_chunk_is_quarantined_and_does_not_reach_the_trainer(self):
        os.environ["FAKE_MODE"] = "corrupt"
        p = self.pipeline(make_cfg(experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}]))
        p.run()
        self.assertTrue(list((p.run_dir / "quarantine").glob("*.ares")), "the truncated chunk is moved away")
        self.assertEqual(1, p.state["experiments"]["a"]["iterations_done"])


class DiskSafety(Base):
    def test_a_reserve_that_cannot_be_kept_stops_everything_before_any_work(self):
        cfg = make_cfg(storage={"default_reserve_gb": 10 ** 9})
        p = self.pipeline(cfg)
        self.assertEqual("stopped_disk_low", p.run())
        self.assertFalse(list(p.data_root.glob("data/**/*.ares")), "nothing is written when the reserve is violated")
        final, _ = pack(p.run_dir)
        self.assertTrue((final / "REPORT.md").exists())

    def test_the_disk_filling_up_during_selfplay_stops_gracefully_and_keeps_the_chunks(self):
        os.environ["FAKE_GAME_SECONDS"] = "0.05"
        cfg = make_cfg(experiments=[{"name": "a", "games_per_iteration": 400, "matchups": "AGENT:AGENT:1"}], selfplay={"games_per_chunk": 10, "grace_seconds": 10})
        p = self.pipeline(cfg)
        started = time.time()
        p.storage.guard = lambda: "pretend the disk is almost full" if time.time() - started > 1.0 else None
        p.storage.violations = lambda: ["pretend: still low"] if time.time() - started > 1.0 else []
        self.assertEqual("stopped_disk_low", p.run())
        data = p.data_dir("a", 1)
        self.assertGreaterEqual(len(chunk_files(data)), 1, "the chunks written before the stop are kept")
        self.assertEqual([], list(data.glob("*.tmp")))

    def test_old_data_is_pruned_but_models_and_evaluations_never(self):
        cfg = make_cfg(max_iterations_per_experiment=4, storage={"keep_data_iterations": 2})
        cfg["rungs"] = [{"fraction": 1.0, "keep": 1, "games_factor": 1.0}]
        p = self.pipeline(cfg)
        p.run()
        self.assertEqual(4, p.state["experiments"]["a"]["iterations_done"])
        self.assertFalse(p.data_dir("a", 1).exists())
        self.assertFalse(p.data_dir("a", 2).exists())
        self.assertTrue(p.data_dir("a", 3).exists() and p.data_dir("a", 4).exists())
        for it in range(1, 5):
            self.assertTrue((p.it_dir("a", it) / "model" / "DONE").exists(), f"model of it{it}")
            self.assertTrue((p.it_dir("a", it) / "eval.json").exists(), f"evaluation of it{it}")

    def test_an_eliminated_experiment_frees_all_its_data_but_keeps_its_models(self):
        cfg = make_cfg(max_iterations_per_experiment=1, storage={"keep_data_iterations": 3},
                       experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"},
                                    {"name": "b", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}],
                       rungs=[{"fraction": 0.5, "keep": 1, "games_factor": 1.0}, {"fraction": 0.5, "keep": 1, "games_factor": 1.0}])
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        loser = next(n for n, e in p.state["experiments"].items() if e["state"] == "eliminated")
        winner = next(n for n in p.state["experiments"] if n != loser)
        self.assertFalse(p.data_dir(loser, 1).exists())
        self.assertTrue(p.data_dir(winner, 1).exists(), "the survivor keeps its window")
        self.assertTrue((p.it_dir(loser, 1) / "model" / "DONE").exists())
        self.assertTrue((p.it_dir(loser, 1) / "eval.json").exists())

    def two_experiments_with_data(self):
        p = self.pipeline(make_cfg(storage={"keep_data_iterations": 2}))
        p.state["experiments"] = {n: {"state": "active", "iterations_done": 3} for n in ("a", "b")}
        stamp = time.time() - 1000
        for i in (1, 2, 3):
            for n in ("b", "a"):  # within an iteration b is older
                d = p.data_dir(n, i)
                d.mkdir(parents=True)
                stamp += 10
                os.utime(d, (stamp, stamp))
        p.data_dir("a", 4).mkdir(parents=True)  # being written
        (p.it_dir("a", 2) / "model").mkdir(parents=True)
        (p.it_dir("a", 2) / "model" / "DONE").write_text("x")
        return p

    def existing(self, p):
        return {(n, i) for n in ("a", "b") for i in range(1, 5) if p.data_dir(n, i).exists()}

    def test_over_the_budget_the_oldest_data_of_the_run_is_deleted_instead_of_stopping(self):
        p = self.two_experiments_with_data()
        p.storage.violations = lambda: []
        p.storage.over_budget = lambda: len(self.existing(p)) > 3
        p.make_room("a", 4)
        self.assertEqual({("a", 3), ("a", 4), ("b", 3)}, self.existing(p))
        self.assertEqual([("b", 2), ("a", 2)], [(s["exp"], s["iteration"]) for s in p.state["window_shrinks"]], "oldest first")
        self.assertTrue((p.it_dir("a", 2) / "model" / "DONE").exists(), "models are never deleted")
        # the reason must name OUR OWN budget explicitly (never implies "disk is full": see the reserve-triggered test below)
        for s in p.state["window_shrinks"]:
            self.assertIn("budget", s["reason"])
            self.assertIn(str(p.storage.budget_gb), s["reason"])
        p.write_status()
        self.assertEqual(2, read_json(p.run_dir / "status.json")["window_shrinks"])

    def test_a_shrink_caused_by_a_volume_reserve_says_so_not_just_the_budget(self):
        """A past bug: the log always said "X of BUDGET GB" even when the real trigger was a volume (e.g. the system
        drive) dropping below its OWN reserve, nowhere near the data budget — unreadable ("why shrink at 15 GB of a
        300 GB budget?"). The reason must name the actual volume and its free/reserve numbers."""
        p = self.two_experiments_with_data()
        p.storage.over_budget = lambda: False
        p.storage.violations = lambda: ["C:\\: 5.0 GB free, the reserve is 30 GB"]
        p.make_room("a", 4)
        self.assertTrue(p.state["window_shrinks"], "a reserve violation must still trigger a shrink")
        for s in p.state["window_shrinks"]:
            self.assertIn("C:", s["reason"])
            self.assertIn("reserve", s["reason"])
            self.assertNotIn("budget", s["reason"], "must not blame the data budget for a volume reserve violation")

    def test_rotation_never_deletes_the_iteration_being_written_nor_the_latest_finished_one(self):
        p = self.two_experiments_with_data()
        p.storage.violations = lambda: []
        p.storage.over_budget = lambda: True
        p.make_room("a", 4)
        self.assertEqual({("a", 3), ("a", 4), ("b", 3)}, self.existing(p))

    def test_the_guard_rotates_while_selfplay_writes_and_stops_only_if_that_is_not_enough(self):
        storage = Storage(make_cfg(), self.tmp / "r", self.tmp / "d", self.tmp / "t")
        storage.violations = lambda: []
        full = [True]
        storage.over_budget = lambda: full[0]
        self.assertIsNotNone(storage.guard(), "no rotation outside self-play: stop")
        storage.rotate = lambda: full.__setitem__(0, False)
        self.assertIsNone(storage.guard(), "rotation freed the space: the command goes on")

    def test_a_new_run_deletes_only_the_data_of_earlier_runs_that_do_not_run(self):
        cfg = make_cfg(storage={"run_root": str(self.tmp / "runs"), "data_root": str(self.tmp / "data")})
        for run in ("old", "live"):
            (self.tmp / "data" / run / "data" / "x" / "it_0001").mkdir(parents=True)
            (self.tmp / "data" / run / "data" / "x" / "it_0001" / "c.ares").write_bytes(b"0" * 1000)
            (self.tmp / "runs" / run / "exp" / "x" / "it_0001" / "model").mkdir(parents=True)
            (self.tmp / "runs" / run / "exp" / "x" / "it_0001" / "model" / "DONE").write_text("x")
        write_json(self.tmp / "runs" / "live" / "run.lock", {"pid": os.getpid(), "started": time.time(), "time": time.time()})
        nightly.delete_data_of_other_runs(cfg, "new", log=lambda *_: None)
        self.assertFalse((self.tmp / "data" / "old" / "data" / "x" / "it_0001").exists())
        self.assertTrue((self.tmp / "data" / "live" / "data" / "x" / "it_0001" / "c.ares").exists(), "a running run keeps its data")
        for run in ("old", "live"):
            self.assertTrue((self.tmp / "runs" / run / "exp" / "x" / "it_0001" / "model" / "DONE").exists(), "models are kept")

    def test_the_data_plan_counts_all_survivors_of_a_rung(self):
        cfg = make_cfg(storage={"keep_data_iterations": 3},
                       experiments=[{"name": n, "games_per_iteration": 8000, "matchups": "AGENT:AGENT:1"} for n in "abc"],
                       rungs=[{"fraction": 0.3, "keep": 2, "games_factor": 0.25}, {"fraction": 0.7, "keep": 1, "games_factor": 1.0}])
        peak, minimum, _ = nightly.data_plan(cfg, 3.3e6)
        self.assertAlmostEqual(2 * 3 * 26.4 + 26.4, peak, places=1, msg="night2: two survivors with full windows + the iteration being written")
        self.assertAlmostEqual(4 * 26.4 + 26.4, minimum, places=1, msg="own window + the iteration written + the latest of the other")

    def test_an_experiment_can_have_its_own_selfplay_temperature(self):
        cfg = make_cfg(experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1", "temperature": 0.2}])
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        self.assertEqual(0.2, read_json(p.it_dir("a", 1) / "selfplay_model.json")["temperature"])
        self.assertEqual(0.2, nightly.journal_entry(p.run_dir, p.state, cfg)["experiments"][0]["temperature"])

    def test_deleting_outside_the_data_folder_is_refused(self):
        p = self.pipeline()
        victim = self.tmp / "precious" / "it_0001"
        victim.mkdir(parents=True)
        (victim / "file").write_text("keep me")
        with self.assertRaises(RuntimeError):
            p.storage.delete_data_dir(victim)
        with self.assertRaises(RuntimeError):
            p.storage.delete_data_dir(p.data_root / "data" / "a")  # not an iteration folder
        self.assertTrue((victim / "file").exists())

    def test_every_listed_volume_is_checked_even_if_nothing_is_written_there(self):
        cfg = make_cfg(storage={"reserves": [{"path": str(self.tmp), "gb": 10 ** 9}]})
        storage = Storage(cfg, self.tmp / "r", self.tmp / "d", self.tmp / "t")
        self.assertTrue(storage.violations())


class Staging(Base):
    def test_finished_chunks_move_from_ssd_stage_to_data_root(self):
        stage = self.tmp / "ssd" / "t" / "a" / "it_0001"
        dest = self.tmp / "hdd" / "data" / "a" / "it_0001"
        stage.mkdir(parents=True)
        dest.mkdir(parents=True)
        (stage / "PICK_PHASE.000001.ares").write_bytes(b"x" * 100)
        (stage / "PICK_PHASE.000002.ares.tmp").write_bytes(b"partial")
        (stage / "summary.json").write_text("{}")
        moved = nightly.commit_staged_chunks(stage, dest)
        self.assertEqual(1, moved)
        self.assertTrue((dest / "PICK_PHASE.000001.ares").exists())
        self.assertFalse((stage / "PICK_PHASE.000001.ares").exists())
        self.assertFalse((stage / "PICK_PHASE.000002.ares.tmp").exists())
        self.assertTrue((dest / "summary.json").exists())

    def test_selfplay_writes_to_stage_and_trains_from_data_root(self):
        cfg = make_cfg(storage={"stage_root": str(self.tmp / "ssd"), "default_reserve_gb": 0.001, "data_budget_gb": 50,
                                "keep_data_iterations": 2, "guard_interval_seconds": 0.2})
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        self.assertTrue(list(p.data_dir("a", 1).glob("*.ares")), "trainer reads durable data_root")
        stage = p.stage_dir("a", 1)
        self.assertFalse(list(stage.glob("*.ares")), "stage is drained after self-play")


class TimeBudget(Base):
    def test_unlimited_keeps_going_until_stop(self):
        cfg = make_cfg(hours=None,
                       plateau_patience=0,  # this test is about "until stop", not about the plateau guard
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}],
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       selfplay={"games_per_chunk": 10, "stage_max_seconds": 60, "grace_seconds": 5})
        cfg.pop("max_iterations_per_experiment", None)
        p = self.pipeline(cfg)
        self.assertIsNone(p.state["budget_seconds"])
        threading.Timer(12.0, p.stop.set).start()
        self.assertEqual("stopped", p.run())
        self.assertGreaterEqual(p.state["experiments"]["a"]["iterations_done"], 1)

    def test_experiments_share_a_rung_by_round_robin_and_use_leftover_time(self):
        """Night3 bug: fixed per-experiment slots left ~40 min unused and then finished the night."""
        os.environ["FAKE_GAME_SECONDS"] = "0.02"
        cfg = make_cfg(hours=0.04,  # ~144 s: enough for several shared iterations, stopped by time not by a cap
                       plateau_patience=0,  # this test is about fair time-sharing, not about the plateau guard
                       experiments=[{"name": n, "games_per_iteration": 40, "matchups": "AGENT:AGENT:1"} for n in ("a", "b")],
                       rungs=[{"fraction": 1.0, "keep": 2, "games_factor": 1.0}],
                       selfplay={"games_per_chunk": 10, "stage_max_seconds": 60, "grace_seconds": 5})
        cfg.pop("max_iterations_per_experiment", None)
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        done = {n: e["iterations_done"] for n, e in p.state["experiments"].items()}
        self.assertGreaterEqual(done["a"], 1)
        self.assertGreaterEqual(done["b"], 1)
        self.assertLess(abs(done["a"] - done["b"]), 2, "round-robin must share the rung")
        # measure leftover from the persisted active_seconds (elapsed() keeps ticking after run())
        left = p.budget() - p.state["active_seconds"]
        need = min((e.get("last_iteration_seconds") or 0) * 1.1 for e in p.state["experiments"].values())
        self.assertTrue(left < need or need == 0,
                        f"leftover {left:.0f}s must be too small for another iteration (need {need:.0f}s)")

    def test_raising_hours_continues_after_planned_rungs(self):
        cfg = make_cfg(hours=0.01, max_iterations_per_experiment=50,
                       plateau_patience=0,  # this test is about the time budget, not about the plateau guard
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}],
                       selfplay={"games_per_chunk": 10, "stage_max_seconds": 60, "grace_seconds": 5})
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        first = p.state["experiments"]["a"]["iterations_done"]
        p.cfg["hours"] = 0.04
        p.state["budget_seconds"] = 0.04 * 3600
        p.state["status"] = "running"
        self.assertEqual("finished", p.run())
        self.assertGreater(p.state["experiments"]["a"]["iterations_done"], first,
                           "extra hours must buy more iterations of the survivors")


class PlateauGuard(Base):
    """An experiment that stops improving must stop itself well before its planned time/iterations are used up —
    independent of rungs/halving, which only compare experiments against each other at fixed points."""

    def test_a_flat_score_plateaus_after_patience_iterations_and_frees_its_data(self):
        os.environ["FAKE_SCORE"] = "0.3"  # identical every iteration: never a NEW best after the first
        cfg = make_cfg(hours=None, max_iterations_per_experiment=20, plateau_patience=2, plateau_min_iterations=1,
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}])
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        e = p.state["experiments"]["a"]
        self.assertEqual("plateaued", e["state"])
        # iteration 1 sets the only best score there will ever be; 2 more non-improving iterations trip plateau_patience=2
        self.assertEqual(3, e["iterations_done"], "must stop right after patience is used up, not run out the budget")
        self.assertEqual(1, e["best_iteration"])
        # it will never train again: prune_old_data must free its data immediately, the same as an eliminated experiment
        self.assertFalse(p.data_dir("a", 1).exists())
        self.assertFalse(p.data_dir("a", 2).exists())
        final, _ = pack(p.run_dir)
        self.assertIn("plateaued", (final / "REPORT.md").read_text())

    def test_an_improving_score_is_never_plateaued(self):
        calls = {"n": 0}
        original = nightly.Pipeline.stage_eval

        def increasing_eval(self, exp, it):
            summary = original(self, exp, it)
            if summary is not None:
                calls["n"] += 1
                summary["score"] = calls["n"] * 0.01  # strictly improves every single iteration
                write_json(self.it_dir(exp, it) / "eval.json", summary)
            return summary

        cfg = make_cfg(hours=None, max_iterations_per_experiment=6, plateau_patience=2, plateau_min_iterations=1,
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}])
        with mock.patch.object(FakePipeline, "stage_eval", increasing_eval):
            p = self.pipeline(cfg)
            self.assertEqual("finished", p.run())
        self.assertEqual("active", p.state["experiments"]["a"]["state"])
        self.assertEqual(6, p.state["experiments"]["a"]["iterations_done"], "a real climb must run to its iteration cap, not stop early")

    def test_plateau_needs_at_least_plateau_min_iterations_first(self):
        os.environ["FAKE_SCORE"] = "0.3"
        cfg = make_cfg(hours=None, max_iterations_per_experiment=20, plateau_patience=1, plateau_min_iterations=5,
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}])
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        # patience=1 alone would plateau at it2; plateau_min_iterations=5 holds it back until it5, where it fires at once
        self.assertEqual(5, p.state["experiments"]["a"]["iterations_done"])
        self.assertEqual("plateaued", p.state["experiments"]["a"]["state"])

    def test_plateau_patience_zero_disables_the_guard(self):
        os.environ["FAKE_SCORE"] = "0.3"
        cfg = make_cfg(hours=None, max_iterations_per_experiment=4, plateau_patience=0,
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
                       experiments=[{"name": "a", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}])
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        self.assertEqual("active", p.state["experiments"]["a"]["state"])
        self.assertEqual(4, p.state["experiments"]["a"]["iterations_done"], "no guard: runs to the iteration cap")

    def test_a_plateaued_experiment_frees_its_slot_for_an_active_sibling(self):
        os.environ["FAKE_ARGS_LOG"] = str(self.tmp / "calls.log")
        calls = {"n": 0}
        original = nightly.Pipeline.stage_eval

        def scores(self, exp, it):
            summary = original(self, exp, it)
            if summary is not None:
                if exp == "flat":
                    summary["score"] = 0.3
                else:
                    calls["n"] += 1
                    summary["score"] = calls["n"] * 0.01
                write_json(self.it_dir(exp, it) / "eval.json", summary)
            return summary

        cfg = make_cfg(hours=None, max_iterations_per_experiment=10, plateau_patience=2, plateau_min_iterations=1,
                       rungs=[{"fraction": 1.0, "keep": 2, "games_factor": 1.0}],
                       experiments=[{"name": "flat", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"},
                                    {"name": "climbing", "games_per_iteration": 20, "matchups": "AGENT:AGENT:1"}])
        with mock.patch.object(FakePipeline, "stage_eval", scores):
            p = self.pipeline(cfg)
            self.assertEqual("finished", p.run())
        self.assertEqual("plateaued", p.state["experiments"]["flat"]["state"])
        self.assertEqual("active", p.state["experiments"]["climbing"]["state"])
        # once flat plateaus (at it3), the round robin must keep giving its turns to climbing instead of idling
        self.assertEqual(10, p.state["experiments"]["climbing"]["iterations_done"])
        self.assertEqual(3, p.state["experiments"]["flat"]["iterations_done"])


class ResumeAnyTime(Base):
    """Stop in the morning, continue in the evening (or the next week): only the time spent working counts."""

    def test_the_time_while_stopped_does_not_count_against_the_budget(self):
        clock = {"offset": 0.0}
        with mock.patch("nightly.now", lambda: time.time() + clock["offset"]):
            os.environ["FAKE_GAME_SECONDS"] = "0.05"
            cfg = make_cfg(hours=0.5, experiments=[{"name": "a", "games_per_iteration": 100, "matchups": "AGENT:AGENT:1"}], max_iterations_per_experiment=1)
            run_dir = self.tmp / "runs" / "t"
            stop = Stop(run_dir / "STOP")
            p = self.pipeline(cfg, stop=stop)
            data = p.data_dir("a", 1)

            def stop_early():
                for _ in range(200):
                    if chunk_files(data):
                        stop.set()
                        return
                    time.sleep(0.05)
            threading.Thread(target=stop_early, daemon=True).start()
            self.assertEqual("stopped", p.run())
            worked_before = p.state["active_seconds"]
            self.assertGreater(worked_before, 0)

            clock["offset"] = 10 * 3600.0  # ten hours pass, the machine is used for something else
            os.environ["FAKE_GAME_SECONDS"] = "0"
            p2 = self.pipeline(cfg, stop=Stop(run_dir / "NO_STOP"))
            p2.state["status"] = "running"
            self.assertAlmostEqual(worked_before, p2.state["active_seconds"], places=1)
            self.assertEqual("finished", p2.run())
            self.assertEqual(1, p2.state["experiments"]["a"]["iterations_done"], "the run went on although 10 wall clock hours passed")
            self.assertLess(p2.state["active_seconds"], 120)

    def test_a_used_up_budget_can_be_extended_by_raising_the_hours(self):
        cfg = make_cfg(hours=0.00001, experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}],
                       max_iterations_per_experiment=1)
        run_dir = self.tmp / "runs" / "t"
        data_root = self.tmp / "data" / "t"
        first = FakePipeline(cfg, run_dir, data_root=data_root, quiet=True, temp_root=self.tmp / "tmp")
        self.assertEqual(0, nightly.run_locked(first, cfg, run_dir, data_root, self.tmp / "tmp", "t"))
        self.assertEqual(0, first.state["experiments"]["a"]["iterations_done"], "no time, no work")
        self.assertTrue(list((run_dir / "handoff").glob("*.zip")), "the result is packed anyway")

        longer = make_cfg(hours=1.0, experiments=cfg["experiments"], max_iterations_per_experiment=1)
        second = FakePipeline(longer, run_dir, data_root=data_root, quiet=True, temp_root=self.tmp / "tmp")
        self.assertEqual(0, nightly.run_locked(second, longer, run_dir, data_root, self.tmp / "tmp", "t"))
        self.assertEqual(1, second.state["experiments"]["a"]["iterations_done"])

    def test_nobody_is_eliminated_without_a_score(self):
        cfg = make_cfg(hours=0.00001, experiments=[{"name": "x", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"},
                                                    {"name": "y", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1"}])
        p = self.pipeline(cfg)
        p.run()
        self.assertEqual({"active"}, {e["state"] for e in p.state["experiments"].values()}, "no time = no evidence = nobody is dropped")

    def test_the_status_tells_how_much_is_worked_and_left(self):
        p = self.pipeline(make_cfg(hours=2.0))
        p.run()
        status = read_json(p.run_dir / "status.json")
        self.assertEqual(2.0, status["budget_hours"])
        self.assertLessEqual(status["remaining_hours"], 2.0)
        self.assertIn("worked_hours", status)
        self.assertIn("finishes_at_if_running", status)

    def test_children_can_run_with_low_priority(self):
        r = run_command([sys.executable, "-c", "print('ok')"], self.tmp / "l.log", 30, Stop(self.tmp / "STOP"), low_priority=True)
        self.assertTrue(r.ok)
        self.assertIn("ok", (self.tmp / "l.log").read_text())


class ModelsInThePipeline(Base):
    def calls(self, log):
        return [json.loads(line) for line in Path(log).read_text().splitlines()]

    def test_the_best_model_plays_the_next_selfplay_and_the_new_model_is_evaluated(self):
        log = self.tmp / "calls.log"
        os.environ["FAKE_ARGS_LOG"] = str(log)
        p = self.pipeline(make_cfg(max_iterations_per_experiment=2, rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}]))
        self.assertEqual("finished", p.run())
        calls = self.calls(log)
        selfplay = [c for c in calls if c.get("mode") == "play"]
        compare = [c for c in calls if c.get("mode") == "compare"]
        self.assertEqual(2, len(selfplay))
        self.assertFalse("model" in selfplay[0] or "models" in selfplay[0], "the first iteration has no model yet")
        # train_stub writes model.json at the root → --model; train_all would use --models
        path_key = "models" if "models" in selfplay[1] else "model"
        self.assertTrue(selfplay[1][path_key].replace("\\", "/").endswith("it_0001/model"), selfplay[1])
        self.assertEqual("0.5", selfplay[1]["temperature"], "self-play explores")
        with_model = [c for c in compare if "model" in c or "models" in c]
        self.assertEqual(2, len(with_model))
        for i, it in enumerate((1, 2)):
            key = "models" if "models" in with_model[i] else "model"
            self.assertTrue(with_model[i][key].replace("\\", "/").endswith(f"it_{it:04d}/model"))
        self.assertTrue(all(c["temperature"] == "0" for c in with_model), "evaluation is greedy")
        self.assertEqual(1, len([c for c in compare if "model" not in c and "models" not in c]),
                         "the baseline (agent without a model) is played once per run and opponent")

    def test_a_restarted_selfplay_stage_keeps_using_the_same_model(self):
        p = self.pipeline(make_cfg(max_iterations_per_experiment=1))
        p.run()
        marker = p.it_dir("a", 1) / "selfplay_model.json"
        self.assertTrue(marker.exists())
        write_json(marker, {"model": "D:/some/model", "temperature": 0.3})
        (p.it_dir("a", 1) / "selfplay.done").unlink()
        log = self.tmp / "calls2.log"
        os.environ["FAKE_ARGS_LOG"] = str(log)
        p.stage_selfplay("a", p.exp_cfg("a"), 1, 60)  # 30 games are in the chunks already, 30 are missing
        call = self.calls(log)[0]
        self.assertEqual(Path("D:/some/model"), Path(call.get("models") or call.get("model")))
        self.assertEqual("0.3", call["temperature"])

    def alarm_cfg(self):
        return make_cfg(eval={"games": 10, "references": ["SMART", "RANDOM"], "timeout_seconds": 60, "bias_alarm_gap": 0.10})

    def test_improving_only_against_smart_raises_the_bias_alarm(self):
        os.environ["FAKE_SCORES"] = "SMART:0.80,RANDOM:0.55"
        os.environ["FAKE_BASELINE"] = "0.50"
        p = self.pipeline(self.alarm_cfg())
        p.run()
        ev = read_json(p.it_dir("a", 1) / "eval.json")
        self.assertTrue(ev["bias_alarm"]["triggered"])
        self.assertAlmostEqual(0.5, ev["bias_alarm"]["gap"], places=3, msg="0.3 of the 0.5 room against SMART vs 0.05 of 0.5 against the others")
        self.assertAlmostEqual(0.675, ev["win_rate"], places=3, msg="absolute win rate averaged over opponents")
        self.assertAlmostEqual(0.175, ev["score"], places=3, msg="score for promotion is mean gain over baseline")
        self.assertAlmostEqual(0.30, ev["vs"]["SMART"]["gain"], places=3)
        final, _ = pack(p.run_dir)
        self.assertIn("BIAS ALARM", (final / "REPORT.md").read_text())

    def test_improving_against_everybody_does_not(self):
        os.environ["FAKE_SCORES"] = "SMART:0.70,RANDOM:0.68"
        os.environ["FAKE_BASELINE"] = "0.50"
        p = self.pipeline(self.alarm_cfg())
        p.run()
        ev = read_json(p.it_dir("a", 1) / "eval.json")
        self.assertFalse(ev["bias_alarm"]["triggered"])
        final, _ = pack(p.run_dir)
        self.assertNotIn("BIAS ALARM", (final / "REPORT.md").read_text())


class BenchmarkAndJournal(Base):
    def cfg(self, **override):
        return make_cfg(
            experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1",
                          "hypothesis": "H-test: a wider net learns better", "train_args": ["--hidden", "64,32"]}],
            rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
            benchmark={"references": ["NETWORK_V2"], "games": 4, "concurrency": 2, "timeout_seconds": 60,
                       "min_win_rate": 0}, **override)

    def test_slow_opponents_play_only_the_best_model_once_at_the_end(self):
        log = self.tmp / "calls.log"
        os.environ["FAKE_ARGS_LOG"] = str(log)
        p = self.pipeline(self.cfg(max_iterations_per_experiment=2))
        self.assertEqual("finished", p.run())
        calls = [json.loads(line) for line in log.read_text().splitlines()]
        self.assertFalse(any("NETWORK_V2" in c.get("matchups", "") for c in calls), "never a source of training data")
        v2 = [c for c in calls if c.get("b") == "NETWORK_V2"]
        self.assertEqual(1, len(v2), "once per run, not per iteration")
        best = p.state["experiments"]["a"]["best_model"]
        self.assertEqual(Path(best), Path(v2[0].get("model") or v2[0].get("models")))
        self.assertEqual("4", v2[0]["games"])
        (result,) = p.state["benchmark"].values()
        self.assertNotIn("failed", result)
        final, _ = pack(p.run_dir)
        self.assertIn("vs NETWORK_V2", (final / "REPORT.md").read_text(encoding="utf-8"))

    def test_benchmark_is_skipped_until_a_fast_yardstick_is_beaten(self):
        log = self.tmp / "calls.log"
        os.environ["FAKE_ARGS_LOG"] = str(log)
        os.environ["FAKE_SCORES"] = "RANDOM:0.40,SMART:0.05"
        cfg = self.cfg()
        cfg["benchmark"] = {"references": ["NETWORK"], "games": 4, "concurrency": 2, "timeout_seconds": 60,
                            "gate_refs": ["RANDOM", "SMART"], "min_win_rate": 0.5}
        p = self.pipeline(cfg)
        self.assertEqual("finished", p.run())
        self.assertFalse(any(c.get("b") == "NETWORK" for c in (json.loads(l) for l in log.read_text().splitlines())))
        self.assertIn("skipped", p.state["benchmark"])
        self.assertIn("need ≥50%", p.state["benchmark"]["skipped"]["reason"])

    def test_a_failing_benchmark_is_reported_but_does_not_fail_the_run(self):
        p = self.pipeline(self.cfg())
        p.cfg["benchmark"]["references"] = ["NETWORK_V2"]
        real = p.compare_once

        def broken(path, log_path, a, b, *rest):
            return nightly.Result(1, "exit", 0) if b == "NETWORK_V2" else real(path, log_path, a, b, *rest)
        p.compare_once = broken
        self.assertEqual("finished", p.run())
        (result,) = p.state["benchmark"].values()
        self.assertEqual("exit 1 (exit)", result["failed"])

    def test_every_run_leaves_its_hypotheses_and_results_in_the_journal(self):
        p = self.pipeline(self.cfg())
        write_json(p.run_dir / "config.json", p.cfg)
        self.assertEqual("finished", p.run())
        self.assertIn("--hidden 64,32", (p.it_dir("a", 1) / "train.log").read_text(), "experiment flags reach the trainer")
        final, _ = pack(p.run_dir)
        entry = read_json(final / "journal.json")
        (exp,) = entry["experiments"]
        self.assertEqual("H-test: a wider net learns better", exp["hypothesis"])
        self.assertEqual(["--hidden", "64,32"], exp["train_args"])
        self.assertIsNotNone(exp["best_score"])
        self.assertIn("SMART", exp["best_eval"])
        self.assertIn("H-test: a wider net learns better", (final / "REPORT.md").read_text(encoding="utf-8"))
        # the journal next to all runs keeps one line per run: packing again replaces it
        pack(p.run_dir)
        journal = (p.run_dir.parent / "JOURNAL.jsonl").read_text(encoding="utf-8").splitlines()
        self.assertEqual(["t"], [json.loads(line)["run"] for line in journal])


class MavenFailure(unittest.TestCase):
    def test_the_real_error_is_shown_not_the_jdk_warnings_after_it(self):
        output = "\n".join(["[INFO] Scanning", "[ERROR] COMPILATION ERROR :",
                            "[ERROR] /x/Foo.java:[3,1] cannot find symbol",
                            "[ERROR] Failed to execute goal maven-compiler-plugin"]
                           + ["WARNING: sun.misc.Unsafe::objectFieldOffset will be removed"] * 30)
        summary = nightly.maven_failure_summary(output)
        self.assertIn("cannot find symbol", summary)
        self.assertNotIn("Unsafe", summary)

    def test_without_error_lines_the_tail_without_warnings_is_shown(self):
        summary = nightly.maven_failure_summary("something broke\nWARNING: noise\nWARNING: noise")
        self.assertEqual("something broke", summary)


class DataQualityReport(unittest.TestCase):
    def entry(self, **pool):
        base = {"train_games": 24000, "val_games": 2600, "rows_per_game_mean": 140.0, "rows_per_game_cap": 32,
                "label_mean": 0.5, "pool_gb": 5.1}
        base.update(pool)
        return {"experiments": [{"name": "a", "matchups": "AGENT:AGENT:1", "best_training": {"pool": base}},
                                {"name": "b", "matchups": "AGENT:AGENT:1"}]}

    def test_good_data_has_no_warnings(self):
        text = "\n".join(nightly.data_quality_lines(self.entry(), {"selfplay": {"max_rows_per_game_type": 32}}))
        self.assertIn("| a | AGENT:AGENT:1 | 24000 | 2600 | 140.0 (32) | 0.500 | 5.1 | ok |", text)
        self.assertIn("no trained model", text)

    def test_plain_defects_are_flagged_but_the_number_of_games_is_left_to_the_evidence(self):
        text = "\n".join(nightly.data_quality_lines(self.entry(train_games=4000, label_mean=0.1, rows_per_game_cap=0), {}))
        self.assertIn("labels unbalanced", text)
        self.assertIn("no cap on rows per game", text)
        self.assertNotIn("4000 games", text, "how many games are enough is a hypothesis, not a rule")

    def test_every_iteration_is_evidence(self):
        entry = self.entry()
        entry["experiments"][0]["iterations"] = [
            {"iteration": 1, "train_games": 1800, "rows_per_game_mean": 150.0, "feed": "mixed", "best_epoch": 2, "val_gap": 0.031, "score": 0.01},
            {"iteration": 2, "train_games": 9000, "rows_per_game_mean": 140.0, "feed": "mixed", "best_epoch": 6, "val_gap": -0.012, "score": 0.08}]
        text = "\n".join(nightly.data_quality_lines(entry, {}))
        self.assertIn("| a | 1 | 1800 | 150.0 | mixed | 2 | +0.0310 | +0.010 |", text)
        self.assertIn("| a | 2 | 9000 | 140.0 | mixed | 6 | -0.0120 | +0.080 |", text)


class StartFromAnEarlierRun(Base):
    def test_the_best_model_of_the_previous_night_is_iteration_zero_of_the_next(self):
        first = self.pipeline(name="night1")
        self.assertEqual("finished", first.run())
        previous_best = Path(first.state["experiments"]["a"]["best_model"])

        log = self.tmp / "calls.log"
        os.environ["FAKE_ARGS_LOG"] = str(log)
        cfg = make_cfg(experiments=[{"name": "a", "games_per_iteration": 30, "matchups": "AGENT:AGENT:1", "start_model": "night1"}],
                       rungs=[{"fraction": 1.0, "keep": 1, "games_factor": 1.0}])
        second = self.pipeline(cfg, name="night2")
        e = second.state["experiments"]["a"]
        self.assertEqual(previous_best, Path(e["start_model"]))
        start_copy = second.it_dir("a", 0) / "model"
        self.assertTrue((start_copy / "DONE").exists(), "copied: the new run does not depend on the old folder")
        self.assertEqual("finished", second.run())

        calls = [json.loads(line) for line in log.read_text().splitlines()]
        first_play = next(c for c in calls if c.get("mode") == "play")
        self.assertEqual(start_copy, Path(first_play.get("model") or first_play.get("models")), "the first self-play uses it")
        self.assertTrue((second.it_dir("a", 0) / "eval.json").exists(), "it is evaluated, so new models have to beat it")
        self.assertNotIn("start_pending_eval", e)

    def test_an_unknown_start_model_is_refused(self):
        with self.assertRaises(ValueError):
            nightly.resolve_start_model("no_such_night", self.tmp / "runs")


if __name__ == "__main__":
    unittest.main()
