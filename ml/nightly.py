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
    os.replace(tmp, path)


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
    """JAVA_HOME for Maven: the folder above bin/."""
    return str(Path(java_bin).resolve().parent.parent)


# ------------------------------------------------------------------------------------------------ configuration

DEFAULT_CONFIG = {
    "hours": 8.0,                              # budget of ACTIVE working time (stopped time does not count); raise it to continue a finished run
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
        "reserves": [],              # [{"path": "C:/", "gb": 20}, {"path": "D:/", "gb": 30}]
        "default_reserve_gb": 30,    # for run_root, data_root and temp_root when they are not listed
        "data_budget_gb": 150,       # this run never keeps more than this much training data
        "keep_data_iterations": 3,   # training window: the data of older iterations is deleted (models never)
        "guard_interval_seconds": 5,
    },
    "retries": 3,
    "retry_backoff_seconds": [10, 60, 300],
    "max_consecutive_failures": 3,
    "selfplay": {"games_per_chunk": 500, "stage_max_seconds": 3600, "grace_seconds": 90, "max_failure_ratio": 0.05},
    "train": {"command": ["{python}", "ml/train_stub.py", "--data", "{data}", "--out", "{model_dir}", "--prev", "{prev_model}"], "timeout_seconds": 3600},
    "eval": {"games": 200, "references": ["SMART", "RANDOM"], "agent": "AGENT", "concurrency": 32, "timeout_seconds": 1800},
    # successive halving: each rung gets a fraction of the night; only `keep` best experiments continue to the next rung
    "rungs": [{"fraction": 0.3, "keep": 2, "games_factor": 0.25}, {"fraction": 0.7, "keep": 1, "games_factor": 1.0}],
    "experiments": [{"name": "default", "games_per_iteration": 2000, "matchups": "AGENT:AGENT:1"}],
    "low_priority": True,                    # child processes run below normal priority: you can work on the machine meanwhile
    "promote_margin": 0.0,
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
    fractions = sum(r["fraction"] for r in cfg["rungs"])
    if not 0.99 <= fractions <= 1.01:
        raise SystemExit(f"rung fractions must add up to 1, got {fractions}")
    return cfg


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
        reserves = {}
        for entry in s["reserves"]:
            reserves[str(entry["path"])] = max(entry["gb"], reserves.get(str(entry["path"]), 0))
        for path in (run_dir, data_root, temp_root):
            reserves.setdefault(str(path), s["default_reserve_gb"])
        self.reserves = [(Path(p), gb) for p, gb in reserves.items()]

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
        """Cheap check used while a command runs. Returns a message when the command must stop."""
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
        return self.state["budget_seconds"]

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
            "worked_hours": round(self.elapsed() / 3600, 2) if st.get("budget_seconds") else None,
            "budget_hours": round(st["budget_seconds"] / 3600, 2) if st.get("budget_seconds") else None,
            "remaining_hours": round((st["budget_seconds"] - self.elapsed()) / 3600, 2) if st.get("budget_seconds") else None,
            "finishes_at_if_running": stamp(now() + st["budget_seconds"] - self.elapsed()) if st.get("budget_seconds") else None,
            "rung": st.get("rung"),
            "current": self.current,
            "free_disk_gb": {str(p): round(free_gb(p), 1) for p, _ in self.storage.reserves},
            "data_gb": round(self.storage.data_gb(), 1),
            "last_error": self.last_error,
            "experiments": {
                name: {k: e.get(k) for k in ("state", "iterations_done", "best_score", "scores", "failures")}
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

    def with_retries(self, exp, it, stage, attempt):
        """attempt() -> (ok, Result, log_path). Retries with a backoff; raises StopRequested on stop."""
        retries = self.cfg["retries"]
        backoff = self.cfg["retry_backoff_seconds"]
        for n in range(retries):
            self.stop.check()
            ok, result, log_path = attempt()
            if ok:
                self.failed_attempts_in_a_row = 0
                return True
            if result.reason == "disk" or result.code == 5:
                self.record_failure(exp, it, stage, result, log_path, extra="not enough disk space")
                if self.prune_old_data() and not self.storage.violations():
                    self.log("freed space by deleting old training data, trying again")
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
        return False

    # ---- stages
    def stage_selfplay(self, exp, exp_cfg, it, games_target):
        done_marker = self.it_dir(exp, it) / "selfplay.done"
        if done_marker.exists():
            return True
        out = self.data_dir(exp, it)
        out.mkdir(parents=True, exist_ok=True)
        log_path = self.it_dir(exp, it) / "selfplay.log"
        self.it_dir(exp, it).mkdir(parents=True, exist_ok=True)
        sp = self.cfg["selfplay"]

        def attempt():
            self.check_disk()
            # chunks of a previous attempt count: only the missing games are played
            for tmp in out.glob("*.tmp"):
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
            cmd = self.java_cmd("--mode", "play", "--out", out.as_posix(), "--games", str(remaining),
                                "--games-per-chunk", str(sp["games_per_chunk"]), "--matchups", exp_cfg["matchups"],
                                "--max-seconds", str(sp["stage_max_seconds"]), "--stop-file", stop_file.as_posix(),
                                "--max-failure-ratio", str(sp["max_failure_ratio"]),
                                # the Java process also guards itself, in case this supervisor dies
                                "--min-free-gb", str(max((r for p, r in self.storage.reserves if volume_key(p) == volume_key(out)), default=0)),
                                "--threads", str(self.cfg["java"]["threads"] or os.cpu_count()))
            result = run_command(cmd, log_path, sp["stage_max_seconds"] + 600, self.stop, graceful_file=stop_file, grace=sp["grace_seconds"],
                                 env=self.storage.child_env(), guard=self.storage.guard, guard_interval=self.storage.interval, tick=self.tick,
                                 low_priority=self.cfg["low_priority"])
            quarantine_bad_chunks(out, self.run_dir / "quarantine", self.log)
            have = count_games(out)
            # exit 3 = the time limit of the stage: fewer games than planned is a legitimate result, exit 4/1/2 are failures
            ok = result.code in (0, 3) and have > 0 and result.reason == "exit"
            return ok, result, log_path

        ok = self.with_retries(exp, it, "selfplay", attempt)
        if ok:
            write_json(done_marker, {"games": count_games(out), "time": stamp()})
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
            cmd = self.template(tr["command"], data=",".join(str(w) for w in window), model_dir=str(model_dir),
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

    def stage_eval(self, exp, it):
        done_marker = self.it_dir(exp, it) / "eval.done"
        if done_marker.exists():
            return read_json(self.it_dir(exp, it) / "eval.json")
        ev = self.cfg["eval"]
        log_path = self.it_dir(exp, it) / "eval.log"
        scores = {}

        def attempt():
            self.check_disk()
            for ref in ev["references"]:
                path = self.it_dir(exp, it) / f"eval_vs_{ref}.json"
                if path.exists():
                    scores[ref] = read_json(path)
                    continue
                self.current = {"exp": exp, "iteration": it, "stage": f"eval vs {ref}"}
                self.write_status()
                cmd = self.java_cmd("--mode", "compare", "--a", ev["agent"], "--b", ref, "--games", str(ev["games"]),
                                    "--concurrency", str(ev["concurrency"]), "--out", path.as_posix())
                result = run_command(cmd, log_path, ev["timeout_seconds"], self.stop, env=self.storage.child_env(),
                                     guard=self.storage.guard, guard_interval=self.storage.interval, tick=self.tick,
                                 low_priority=self.cfg["low_priority"])
                if not (result.ok and path.exists()):
                    return False, result, log_path
                scores[ref] = read_json(path)
            return True, Result(0, "exit", 0), log_path

        if not self.with_retries(exp, it, "eval", attempt):
            return None
        summary = {
            "score": scores[ev["references"][0]]["aScore"],
            "vs": {ref: {"score": s["aScore"], "low": s["aScoreCi95Low"], "high": s["aScoreCi95High"], "games": s["games"]} for ref, s in scores.items()},
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
            for i in range(1, e.get("iterations_done", 0) - self.storage.keep + 1):
                d = self.data_dir(exp, i)
                if d.exists() and not (exp == protect_exp and i == protect_it):
                    self.storage.delete_data_dir(d)
                    removed += 1
        return removed

    def check_disk(self):
        """Before every stage: below a reserve -> delete old data once, still below -> stop the run (nothing is lost)."""
        problems = self.storage.violations()
        if problems or self.storage.over_budget():
            removed = self.prune_old_data()
            self.log(f"disk: {problems or 'over the data budget'}; deleted {removed} old data folders")
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
        if best is None or summary["score"] > best + self.cfg["promote_margin"]:
            e["best_score"] = round(summary["score"], 4)
            e["best_model"] = str(self.it_dir(exp, it) / "model")
            e["best_iteration"] = it
            self.log(f"{exp} it{it}: score {summary['score']:.3f} -> PROMOTED to best")
        else:
            self.log(f"{exp} it{it}: score {summary['score']:.3f} (best stays {best:.3f}, it{e.get('best_iteration')})")
        self.save_state()
        self.prune_old_data(protect_exp=exp, protect_it=it)
        return True

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
            "name": name, "status": "running", "started": started, "budget_seconds": self.cfg["hours"] * 3600, "active_seconds": 0.0,
            "rung": 0, "rung_windows": {},
            "experiments": {e["name"]: {"state": "active", "iterations_done": 0, "scores": [], "failures": 0, "best_score": None}
                            for e in self.cfg["experiments"]},
        }
        self.save_state()

    def exp_cfg(self, name):
        return next(e for e in self.cfg["experiments"] if e["name"] == name)

    def ranked_active(self):
        active = [n for n, e in self.state["experiments"].items() if e["state"] == "active"]
        return sorted(active, key=lambda n: -(self.state["experiments"][n].get("best_score") or -1))

    def run(self):
        st = self.state
        if st.get("budget_for_windows") != st["budget_seconds"]:
            # the budget changed (the config was edited): the windows of the unfinished rungs are planned again
            for key in [k for k in st["rung_windows"] if int(k) >= st["rung"]]:
                del st["rung_windows"][key]
            st["budget_for_windows"] = st["budget_seconds"]
        if st["rung"] >= len(self.cfg["rungs"]) and self.elapsed() < self.budget():
            # a finished run that got more time (hours raised): the survivors of the last rung simply go on working
            st["rung"] = len(self.cfg["rungs"]) - 1
            st["rung_windows"].pop(str(st["rung"]), None)
            self.log("the run had finished but there is time left: continuing the last rung")
        try:
            for rung_index, rung in enumerate(self.cfg["rungs"]):
                if st["rung"] > rung_index:
                    continue
                st["rung"] = rung_index
                active = [n for n, e in st["experiments"].items() if e["state"] == "active"]
                if not active:
                    break
                # window of the rung is fixed once, so restarts neither extend nor shorten it
                windows = st["rung_windows"].setdefault(str(rung_index), {})
                if not windows:
                    rung_end = self.budget() * sum(r["fraction"] for r in self.cfg["rungs"][:rung_index + 1])
                    start = self.elapsed()
                    slot = max(0.0, (rung_end - start) / len(active))
                    for i, n in enumerate(active):
                        windows[n] = [start + i * slot, start + (i + 1) * slot]
                    self.save_state()
                self.log(f"=== rung {rung_index}: {active} (keep {rung['keep']}, games factor {rung['games_factor']})")
                for n in active:
                    self.run_slot(n, windows[n][1], rung["games_factor"])
                # drop the weaker ones - but only on facts: nobody is dropped for lack of time, i.e. without a score
                ranked = self.ranked_active()
                scored = [n for n in ranked if st["experiments"][n].get("best_score") is not None]
                losers = ranked[rung["keep"]:] if len(scored) >= len(ranked) else [n for n in scored[rung["keep"]:]]
                for loser in losers:
                    st["experiments"][loser]["state"] = "eliminated"
                    self.log(f"experiment {loser} eliminated after rung {rung_index} (best score {st['experiments'][loser].get('best_score')})")
                st["rung"] = rung_index + 1
                self.save_state()
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

    def run_slot(self, name, slot_end, games_factor):
        e = self.state["experiments"][name]
        hard_end = self.budget()
        limit = self.cfg.get("max_iterations_per_experiment")
        while e["state"] == "active":
            self.stop.check()
            if limit and e["iterations_done"] >= limit:
                return
            remaining = min(slot_end, hard_end) - self.elapsed()
            estimate = (e.get("last_iteration_seconds") or 0) * 1.1
            if remaining <= 0 or (estimate and remaining < estimate):
                self.log(f"{name}: slot over ({remaining / 60:.0f} min left, an iteration takes {estimate / 60:.0f} min)")
                return
            self.run_iteration(name, self.exp_cfg(name), games_factor)


# ------------------------------------------------------------------------------------------------ data helpers

def chunk_files(directory):
    return sorted(Path(directory).glob("*.[0-9]*.ares"))


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

def pack(run_dir, handoff_root=None):
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

    errors = sorted((run_dir / "errors").glob("*.txt")) if (run_dir / "errors").exists() else []
    lines.append(f"errors: {len(errors)} (files in `logs/`; each has the command, the exit code and the end of the process output)" + (f"; last: {status.get('last_error')}" if status.get("last_error") else ""))
    for f in errors[-15:]:
        head = f.read_text(encoding="utf-8", errors="replace").splitlines()[:2]
        lines.append(f"- `{f.name}`: " + " | ".join(h.strip() for h in head))
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
    (work / "metrics.csv").write_text("\n".join(rows) + "\n", encoding="utf-8")

    # logs: tails only, errors in full
    logs = work / "logs"
    logs.mkdir()
    if (run_dir / "log.txt").exists():
        (logs / "pipeline_log_tail.txt").write_text(tail(run_dir / "log.txt", 500), encoding="utf-8")
    for f in errors:
        shutil.copyfile(f, logs / f.name)
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

    os.replace(work, final)
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
        check.add("java >= 21", True, f"{java} (version {major})")
    else:
        check.add("java >= 21", False, "none found (tried java.bin, JAVA_HOME, PATH, ~/.jdks, Program Files); install a JDK 21+ or set java.bin in the config")
        return check

    # ---- storage: where everything goes and that the reserves are respected right now
    storage = Storage(cfg, run_dir, data_root, temp_root)
    for label, path in (("run state, models, logs", run_dir), ("training data", data_root), ("temp files of the children", temp_root)):
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
            check.add(f"maven: {label}", r.returncode == 0, "" if r.returncode == 0 else (r.stdout + r.stderr)[-600:])
            if r.returncode != 0:
                return check
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

    if smoke and check.ok:
        smoke_ok, detail, bytes_per_game = smoke_run(cfg, run_dir, data_root, temp_root)
        check.add("smoke run (selfplay -> train -> eval, tiny)", smoke_ok, detail)
        if smoke_ok and bytes_per_game:
            budget = cfg["storage"]["data_budget_gb"]
            for e in cfg["experiments"]:
                per_iteration = e["games_per_iteration"] * bytes_per_game / 1e9
                window = per_iteration * cfg["storage"]["keep_data_iterations"]
                free_for_data = min(free_gb(data_root) - r for p, r in storage.reserves if volume_key(p) == volume_key(data_root))
                fits = window <= budget and window <= free_for_data
                check.add(f"data plan '{e['name']}'", fits,
                          f"{bytes_per_game / 1e3:.0f} KB/game -> {per_iteration:.1f} GB per iteration, {window:.1f} GB for the window of "
                          f"{cfg['storage']['keep_data_iterations']} (budget {budget} GB, usable now {free_for_data:.0f} GB)")
    return check


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
            final, archive = pack(smoke_dir)
            ok = (final / "best_model").exists() and (final / "REPORT.md").exists()
        detail = f"status={status}, iterations={e['iterations_done']}, score={e.get('best_score')}"
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
    if args.hours:
        cfg["hours"] = args.hours
    run_dir, data_root, temp_root = storage_paths(cfg, args.name)
    run_dir.mkdir(parents=True, exist_ok=True)
    if not args.skip_check:
        existing = read_json(run_dir / "state.json", None)
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
        pipeline.log(f"NEW RUN {args.name}: {cfg['hours']} h, experiments {[e['name'] for e in cfg['experiments']]}")
        pipeline.log(f"run folder {run_dir}, data {data_root}, temp {temp_root}")
    else:
        pipeline.state["status"] = "running"
        pipeline.state["budget_seconds"] = cfg["hours"] * 3600
        pipeline.log(f"RESUMING {args.name}: worked {pipeline.elapsed() / 3600:.2f} h of {cfg['hours']} h, "
                     f"{max(0.0, pipeline.budget() - pipeline.elapsed()) / 3600:.2f} h left (the time while it was stopped does not count)")
        for exp_name, e in pipeline.state["experiments"].items():
            if e["state"] == "failed":
                # a restart is a new chance: the failures may have been caused by the environment (reboot, full disk, ...)
                e["state"] = "active"
                e["failures"] = 0
                pipeline.log(f"experiment {exp_name} was dropped after failures, trying it again")
        if pipeline.elapsed() >= pipeline.budget():
            pipeline.log("the time budget of this run is used up: packing what exists. To continue raise `hours` in the config "
                         "(or run with --hours N)")
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
            p.add_argument("--hours", type=float, default=None, help="working time budget (overrides the config; raise it to continue a finished run)")
    args = parser.parse_args(argv)
    return args.fn(args)


if __name__ == "__main__":
    sys.exit(main())
