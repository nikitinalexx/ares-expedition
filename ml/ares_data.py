"""Reader of the binary decision files written by the Java simulator (DecisionFile.java).

Usage:
    schema, rows = read_decisions("run1/PICK_PHASE.ares")
    rows["obs"]      float16 [n, obsSize]   what the player saw (convert to float32 per batch)
    rows["count"]    uint8   [n]            number of legal candidates
    rows["ids"]      uint8 (version 2) or uint16 (version 3) [n, maxCand]   candidate ids, padded with 0.
                                    Version 3 exists because a card id does not fit in one byte.
    rows["feat"]     float32 [n, maxCand, candFeatures]
    rows["chosen"]   uint8   [n]            index of the chosen candidate
    rows["seat"]     uint8   [n]            0/1: player index in the game (by name)
    rows["context"]  uint8   [n]            who played everything the agent does not decide yet: 0 SMART, 1 RANDOM
    rows["round"]    uint16  [n]
    rows["outcome"]  float32 [n]            result of the game for the decision maker: 1 win / 0.5 draw / 0 loss
    rows["margin"]   float32 [n]            win points of the decision maker minus the opponent
    rows["game"]     uint64  [n]            unique over the whole run, also across restarts of the simulator
    rows["step"]     uint16  [n]            decision number of this player within the game
"""
import json
import struct

import numpy as np

MAGIC = b"ARES"
VERSIONS = (2, 3)


def record_dtype(schema, version=2):
    """version 2 stores candidate ids in one byte, version 3 in two (card ids are larger than 255)."""
    if version not in VERSIONS:
        raise ValueError(f"unsupported version {version}")
    obs, cand, feat = schema["obsSize"], schema["maxCandidates"], schema["candidateFeatures"]
    return np.dtype([
        ("obs", "<f2", (obs,)),
        ("count", "u1"),
        ("ids", "u1" if version < 3 else "<u2", (cand,)),
        ("feat", "<f4", (cand, feat)),
        ("chosen", "u1"),
        ("seat", "u1"),
        ("context", "u1"),
        ("round", "<u2"),
        ("outcome", "<f4"),
        ("margin", "<f4"),
        ("game", "<u8"),
        ("step", "<u2"),
    ])


def read_decisions(path, expected_hash=None):
    """Returns (schema dict, numpy structured array). Fails loudly on anything unexpected."""
    with open(path, "rb") as f:
        raw = f.read()
    if raw[:4] != MAGIC:
        raise ValueError(f"{path} is not an ARES decision file")
    version, json_length = struct.unpack_from("<HI", raw, 4)
    if version not in VERSIONS:
        raise ValueError(f"unsupported version {version}")
    offset = 10 + json_length
    schema = json.loads(raw[10:offset].decode("utf-8"))
    if expected_hash is not None and schema["hash"] != expected_hash:
        raise ValueError(f"schema hash {schema['hash']} != expected {expected_hash}: the data was produced by a different encoder")

    dtype = record_dtype(schema, version)
    body = len(raw) - offset
    if body % dtype.itemsize != 0:
        raise ValueError(f"file is truncated or corrupt: {body} bytes is not a multiple of the record size {dtype.itemsize}")
    rows = np.frombuffer(raw, dtype=dtype, count=body // dtype.itemsize, offset=offset)
    return schema, rows


def open_memmap(path, expected_hash=None):
    """Like read_decisions, but the rows stay on the disk (numpy memmap): safe for files much larger than the RAM.

    Random access on an SSD is fast; on a hard disk read in order (whole chunks) and shuffle inside a buffer.
    """
    with open(path, "rb") as f:
        head = f.read(10)
        if head[:4] != MAGIC:
            raise ValueError(f"{path} is not an ARES decision file")
        version, json_length = struct.unpack("<HI", head[4:])
        if version not in VERSIONS:
            raise ValueError(f"unsupported version {version}")
        schema = json.loads(f.read(json_length).decode("utf-8"))
    if expected_hash is not None and schema["hash"] != expected_hash:
        raise ValueError(f"schema hash {schema['hash']} != expected {expected_hash}: the data was produced by a different encoder")
    dtype = record_dtype(schema, version)
    offset = 10 + json_length
    import os
    body = os.path.getsize(path) - offset
    if body % dtype.itemsize != 0:
        raise ValueError(f"file is truncated or corrupt: {body} bytes is not a multiple of the record size {dtype.itemsize}")
    count = body // dtype.itemsize
    if count == 0:
        return schema, np.empty(0, dtype=dtype)
    return schema, np.memmap(path, dtype=dtype, mode="r", offset=offset, shape=(count,))


def chunk_paths(directory, decision_type):
    """Finished chunks of a decision type in the order they were written (unfinished .tmp files are never listed)."""
    from pathlib import Path
    return sorted(Path(directory).glob(f"{decision_type}.[0-9]*.ares"))


def discover_decisions(data_dirs):
    """Decision type names that have at least one finished chunk in any of the comma-separated directories.

    Names come from the file prefix (`PICK_PHASE.000001.ares` → `PICK_PHASE`), sorted for stable training order.
    """
    from pathlib import Path
    found = set()
    for directory in [d for d in (data_dirs.split(",") if isinstance(data_dirs, str) else data_dirs) if d]:
        for path in Path(directory).glob("*.[0-9][0-9][0-9][0-9][0-9][0-9].ares"):
            name = path.name
            # TYPE.NNNNNN.ares — drop the two trailing dotted parts
            parts = name.rsplit(".", 2)
            if len(parts) == 3 and parts[1].isdigit() and parts[2] == "ares":
                found.add(parts[0])
    return sorted(found)


def validate(schema, rows):
    """Cheap sanity checks every training run should do before it starts."""
    problems = []
    if len(rows) == 0:
        return ["no rows"]
    if not np.isfinite(rows["obs"]).all():
        problems.append("observation contains NaN/Inf")
    if not (rows["count"] >= 1).all() or not (rows["count"] <= schema["maxCandidates"]).all():
        problems.append("candidate count out of range")
    if not (rows["chosen"] < rows["count"]).all():
        problems.append("chosen is not a legal candidate")
    if not np.isin(rows["outcome"], [0.0, 0.5, 1.0]).all():
        problems.append("outcome is not 0 / 0.5 / 1")
    if not np.all(np.sign(rows["margin"]) == np.sign(rows["outcome"] - 0.5)):
        problems.append("margin disagrees with outcome")
    return problems
