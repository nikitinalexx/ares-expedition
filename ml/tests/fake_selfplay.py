"""Stand-in for the Java SelfPlayMain used by the pipeline tests: same command line, same files, same exit codes.

Behaviour is controlled with environment variables:
  FAKE_MODE=ok          play normally (default)
  FAKE_MODE=crash       exit 1 immediately
  FAKE_MODE=fail_first  exit 1 the first time (marker file next to --out), then behave normally
  FAKE_MODE=diskfull    exit 5 (the Java process found the disk almost full)
  FAKE_MODE=hang        never finishes (killed by the pipeline timeout / stop)
  FAKE_MODE=corrupt     write a chunk that is truncated, as a damaged file would be
  FAKE_SCORE=0.7        score reported in --mode compare (default: derived from the output path, so experiments differ)
  FAKE_SCORES=SMART:0.8,RANDOM:0.55  scores of an agent WITH a model per opponent;  FAKE_BASELINE=0.5  score of the agent without a model
  FAKE_ARGS_LOG=file    every call is appended there as JSON
  FAKE_GAME_SECONDS=0.05  time per game (default 0)
"""
import json
import os
import struct
import sys
import time
import zlib
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ares_data import record_dtype  # noqa: E402

OBS = 12


def args_of(argv):
    return {argv[i][2:]: argv[i + 1] for i in range(0, len(argv), 2)}


def write_chunk(path, games, first_game, rows_per_game=6, truncate=False):
    schema = {"type": "PICK_PHASE", "obsSize": OBS, "maxCandidates": 5, "candidateFeatures": 6, "obsDtype": "f2",
              "hash": "fake0000fake0000", "observationLayout": "fake"}
    dtype = record_dtype(schema)
    n = games * rows_per_game
    rows = np.zeros(n, dtype=dtype)
    rng = np.random.default_rng(first_game)
    rows["obs"] = rng.random((n, OBS), dtype=np.float32)
    rows["count"] = 4
    rows["ids"][:, :4] = np.arange(1, 5)
    rows["chosen"] = rng.integers(0, 4, n)
    game_ids = np.repeat(np.arange(first_game, first_game + games), rows_per_game)
    rows["game"] = game_ids
    rows["seat"] = rng.integers(0, 2, n)
    outcome = (game_ids * 2654435761 % 3).astype(np.float32) / 2.0
    rows["outcome"] = outcome
    rows["margin"] = np.where(outcome > 0.5, 5.0, np.where(outcome < 0.5, -5.0, 0.0))
    rows["step"] = np.tile(np.arange(rows_per_game), games)
    header = json.dumps(schema).encode()
    data = b"ARES" + struct.pack("<HI", 2, len(header)) + header + rows.tobytes()
    if truncate:
        data = data[:-7]
    tmp = str(path) + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.replace(tmp, path)


def main():
    a = args_of(sys.argv[1:])
    if os.environ.get("FAKE_ARGS_LOG"):  # lets the tests see exactly how the pipeline called the Java process
        with open(os.environ["FAKE_ARGS_LOG"], "a") as log:
            log.write(json.dumps(a) + chr(10))
    mode = os.environ.get("FAKE_MODE", "ok")
    out = Path(a["out"])
    if mode == "crash":
        print("FAKE crash", file=sys.stderr)
        return 1
    if mode == "diskfull":
        print("FAKE: disk almost full", file=sys.stderr)
        return 5
    if mode == "fail_first":
        marker = out.parent / (out.name + ".failed_once")
        out.parent.mkdir(parents=True, exist_ok=True)
        if not marker.exists():
            marker.write_text("x")
            print("FAKE first attempt fails", file=sys.stderr)
            return 1
    if mode == "hang":
        time.sleep(3600)

    if a.get("mode") == "compare":
        score = float(os.environ.get("FAKE_SCORE", (zlib.crc32(str(out).encode()) % 1000) / 1000.0))
        if "model" not in a:  # the agent without a model: the baseline
            score = float(os.environ.get("FAKE_BASELINE", score))
        elif os.environ.get("FAKE_SCORES"):  # "SMART:0.8,RANDOM:0.55"
            score = float(dict(x.split(":") for x in os.environ["FAKE_SCORES"].split(","))[a["b"]])
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps({"aScore": score, "aScoreCi95Low": score - 0.1, "aScoreCi95High": score + 0.1, "games": int(a["games"]), "failedGames": 0}))
        return 0

    out.mkdir(parents=True, exist_ok=True)
    for tmp in out.glob("*.tmp"):
        tmp.unlink()
    existing = [int(p.name.split(".")[1]) for p in out.glob("PICK_PHASE.[0-9]*.ares")]
    chunk = max(existing, default=0) + 1
    games = int(a["games"])
    per_chunk = int(a.get("games-per-chunk", "500"))
    stop_file = Path(a["stop-file"]) if "stop-file" in a else None
    game_seconds = float(os.environ.get("FAKE_GAME_SECONDS", "0"))
    played = 0
    first_game = (chunk - 1) * 1_000_000
    while played < games:
        if stop_file and stop_file.exists():
            print("FAKE: stop file seen")
            return 3
        n = min(per_chunk, games - played)
        time.sleep(game_seconds * n)
        write_chunk(out / f"PICK_PHASE.{chunk:06d}.ares", n, first_game + played, truncate=(mode == "corrupt" and chunk == 1))
        chunk += 1
        played += n
    print(f"FAKE played {played}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
