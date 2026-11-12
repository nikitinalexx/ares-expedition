#!/usr/bin/env python
"""Night pipeline: ONE command, safe to kill and to restart at any moment, keeps everything that was finished.

    python ml/nightly.py check  --config ml/nightly_config.json            # preflight + a tiny end-to-end run (do this before going to bed)
    python ml/nightly.py run    --config ml/nightly_config.json --name n1  # start; run the same command again to resume
    python ml/nightly.py status --name n1                                  # what is going on
    python ml/nightly.py stop   --name n1                                  # graceful stop (Ctrl+C in the console does the same)
    python ml/nightly.py pack   --name n1                                  # (re)build the folder to hand over; done automatically on stop/end

Design (see docs/NIGHTLY.md):
  * The unit of work is a stage of an iteration: selfplay (Java process, finished CHUNK files), train, eval. A stage that has a
    ".done" marker is never repeated; a half finished selfplay stage continues after the chunks that exist.
  * Several experiments ("theories") share the night: short probes, then the worse ones are dropped (successive halving).
  * Every failure is written to errors/ with the command, the exit code and the tail of the log, then retried; an experiment
    that keeps failing is dropped, the others go on.
  * state.json / status.json are replaced atomically, so a power loss never leaves a half written state.
"""
import argparse
import datetime
import json
import math
import os
import platform
import re
import shutil
import signal
import subprocess
import sys
import time
import traceback
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))

EXIT_OK = 0
EXIT_FAILED = 1
EXIT_BAD_CHECK = 2
EXIT_STOPPED = 3
EXIT_DISK = 5


# ------------------------------------------------------------------------------------------------ small utilities

def now():
    return time.time()


def stamp(t=None):
    return datetime.datetime.fromtimestamp(t or now()).strftime("%Y-%m-%d %H:%M:%S")


def write_json(path, obj):
    """Atomic replace: readers see either the old or the new content, never half of it."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp")
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, sort_keys=True)
        f.flush()
        os.fsync(f.fileno())
    replace(tmp, path)


def replace(src, dst, attempts=20, pause=0.5):
    """os.replace that survives Windows: an antivirus / indexer / reader may hold the target for a moment (WinError 5)."""
    for attempt in range(attempts):
        try:
            os.replace(src, dst)
            return
        except PermissionError:
            if attempt == attempts - 1:
                raise
            time.sleep(pause)


def read_json(path, default=None):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default
    except json.JSONDecodeError:
        backup = Path(str(path) + ".bak")
        if backup.exists():
            with open(backup, encoding="utf-8") as f:
                return json.load(f)
        raise


def free_gb(path):
    Path(path).mkdir(parents=True, exist_ok=True)
    return shutil.disk_usage(path).free / 1e9


def dir_size_gb(path):
    total = 0
    for p in Path(path).rglob("*"):
        try:
            if p.is_file():
                total += p.stat().st_size
        except OSError:
            pass
    return total / 1e9


def tail(path, lines=200):
    try:
        with open(path, "rb") as f:
            data = f.read()[-200_000:]
        return "\n".join(data.decode("utf-8", "replace").splitlines()[-lines:])
    except OSError:
        return ""


def kill_tree(proc):
    """Kills the process and everything it started (a training script may have workers)."""
    try:
        if platform.system() == "Windows":
            subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
        else:
            proc.kill()
    except Exception:
        pass


class StopRequested(Exception):
    pass


class EnvironmentBroken(Exception):
    pass


class AlreadyRunning(Exception):
    pass


def pid_alive(pid):
    """Is a process with this id running (works on Windows without extra packages)?"""
    if pid <= 0:
        return False
    if platform.system() == "Windows":
        import ctypes
        handle = ctypes.windll.kernel32.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
        if not handle:
            return False
        code = ctypes.c_ulong()
        ctypes.windll.kernel32.GetExitCodeProcess(handle, ctypes.byref(code))
        ctypes.windll.kernel32.CloseHandle(handle)
        return code.value == 259  # STILL_ACTIVE
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


class RunLock:
    """Two pipelines on one run would corrupt each other: the second start is refused while the first one is alive."""

    def __init__(self, path):
        self.path = Path(path)

    def acquire(self):
        info = read_json(self.path, None) if self.path.exists() else None
        if info and info.get("pid") != os.getpid() and pid_alive(info.get("pid", 0)) and now() - info.get("time", 0) < 6 * 3600:
            raise AlreadyRunning(f"run is already running (pid {info['pid']}, started {stamp(info.get('started', 0))}); "
                                 f"stop it first (`stop`) or wait. If you are sure it is dead, delete {self.path}")
        write_json(self.path, {"pid": os.getpid(), "started": now(), "time": now()})

    def touch(self):
        info = read_json(self.path, None) or {"pid": os.getpid(), "started": now()}
        if info.get("pid") == os.getpid():
            info["time"] = now()
            write_json(self.path, info)

    def release(self):
        info = read_json(self.path, None) if self.path.exists() else None
        if info and info.get("pid") == os.getpid():
            try:
                self.path.unlink()
            except OSError:
                pass


class Stop:
    """Stop flag: a file in the run folder (the `stop` command) or Ctrl+C."""

    def __init__(self, flag_file):
        self.flag_file = Path(flag_file)
        self.requested = False

    def set(self):
        self.requested = True

    def is_set(self):
        return self.requested or self.flag_file.exists()

    def check(self):
        if self.is_set():
            raise StopRequested()


# ------------------------------------------------------------------------------------------------ running commands

class Result:
    def __init__(self, code, reason, seconds):
        self.code, self.reason, self.seconds = code, reason, seconds

    @property
    def ok(self):
        return self.code == 0 and self.reason == "exit"


def run_command(cmd, log_path, timeout, stop, cwd=ROOT, env=None, graceful_file=None, grace=90, poll=0.5, guard=None, guard_interval=5, tick=None, low_priority=False):
    """Runs a command with its output appended to log_path.

    A stop request makes a command that supports it (graceful_file) finish by itself; otherwise it is terminated.
    Returns Result(code, reason in {exit, timeout, stopped}, seconds).
    """
    Path(log_path).parent.mkdir(parents=True, exist_ok=True)
    started = now()
    with open(log_path, "ab") as log:
        log.write(f"\n===== {stamp()} $ {' '.join(map(str, cmd))}\n".encode())
        log.flush()
        try:
            extra = {}
            if low_priority:  # the machine stays usable while the pipeline works
                if platform.system() == "Windows":
                    extra["creationflags"] = subprocess.BELOW_NORMAL_PRIORITY_CLASS
                else:
                    extra["preexec_fn"] = lambda: os.nice(10)
            proc = subprocess.Popen([str(c) for c in cmd], cwd=str(cwd), stdout=log, stderr=subprocess.STDOUT, env=env, **extra)
        except OSError as e:
            log.write(f"COULD NOT START: {e}\n".encode())
            return Result(127, "exit", now() - started)
        stop_seen_at = None
        guard_reason = None
        last_guard = now()
        last_tick = now()
        while True:
            code = proc.poll()
            if code is not None:
                log.write(f"===== exit code {code} after {now() - started:.0f}s\n".encode())
                reason = "disk" if guard_reason else ("stopped" if stop_seen_at else "exit")
                return Result(code, reason, now() - started)
            if tick is not None and now() - last_tick >= 30:
                last_tick = now()
                tick()
            if guard is not None and guard_reason is None and now() - last_guard >= guard_interval:
                last_guard = now()
                guard_reason = guard()
                if guard_reason:
                    log.write(f"===== guard: {guard_reason}; asking the command to finish\n".encode())
                    log.flush()
                    stop_seen_at = now()
                    if graceful_file is not None:
                        Path(graceful_file).write_text("stop")
            if guard_reason and (graceful_file is None or now() - stop_seen_at > grace):
                kill_tree(proc)
                proc.wait()
                log.write(b"===== killed by the guard\n")
                return Result(-3, "disk", now() - started)
            if timeout and now() - started > timeout:
                kill_tree(proc)
                proc.wait()
                log.write(f"===== killed: timeout {timeout}s\n".encode())
                return Result(-1, "timeout", now() - started)
            if stop.is_set():
                if stop_seen_at is None:
                    stop_seen_at = now()
                    if graceful_file is not None:
                        Path(graceful_file).write_text("stop")
                if graceful_file is None or now() - stop_seen_at > grace:
                    kill_tree(proc)
                    proc.wait()
                    log.write(b"===== killed: stop requested\n")
                    return Result(-2, "stopped", now() - started)
            time.sleep(poll)


# ------------------------------------------------------------------------------------------------ Java discovery

def java_major(java_bin):
    """Major version of a java executable or None if it does not run."""
    try:
        out = subprocess.run([str(java_bin), "-version"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return None
    match = re.search(r'version "(\d+)', (out.stderr or "") + (out.stdout or ""))
    return int(match.group(1)) if match else None


def find_java(configured="auto", minimum=21):
    """The first java >= minimum: the configured one, JAVA_HOME, PATH, then the usual JDK folders (IntelliJ keeps its JDKs in ~/.jdks).

    Returns (path, major) or (None, None). A Java 11 on PATH (common) is skipped instead of failing later with a confusing error."""
    exe = "java.exe" if platform.system() == "Windows" else "java"
    candidates = []
    if configured and configured != "auto":
        candidates.append(configured)
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]) / "bin" / exe)
        candidates.append(Path(os.environ["JAVA_HOME"]) / exe)  # a common mistake: JAVA_HOME set to ...\bin
    on_path = shutil.which("java")
    if on_path:
        candidates.append(on_path)
    roots = [Path.home() / ".jdks"]
    for env in ("ProgramFiles", "ProgramFiles(x86)"):
        base = os.environ.get(env)
        if base:
            roots += [Path(base) / "Java", Path(base) / "Eclipse Adoptium", Path(base) / "Microsoft", Path(base) / "Amazon Corretto",
                      Path(base) / "BellSoft", Path(base) / "Zulu"]
    for root in roots:
        if root.exists():
            candidates += sorted(root.glob(f"*/bin/{exe}"), reverse=True)
    seen = set()
    for candidate in candidates:
        key = str(candidate)
        if key in seen:
            continue
        seen.add(key)
        major = java_major(candidate)
        if major and major >= minimum:
            return str(candidate), major
    return None, None


def java_home_of(java_bin):
    """JAVA_HOME for Maven, as the JVM itself reports it. The folder above bin/ is wrong for launcher stubs such as
    C:\\Program Files\\Common Files\\Oracle\\Java\\javapath\\java.exe, which Oracle's installer puts on PATH."""
    try:
        r = subprocess.run([str(java_bin), "-XshowSettings:properties", "-version"], capture_output=True, text=True, timeout=60)
        for line in (r.stderr + r.stdout).splitlines():
            if line.strip().startswith("java.home ="):
                home = line.split("=", 1)[1].strip()
                if is_java_home(home):
                    return home
    except (OSError, subprocess.SubprocessError):
        pass
    return str(Path(java_bin).resolve().parent.parent)


def maven_failure_summary(output, limit=15):
    """The [ERROR] lines of a failed Maven run. New JDKs print pages of WARNINGs about Maven's own libraries (Unsafe, final field
    mutation) that would push the real error out of a tail."""
    lines = [l.rstrip() for l in output.splitlines() if l.strip()]
    errors = [l for l in lines if l.lstrip().startswith(("[ERROR]", "ERROR", "Error:", "Exception", "Caused by"))
              or ("error:" in l and ".java" in l)]
    if not errors:
        errors = [l for l in lines if not l.lstrip().startswith("WARNING")][-limit:]
    return "\n      ".join(errors[:limit]) or "(no output)"


def is_java_home(path):
    exe = "java.exe" if platform.system() == "Windows" else "java"
    return bool(path) and (Path(path) / "bin" / exe).exists()


# ------------------------------------------------------------------------------------------------ configuration

JAVA_EXIT_OUT_OF_MEMORY = 6  # SelfPlayMain / FatalErrors

DEFAULT_CONFIG = {
    # null/0 = run until stop/Ctrl+C/disk (default). A positive number is an optional timed budget only.
    "hours": None,
    "python": sys.executable,
    "java": {"bin": "auto", "xmx": "8g", "threads": 0, "classes": "target/classes", "classpath_file": "target/cp.txt"},
    "maven": {"command": "mvn", "args": []},
    # Where things go and how much disk may never be touched. Put run_root/data_root/temp_root on the big disk (D:);
    # `reserves` are checked on EVERY listed volume (also the ones we do not write to, e.g. the system SSD C:) before each
    # stage and every few seconds while a stage runs: below the reserve everything stops gracefully and the results are kept.
    "storage": {
        "run_root": "runs",          # state, logs, models, hand-over folder (small)
        "data_root": None,           # decision chunks (big); default: inside the run folder
        "temp_root": None,           # TMP/TEMP of the child processes; default: inside the run folder
        # Optional SSD stage: self-play writes here, finished .ares are moved to data_root (HDD). Keeps the write path
        # off a slow disk; respects the volume reserve of stage_root. None = write directly to data_root.
        "stage_root": None,
        "reserves": [],              # [{"path": "C:/", "gb": 20}, {"path": "D:/", "gb": 30}]
        "default_reserve_gb": 30,    # for run_root, data_root and temp_root when they are not listed
        "data_budget_gb": 150,       # this run never keeps more than this much training data
        "keep_data_iterations": 3,   # training window: the data of older iterations is deleted (models never)
        # a NEW run deletes the self-play data of earlier runs (not running ones); their models, evaluations, logs stay
        "delete_data_of_other_runs": True,
        "guard_interval_seconds": 5,
    },
    "retries": 3,
    "retry_backoff_seconds": [10, 60, 300],
    "oom_restarts": 20,                       # Java exit 6 (out of memory) = restart the stage at once, not a failure
    "max_consecutive_failures": 3,
    # max_rows_per_game_type: decisions of one game written per type (0 = all); how many are useful is hypothesis H-D3
    "selfplay": {"temperature": 0.5, "games_per_chunk": 500, "stage_max_seconds": 3600, "grace_seconds": 90, "max_failure_ratio": 0.05,
                 "max_rows_per_game_type": 64},
    "train": {"command": ["{python}", "ml/train_all.py", "--data", "{data}", "--out", "{model_dir}", "--prev", "{prev_model}", "--epochs", "3"], "timeout_seconds": 7200},
    "eval": {"games": 200, "references": ["SMART", "RANDOM"], "agent": "AGENT", "concurrency": 32, "timeout_seconds": 1800, "bias_alarm_gap": 0.10},
    # slow opponents (NETWORK…): once at end, best model only; never training data. Skipped until the best model’s
    # win rate vs a gate_ref reaches min_win_rate (skill gap is huge — 40% vs RANDOM still means ~0% vs NETWORK).
    "benchmark": {"references": [], "games": 100, "concurrency": 16, "timeout_seconds": 3600,
                  "gate_refs": ["RANDOM", "SMART"], "min_win_rate": 0.5},
    # successive halving. With a timed budget, `fraction` splits active time. With unlimited time, early rungs advance after
    # `min_iterations` each (default 1); the last rung runs until stop. `keep` best continue.
    "rungs": [{"fraction": 0.3, "keep": 2, "games_factor": 0.25, "min_iterations": 1},
              {"fraction": 0.7, "keep": 1, "games_factor": 1.0}],
    "experiments": [{"name": "default", "games_per_iteration": 2000, "matchups": "AGENT:AGENT:1"}],
    "low_priority": True,                    # child processes run below normal priority: you can work on the machine meanwhile
    "promote_margin": 0.0,
    # Per-experiment early stop, independent of the rung schedule: an experiment that has not beaten its own best score
    # for this many of its OWN iterations in a row is marked "plateaued" and stops (its slot/data go to the others
    # straight away, instead of occupying a fixed time share for a result that is already in). 0/None disables it.
    # Needs at least plateau_min_iterations finished first (score at 1-2 iterations is too noisy to call a plateau).
    # Per-experiment override: put the same two keys inside that experiment's entry.
    "plateau_patience": 5,
    "plateau_min_iterations": 3,
    "max_failures_in_a_row": 8,              # that many failed attempts without any success = the environment is broken, stop
    "max_iterations_per_experiment": None,   # for tests / the smoke run: stop an experiment after this many iterations
}


def deep_merge(base, override):
    result = json.loads(json.dumps(base))
    for key, value in (override or {}).items():
        if isinstance(value, dict) and isinstance(result.get(key), dict):
            result[key] = deep_merge(result[key], value)
        else:
            result[key] = value
    return result


def load_config(path):
    user = read_json(path, None) if path else {}
    if user is None:
        raise SystemExit(f"config file not found: {path}")
    cfg = deep_merge(DEFAULT_CONFIG, user)
    names = [e["name"] for e in cfg["experiments"]]
    if len(set(names)) != len(names):
        raise SystemExit(f"experiment names must be unique: {names}")
    if budget_seconds(cfg) is not None:
        fractions = sum(r.get("fraction") or 0 for r in cfg["rungs"])
        if not 0.99 <= fractions <= 1.01:
            raise SystemExit(f"rung fractions must add up to 1 when hours is set, got {fractions}")
    return cfg


def budget_seconds(cfg):
    """None = unlimited (run until stop). Positive hours → seconds."""
    h = cfg.get("hours")
    if h is None or h <= 0:
        return None
    return float(h) * 3600


def budget_label(cfg_or_seconds):
    if isinstance(cfg_or_seconds, dict):
        cfg_or_seconds = budget_seconds(cfg_or_seconds)
    return "until stop" if cfg_or_seconds is None else f"{cfg_or_seconds / 3600:.2f} h"


# ------------------------------------------------------------------------------------------------ the pipeline

class DiskLow(Exception):
    pass


def volume_key(path):
    p = Path(path)
    while not p.exists() and p != p.parent:
        p = p.parent
    return os.stat(p).st_dev


class Storage:
    """Knows the reserves of every volume and what may be deleted. Nothing here deletes outside <data_root>/data."""

    def __init__(self, cfg, run_dir, data_root, temp_root):
        s = cfg["storage"]
        self.keep = s["keep_data_iterations"]
        self.budget_gb = s["data_budget_gb"]
        self.interval = s["guard_interval_seconds"]
        self.data_path = Path(data_root) / "data"
        self.temp_root = Path(temp_root)
        stage = s.get("stage_root")
        self.stage_root = (Path(stage) if Path(stage).is_absolute() else ROOT / stage) if stage else None
        reserves = {}
        for entry in s["reserves"]:
            reserves[str(entry["path"])] = max(entry["gb"], reserves.get(str(entry["path"]), 0))
        for path in (run_dir, data_root, temp_root, *((self.stage_root,) if self.stage_root else ())):
            reserves.setdefault(str(path), s["default_reserve_gb"])
        self.reserves = [(Path(p), gb) for p, gb in reserves.items()]
        self.rotate = None

    def violations(self):
        found = []
        for path, reserve in self.reserves:
            try:
                free = free_gb(path)
            except OSError as e:
                found.append(f"{path}: cannot read the free space ({e})")
                continue
            if free < reserve:
                found.append(f"{path}: {free:.1f} GB free, the reserve is {reserve} GB")
        return found

    def data_gb(self):
        return dir_size_gb(self.data_path) if self.data_path.exists() else 0.0

    def over_budget(self):
        return self.data_gb() > self.budget_gb

    def guard(self):
        """Cheap check used while a command runs. Returns a message when the command must stop.
        While self-play writes (and only then: a trainer may be reading the window), `rotate` frees space instead of stopping."""
        if self.rotate is not None and (self.violations() or self.over_budget()):
            self.rotate()
        found = self.violations()
        if found:
            return "disk reserve: " + "; ".join(found)
        if self.over_budget():
            return f"data of the run is {self.data_gb():.1f} GB, the budget is {self.budget_gb} GB"
        return None

    def delete_data_dir(self, directory):
        directory = Path(directory).resolve()
        if self.data_path.resolve() not in directory.parents or not directory.name.startswith("it_"):
            raise RuntimeError(f"refusing to delete {directory}: not an iteration folder inside {self.data_path}")
        shutil.rmtree(directory, ignore_errors=True)

    def child_env(self):
        self.temp_root.mkdir(parents=True, exist_ok=True)
        env = dict(os.environ)
        for key in ("TMP", "TEMP", "TMPDIR"):
            env[key] = str(self.temp_root)
        return env


class Pipeline:
    def __init__(self, cfg, run_dir, data_root=None, stop=None, quiet=False, temp_root=None):
        self.cfg = cfg
        self.run_dir = Path(run_dir)
        self.data_root = Path(data_root) if data_root else self.run_dir
        self.storage = Storage(cfg, self.run_dir, self.data_root, temp_root or (self.run_dir / "tmp"))
        self.stop = stop or Stop(self.run_dir / "STOP")
        self.quiet = quiet
        self.state_path = self.run_dir / "state.json"
        self.state = read_json(self.state_path, None)
        self.current = {}
        self.last_error = None
        self.lock = RunLock(self.run_dir / "run.lock")
        self.session_start = now()
        self.active_base = (self.state or {}).get("active_seconds", 0.0)
        self.failed_attempts_in_a_row = 0

    # ---- logging / status
    def log(self, message):
        line = f"{stamp()} {message}"
        if not self.quiet:
            print(line, flush=True)
        self.run_dir.mkdir(parents=True, exist_ok=True)
        with open(self.run_dir / "log.txt", "a", encoding="utf-8") as f:
            f.write(line + "\n")

    def elapsed(self):
        """Seconds this run has really been working (all sessions); the time between a stop and a restart does not count."""
        return self.active_base + (now() - self.session_start)

    def budget(self):
        b = self.state.get("budget_seconds")
        return math.inf if b is None else b

    def has_time_budget(self):
        return self.state.get("budget_seconds") is not None

    def save_state(self):
        if self.state is not None:
            self.state["active_seconds"] = round(self.elapsed(), 1)
        if self.state_path.exists():
            try:
                shutil.copyfile(self.state_path, str(self.state_path) + ".bak")
            except OSError:
                pass
        write_json(self.state_path, self.state)

    def tick(self):
        self.lock.touch()
        self.save_state()
        self.write_status()

    def write_status(self, **extra):
        st = self.state or {}
        status = {
            "updated": stamp(),
            "run": st.get("name"),
            "state": st.get("status"),
            "started": stamp(st["started"]) if st.get("started") else None,
            "worked_hours": round(self.elapsed() / 3600, 2),
            "budget_hours": None if st.get("budget_seconds") is None else round(st["budget_seconds"] / 3600, 2),
            "remaining_hours": (None if st.get("budget_seconds") is None
                                else round((st["budget_seconds"] - self.elapsed()) / 3600, 2)),
            "finishes_at_if_running": (None if st.get("budget_seconds") is None
                                       else stamp(now() + st["budget_seconds"] - self.elapsed())),
            "runs_until": "stop" if st.get("budget_seconds") is None else "budget",
            "rung": st.get("rung"),
            "current": self.current,
            "free_disk_gb": {str(p): round(free_gb(p), 1) for p, _ in self.storage.reserves},
            "data_gb": round(self.storage.data_gb(), 1),
            "last_error": self.last_error,
            "oom_restarts": len(st.get("oom_restarts") or []),
            "window_shrinks": len(st.get("window_shrinks") or []),
            "experiments": {
                name: {k: e.get(k) for k in ("state", "iterations_done", "best_score", "scores", "failures", "iterations_since_best")}
                for name, e in (st.get("experiments") or {}).items()
            },
        }
        status.update(extra)
        write_json(self.run_dir / "status.json", status)

    # ---- paths
    def exp_dir(self, exp):
        return self.run_dir / "exp" / exp

    def it_dir(self, exp, it):
        return self.exp_dir(exp) / f"it_{it:04d}"

    def data_dir(self, exp, it):
        return self.data_root / "data" / exp / f"it_{it:04d}"

    def stage_dir(self, exp, it):
        """Where Java writes during self-play (SSD stage if configured, else the final data dir)."""
        if not self.storage.stage_root:
            return self.data_dir(exp, it)
        return self.storage.stage_root / self.state["name"] / exp / f"it_{it:04d}"

    # ---- commands
    def java_args_file(self):
        return self.run_dir / "java.args"

    def prepare_java(self):
        j = self.cfg["java"]
        if j["bin"] == "auto" or not Path(j["bin"]).exists() and not shutil.which(j["bin"]):
            found, _ = find_java(j["bin"])
            if not found:
                raise RuntimeError("no Java 21 or newer found (looked in java.bin, JAVA_HOME, PATH, ~/.jdks, Program Files); set java.bin in the config")
            j["bin"] = found
        cp_file = ROOT / j["classpath_file"]
        if not cp_file.exists():
            raise RuntimeError(f"classpath file {cp_file} not found; run `python ml/nightly.py check` (it builds it)")
        classes = (ROOT / j["classes"]).as_posix()
        classpath = cp_file.read_text().strip().replace("\\", "/")
        sep = ";" if platform.system() == "Windows" else ":"
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.java_args_file().write_text(f'-cp\n"{classes}{sep}{classpath}"\n', encoding="utf-8")

    def java_cmd(self, *args):
        j = self.cfg["java"]
        return [j["bin"], f"-Xmx{j['xmx']}", f"@{self.java_args_file().as_posix()}",
                "com.terraforming.ares.selfplay.SelfPlayMain", *args]

    def template(self, parts, **values):
        values.setdefault("python", self.cfg["python"])
        return [str(p).format(**values) for p in parts]

    # ---- failures
    def record_failure(self, exp, it, stage, result, log_path, extra=""):
        (self.run_dir / "errors").mkdir(parents=True, exist_ok=True)
        name = f"{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}_{exp}_it{it}_{stage}.txt"
        text = (f"experiment={exp} iteration={it} stage={stage}\n"
                f"time={stamp()} exit_code={result.code} reason={result.reason} seconds={result.seconds:.0f}\n{extra}\n"
                f"----- last 200 lines of {log_path} -----\n{tail(log_path, 200)}\n")
        err_log = self.data_dir(exp, it) / "errors.log"
        if err_log.exists():
            text += f"----- selfplay errors.log (tail) -----\n{tail(err_log, 120)}\n"
        (self.run_dir / "errors" / name).write_text(text, encoding="utf-8")
        self.last_error = f"{exp}/it{it}/{stage}: exit {result.code} ({result.reason}) -> errors/{name}"
        self.log(f"FAILED {self.last_error}")
        self.failed_attempts_in_a_row += 1
        limit = self.cfg["max_failures_in_a_row"]
        if self.failed_attempts_in_a_row >= limit:
            raise EnvironmentBroken(f"{limit} failed attempts in a row without a single success - the environment is broken "
                                    f"(java, python, disk, GPU?). Last: {self.last_error}")

    def record_oom(self, exp, it, stage, log_path):
        """An out-of-memory restart does not fail the run, but it is a bug to fix: keep it visible in the morning report."""
        (self.run_dir / "warnings").mkdir(parents=True, exist_ok=True)
        name = f"{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}_{exp}_it{it}_{stage.replace(' ', '_')}_oom.txt"
        (self.run_dir / "warnings" / name).write_text(
            f"experiment={exp} iteration={it} stage={stage}\ntime={stamp()} Java ran out of memory and was restarted\n"
            f"java.xmx={self.cfg['java']['xmx']} java.threads={self.cfg['java']['threads']}\n"
            f"----- last 200 lines of {log_path} -----\n{tail(log_path, 200)}\n", encoding="utf-8")
        events = self.state.setdefault("oom_restarts", [])
        events.append({"time": stamp(), "exp": exp, "iteration": it, "stage": stage, "file": f"warnings/{name}"})
        self.save_state()
        self.log(f"OUT OF MEMORY {exp}/it{it}/{stage}: Java restarted ({len(events)} in this run) -> warnings/{name}")

    def with_retries(self, exp, it, stage, attempt):
        """attempt() -> (ok, Result, log_path). Retries with a backoff; raises StopRequested on stop."""
        retries = self.cfg["retries"]
        backoff = self.cfg["retry_backoff_seconds"]
        oom_restarts = 0
        n = 0
        while n < retries:
            self.stop.check()
            ok, result, log_path = attempt()
            if ok:
                self.failed_attempts_in_a_row = 0
                return True
            if result.code == JAVA_EXIT_OUT_OF_MEMORY and result.reason == "exit" and oom_restarts < self.cfg["oom_restarts"]:
                # finished chunks survive; the stage simply continues in a fresh JVM, it is not a failure
                oom_restarts += 1
                self.record_oom(exp, it, stage, log_path)
                continue
            if result.reason == "disk" or result.code == 5:
                self.record_failure(exp, it, stage, result, log_path, extra="not enough disk space")
                if self.make_room(exp, it) and not self.short_of_space():
                    self.log("freed space by deleting old training data, trying again")
                    n += 1
                    continue
                raise DiskLow(self.storage.guard() or "the disk is almost full")
            if result.reason == "stopped" or self.stop.is_set():
                raise StopRequested()
            self.record_failure(exp, it, stage, result, log_path, extra=f"attempt {n + 1}/{retries}")
            if n + 1 < retries:
                wait = backoff[min(n, len(backoff) - 1)]
                self.log(f"retry {n + 2}/{retries} of {exp}/it{it}/{stage} in {wait}s")
                end = now() + wait
                while now() < end:
                    self.stop.check()
                    time.sleep(min(1, max(0.0, end - now())))
            n += 1
        return False

    # ---- stages
    def stage_selfplay(self, exp, exp_cfg, it, games_target):
        done_marker = self.it_dir(exp, it) / "selfplay.done"
        if done_marker.exists():
            return True
        out = self.data_dir(exp, it)          # durable (HDD)
        stage = self.stage_dir(exp, it)      # write path (SSD if stage_root set)
        out.mkdir(parents=True, exist_ok=True)
        stage.mkdir(parents=True, exist_ok=True)
        log_path = self.it_dir(exp, it) / "selfplay.log"
        self.it_dir(exp, it).mkdir(parents=True, exist_ok=True)
        sp = self.cfg["selfplay"]
        chosen = read_json(self.it_dir(exp, it) / "selfplay_model.json", None)
        if chosen is None:
            best = self.best_model_dir(exp)
            chosen = {"model": str(best) if best else None, "temperature": exp_cfg.get("temperature", sp["temperature"])}
            write_json(self.it_dir(exp, it) / "selfplay_model.json", chosen)  # the data of one iteration comes from ONE model bundle
        model_args = java_model_args(chosen["model"], chosen["temperature"]) if chosen["model"] else []

        def attempt():
            self.check_disk()
            commit_staged_chunks(stage, out, self.log)  # leftovers of a crashed attempt
            for tmp in list(stage.glob("*.tmp")) + list(out.glob("*.tmp")):
                tmp.unlink()
            have = count_games(out)
            remaining = games_target - have
            if remaining <= 0:
                return True, Result(0, "exit", 0), log_path
            self.current = {"exp": exp, "iteration": it, "stage": "selfplay", "games_done": have, "games_target": games_target}
            self.write_status()
            stop_file = self.run_dir / f"java_stop_{exp}"
            if stop_file.exists():
                stop_file.unlink()
            write_vol = volume_key(stage)
            cmd = self.java_cmd("--mode", "play", "--out", stage.as_posix(), "--games", str(remaining),
                                "--games-per-chunk", str(sp["games_per_chunk"]), "--matchups", exp_cfg["matchups"],
                                "--max-seconds", str(sp["stage_max_seconds"]), "--stop-file", stop_file.as_posix(),
                                "--max-failure-ratio", str(sp["max_failure_ratio"]),
                                "--max-rows-per-game-type", str(exp_cfg.get("max_rows_per_game_type", sp["max_rows_per_game_type"])),
                                "--min-free-gb", str(max((r for p, r in self.storage.reserves if volume_key(p) == write_vol), default=0)),
                                "--threads", str(self.cfg["java"]["threads"] or (os.cpu_count() or 8) * 4), *model_args)
            if stage.resolve() != out.resolve():
                cmd += ["--count-chunks-from", out.as_posix()]
            self.storage.rotate = lambda: self.make_room(exp, it)
            try:
                result = run_command(cmd, log_path, sp["stage_max_seconds"] + 600, self.stop, graceful_file=stop_file, grace=sp["grace_seconds"],
                                     env=self.storage.child_env(), guard=self.storage.guard, guard_interval=self.storage.interval,
                                     tick=self.tick, low_priority=self.cfg["low_priority"])
            finally:
                self.storage.rotate = None
                commit_staged_chunks(stage, out, self.log)
            quarantine_bad_chunks(out, self.run_dir / "quarantine", self.log)
            have = count_games(out)
            # exit 3 = the time limit of the stage: fewer games than planned is a legitimate result, exit 4/1/2 are failures
            ok = result.code in (0, 3) and have > 0 and result.reason == "exit"
            return ok, result, log_path

        ok = self.with_retries(exp, it, "selfplay", attempt)
        if ok:
            perf = read_json(out / "summary.json", {})
            payload = {"games": count_games(out), "time": stamp()}
            if perf:
                payload["gamesPerSecond"] = perf.get("gamesPerSecond")
                payload["seconds"] = perf.get("seconds")
                payload["threads"] = perf.get("threads")
                payload["perf"] = perf.get("perf")
                hint = (perf.get("perf") or {}).get("hint")
                gps = perf.get("gamesPerSecond")
                share = (perf.get("perf") or {}).get("neuralCpuShare")
                self.log(f"{exp} it{it} selfplay: {gps} games/s, neuralCpuShare={share}, {hint}")
            write_json(done_marker, payload)
        return ok

    def stage_train(self, exp, exp_cfg, it):
        done_marker = self.it_dir(exp, it) / "train.done"
        if done_marker.exists():
            return True
        model_dir = self.it_dir(exp, it) / "model"
        log_path = self.it_dir(exp, it) / "train.log"
        window = self.data_window(exp, it)
        prev = self.best_model_dir(exp)
        tr = self.cfg["train"]

        def attempt():
            self.check_disk()
            self.current = {"exp": exp, "iteration": it, "stage": "train"}
            self.write_status()
            model_dir.mkdir(parents=True, exist_ok=True)
            # per-experiment trainer flags (architecture, lr, ...) go last, so they override the common ones
            cmd = self.template(list(tr["command"]) + list(exp_cfg.get("train_args", [])),
                                data=",".join(str(w) for w in window), model_dir=str(model_dir),
                                prev_model=str(prev) if prev else "", exp=exp, iteration=it)
            result = run_command(cmd, log_path, tr["timeout_seconds"], self.stop, env=self.storage.child_env(),
                                 guard=self.storage.guard, guard_interval=self.storage.interval, tick=self.tick,
                                 low_priority=self.cfg["low_priority"])
            ok = result.ok and (model_dir / "DONE").exists()
            if result.ok and not ok:
                result = Result(1, "exit", result.seconds)
                with open(log_path, "a") as f:
                    f.write("trainer exited with 0 but did not write DONE: the model is not trusted\n")
            return ok, result, log_path

        ok = self.with_retries(exp, it, "train", attempt)
        if ok:
            write_json(done_marker, {"time": stamp()})
        return ok

    def baseline_score(self, ref):
        """The same agent WITHOUT a model (it picks phases at random) against `ref`: what the model has to beat. Played once per run."""
        ev = self.cfg["eval"]
        path = self.run_dir / "baseline" / f"vs_{ref}.json"
        result = self.compare_once(path, self.run_dir / "baseline" / "baseline.log", ev["agent"], ref, ev["games"],
                                   ev["concurrency"], ev["timeout_seconds"], [], ("baseline", 0, f"baseline vs {ref}"))
        return read_json(path) if result.ok else None

    def compare_once(self, path, log_path, a, b, games, concurrency, timeout, model_args, where):
        """One SelfPlayMain compare outside with_retries (baseline, benchmark); an out-of-memory exit is recorded and restarted."""
        restarts = 0
        while not path.exists():
            cmd = self.java_cmd("--mode", "compare", "--a", a, "--b", b, "--games", str(games),
                                "--concurrency", str(concurrency), "--out", path.as_posix(), *model_args)
            result = run_command(cmd, log_path, timeout, self.stop, env=self.storage.child_env(),
                                 guard=self.storage.guard, guard_interval=self.storage.interval, tick=self.tick, low_priority=self.cfg["low_priority"])
            if result.code == JAVA_EXIT_OUT_OF_MEMORY and result.reason == "exit" and restarts < self.cfg["oom_restarts"]:
                restarts += 1
                self.record_oom(*where, log_path)
                continue
            if not (result.ok and path.exists()):
                return result if not result.ok else Result(1, "exit", result.seconds)
        return Result(0, "exit", 0)

    def benchmark_gate(self, exp, it, bm):
        """(ok, detail). Slow refs are only worth time after the agent clearly beats a fast yardstick."""
        min_wr = float(bm.get("min_win_rate") or 0)
        if min_wr <= 0:
            return True, "gate off"
        gate_refs = bm.get("gate_refs") or list(self.cfg.get("eval", {}).get("references") or [])
        ev = read_json(self.it_dir(exp, it) / "eval.json", {}) or {}
        vs = ev.get("vs") or {}
        rates = {ref: (vs.get(ref) or {}).get("score") for ref in gate_refs}
        cleared = [ref for ref, wr in rates.items() if isinstance(wr, (int, float)) and wr >= min_wr]
        detail = ", ".join(f"{ref}={rates[ref]}" if rates.get(ref) is not None else f"{ref}=?" for ref in gate_refs)
        if cleared:
            return True, f"{cleared[0]} win rate clears {min_wr:.0%} ({detail})"
        return False, f"need ≥{min_wr:.0%} win rate vs any of {gate_refs} ({detail})"

    def stage_benchmark(self):
        """Rare slow opponents (NETWORK…): best model once at the end, only if the RANDOM/SMART gate clears.
        Never training data. A failed compare is a warning, not a failed run."""
        bm = self.cfg.get("benchmark") or {}
        exps = self.state["experiments"]
        candidates = [n for n, e in exps.items() if e.get("best_score") is not None and e.get("best_model")]
        if not bm.get("references") or not candidates:
            return
        exp = max(candidates, key=lambda n: exps[n]["best_score"])
        it = exps[exp]["best_iteration"]
        model_dir = exps[exp]["best_model"]
        results = self.state.setdefault("benchmark", {})
        ok, reason = self.benchmark_gate(exp, it, bm)
        if not ok:
            results["skipped"] = {"exp": exp, "iteration": it, "reason": reason}
            self.save_state()
            self.log(f"benchmark skipped: {reason}")
            return
        out = self.run_dir / "benchmark"
        out.mkdir(parents=True, exist_ok=True)
        self.log(f"benchmark gate open: {reason}")
        for ref in bm["references"]:
            key = f"{exp}/it{it}/{ref}"
            if key in results:
                continue
            self.current = {"exp": exp, "iteration": it, "stage": f"benchmark vs {ref}"}
            self.write_status()
            path = out / f"{exp}_it{it}_vs_{ref}.json"
            self.log(f"benchmark: best model ({exp} it{it}) vs {ref}, {bm['games']} games")
            started = now()
            result = self.compare_once(path, out / "benchmark.log", self.cfg["eval"]["agent"], ref, bm["games"], bm["concurrency"],
                                       bm["timeout_seconds"], java_model_args(model_dir, "0"), (exp, it, f"benchmark vs {ref}"))
            if result.ok:
                r = read_json(path)
                results[key] = {"exp": exp, "iteration": it, "ref": ref, "score": r["aScore"], "low": r["aScoreCi95Low"],
                                "high": r["aScoreCi95High"], "games": r["games"], "seconds": round(now() - started, 1)}
                self.log(f"benchmark vs {ref}: {r['aScore']:.3f} [{r['aScoreCi95Low']:.3f}, {r['aScoreCi95High']:.3f}]")
            else:
                results[key] = {"exp": exp, "iteration": it, "ref": ref, "failed": f"exit {result.code} ({result.reason})"}
                self.log(f"benchmark vs {ref} failed: exit {result.code} ({result.reason}), see benchmark/benchmark.log")
            self.save_state()

    def stage_eval(self, exp, it):
        done_marker = self.it_dir(exp, it) / "eval.done"
        if done_marker.exists():
            return read_json(self.it_dir(exp, it) / "eval.json")
        ev = self.cfg["eval"]
        log_path = self.it_dir(exp, it) / "eval.log"
        model_dir = self.it_dir(exp, it) / "model"
        scores = {}
        baselines = {}

        def attempt():
            self.check_disk()
            for ref in ev["references"]:
                path = self.it_dir(exp, it) / f"eval_vs_{ref}.json"
                if not path.exists():
                    self.current = {"exp": exp, "iteration": it, "stage": f"eval vs {ref}"}
                    self.write_status()
                    cmd = self.java_cmd("--mode", "compare", "--a", ev["agent"], "--b", ref, "--games", str(ev["games"]),
                                        "--concurrency", str(ev["concurrency"]), "--out", path.as_posix(),
                                        *java_model_args(str(model_dir), "0"))
                    result = run_command(cmd, log_path, ev["timeout_seconds"], self.stop, env=self.storage.child_env(),
                                         guard=self.storage.guard, guard_interval=self.storage.interval, tick=self.tick,
                                         low_priority=self.cfg["low_priority"])
                    if not (result.ok and path.exists()):
                        return False, result, log_path
                scores[ref] = read_json(path)
                baseline = self.baseline_score(ref)
                if baseline is None:
                    return False, Result(1, "exit", 0), self.run_dir / "baseline" / "baseline.log"
                baselines[ref] = baseline
            return True, Result(0, "exit", 0), log_path

        if not self.with_retries(exp, it, "eval", attempt):
            return None
        refs = ev["references"]
        delta = {ref: scores[ref]["aScore"] - baselines[ref]["aScore"] for ref in refs}
        # a gain against an opponent the baseline already beats 85% of the time can not be big: measure it against the room that was left
        relative = {ref: delta[ref] / max(0.1, 1.0 - baselines[ref]["aScore"]) for ref in refs}
        # Absolute win-rate vs SMART/NETWORK is near 0 early on; promotion uses mean gain over the no-model baseline.
        win_rate = sum(scores[ref]["aScore"] for ref in refs) / len(refs)
        score = sum(delta[ref] for ref in refs) / len(refs)
        alarm = None
        if "SMART" in refs and len(refs) > 1:
            others = [relative[r] for r in refs if r != "SMART"]
            gap = relative["SMART"] - sum(others) / len(others)
            alarm = {"gap": round(gap, 4), "triggered": bool(gap > ev["bias_alarm_gap"]),
                     "meaning": "the model gained much more of the room it had against SMART than against the other opponents: it may have adapted to SMART's heuristics (noisy with few games)"}
        summary = {
            "score": score,
            "win_rate": win_rate,
            "vs": {ref: {"score": s["aScore"], "low": s["aScoreCi95Low"], "high": s["aScoreCi95High"], "games": s["games"],
                         "baseline": baselines[ref]["aScore"], "gain": round(delta[ref], 4),
                         "gain_of_room": round(relative[ref], 4)} for ref, s in scores.items()},
            "bias_alarm": alarm,
        }
        write_json(self.it_dir(exp, it) / "eval.json", summary)
        write_json(done_marker, {"time": stamp()})
        return summary

    # ---- bookkeeping per experiment
    def best_model_dir(self, exp):
        best = self.state["experiments"][exp].get("best_model")
        return Path(best) if best and (Path(best) / "DONE").exists() else None

    def data_window(self, exp, it):
        keep = self.storage.keep
        return [self.data_dir(exp, i) for i in range(max(1, it - keep + 1), it + 1) if self.data_dir(exp, i).exists()]

    def prune_old_data(self, protect_exp=None, protect_it=None):
        """Deletes the data of iterations that fell out of the window. Models, evaluations and logs are never deleted."""
        removed = 0
        for exp, e in (self.state.get("experiments") or {}).items():
            # an experiment that will never train again (halving loser or plateaued) frees its whole window at once
            last = e.get("iterations_done", 0) + (1 if e.get("state") in ("eliminated", "plateaued") else -self.storage.keep + 1)
            for i in range(1, last):
                d = self.data_dir(exp, i)
                if d.exists() and not (exp == protect_exp and i == protect_it):
                    self.storage.delete_data_dir(d)
                    removed += 1
        return removed

    def short_of_space(self):
        return bool(self.storage.violations()) or self.storage.over_budget()

    def make_room(self, protect_exp=None, protect_it=None):
        """Rotation: first the data outside the training windows; if that is not enough, the OLDEST data of this run (the windows
        shrink), never the iteration being written and never the latest finished iteration of an active experiment.
        Models and evaluations are never touched. Returns the number of deleted folders."""
        removed = self.prune_old_data(protect_exp, protect_it)
        while self.short_of_space():
            # WHY, specifically: a named volume below its reserve, or our own data budget — never just "300 GB" with
            # no indication that 300 GB is OUR cap, not how much free space triggered this (see docs/NIGHTLY.md).
            violations = self.storage.violations()
            reason = "; ".join(violations) if violations else f"data budget {self.storage.budget_gb} GB exceeded ({self.storage.data_gb():.1f} GB used)"
            exps = self.state.get("experiments") or {}
            keep = {(n, e.get("iterations_done", 0)) for n, e in exps.items() if e.get("state") == "active"}
            keep.add((protect_exp, protect_it))
            candidates = [(self.data_dir(n, i).stat().st_mtime, n, i) for n, e in exps.items()
                          for i in range(1, e.get("iterations_done", 0) + 2)
                          if (n, i) not in keep and self.data_dir(n, i).exists()]
            if not candidates:
                self.log(f"DATA WINDOW SHRUNK: {reason}, but there is no more of this run's data left to delete")
                break
            _, n, i = min(candidates)
            self.storage.delete_data_dir(self.data_dir(n, i))
            removed += 1
            shrinks = self.state.setdefault("window_shrinks", [])
            shrinks.append({"time": stamp(), "exp": n, "iteration": i, "reason": reason, "data_gb_after": round(self.storage.data_gb(), 1)})
            self.save_state()
            self.log(f"DATA WINDOW SHRUNK: deleted the data of {n} it{i} because {reason} "
                     f"(this run's data is now {self.storage.data_gb():.1f} GB)")
        return removed

    def check_disk(self):
        """Before every stage: short of space -> rotate the data of this run; still short -> stop the run (nothing is lost)."""
        if self.short_of_space():
            cur = self.current or {}
            removed = self.make_room(cur.get("exp"), cur.get("iteration"))
            self.log(f"disk: {self.storage.violations() or 'data budget'}; deleted {removed} old data folders")
            problems = self.storage.violations()
            if problems:
                raise DiskLow("; ".join(problems))
            if self.storage.over_budget():
                raise DiskLow(f"data of the run is {self.storage.data_gb():.1f} GB, the budget is {self.storage.budget_gb} GB")

    def run_iteration(self, exp, exp_cfg, games_factor):
        e = self.state["experiments"][exp]
        it = e["iterations_done"] + 1
        started = now()
        games = max(10, int(exp_cfg["games_per_iteration"] * games_factor))
        self.log(f"--- {exp} iteration {it}: {games} games")
        if not self.stage_selfplay(exp, exp_cfg, it, games):
            return self.iteration_failed(exp, it)
        if not self.stage_train(exp, exp_cfg, it):
            return self.iteration_failed(exp, it)
        summary = self.stage_eval(exp, it)
        if summary is None:
            return self.iteration_failed(exp, it)

        e["failures"] = 0
        e["iterations_done"] = it
        e["scores"].append(round(summary["score"], 4))
        e["last_iteration_seconds"] = round(now() - started, 1)
        best = e.get("best_score")
        # "win_rate" in eval.json is the MEAN raw win rate across eval.references, not any single opponent's — spell
        # out the per-reference breakdown here so the log line cannot be misread as "win rate vs RANDOM".
        vs_txt = ", ".join(f"{ref}={v['score']:.1%}" for ref, v in (summary.get("vs") or {}).items())
        wr_txt = f" | win rate {vs_txt}" if vs_txt else ""
        if best is None or summary["score"] > best + self.cfg["promote_margin"]:
            e["best_score"] = round(summary["score"], 4)
            e["best_model"] = str(self.it_dir(exp, it) / "model")
            e["best_iteration"] = it
            e["iterations_since_best"] = 0
            why = "first scored model" if best is None else f"beat best {best:+.3f}"
            self.log(f"{exp} it{it}: score(mean gain vs no-model baseline)={summary['score']:+.3f}{wr_txt} -> PROMOTED ({why})")
        else:
            e["iterations_since_best"] = e.get("iterations_since_best", 0) + 1
            self.log(f"{exp} it{it}: score(mean gain vs no-model baseline)={summary['score']:+.3f}{wr_txt} "
                     f"(best stays {best:+.3f} at it{e.get('best_iteration')}, {e['iterations_since_best']} iteration(s) since)")
        self.save_state()
        self.check_plateau(exp, exp_cfg)
        self.prune_old_data(protect_exp=exp, protect_it=it)
        return True

    def check_plateau(self, exp, exp_cfg):
        """Independent of the rung schedule: this experiment's own iterations have stopped improving it, so it stops
        now instead of occupying a time share or a data window for the rest of the night. Successive halving (rungs)
        still compares experiments against EACH OTHER at fixed points; this catches "not even improving against
        itself" as soon as the evidence is in, whatever the rung timing is - which is what lets more experiments run
        per night without each one needing a smaller guaranteed slice: a loser frees its slice as soon as it is one."""
        e = self.state["experiments"][exp]
        patience = exp_cfg.get("plateau_patience", self.cfg.get("plateau_patience"))
        min_its = exp_cfg.get("plateau_min_iterations", self.cfg.get("plateau_min_iterations", 1))
        if not patience or e["state"] != "active" or e["iterations_done"] < max(1, min_its):
            return
        if e.get("iterations_since_best", 0) >= patience:
            e["state"] = "plateaued"
            self.save_state()
            self.log(f"experiment {exp} PLATEAUED: no better score for {patience} of its own iterations in a row "
                     f"(best {e.get('best_score')} at it{e.get('best_iteration')}); stopping it now and freeing its "
                     f"time/data for the others instead of running it out its full share")

    def iteration_failed(self, exp, it):
        e = self.state["experiments"][exp]
        e["failures"] = e.get("failures", 0) + 1
        self.log(f"{exp} it{it} failed ({e['failures']}/{self.cfg['max_consecutive_failures']})")
        if e["failures"] >= self.cfg["max_consecutive_failures"]:
            e["state"] = "failed"
            self.log(f"experiment {exp} is dropped after repeated failures; the others continue")
        self.save_state()
        return False

    # ---- scheduling
    def init_state(self, name):
        started = now()
        self.state = {
            "name": name, "status": "running", "started": started, "budget_seconds": budget_seconds(self.cfg), "active_seconds": 0.0,
            "rung": 0, "rung_windows": {},
            "experiments": {e["name"]: {"state": "active", "iterations_done": 0, "scores": [], "failures": 0,
                                        "best_score": None, "iterations_since_best": 0}
                            for e in self.cfg["experiments"]},
        }
        for e_cfg in self.cfg["experiments"]:
            if e_cfg.get("start_model"):
                # the model of an earlier night becomes iteration 0: it plays the first self-play, warm-starts the trainer and is
                # evaluated like any iteration, so a new model replaces it only by being better. Copied: this run stays self-contained.
                source = resolve_start_model(e_cfg["start_model"], self.run_dir.parent)
                target = self.it_dir(e_cfg["name"], 0) / "model"
                if target.exists():
                    shutil.rmtree(target)
                shutil.copytree(source, target)
                e = self.state["experiments"][e_cfg["name"]]
                e.update({"start_model": str(source), "best_model": str(target), "best_iteration": 0, "start_pending_eval": True})
        self.save_state()

    def score_start_model(self, exp):
        e = self.state["experiments"][exp]
        summary = self.stage_eval(exp, 0)
        e.pop("start_pending_eval", None)
        if summary is None:
            self.log(f"{exp}: the start model {e['start_model']} could not be evaluated; the first trained model replaces it")
            e["best_model"], e["best_iteration"] = None, None
        else:
            e["best_score"] = round(summary["score"], 4)
            self.log(f"{exp}: start model {e['start_model']} has gain {summary['score']:+.3f}; new models must beat it")
        self.save_state()

    def exp_cfg(self, name):
        return next(e for e in self.cfg["experiments"] if e["name"] == name)

    def ranked_active(self):
        active = [n for n, e in self.state["experiments"].items() if e["state"] == "active"]
        return sorted(active, key=lambda n: -(self.state["experiments"][n].get("best_score") or -1))

    def rung_deadline(self, rung_index):
        if not self.has_time_budget():
            return math.inf
        return self.budget() * sum(r.get("fraction") or 0 for r in self.cfg["rungs"][:rung_index + 1])

    def rung_min_iterations(self, rung_index, rung):
        """How many iterations each active experiment must finish in this rung before elimination.
        Timed budget: optional. Unlimited: early rungs default to 1; last rung has no cap (runs until stop)."""
        if "min_iterations" in rung:
            return rung["min_iterations"]
        if self.has_time_budget() or rung_index >= len(self.cfg["rungs"]) - 1:
            return None
        return 1

    def run(self):
        st = self.state
        if st.get("budget_for_windows") != st["budget_seconds"]:
            # hours changed in config / CLI: replan unfinished rungs
            for key in [k for k in st["rung_windows"] if int(k) >= st["rung"]]:
                del st["rung_windows"][key]
            st["budget_for_windows"] = st["budget_seconds"]
        if st["rung"] >= len(self.cfg["rungs"]) and self.elapsed() < self.budget():
            st["rung"] = len(self.cfg["rungs"]) - 1
            st["rung_windows"].pop(str(st["rung"]), None)
            self.log("planned rungs done with time/capacity left: continuing the survivors")
        try:
            while True:
                for rung_index, rung in enumerate(self.cfg["rungs"]):
                    if st["rung"] > rung_index:
                        continue
                    st["rung"] = rung_index
                    active = [n for n, e in st["experiments"].items() if e["state"] == "active"]
                    if not active:
                        break
                    windows = st["rung_windows"].setdefault(str(rung_index), {})
                    if "end" not in windows:
                        windows["end"] = self.rung_deadline(rung_index)
                        windows["min_iterations"] = self.rung_min_iterations(rung_index, rung)
                        windows["baseline"] = {n: st["experiments"][n]["iterations_done"] for n in active}
                        windows["next"] = 0
                        self.save_state()
                    until = ("until stop" if not math.isfinite(windows["end"])
                             else f"until {windows['end'] / 3600:.2f} h of work")
                    min_it = windows.get("min_iterations")
                    self.log(f"=== rung {rung_index}: {active} {until}"
                             + (f", min {min_it} it each" if min_it else "")
                             + f" (keep {rung['keep']}, games factor {rung['games_factor']})")
                    self.run_rung_round_robin(active, windows, rung["games_factor"])
                    ranked = self.ranked_active()
                    scored = [n for n in ranked if st["experiments"][n].get("best_score") is not None]
                    losers = ranked[rung["keep"]:] if len(scored) >= len(ranked) else [n for n in scored[rung["keep"]:]]
                    for loser in losers:
                        st["experiments"][loser]["state"] = "eliminated"
                        self.log(f"experiment {loser} eliminated after rung {rung_index} "
                                 f"(best score {st['experiments'][loser].get('best_score')})")
                    st["rung"] = rung_index + 1
                    self.save_state()
                    if losers:
                        self.log(f"freed the data of eliminated experiments: {self.prune_old_data()} folders (their models stay)")
                # planned rungs done: keep searching while there is capacity (unlimited → until stop; timed → until budget)
                active = [n for n, e in st["experiments"].items() if e["state"] == "active"]
                limit = self.cfg.get("max_iterations_per_experiment")
                left = self.budget() - self.elapsed()
                need = min((st["experiments"][n].get("last_iteration_seconds") or 0) * 1.1
                           for n in active) if active else 0
                can_continue = any(not (limit and st["experiments"][n]["iterations_done"] >= limit) for n in active)
                if not can_continue or left <= 0 or (math.isfinite(left) and need and left < need):
                    if math.isfinite(left) and left > 0 and need and left < need:
                        self.log(f"budget leftover {left / 60:.0f} min is less than the next iteration ({need / 60:.0f} min); packing")
                    break
                st["rung"] = len(self.cfg["rungs"]) - 1
                st["rung_windows"][str(st["rung"])] = {
                    "end": self.budget(),
                    "min_iterations": None,
                    "baseline": {n: st["experiments"][n]["iterations_done"] for n in active},
                    "next": 0,
                }
                self.log(f"continuing {active} ({budget_label(st.get('budget_seconds'))})")
            self.stage_benchmark()
            st["status"] = "finished"
        except StopRequested:
            st["status"] = "stopped"
            self.log("stop requested: everything finished so far is kept")
        except EnvironmentBroken as e:
            st["status"] = "failed_environment"
            self.last_error = str(e)
            self.log(f"STOPPING: {e}")
        except DiskLow as e:
            st["status"] = "stopped_disk_low"
            self.last_error = str(e)
            self.log(f"stopping: {e}")
        except Exception as e:
            st["status"] = "crashed"
            self.last_error = f"{type(e).__name__}: {e}"
            self.log("PIPELINE ERROR: " + traceback.format_exc())
            (self.run_dir / "errors").mkdir(exist_ok=True)
            (self.run_dir / "errors" / f"{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}_pipeline.txt").write_text(traceback.format_exc())
        finally:
            self.current = {}
            if all(e["state"] == "failed" for e in st["experiments"].values()):
                st["status"] = "failed"
            self.save_state()
            self.write_status()
        return st["status"]

    def run_rung_round_robin(self, names, windows, games_factor):
        """One iteration at a time, cycling active experiments until deadline, min_iterations, or stop."""
        limit = self.cfg.get("max_iterations_per_experiment")
        hard_end = min(windows.get("end", math.inf), self.budget())
        min_it = windows.get("min_iterations")
        baseline = windows.setdefault("baseline", {n: self.state["experiments"][n]["iterations_done"] for n in names})
        for name in names:
            if self.state["experiments"][name].get("start_pending_eval"):
                self.score_start_model(name)
        while True:
            self.stop.check()
            if min_it:
                active_now = [n for n in names if self.state["experiments"][n]["state"] == "active"]
                if active_now and all(
                        self.state["experiments"][n]["iterations_done"] - baseline.get(n, 0) >= min_it
                        for n in active_now):
                    self.log(f"rung min_iterations={min_it} reached")
                    return
            runnable = [n for n in names if self.state["experiments"][n]["state"] == "active"
                        and not (limit and self.state["experiments"][n]["iterations_done"] >= limit)]
            if not runnable:
                return
            progressed = False
            start = windows.get("next", 0) % len(runnable)
            windows["next"] = start
            for offset in range(len(runnable)):
                name = runnable[(start + offset) % len(runnable)]
                e = self.state["experiments"][name]
                remaining = hard_end - self.elapsed()
                estimate = (e.get("last_iteration_seconds") or 0) * 1.1
                if math.isfinite(remaining) and (remaining <= 0 or (estimate and remaining < estimate)):
                    continue
                self.run_iteration(name, self.exp_cfg(name), games_factor)
                windows["next"] = (start + offset + 1) % len(runnable)
                self.save_state()
                progressed = True
                break
            if not progressed:
                left = hard_end - self.elapsed()
                self.log(f"rung time over ({left / 60:.0f} min left, next iteration needs more than that)")
                return

    def run_slot(self, name, slot_end, games_factor):
        """Back-compat helper for tests: run one experiment until slot_end."""
        e = self.state["experiments"][name]
        if e.get("start_pending_eval"):
            self.score_start_model(name)
        windows = {"end": slot_end, "min_iterations": None, "baseline": {name: e["iterations_done"]}, "next": 0}
        self.run_rung_round_robin([name], windows, games_factor)


# ------------------------------------------------------------------------------------------------ data helpers

def resolve_start_model(spec, run_root):
    """`spec` is a model folder (with DONE) or a run (name inside run_root, or its folder): then the best model of that run."""
    path = Path(spec)
    if not path.is_absolute() and (Path(run_root) / spec / "state.json").exists():
        path = Path(run_root) / spec
    if (path / "state.json").exists():
        exps = (read_json(path / "state.json", {}) or {}).get("experiments", {})
        scored = [e for e in exps.values() if e.get("best_score") is not None and e.get("best_model")]
        if not scored:
            raise ValueError(f"run {path} has no evaluated model to start from")
        path = Path(max(scored, key=lambda e: e["best_score"])["best_model"])
    if not (path / "DONE").exists():
        raise ValueError(f"{spec}: not a finished model folder (no DONE) and not a run folder")
    return path


def java_model_args(model_dir, temperature):
    """CLI flags for SelfPlayMain: multi-type bundle (--models) or a single exported folder (--model)."""
    root = Path(model_dir)
    temp = str(temperature)
    if root.exists():
        has_typed = any((p / "model.json").exists() for p in root.iterdir() if p.is_dir())
        if has_typed:
            return ["--models", str(root), "--temperature", temp]
        if (root / "model.json").exists():
            return ["--model", str(root), "--temperature", temp]
        return []
    # path not on disk (tests / just-written path): default to the train_all layout
    return ["--models", str(root), "--temperature", temp]


def chunk_files(directory):
    return sorted(Path(directory).glob("*.[0-9]*.ares"))


def commit_staged_chunks(stage, dest, log=None):
    """Move finished .ares from an SSD stage to the durable data dir (copy+delete; cross-volume safe)."""
    stage, dest = Path(stage), Path(dest)
    if not stage.exists() or stage.resolve() == dest.resolve():
        return 0
    dest.mkdir(parents=True, exist_ok=True)
    moved = 0
    for f in chunk_files(stage):
        target = dest / f.name
        if target.exists():
            f.unlink(missing_ok=True)
            continue
        shutil.copy2(f, target)
        f.unlink()
        moved += 1
    for name in ("summary.json", "errors.log"):
        src = stage / name
        if src.exists():
            if name == "errors.log" and (dest / name).exists():
                with open(dest / name, "ab") as out, open(src, "rb") as inp:
                    out.write(inp.read())
                src.unlink(missing_ok=True)
            else:
                shutil.copy2(src, dest / name)
                if name == "summary.json":
                    src.unlink(missing_ok=True)
    for tmp in stage.glob("*.tmp"):
        tmp.unlink(missing_ok=True)
    if log and moved:
        log(f"committed {moved} chunks from stage -> {dest}")
    return moved


def count_games(directory):
    """Number of different games in the finished chunks (cheap: only the game column is read)."""
    try:
        import numpy as np
        from ares_data import open_memmap
    except ImportError:
        return 0
    games = set()
    for f in chunk_files(directory):
        try:
            _, rows = open_memmap(f)
            games.update(np.unique(rows["game"]).tolist())
        except Exception:
            continue
    return len(games)


def quarantine_bad_chunks(directory, quarantine, log):
    """Chunks that cannot be read or fail the sanity checks must not reach the trainer."""
    from ares_data import open_memmap, validate
    bad = 0
    for f in chunk_files(directory):
        try:
            schema, rows = open_memmap(f)
            problems = validate(schema, rows[: min(len(rows), 5000)])
        except Exception as e:
            problems = [f"unreadable: {e}"]
        if problems:
            Path(quarantine).mkdir(parents=True, exist_ok=True)
            shutil.move(str(f), str(Path(quarantine) / f.name))
            log(f"QUARANTINED {f.name}: {problems}")
            bad += 1
    return bad


# ------------------------------------------------------------------------------------------------ pack (hand over)

def journal_entry(run_dir, state, cfg):
    """What this run tried and what came out: one record per run, so the next nights do not test the same idea again."""
    run_dir = Path(run_dir)
    exps = state.get("experiments", {})
    entry = {"run": state.get("name", run_dir.name), "status": state.get("status"),
             "started": stamp(state["started"]) if state.get("started") else None, "packed": stamp(),
             "active_hours": round((state.get("active_seconds") or 0) / 3600, 2),
             "train_command": (cfg.get("train") or {}).get("command"),
             "eval_references": (cfg.get("eval") or {}).get("references"),
             "oom_restarts": len(state.get("oom_restarts") or []),
             "window_shrinks": len(state.get("window_shrinks") or []),
             "errors": len(list((run_dir / "errors").glob("*.txt"))) if (run_dir / "errors").exists() else 0,
             "benchmark": list((state.get("benchmark") or {}).values()),
             "experiments": []}
    for e_cfg in cfg.get("experiments", []):
        n = e_cfg["name"]
        e = exps.get(n, {})
        record = {"name": n, "hypothesis": e_cfg.get("hypothesis", ""), "matchups": e_cfg.get("matchups"),
                  "games_per_iteration": e_cfg.get("games_per_iteration"), "train_args": e_cfg.get("train_args", []),
                  "temperature": e_cfg.get("temperature", (cfg.get("selfplay") or {}).get("temperature")),
                  "start_model": e.get("start_model"),
                  "state": e.get("state"), "iterations_done": e.get("iterations_done", 0), "scores": e.get("scores", []),
                  "best_score": e.get("best_score"), "best_iteration": e.get("best_iteration"), "iterations": []}
        for i in range(1, e.get("iterations_done", 0) + 1):
            i_dir = run_dir / "exp" / n / f"it_{i:04d}"
            m_path = next(iter(sorted((i_dir / "model").glob("*/train_metrics.json"))), None) if (i_dir / "model").exists() else None
            m = read_json(m_path, {}) if m_path else {}
            pool = m.get("pool") or {}
            val, const = m.get("best_val_loss"), m.get("constant_val_loss")
            record["iterations"].append({
                "iteration": i, "score": (read_json(i_dir / "eval.json") or {}).get("score"),
                "train_games": pool.get("train_games"), "rows_per_game_mean": pool.get("rows_per_game_mean"),
                "rows_per_game_cap": pool.get("rows_per_game_cap"), "feed": pool.get("feed"), "best_epoch": m.get("best_epoch"),
                "val_gap": round(val - const, 4) if isinstance(val, (int, float)) and isinstance(const, (int, float)) else None})
        it = e.get("best_iteration")
        if it:
            it_dir = run_dir / "exp" / n / f"it_{it:04d}"
            ev = read_json(it_dir / "eval.json") or {}
            record["best_eval"] = {ref: {k: v.get(k) for k in ("score", "low", "high", "baseline", "gain")}
                                   for ref, v in (ev.get("vs") or {}).items()}
            metrics = next(iter(sorted((it_dir / "model").glob("*/train_metrics.json"))), None) if (it_dir / "model").exists() else None
            if metrics:
                m = read_json(metrics, {})
                record["best_training"] = {"best_epoch": m.get("best_epoch"), "best_val_loss": m.get("best_val_loss"),
                                           "constant_val_loss": m.get("constant_val_loss"), "pool": m.get("pool"),
                                           "history": m.get("history")}
        speeds = [(read_json(run_dir / "exp" / n / f"it_{i:04d}" / "selfplay.done", {}) or {}).get("gamesPerSecond")
                  for i in range(1, e.get("iterations_done", 0) + 1)]
        speeds = [s for s in speeds if isinstance(s, (int, float))]
        record["games_per_second"] = round(sum(speeds) / len(speeds), 2) if speeds else None
        entry["experiments"].append(record)
    return entry


def data_quality_lines(journal, cfg):
    """Point 1 of the basics (docs/EXPERIMENTS.md): what the best model of every experiment was trained on.
    Only facts and plain defects are flagged here; how many games are "enough" is a hypothesis that the per-iteration table tests."""
    lines = ["## Data quality (training pool of the best model)", "",
             "| experiment | matchups | train games | val games | rows/game (cap) | label win share | pool GB | warnings |",
             "|---|---|---:|---:|---|---:|---:|---|"]
    selfplay_cap = (cfg.get("selfplay") or {}).get("max_rows_per_game_type")
    for r in journal["experiments"]:
        pool = (r.get("best_training") or {}).get("pool")
        if not pool:
            lines.append(f"| {r['name']} | {r['matchups']} | - | - | - | - | - | no trained model / no pool stats |")
            continue
        warnings = []
        if not 0.3 <= pool["label_mean"] <= 0.7:
            warnings.append(f"labels unbalanced ({pool['label_mean']:.2f} wins)")
        if not pool.get("rows_per_game_cap"):
            warnings.append("no cap on rows per game")
        if not pool.get("val_games"):
            warnings.append("no validation games")
        lines.append(f"| {r['name']} | {r['matchups']} | {pool['train_games']} | {pool.get('val_games', '-')} | "
                     f"{pool['rows_per_game_mean']} ({pool.get('rows_per_game_cap')}) | {pool['label_mean']:.3f} | {pool['pool_gb']} | "
                     f"{'; '.join(warnings) or 'ok'} |")
    lines += ["", f"self-play writes at most {selfplay_cap or 'all'} decisions of a game per type; "
              "if a warning is shown, fix the data before drawing conclusions about the training.", ""]
    # evidence for the theory: does the overfitting gap shrink with more games / mixing / caps? (every iteration, not only the best)
    lines += ["## Evidence per iteration (data -> generalisation -> strength)", "",
              "| experiment | it | train games | rows/game | feed | best epoch | val_loss − constant | gain |", "|---|---:|---:|---:|---|---:|---:|---:|"]
    for r in journal["experiments"]:
        for row in r.get("iterations", []):
            gap = "-" if row.get("val_gap") is None else format(row["val_gap"], "+.4f")
            score = "-" if row.get("score") is None else format(row["score"], "+.3f")
            lines.append(f"| {r['name']} | {row['iteration']} | {row.get('train_games', '-')} | {row.get('rows_per_game_mean', '-')} | "
                         f"{row.get('feed', '-')} | {row.get('best_epoch', '-')} | {gap} | {score} |")
    lines += ["", "val_loss − constant < 0: the model generalises beyond predicting the win rate; > 0: it memorised games.", ""]
    return lines


def upsert_journal(journal_path, entry):
    """JOURNAL.jsonl next to all runs: one line per run, the latest pack of a run replaces its line."""
    journal_path = Path(journal_path)
    lines = []
    if journal_path.exists():
        for line in journal_path.read_text(encoding="utf-8").splitlines():
            try:
                if json.loads(line).get("run") != entry["run"]:
                    lines.append(line)
            except ValueError:
                lines.append(line)
    lines.append(json.dumps(entry, ensure_ascii=False))
    tmp = journal_path.with_name(journal_path.name + ".tmp")
    tmp.write_text("\n".join(lines) + "\n", encoding="utf-8")
    replace(tmp, journal_path)


def pack(run_dir, handoff_root=None, update_journal=True):
    run_dir = Path(run_dir)
    state = read_json(run_dir / "state.json", {})
    status = read_json(run_dir / "status.json", {})
    name = state.get("name", run_dir.name)
    handoff_root = Path(handoff_root) if handoff_root else run_dir / "handoff"
    base = f"{name}_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"
    final = handoff_root / base
    counter = 1
    while final.exists() or final.with_name(final.name + ".zip").exists():  # two packs in one second must not collide
        counter += 1
        final = handoff_root / f"{base}_{counter}"
    work = handoff_root / (final.name + ".partial")
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)

    exps = state.get("experiments", {})
    best_name = max(exps, key=lambda n: exps[n].get("best_score") or -1, default=None)
    lines = [f"# Run {name}", "", f"status: **{state.get('status')}**", f"started: {stamp(state['started']) if state.get('started') else '-'}",
             f"packed: {stamp()}", ""]
    lines += ["| experiment | state | iterations | best score | scores per iteration |", "|---|---|---|---|---|"]
    for n, e in exps.items():
        lines.append(f"| {n} | {e['state']} | {e['iterations_done']} | {e.get('best_score')} | {e.get('scores')} |")
    lines.append("")
    lines += ["## Self-play throughput", "",
              "| experiment | it | games/s | neural µs | inf/s | cpuShare | hint |",
              "|---|---:|---:|---:|---:|---:|---|"]
    for n in exps:
        for i in range(1, exps[n]["iterations_done"] + 1):
            sp = read_json(run_dir / "exp" / n / f"it_{i:04d}" / "selfplay.done", {}) or {}
            perf = sp.get("perf") or {}
            lines.append(
                f"| {n} | {i} | {sp.get('gamesPerSecond', '-')} | {perf.get('neuralUsMean', '-')} | "
                f"{perf.get('neuralInferencesPerSecond', '-')} | {perf.get('neuralCpuShare', '-')} | {perf.get('hint', '')} |"
            )
    lines.append("")
    if best_name and exps[best_name].get("best_model") and Path(exps[best_name]["best_model"]).exists():
        shutil.copytree(exps[best_name]["best_model"], work / "best_model")
        info = {"experiment": best_name, "iteration": exps[best_name].get("best_iteration"), "score": exps[best_name].get("best_score")}
        it_dir = Path(exps[best_name]["best_model"]).parent
        info["eval"] = read_json(it_dir / "eval.json")
        write_json(work / "best_model" / "MODEL_INFO.json", info)
        lines.append(f"**best model**: experiment `{best_name}`, iteration {info['iteration']}, score {info['score']} -> `best_model/`")
    else:
        lines.append("**no model has been evaluated yet** (see the logs below)")
    lines.append("")

    cfg = read_json(run_dir / "config.json", {}) or {}
    journal = journal_entry(run_dir, state, cfg)
    lines += ["## Hypotheses of this run", "",
              "| experiment | hypothesis | train args | best gain | best epoch / val_loss (constant predictor) |", "|---|---|---|---:|---|"]
    for r in journal["experiments"]:
        tr = r.get("best_training") or {}
        loss = f"{tr.get('best_epoch')} / {tr['best_val_loss']:.4f}" if isinstance(tr.get("best_val_loss"), (int, float)) else "-"
        if isinstance(tr.get("constant_val_loss"), (int, float)) and isinstance(tr.get("best_val_loss"), (int, float)):
            loss += f" ({tr['constant_val_loss']:.4f}{', WORSE than constant' if tr['best_val_loss'] >= tr['constant_val_loss'] else ''})"
        lines.append(f"| {r['name']} | {r['hypothesis'] or '**no hypothesis written**'} | {' '.join(r['train_args']) or '-'} | "
                     f"{r['best_score']} | {loss} |")
    lines += ["", "Conclusions go to `docs/EXPERIMENTS.md` (what was ruled out, what to try next); the raw record is `journal.json`.", ""]
    lines += data_quality_lines(journal, cfg)
    if journal["benchmark"]:
        lines += ["## Benchmark (slow opponents, best model only)", ""]
        for b in journal["benchmark"]:
            if b.get("reason") and "ref" not in b:
                lines.append(f"- skipped: {b['reason']}")
                continue
            result = b.get("failed") or f"{b['score']:.3f} [{b['low']:.3f}, {b['high']:.3f}] in {b['games']} games, {b['seconds']:.0f} s"
            lines.append(f"- {b['exp']} it{b['iteration']} vs {b['ref']}: {result}")
        lines.append("")
    write_json(work / "journal.json", journal)
    if update_journal:
        upsert_journal(run_dir.parent / "JOURNAL.jsonl", journal)
        shutil.copyfile(run_dir.parent / "JOURNAL.jsonl", work / "JOURNAL_all_runs.jsonl")

    errors = sorted((run_dir / "errors").glob("*.txt")) if (run_dir / "errors").exists() else []
    lines.append(f"errors: {len(errors)} (files in `logs/`; each has the command, the exit code and the end of the process output)" + (f"; last: {status.get('last_error')}" if status.get("last_error") else ""))
    for f in errors[-15:]:
        head = f.read_text(encoding="utf-8", errors="replace").splitlines()[:2]
        lines.append(f"- `{f.name}`: " + " | ".join(h.strip() for h in head))
    ooms = state.get("oom_restarts") or []
    if ooms:
        lines.append("")
        lines.append(f"**OUT OF MEMORY: Java was restarted {len(ooms)} times** (the run continued, but memory must be fixed; "
                     f"stage logs in `logs/warnings/`)")
        for o in ooms[-15:]:
            lines.append(f"- {o['time']} {o['exp']} it{o['iteration']} {o['stage']} -> `{o['file']}`")
    shrinks = state.get("window_shrinks") or []
    if shrinks:
        lines.append("")
        lines.append(f"**DATA WINDOW SHRUNK {len(shrinks)} times**: the data budget / disk reserves did not hold the configured "
                     f"windows, the oldest data of the run was deleted (later iterations trained on fewer games than "
                     f"`keep_data_iterations` promises; raise the budget or lower games / keep)")
        for s in shrinks[-15:]:
            why = f" ({s['reason']})" if s.get("reason") else ""
            lines.append(f"- {s['time']} deleted {s['exp']} it{s['iteration']}{why} -> {s['data_gb_after']} GB")
    q = list((run_dir / "quarantine").glob("*")) if (run_dir / "quarantine").exists() else []
    if q:
        lines.append(f"quarantined chunks: {len(q)}")
    (work / "REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    # metrics
    rows = ["experiment,iteration,score,vs"]
    for n in exps:
        for i in range(1, exps[n]["iterations_done"] + 1):
            ev = read_json(run_dir / "exp" / n / f"it_{i:04d}" / "eval.json")
            if ev:
                rows.append(f"{n},{i},{ev['score']},\"{ev['vs']}\"")
                if (ev.get("bias_alarm") or {}).get("triggered"):
                    lines.append(f"- BIAS ALARM {n} it{i}: {ev['bias_alarm']['meaning']} (gap {ev['bias_alarm']['gap']})")
    (work / "metrics.csv").write_text("\n".join(rows) + "\n", encoding="utf-8")
    (work / "REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8")  # again: now with the bias alarms found above

    # logs: tails only, errors in full
    logs = work / "logs"
    logs.mkdir()
    if (run_dir / "log.txt").exists():
        (logs / "pipeline_log_tail.txt").write_text(tail(run_dir / "log.txt", 500), encoding="utf-8")
    for f in errors:
        shutil.copyfile(f, logs / f.name)
    if (run_dir / "warnings").exists():
        (logs / "warnings").mkdir()
        for f in sorted((run_dir / "warnings").glob("*.txt")):
            shutil.copyfile(f, logs / "warnings" / f.name)
    for log_file in sorted((run_dir / "exp").glob("*/it_*/*.log")):
        target = logs / f"{log_file.parent.parent.name}_{log_file.parent.name}_{log_file.name}"
        target.write_text(tail(log_file, 150), encoding="utf-8")
    for f in ("state.json", "status.json"):
        if (run_dir / f).exists():
            shutil.copyfile(run_dir / f, work / f)
    if (run_dir / "config.json").exists():
        shutil.copyfile(run_dir / "config.json", work / "config.json")
    versions = {"python": sys.version.split()[0], "platform": platform.platform()}
    try:
        versions["git"] = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT, capture_output=True, text=True).stdout.strip()
    except OSError:
        pass
    write_json(work / "versions.json", versions)

    replace(work, final)
    archive = shutil.make_archive(str(final), "zip", str(final))
    return final, Path(archive)


# ------------------------------------------------------------------------------------------------ preflight / check

def total_ram_gb():
    try:
        return os.sysconf("SC_PAGE_SIZE") * os.sysconf("SC_PHYS_PAGES") / 1e9
    except (AttributeError, ValueError, OSError):
        pass
    try:
        import ctypes

        class Memory(ctypes.Structure):
            _fields_ = [("length", ctypes.c_ulong), ("load", ctypes.c_ulong), ("total", ctypes.c_ulonglong), ("avail", ctypes.c_ulonglong),
                        ("pageTotal", ctypes.c_ulonglong), ("pageAvail", ctypes.c_ulonglong), ("virtTotal", ctypes.c_ulonglong),
                        ("virtAvail", ctypes.c_ulonglong), ("ext", ctypes.c_ulonglong)]

        m = Memory()
        m.length = ctypes.sizeof(Memory)
        ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(m))
        return m.total / 1e9
    except Exception:
        return None


class Check:
    def __init__(self):
        self.results = []

    def add(self, name, ok, detail="", fatal=True):
        self.results.append((name, ok, detail, fatal))
        mark = "OK  " if ok else ("FAIL" if fatal else "WARN")
        print(f"[{mark}] {name}{': ' + detail if detail else ''}", flush=True)

    @property
    def ok(self):
        return all(ok or not fatal for _, ok, _, fatal in self.results)


def storage_paths(cfg, name):
    """(run_dir, data_root, temp_root) of a run: state/models/logs, big chunk files, TMP of the children."""
    s = cfg["storage"]

    def resolve(path):
        p = Path(path)
        return p if p.is_absolute() else ROOT / p

    run_dir = resolve(s["run_root"]) / name
    data_root = resolve(s["data_root"]) / name if s["data_root"] else run_dir
    temp_root = resolve(s["temp_root"]) / name if s["temp_root"] else run_dir / "tmp"
    return run_dir, data_root, temp_root


def run_is_alive(run_dir):
    info = read_json(Path(run_dir) / "run.lock", None) if (Path(run_dir) / "run.lock").exists() else None
    return bool(info) and pid_alive(info.get("pid", 0)) and now() - info.get("time", 0) < 6 * 3600


def delete_data_of_other_runs(cfg, name, log=print):
    """Self-play data of a finished night is only the raw material of its models: a new night frees that space.
    Deletes only `<data_root of the other run>/data/<experiment>/it_*`; never models, evaluations, logs or a running run.
    Returns GB freed."""
    s = cfg["storage"]
    roots = {Path(p) if Path(p).is_absolute() else ROOT / p for p in (s["run_root"], s["data_root"]) if p}
    freed = 0.0
    for other in sorted({d.name for r in roots if r.is_dir() for d in r.iterdir() if d.is_dir()} - {name}):
        other_run, other_data_root, _ = storage_paths(cfg, other)
        data = other_data_root / "data"
        if not data.is_dir():
            continue
        if run_is_alive(other_run):
            log(f"data of run {other} kept: it is running")
            continue
        for d in sorted(data.glob("*/it_*")):
            if d.is_dir():
                gb = dir_size_gb(d)
                shutil.rmtree(d, ignore_errors=True)
                freed += gb - (dir_size_gb(d) if d.exists() else 0.0)
        log(f"deleted the self-play data of the earlier run {other} ({data}); its models and reports are kept")
    return freed


def write_test(directory):
    """A real write, fsync and delete: catches read-only disks, missing permissions and full volumes."""
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    probe = directory / ".write_test"
    with open(probe, "wb") as f:
        f.write(b"x" * 1_000_000)
        f.flush()
        os.fsync(f.fileno())
    probe.unlink()


def preflight(cfg, run_dir, data_root, temp_root, smoke=True):
    check = Check()
    check.add("python >= 3.9", sys.version_info >= (3, 9), sys.version.split()[0])
    try:
        import numpy
        check.add("numpy", True, numpy.__version__)
    except ImportError:
        check.add("numpy", False, "pip install numpy")
        return check

    java, major = find_java(cfg["java"]["bin"])
    if java:
        cfg["java"]["bin"] = java
        check.add("java >= 21", True, f"{java} (version {major}, JDK folder {java_home_of(java)})")
        env_home = os.environ.get("JAVA_HOME")
        if env_home and not is_java_home(env_home):
            check.add("JAVA_HOME", False, f"'{env_home}' is not a JDK folder (it must be the folder that CONTAINS bin, e.g. "
                      f"{Path(env_home).parent if Path(env_home).name.lower() == 'bin' else 'C:/Program Files/Java/jdk-21'}); "
                      "the pipeline gives Maven the right one itself, but other tools (mvn by hand, IDE) will fail", fatal=False)
    else:
        check.add("java >= 21", False, "none found (tried java.bin, JAVA_HOME, PATH, ~/.jdks, Program Files); install a JDK 21+ or set java.bin in the config")
        return check

    # ---- storage: where everything goes and that the reserves are respected right now
    storage = Storage(cfg, run_dir, data_root, temp_root)
    write_targets = [("run state, models, logs", run_dir), ("training data", data_root), ("temp files of the children", temp_root)]
    if storage.stage_root:
        write_targets.append(("SSD self-play stage", storage.stage_root))
    for label, path in write_targets:
        try:
            write_test(path)
            check.add(f"write test: {label}", True, str(path))
        except OSError as e:
            check.add(f"write test: {label}", False, f"{path}: {e}")
    for path, reserve in storage.reserves:
        try:
            free = free_gb(path)
            check.add(f"disk reserve {path}", free >= reserve, f"{free:.0f} GB free, never going below {reserve} GB")
        except OSError as e:
            check.add(f"disk reserve {path}", False, str(e))
    if not check.ok:
        return check

    mvn = shutil.which(cfg["maven"]["command"])
    classes = ROOT / cfg["java"]["classes"]
    cp_file = ROOT / cfg["java"]["classpath_file"]
    if mvn:
        for label, args in (("compile the project", ["-q", "-DskipTests", "compile"]),
                            ("build the classpath", ["-q", "dependency:build-classpath", f"-Dmdep.outputFile={cp_file.as_posix()}"])):
            env = dict(os.environ, JAVA_HOME=java_home_of(java))  # compile with the same JDK that will run
            r = subprocess.run([mvn, *cfg["maven"]["args"], *args], cwd=ROOT, capture_output=True, text=True, env=env)
            if r.returncode != 0:
                log = Path(run_dir) / "maven_check.log"
                log.parent.mkdir(parents=True, exist_ok=True)
                log.write_text(r.stdout + "\n" + r.stderr, encoding="utf-8", errors="replace")
                check.add(f"maven: {label}", False, maven_failure_summary(r.stdout + "\n" + r.stderr) + f"\n      full output: {log}")
                return check
            check.add(f"maven: {label}", True)
    else:
        check.add("maven", cp_file.exists() and classes.exists(), "mvn not found; using the existing build" if cp_file.exists() else "mvn not found and no build")
    check.add("compiled classes", classes.exists(), str(classes))
    check.add("classpath file", cp_file.exists(), str(cp_file))
    if not (classes.exists() and cp_file.exists()):
        return check

    total = total_ram_gb()
    if total:
        check.add("RAM", True, f"{total:.0f} GB, the JVM gets {cfg['java']['xmx']}; the trainer must stream the chunks (ares_data.open_memmap)", fatal=False)
    smi = shutil.which("nvidia-smi")
    if smi:
        r = subprocess.run([smi, "--query-gpu=name,memory.total,memory.used", "--format=csv,noheader"], capture_output=True, text=True)
        check.add("GPU", r.returncode == 0, r.stdout.strip(), fatal=False)
    else:
        check.add("GPU", False, "nvidia-smi not found (the training will run on the CPU)", fatal=False)
    check.add("train command", bool(cfg["train"]["command"]), " ".join(cfg["train"]["command"]))
    for e in cfg["experiments"]:
        if e.get("start_model"):
            try:
                check.add(f"start model of '{e['name']}'", True, str(resolve_start_model(e["start_model"], Path(run_dir).parent)))
            except ValueError as ex:
                check.add(f"start model of '{e['name']}'", False, str(ex))
    missing = [e["name"] for e in cfg["experiments"] if not str(e.get("hypothesis", "")).strip()]
    check.add("every experiment states its hypothesis", not missing,
              f"no hypothesis: {missing} (a night must confirm or rule out something, see docs/EXPERIMENTS.md)" if missing
              else f"{len(cfg['experiments'])} experiments", fatal=False)

    if smoke and check.ok:
        smoke_ok, detail, bytes_per_game = smoke_run(cfg, run_dir, data_root, temp_root)
        check.add("smoke run (selfplay -> train -> eval, tiny)", smoke_ok, detail)
        if smoke_ok and bytes_per_game:
            budget = cfg["storage"]["data_budget_gb"]
            free_for_data = min(free_gb(data_root) - r for p, r in storage.reserves if volume_key(p) == volume_key(data_root))
            peak, minimum, detail = data_plan(cfg, bytes_per_game)
            check.add("data plan: the minimum fits", minimum <= min(budget, free_for_data),
                      f"{bytes_per_game / 1e3:.0f} KB/game; {minimum:.1f} GB = the running experiment's full window + the iteration "
                      f"it writes + the latest iteration of every other one (budget {budget} GB, usable now {free_for_data:.0f} GB)")
            check.add("data plan: full windows fit", peak <= min(budget, free_for_data),
                      f"peak {peak:.1f} GB ({detail}); budget {budget} GB, usable now {free_for_data:.0f} GB"
                      + ("" if peak <= min(budget, free_for_data) else
                         " -> the windows will shrink (oldest data deleted, see REPORT); lower games / keep or raise the budget"),
                      fatal=False)
    return check


def data_plan(cfg, bytes_per_game):
    """(peak GB, minimum GB, explanation). Peak of a rung: all its experiments keep full windows and one of them writes one more
    iteration (the window covers finished iterations only). Minimum: what rotation cannot delete - the running experiment's
    window + the iteration it writes + the latest iteration of every other experiment."""
    keep = cfg["storage"]["keep_data_iterations"]
    games = max(e["games_per_iteration"] for e in cfg["experiments"])
    alive = len(cfg["experiments"])
    peak, minimum, parts = 0.0, 0.0, []
    for i, rung in enumerate(cfg["rungs"]):
        per_iteration = games * rung["games_factor"] * bytes_per_game / 1e9
        rung_peak = alive * keep * per_iteration + per_iteration
        rung_min = (keep + 1) * per_iteration + (alive - 1) * per_iteration
        parts.append(f"rung {i}: {alive} exp x {keep} x {per_iteration:.1f} GB + {per_iteration:.1f} = {rung_peak:.1f} GB")
        peak, minimum = max(peak, rung_peak), max(minimum, rung_min)
        alive = min(alive, rung["keep"])
    return peak, minimum, "; ".join(parts)


def smoke_run(cfg, run_dir, data_root, temp_root):
    """A real, tiny iteration of every stage with the real commands. If this works the night will start.

    Returns (ok, detail, bytes of training data per game)."""
    smoke_cfg = json.loads(json.dumps(cfg))
    smoke_cfg["experiments"] = [{"name": "smoke", "games_per_iteration": 40, "matchups": cfg["experiments"][0]["matchups"]}]
    smoke_cfg["selfplay"]["games_per_chunk"] = 20
    smoke_cfg["selfplay"]["stage_max_seconds"] = 300
    smoke_cfg["eval"]["games"] = 20
    smoke_cfg["eval"]["references"] = cfg["eval"]["references"][:1]
    smoke_cfg["retries"] = 1
    smoke_cfg["rungs"] = [{"fraction": 1.0, "keep": 1, "games_factor": 1.0}]
    smoke_cfg["hours"] = 0.5
    smoke_cfg["max_iterations_per_experiment"] = 1
    # two games per slow opponent: proves it starts in the self-play process before the night relies on it
    smoke_cfg["benchmark"]["games"] = 2
    smoke_cfg["benchmark"]["concurrency"] = 2
    smoke_cfg["benchmark"]["min_win_rate"] = 0  # smoke only checks that the opponent process starts
    smoke_dir = Path(run_dir) / "_smoke"
    smoke_data = Path(data_root) / "_smoke_data"
    for d in (smoke_dir, smoke_data):
        shutil.rmtree(d, ignore_errors=True)
    pipeline = Pipeline(smoke_cfg, smoke_dir, data_root=smoke_data, stop=Stop(smoke_dir / "STOP"), quiet=True, temp_root=Path(temp_root) / "_smoke")
    bytes_per_game = 0
    try:
        pipeline.prepare_java()
        pipeline.init_state("smoke")
        status = pipeline.run()
        e = pipeline.state["experiments"]["smoke"]
        ok = status == "finished" and e["iterations_done"] >= 1 and e.get("best_model")
        data = pipeline.data_dir("smoke", 1)
        if ok:
            games = count_games(data)
            size = sum(f.stat().st_size for f in chunk_files(data)) if data.exists() else 0
            bytes_per_game = size / games if games else 0
            # the result of the smoke run is also the sample of what the morning pack looks like
            final, archive = pack(smoke_dir, update_journal=False)
            ok = (final / "best_model").exists() and (final / "REPORT.md").exists()
        detail = f"status={status}, iterations={e['iterations_done']}, score={e.get('best_score')}"
        failed_benchmarks = [b["ref"] for b in (pipeline.state.get("benchmark") or {}).values() if b.get("failed")]
        if failed_benchmarks:
            ok = False
            detail += f"; benchmark opponents do not start: {failed_benchmarks} (see {smoke_dir / 'benchmark'})"
        if not ok:
            detail += f"; see {smoke_dir / 'errors'} and {smoke_dir / 'exp'}"
        return bool(ok), detail, bytes_per_game
    except Exception as ex:
        return False, f"{type(ex).__name__}: {ex}\n{traceback.format_exc()[-800:]}", 0
    finally:
        shutil.rmtree(smoke_data, ignore_errors=True)
        shutil.rmtree(Path(temp_root) / "_smoke", ignore_errors=True)


# ------------------------------------------------------------------------------------------------ command line

def run_dir_for(cfg, name):
    return storage_paths(cfg, name)[0]


def cmd_check(args):
    cfg = load_config(args.config)
    name = args.name or "check"
    run_dir, data_root, temp_root = storage_paths(cfg, name)
    run_dir.mkdir(parents=True, exist_ok=True)
    check = preflight(cfg, run_dir, data_root, temp_root, smoke=not args.quick)
    print("\nREADY: the pipeline can be started" if check.ok else "\nNOT READY: fix the FAIL lines above")
    return EXIT_OK if check.ok else EXIT_BAD_CHECK


def cmd_run(args):
    cfg = load_config(args.config)
    if args.hours is not None:
        cfg["hours"] = args.hours if args.hours > 0 else None
    run_dir, data_root, temp_root = storage_paths(cfg, args.name)
    run_dir.mkdir(parents=True, exist_ok=True)
    existing = read_json(run_dir / "state.json", None)
    if getattr(args, "start_from", None):
        if existing is not None:
            print(f"--start-from ignored: run {args.name} already exists and continues with its own models")
        else:
            for e in cfg["experiments"]:
                e.setdefault("start_model", args.start_from)
    if existing is not None:
        # a continued run keeps the start models it began with (they are its iteration 0)
        for e in cfg["experiments"]:
            e.pop("start_model", None)
    elif cfg["storage"].get("delete_data_of_other_runs", True):
        freed = delete_data_of_other_runs(cfg, args.name)
        if freed:
            print(f"freed {freed:.1f} GB of self-play data of earlier runs (models and reports kept)")
    if not args.skip_check:
        check = preflight(cfg, run_dir, data_root, temp_root, smoke=existing is None)
        if not check.ok:
            print("\nNOT STARTED: the check failed (use --skip-check only if you know why)")
            return EXIT_BAD_CHECK
    write_json(run_dir / "config.json", cfg)
    stop = Stop(run_dir / "STOP")
    if (run_dir / "STOP").exists():
        (run_dir / "STOP").unlink()  # a stale flag of a previous stop must not stop the new start
    signal.signal(signal.SIGINT, lambda *_: stop.set())
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, lambda *_: stop.set())

    pipeline = Pipeline(cfg, run_dir, data_root=data_root, stop=stop, temp_root=temp_root)
    try:
        pipeline.lock.acquire()
    except AlreadyRunning as e:
        print(f"NOT STARTED: {e}")
        return EXIT_FAILED
    try:
        return run_locked(pipeline, cfg, run_dir, data_root, temp_root, args.name)
    finally:
        pipeline.lock.release()


def run_locked(pipeline, cfg, run_dir, data_root, temp_root, name):
    args = argparse.Namespace(name=name)
    if pipeline.state is None:
        pipeline.init_state(args.name)
        pipeline.log(f"NEW RUN {args.name}: {budget_label(cfg)}, experiments {[e['name'] for e in cfg['experiments']]}")
        pipeline.log(f"run folder {run_dir}, data {data_root}, temp {temp_root}")
    else:
        pipeline.state["status"] = "running"
        pipeline.state["budget_seconds"] = budget_seconds(cfg)
        worked = pipeline.elapsed() / 3600
        if pipeline.has_time_budget():
            left = max(0.0, pipeline.budget() - pipeline.elapsed()) / 3600
            pipeline.log(f"RESUMING {args.name}: worked {worked:.2f} h of {budget_label(cfg)}, "
                         f"{left:.2f} h left (stopped time does not count)")
        else:
            pipeline.log(f"RESUMING {args.name}: worked {worked:.2f} h, runs until stop "
                         f"(stopped time does not count)")
        for exp_name, e in pipeline.state["experiments"].items():
            if e["state"] == "failed":
                # a restart is a new chance: the failures may have been caused by the environment (reboot, full disk, ...)
                e["state"] = "active"
                e["failures"] = 0
                pipeline.log(f"experiment {exp_name} was dropped after failures, trying it again")
        if pipeline.has_time_budget() and pipeline.elapsed() >= pipeline.budget():
            pipeline.log("the optional time budget is used up: packing what exists. "
                         "Clear `hours` (or --hours 0) to continue until stop, or raise hours")
    pipeline.prepare_java()
    pipeline.write_status()
    status = pipeline.run()
    final, archive = pack(run_dir)
    pipeline.log(f"FINISHED with status {status}. Hand over: {archive}")
    print(f"\nRESULT FOLDER: {final}\nZIP:           {archive}")
    return {"finished": EXIT_OK, "stopped": EXIT_STOPPED, "stopped_disk_low": EXIT_DISK}.get(status, EXIT_FAILED)


def cmd_status(args):
    cfg = load_config(args.config) if args.config else DEFAULT_CONFIG
    run_dir = run_dir_for(cfg, args.name)
    status = read_json(run_dir / "status.json", None)
    if status is None:
        print(f"no run named {args.name} in {run_dir.parent}")
        return EXIT_FAILED
    print(json.dumps(status, indent=2, ensure_ascii=False))
    return EXIT_OK


def cmd_stop(args):
    cfg = load_config(args.config) if args.config else DEFAULT_CONFIG
    run_dir = run_dir_for(cfg, args.name)
    run_dir.mkdir(parents=True, exist_ok=True)
    (run_dir / "STOP").write_text("stop")
    print(f"stop requested for {args.name}; the running pipeline finishes by itself and packs the result.\n"
          f"To continue later run the same `run` command again: nothing is lost and the stopped time does not count.")
    return EXIT_OK


def cmd_pack(args):
    cfg = load_config(args.config) if args.config else DEFAULT_CONFIG
    final, archive = pack(run_dir_for(cfg, args.name))
    print(f"RESULT FOLDER: {final}\nZIP:           {archive}")
    return EXIT_OK


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    for name, fn in (("check", cmd_check), ("run", cmd_run), ("status", cmd_status), ("stop", cmd_stop), ("pack", cmd_pack)):
        p = sub.add_parser(name)
        p.add_argument("--config", default=None)
        p.add_argument("--name", default=None if name == "check" else "night")
        p.set_defaults(fn=fn)
        if name == "check":
            p.add_argument("--quick", action="store_true", help="skip the smoke run")
        if name == "run":
            p.add_argument("--skip-check", action="store_true")
            p.add_argument("--hours", type=float, default=None,
                           help="optional timed budget in hours (overrides config). 0 = until stop. Default config runs until stop.")
            p.add_argument("--start-from", default=None,
                           help="NEW run only: experiments without their own start_model start from this model folder or from "
                                "the best model of this earlier run (name or folder)")
    args = parser.parse_args(argv)
    return args.fn(args)


if __name__ == "__main__":
    sys.exit(main())
