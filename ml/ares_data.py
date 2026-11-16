"""Reader of the binary decision files written by the Java simulator (DecisionFile.java).

Usage:
    schema, rows = read_decisions("run1/PICK_PHASE.ares")
    rows["obs"]      float16 [n, obsSize]   what the player saw (convert to float32 per batch)
    rows["count"]    uint8   [n]            number of legal candidates
    rows["ids"]      uint8 (v2) or uint16 (v3+) [n, maxCand]   candidate ids, padded with 0
    rows["feat"]     float32 (v2/v3) or float16 (v4+) [n, maxCand, candFeatures]
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
VERSIONS = (2, 3, 4)
CURRENT_VERSION = 4


def record_dtype(schema, version=CURRENT_VERSION):
    """v2: ids u8, feat f32. v3: ids u16, feat f32. v4: ids u16, feat f16 (candidates are normalized like observations)."""
    if version not in VERSIONS:
        raise ValueError(f"unsupported version {version}")
    obs, cand, feat = schema["obsSize"], schema["maxCandidates"], schema["candidateFeatures"]
    return np.dtype([
        ("obs", "<f2", (obs,)),
        ("count", "u1"),
        ("ids", "u1" if version < 3 else "<u2", (cand,)),
        ("feat", "<f4" if version < 4 else "<f2", (cand, feat)),
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


class PackedObs:
    """Rows of a packed dataset (see pack_dataset.py), addressed like one array: obs[i] / obs[[...]] / obs[a:b].

    The pack keeps one memmap per decision type and the pool is a selection over all of them, so a plain numpy array
    would have to hold every row. This class gathers only what a batch asks for, which is what the trainer does anyway,
    and keeps the OS page cache as the only cache between epochs (an epoch over a 2.4-million-row pool is ~5 GB, so
    after the first pass it is read at memory speed instead of hard-disk speed).
    """

    def __init__(self, arrays, sizes, width, selection=None, drop_columns=None):
        self.arrays = list(arrays)
        self.width = int(width)
        self.selection = None if selection is None else np.asarray(selection, dtype=np.int64)
        self.drop = np.asarray(sorted(drop_columns), dtype=np.int64) if drop_columns else None
        self._starts = np.r_[0, np.cumsum(sizes)].astype(np.int64)
        self.shape = (len(self.selection) if self.selection is not None else int(sum(sizes)), self.width)
        self.dtype = np.dtype("<f2")

    def __len__(self):
        return self.shape[0]

    def _gather_global(self, idx):
        """Rows by their global number in the pack (no selection mapping)."""
        idx = np.asarray(idx, dtype=np.int64)
        out = np.empty((len(idx), self.width), dtype=np.float16)
        type_of = np.searchsorted(self._starts, idx, side="right") - 1
        for t in np.unique(type_of):
            chosen = type_of == t
            out[chosen] = self.arrays[t][idx[chosen] - self._starts[t]]
        return out

    def _gather(self, idx):
        idx = np.asarray(idx, dtype=np.int64)
        if self.selection is not None:
            idx = self.selection[idx]
        out = self._gather_global(idx)
        if self.drop is not None:
            out[:, self.drop] = 0.0
        return out

    def __getitem__(self, item):
        if isinstance(item, slice):
            start, stop, step = item.indices(self.shape[0])
            return self._gather(np.arange(start, stop, step))
        index = np.asarray(item)
        if index.dtype == bool:
            index = np.flatnonzero(index)
        if index.ndim == 0:
            return self._gather(index[None, ...])[0]
        return self._gather(index)

    def to_ram(self, block=1 << 18):
        """Read the selected rows into one contiguous array, in ascending row order (one sequential disk pass).

        An epoch then never touches the disk again: with a 16-rows-per-game pool the array is ~5 GB, which is the
        difference between 4 x 26 GB of reads and 26 GB read once. Rows are gathered in pack order, not pool order, so
        the read stays sequential even though the pool is sorted by (game, seat, step).
        """
        index = self.selection if self.selection is not None else np.arange(self.shape[0])
        out = np.empty((len(index), self.width), dtype=np.float16)
        ascending = np.argsort(index, kind="stable")
        for start in range(0, len(index), block):
            part = ascending[start:start + block]
            out[part] = self._gather_global(index[part])
        if self.drop is not None:
            out[:, self.drop] = 0.0
        return out


class PackedRows:
    """A memmap column block addressed like an array, returned as float32: packed observations (and any other f16 block).

    `selection` maps positions in the pool to rows of the pack, and a boolean mask returns a NEW view over the same
    memmaps - so filtering a policy pool never materialises the whole padded candidate tensor.
    """

    def __init__(self, array, width=None, selection=None, drop_columns=None):
        self.array = array
        self.width = int(array.shape[1] if width is None else width)
        self.selection = None if selection is None else np.asarray(selection, dtype=np.int64)
        self.drop = np.asarray(sorted(drop_columns), dtype=np.int64) if drop_columns else None
        self.shape = (len(self.selection) if self.selection is not None else len(array), self.width)
        self.dtype = np.dtype(np.float32)

    def __len__(self):
        return self.shape[0]

    def subset(self, index):
        index = np.asarray(index)
        if index.dtype == bool:
            index = np.flatnonzero(index)
        if self.selection is not None:
            index = self.selection[index]
        return PackedRows(self.array, self.width, selection=index, drop_columns=self.drop)

    def __getitem__(self, item):
        if isinstance(item, slice):
            start, stop, step = item.indices(self.shape[0])
            return self.take(np.arange(start, stop, step))
        index = np.asarray(item)
        if index.dtype == bool:
            return self.subset(index)
        if index.ndim == 0:
            return self.take(index[None])[0]
        return self.take(index)

    def take(self, index):
        """Materialise rows (int index array); padding-free and float32."""
        index = np.asarray(index, dtype=np.int64)
        if self.selection is not None:
            index = self.selection[index]
        out = np.asarray(self.array[index], dtype=np.float32)
        if self.drop is not None:
            out[:, self.drop] = 0.0
        return out


class PackedCandidates:
    """[b, kmax, features] float32 gather out of the variable-length candidate pack, padded with a zero row.

    The raw corpus stores every row padded to the menu maximum (200 candidates where 3-10 are real). The pack stores
    only the real candidates and their offsets, so a batch pads to the largest row IN THAT BATCH, and the padded slot
    reads a zero row that contributes nothing to any score.
    """

    def __init__(self, flat, ids, offsets, count, features, selection=None, chosen=None):
        self.flat = flat
        self.ids = ids
        self.offsets = np.asarray(offsets, dtype=np.int64)
        self.count = np.asarray(count, dtype=np.int64)
        self.chosen_index = None if chosen is None else np.asarray(chosen, dtype=np.int64)
        self.features = int(features)
        self.selection = None if selection is None else np.asarray(selection, dtype=np.int64)
        self.shape = (len(self.selection) if self.selection is not None else len(self.count), self.features)
        self.dtype = np.dtype(np.float32)
        self._zero = len(flat) - 1

    def __len__(self):
        return self.shape[0]

    def subset(self, index):
        index = np.asarray(index)
        if index.dtype == bool:
            index = np.flatnonzero(index)
        if self.selection is not None:
            index = self.selection[index]
        return PackedCandidates(self.flat, self.ids, self.offsets, self.count, self.features, selection=index,
                                chosen=self.chosen_index)

    def _padded(self, index, source, dtype):
        index = np.asarray(index, dtype=np.int64)
        if index.ndim == 0:
            index = index[None]
        if self.selection is not None:
            index = self.selection[index]
        counts = self.count[index]
        kmax = int(counts.max()) if len(counts) else 1
        slots = np.arange(kmax)[None, :]
        position = self.offsets[index][:, None] + slots
        valid = slots < counts[:, None]
        rows = np.where(valid, position, self._zero)
        return np.asarray(source[rows], dtype=dtype)

    def __getitem__(self, item):
        if isinstance(item, slice):
            start, stop, step = item.indices(self.shape[0])
            return self._padded(np.arange(start, stop, step), self.flat, np.float32)
        index = np.asarray(item)
        if index.dtype == bool:
            return self.subset(index)
        if index.ndim == 0:
            return self._padded(index[None], self.flat, np.float32)[0]
        return self._padded(index, self.flat, np.float32)

    def chosen(self, item):
        """The features of the candidate each row actually took: [b, features] float32, no padding at all.

        This is what the older per-type learner (`train_phase.py`) reads (obs + the chosen candidate), so it can use the
        pack without materialising the padded candidate tensor.
        """
        index = np.asarray(item, dtype=np.int64)
        if self.selection is not None:
            index = self.selection[index]
        if self.chosen_index is None:
            raise ValueError("this packed type has no chosen indices")
        return np.asarray(self.flat[self.offsets[index] + self.chosen_index[index]], dtype=np.float32)

    def gather_ids(self, item):
        index = np.asarray(item, dtype=np.int64)
        if index.ndim == 0:
            return self._padded(index[None], self.ids, np.int64)[0]
        return self._padded(index, self.ids, np.int64)


class PackedIdView:
    """The candidate ids of a packed type, addressed exactly like the raw `ids` array ([b, kmax] int64)."""

    def __init__(self, candidates):
        self.candidates = candidates
        self.shape = (len(candidates), candidates.features)

    def __len__(self):
        return len(self.candidates)

    def __getitem__(self, item):
        index = np.asarray(item)
        if index.dtype == bool:
            return PackedIdView(self.candidates.subset(index))
        if index.ndim == 0:
            return self.candidates.gather_ids(index[None])[0]
        return self.candidates.gather_ids(index)


def open_pack(directory):
    """manifest of a packed dataset, with its observation width and candidate width."""
    import os
    manifest = json.load(open(os.path.join(directory, "manifest.json"), encoding="utf-8"))
    manifest["directory"] = directory
    return manifest


def open_packed_type(directory, decision, manifest=None):
    """One decision type of a pack, with the same keys as a raw chunk read (obs/cands/ids/labels), lazily.

    Values are float32 on read, exactly like `load_type` in train_policy_ce, so the trainer does not care where they
    came from. `keep` is the cross-type de-duplication mask computed once at pack time.
    """
    import os
    manifest = open_pack(directory) if manifest is None else manifest
    meta = manifest["types"][decision]
    rows = int(meta["rows"])
    labels = np.load(os.path.join(directory, f"{decision}.lab.npz"))
    obs = np.memmap(os.path.join(directory, f"{decision}.obs.f16"), dtype="<f2", mode="r",
                    shape=(rows, manifest["obsSize"]))
    data = {"obs": PackedRows(obs), "outcome": labels["outcome"], "margin": labels["margin"],
            "game": labels["game"], "keep": labels["keep"], "seat": labels["seat"],
            "context": labels["context"], "round": labels["round"], "step": labels["step"],
            "obsSize": manifest["obsSize"], "schemaHash": meta["hash"]}
    if "candidates" in meta:
        candidates = np.load(os.path.join(directory, f"{decision}.cand.npz"))
        count = candidates["count"].astype(np.int64)
        flat = np.memmap(os.path.join(directory, f"{decision}.cand.f16"), dtype="<f2", mode="r",
                         shape=(int(meta["candidates"]["rows"]) + 1, int(meta["candidates"]["features"])))
        ids = np.memmap(os.path.join(directory, f"{decision}.cids.u2"), dtype="<u2", mode="r",
                        shape=(int(meta["candidates"]["rows"]) + 1,))
        data["cands"] = PackedCandidates(flat, ids, candidates["offset"], count,
                                         int(meta["candidates"]["features"]),
                                         chosen=candidates["chosen"])
        data["ids"] = PackedIdView(data["cands"])
        data["chosen"] = candidates["chosen"].astype(np.int64)
        data["count"] = count
        data["candFeatures"] = int(meta["candidates"]["features"])
    return data


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
