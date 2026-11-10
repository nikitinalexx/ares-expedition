"""End to end with the REAL Java process: start, kill the whole process tree in the middle (like a power loss), start again.

Slow (about 3 minutes) and needs a built project (target/classes, target/cp.txt: `python ml/nightly.py check` builds them), so it only runs when asked:

    set RUN_E2E=1  &&  python -m unittest ml.tests.test_e2e_real -v

Environment: JAVA_BIN (default: java), MVN_ARGS is not used here (the build must exist).
"""
import json
import os
import platform
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

ML = Path(__file__).resolve().parents[1]
ROOT = ML.parent
sys.path.insert(0, str(ML))

from nightly import chunk_files, count_games, read_json  # noqa: E402


@unittest.skipUnless(os.environ.get("RUN_E2E") == "1", "slow end to end test: set RUN_E2E=1")
class RealEndToEnd(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        self.tmp = Path(self._tmp.name)
        self.config = self.tmp / "config.json"
        self.config.write_text(json.dumps({
            "hours": 0.06,
            "storage": {"run_root": str(self.tmp / "runs"), "data_root": str(self.tmp / "data"), "temp_root": str(self.tmp / "tmp"),
                        "default_reserve_gb": 1, "data_budget_gb": 5, "keep_data_iterations": 2, "guard_interval_seconds": 2},
            "java": {"bin": os.environ.get("JAVA_BIN", "java"), "xmx": "2g", "threads": 4},
            "retries": 2, "retry_backoff_seconds": [1, 2],
            "selfplay": {"games_per_chunk": 20, "stage_max_seconds": 120},
            "eval": {"games": 20, "references": ["SMART"], "concurrency": 8},
            "rungs": [{"fraction": 1.0, "keep": 1, "games_factor": 1.0}],
            "experiments": [{"name": "a", "games_per_iteration": 400, "matchups": "AGENT:AGENT:1,AGENT_R:AGENT:1"}],
            "max_iterations_per_experiment": 1,
        }))

    def tearDown(self):
        self._tmp.cleanup()

    def start(self, name="e2e"):
        log = open(self.tmp / f"orchestrator_{time.time_ns()}.out", "wb")
        flags = subprocess.CREATE_NEW_PROCESS_GROUP if platform.system() == "Windows" else 0
        return subprocess.Popen([sys.executable, str(ML / "nightly.py"), "run", "--config", str(self.config), "--name", name, "--skip-check"],
                                cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, creationflags=flags)

    @staticmethod
    def kill_tree(proc):
        if platform.system() == "Windows":
            subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
        else:
            proc.kill()
        proc.wait()

    def test_kill_in_the_middle_then_resume_with_the_same_command(self):
        data = self.tmp / "data" / "e2e" / "data" / "a" / "it_0001"
        proc = self.start()
        deadline = time.time() + 180
        while time.time() < deadline and len(chunk_files(data)) < 2:
            self.assertIsNone(proc.poll(), "the pipeline ended before it could be killed")
            time.sleep(0.5)
        self.assertGreaterEqual(len(chunk_files(data)), 2, "no chunk appeared in time")

        self.kill_tree(proc)  # the power goes off
        time.sleep(1)

        # what the killed run left behind must be readable
        state = read_json(self.tmp / "runs" / "e2e" / "state.json")
        self.assertEqual("running", state["status"])
        survived = {c.name: c.stat().st_size for c in chunk_files(data)}
        self.assertGreaterEqual(len(survived), 2)
        games_before = count_games(data)

        second = self.start()
        try:
            self.assertEqual(0, second.wait(timeout=600), (self.tmp / "runs" / "e2e" / "log.txt").read_text()[-3000:])
        finally:
            if second.poll() is None:
                self.kill_tree(second)

        state = read_json(self.tmp / "runs" / "e2e" / "state.json")
        self.assertEqual("finished", state["status"])
        self.assertEqual(1, state["experiments"]["a"]["iterations_done"])
        self.assertGreaterEqual(count_games(data), 400)
        self.assertGreaterEqual(count_games(data), games_before)
        for name, size in survived.items():
            self.assertEqual(size, (data / name).stat().st_size, f"{name} survived the kill and must not be rewritten")
        self.assertEqual([], list(data.glob("*.tmp")), "leftovers of the dead process are cleaned")
        self.assertFalse((self.tmp / "runs" / "e2e" / "run.lock").exists(), "the lock is released")
        packs = list((self.tmp / "runs" / "e2e" / "handoff").glob("*.zip"))
        self.assertTrue(packs, "the result for the morning exists")

    def test_a_second_start_while_running_is_refused(self):
        first = self.start("lock")
        try:
            deadline = time.time() + 60
            while time.time() < deadline and not (self.tmp / "runs" / "lock" / "run.lock").exists():
                time.sleep(0.2)
            second = self.start("lock")
            self.assertEqual(1, second.wait(timeout=60))
            self.assertIn("already running", "".join(p.read_text(errors="replace") for p in self.tmp.glob("orchestrator_*.out")))
        finally:
            self.kill_tree(first)


if __name__ == "__main__":
    unittest.main()
