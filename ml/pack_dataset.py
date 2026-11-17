"""Packs a decision corpus into what a critic actually reads: observation + labels, once.

Why: the raw files store the full candidate block of every row, padded to the menu maximum. For the played expansions
that is 78% of 120 GB, and the value model never reads a byte of it. Every training run also re-reads and re-parses all
of it, so a 4-epoch run costs 4 x 120 GB of hard-disk time. This tool reads the corpus ONCE (sequentially) and writes

    <out>/<TYPE>.obs.f16     float16 [n, obsSize], contiguous, one file per decision type
    <out>/<TYPE>.lab.npz     outcome, margin, seat, context, round, game, step, keep
    <out>/manifest.json      schema hash, layout, rows per type, source

With --candidates it also writes the candidate block, WITHOUT the padding to the menu maximum (which is 76-96% of the
raw bytes for most decision types):

    <out>/<TYPE>.cand.f16    float16 [sum(count) + 1, candidateFeatures]  (the last row is zeros, for padding)
    <out>/<TYPE>.cids.u2     uint16  [sum(count) + 1]                     (the last id is 0, for padding)
    <out>/<TYPE>.cand.npz    count, chosen, offset [n+1]

`keep` is the cross-type de-duplication mask over (game, step): the same decision can be recorded by more than one
decision type, and the old loader dropped the duplicates in arrival order. Doing it here once means the trainer does not
have to hold a 12-million-key Python set per run.

Usage:
    py -3 ml/pack_dataset.py --data D:/ares/data/scale1090 --out C:/ares/pack/scale1090
    py -3 ml/pack_dataset.py --data D:/ares/data/scale1090 --out C:/ares/pack/xtest --limit-chunks 3
"""
import argparse
import glob
import hashlib
import json
import os
import sys
import time

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ares_data import read_decisions  # noqa: E402

LABEL_FIELDS = ("outcome", "margin", "seat", "context", "round", "game", "step")


def chunks_of(directory, decision):
    return sorted(glob.glob(os.path.join(directory, f"{decision}.[0-9]*.ares")))


def pack(data_dirs, out, limit_chunks=0, types=None, candidates=False, verbose=True):
    started = time.time()
    os.makedirs(out, exist_ok=True)
    # Refuse before writing instead of dying with ENOSPC halfway: a partial pack is useless and the error arrives after
    # minutes of work. The pack keeps the observation and the real candidates only, which measured ~0.45-0.6 of the raw size
    # on this machine (119.7 GiB -> 24.5 GiB for one corpus, 27 GB -> 15.4 GB for another); 0.7 with a 2 GB margin is safe.
    raw_bytes = 0
    for directory in [d for d in data_dirs.split(',') if d]:
        for path in glob.glob(os.path.join(directory, '*.ares')):
            raw_bytes += os.path.getsize(path)
    free = None
    try:
        import ctypes
        value = ctypes.c_ulonglong(0)
        ctypes.windll.kernel32.GetDiskFreeSpaceExW(ctypes.c_wchar_p(out), None, None, ctypes.byref(value))
        free = value.value
    except Exception:
        pass
    needed = int(raw_bytes * (0.7 if candidates else 0.3)) + 2 * 2 ** 30
    if free is not None and free < needed:
        raise SystemExit(f'{out}: needs about {needed / 2 ** 30:.0f} GiB for a {raw_bytes / 2 ** 30:.0f} GiB corpus, '
                         f'{free / 2 ** 30:.0f} GiB free. Pack to another disk or free space first.')
    directories = [d for d in data_dirs.split(",") if d]
    names = set()
    for directory in directories:
        for path in glob.glob(os.path.join(directory, "*.[0-9][0-9][0-9][0-9][0-9][0-9].ares")):
            names.add(os.path.basename(path).rsplit(".", 2)[0])
    names = sorted(names)
    if types:
        names = [n for n in names if n in set(types)]
    if not names:
        raise SystemExit(f"no decision chunks in {data_dirs}")

    manifest = {"out": os.path.abspath(out), "source": data_dirs, "types": {}, "obsSize": None,
                "observationLayout": None, "chunkLimit": limit_chunks, "writtenAt": time.strftime("%Y-%m-%dT%H:%M:%S")}
    hashes, layouts, labels, rows_by_type = {}, {}, {}, {}

    for decision in names:
        files = []
        for directory in directories:
            files.extend(chunks_of(directory, decision))
        files.sort()
        if limit_chunks:
            files = files[:limit_chunks]
        if not files:
            continue
        obs_path = os.path.join(out, f"{decision}.obs.f16")
        cand_path = os.path.join(out, f"{decision}.cand.f16") if candidates else None
        ids_path = os.path.join(out, f"{decision}.cids.u2") if candidates else None
        kept = {field: [] for field in LABEL_FIELDS}
        cand_meta = {"count": [], "chosen": []}
        cand_rows, cand_features, max_candidates = 0, None, 0
        width, rows_written, bytes_read = None, 0, 0
        cand_sink = open(cand_path, "wb") if candidates else None
        ids_sink = open(ids_path, "wb") if candidates else None
        with open(obs_path, "wb") as sink:
            chunk_seconds = []
            for path in files:
                chunk_started = time.time()
                schema, rows = read_decisions(path)
                if not len(rows):
                    continue
                width = schema["obsSize"]
                if manifest["obsSize"] is None:
                    manifest["obsSize"] = width
                    manifest["observationLayout"] = schema["observationLayout"]
                elif schema["obsSize"] != manifest["obsSize"]:
                    raise SystemExit(f"{path}: observation width {schema['obsSize']} != {manifest['obsSize']}")
                hashes[decision] = schema["hash"]
                layouts[decision] = schema["observationLayout"].split("; candidate=")[0]
                block = np.asarray(rows["obs"], dtype=np.float16)
                sink.write(block.tobytes())
                rows_written += len(block)
                bytes_read += os.path.getsize(path)
                elapsed = time.time() - chunk_started
                for field in LABEL_FIELDS:
                    kept[field].append(np.asarray(rows[field]))
                if candidates:
                    # Keep only the real candidates of every row. The raw record is padded to the menu maximum, and
                    # that padding is what made the corpus 120 GB for 26 GB of observation.
                    count = np.asarray(rows["count"]).astype(np.int64)
                    max_candidates = max(max_candidates, int(count.max()) if len(count) else 0)
                    cand_features = schema["candidateFeatures"]
                    mask = np.arange(count.max(), dtype=np.int64)[None, :] < count[:, None]
                    cand_sink.write(np.asarray(rows["feat"], dtype=np.float16)[:, :count.max()][mask].tobytes())
                    ids_sink.write(np.asarray(rows["ids"], dtype="<u2")[:, :count.max()][mask].tobytes())
                    cand_rows += int(count.sum())
                    cand_meta["count"].append(count.astype(np.uint16))
                    cand_meta["chosen"].append(np.asarray(rows["chosen"], dtype=np.uint8))
                if verbose:
                    print(f"  {decision:22s} {os.path.basename(path):28s} +{len(block):7d} rows "
                          f"({rows_written:8d}), {bytes_read / 2 ** 30:6.2f} GiB read, "
                          f"{os.path.getsize(path) / 2 ** 20 / max(elapsed, 1e-6):5.0f} MB/s", flush=True)
        if cand_sink is not None:
            # one zero row at the end: a gather that pads a short row reads it and adds nothing to the score
            cand_sink.write(np.zeros((1, cand_features or 0), dtype=np.float16).tobytes())
            ids_sink.write(np.zeros(1, dtype="<u2").tobytes())
            cand_sink.close()
            ids_sink.close()
            if rows_written:
                counts = np.concatenate(cand_meta["count"]).astype(np.int64)
                np.savez(os.path.join(out, f"{decision}.cand.npz"),
                         count=counts.astype(np.uint16),
                         chosen=np.concatenate(cand_meta["chosen"]),
                         offset=np.r_[0, np.cumsum(counts)])
        if not rows_written:
            os.remove(obs_path)
            if cand_path:
                os.remove(cand_path)
                os.remove(ids_path)
            continue
        labels[decision] = {field: np.concatenate(kept[field]) for field in LABEL_FIELDS}
        rows_by_type[decision] = rows_written
        manifest["types"][decision] = {"rows": rows_written, "chunks": len(files), "obsSize": width,
                                       "hash": hashes[decision], "layout": layouts[decision],
                                       "obsBytes": os.path.getsize(obs_path)}
        if cand_sink is not None:
            manifest["types"][decision]["candidates"] = {
                "rows": cand_rows, "features": int(cand_features), "maxCount": int(max_candidates),
                "candBytes": os.path.getsize(cand_path)}

    if not manifest["types"]:
        raise SystemExit("nothing packed")

    # ---- the game's own score decomposition (RANKING.<chunk>.jsonl, one line per game): seven channels per seat.
    # A critic trained only on the final margin cannot learn that a card scores, because in our games the margin is dominated
    # by tempo; the channels are measured by the game itself, and a card that adds two printed points adds two to that channel
    # in every game, won or lost.
    breakdown = {}
    for directory in directories:
        for path in glob.glob(os.path.join(directory, "RANKING.*.jsonl")):
            with open(path, encoding="utf-8", errors="ignore") as sidecar:
                for line in sidecar:
                    if "breakdown" not in line:
                        continue
                    try:
                        blob = json.loads(line)
                        breakdown[int(blob["game"])] = blob["breakdown"]
                    except (ValueError, KeyError):
                        continue
    if breakdown:
        for decision in manifest["types"]:
            # `labels` is the in-memory dict of this run (already carrying `keep`), so there is no dependence on the order in
            # which the files are written.
            games = labels[decision]["game"]
            seats = labels[decision]["seat"]
            table = np.zeros((len(games), 7), dtype=np.float32)
            for index, (game, seat) in enumerate(zip(games.tolist(), seats.tolist())):
                entry = breakdown.get(int(game))
                if entry is not None and seat < len(entry):
                    table[index] = entry[seat]
            labels[decision]["breakdown"] = table
            np.savez(os.path.join(out, f"{decision}.lab.npz"), **labels[decision])
        manifest["breakdown"] = ["terraformingRating", "forests", "cards", "resourceCards", "milestones", "awards", "extra"]
    else:
        print("  no RANKING sidecar: the pack carries no score decomposition")

    # ---- cross-type de-duplication, in pack order (type name, then chunk), first arrival wins ----
    #
    # AFTERSTATE rows are not decisions: they are the states one action after a decision, the states the one-ply search
    # actually reads, and they share the game and step number with that decision. They must never take part in the
    # de-duplication - either they would delete real decision rows, or a decision would delete them.
    order = [t for t in manifest["types"]]
    dedup_order = [t for t in order if t != "AFTERSTATE"]
    if dedup_order:
        keys = [labels[t]["game"].astype(np.uint64) * np.uint64(1 << 16) + labels[t]["step"].astype(np.uint64)
                for t in dedup_order]
        all_keys = np.concatenate(keys)
        stable = np.argsort(all_keys, kind="stable")
        ordered = all_keys[stable]
        first = np.empty(len(ordered), dtype=bool)
        first[0] = True
        first[1:] = ordered[1:] != ordered[:-1]
        dedup_keep = np.zeros(len(ordered), dtype=bool)
        dedup_keep[stable[first]] = True
    else:
        dedup_keep = np.zeros(0, dtype=bool)
    dropped = int(len(dedup_keep) - dedup_keep.sum())
    offset = 0
    dedup_offset = 0
    for decision in order:
        rows = manifest["types"][decision]["rows"]
        if decision == "AFTERSTATE":
            labels[decision]["keep"] = np.ones(rows, dtype=bool)
        else:
            labels[decision]["keep"] = dedup_keep[dedup_offset:dedup_offset + rows]
            dedup_offset += rows
        np.savez(os.path.join(out, f"{decision}.lab.npz"), **labels[decision])
        offset += rows
    manifest["rows"] = int(sum(rows_by_type.values()))
    manifest["duplicateRows"] = dropped
    manifest["obsSize"] = int(manifest["obsSize"])
    manifest["observationHash"] = hashlib.sha256(
        manifest["observationLayout"].split("; candidate=")[0].encode("utf-8")).hexdigest()[:16]

    total = sum(manifest["types"][t]["obsBytes"] for t in manifest["types"])
    manifest["obsBytes"] = int(total)
    with open(os.path.join(out, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=1)
    if verbose:
        print(json.dumps({"rows": manifest["rows"], "duplicatesDropped": dropped,
                          "obsGiB": round(total / 2 ** 30, 1),
                          "seconds": round(time.time() - started, 1)}, indent=1), flush=True)
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="comma-separated directories with *.NNNNNN.ares chunks")
    parser.add_argument("--out", required=True)
    parser.add_argument("--limit-chunks", type=int, default=0, help="only the first N chunks per type (a quick test)")
    parser.add_argument("--types", default="", help="comma-separated decision types to pack (default: all)")
    parser.add_argument("--candidates", action="store_true",
                        help="also pack the candidate block, without padding to the menu maximum (for a policy head)")
    args = parser.parse_args()
    pack(args.data, args.out, limit_chunks=args.limit_chunks,
         types=[t for t in args.types.split(",") if t] or None, candidates=args.candidates)


if __name__ == "__main__":
    main()
