"""Merge several NetDataWriter binaries (ND2BIN01) into one, streaming, with disjoint game ids.

    py -3 ml/merge_bin.py --out D:/ares/data/mixed/netdata.bin --in A/netdata.bin B/netdata.bin

Why: the trainer reads ONE corpus and splits train/validation by GAME ID. Every generation run numbers its games from 1, so a
plain concatenation makes two different games share an id - the merge reported 10 000 games as 5 000 (776 rows per id) and
the split mixed the two corpora. `--offset-ids` (default ON here) adds the running maximum id to every later corpus, so the
merged file has one id per game; `--no-offset-ids` keeps the raw ids for debugging.

The merge streams in 8 MB blocks and rewrites only the int32 id of each record, so a 20 GB pair never sits in RAM.
"""
import argparse
import os
import sys

import numpy as np

MAGIC = b"ND2BIN01"
HEADER = len(MAGIC) + 2


def record_dtype(dimension):
    return np.dtype([("game", "<i4"), ("label", "<f2"), ("x", "<f2", (dimension,))])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--in", nargs="+", required=True, dest="inputs")
    parser.add_argument("--offset-ids", dest="offset", action="store_true", default=True)
    parser.add_argument("--no-offset-ids", dest="offset", action="store_false")
    args = parser.parse_args()

    dimension = None
    rows = 0
    shift = 0
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "wb") as out:
        for path in args.inputs:
            with open(path, "rb") as f:
                magic = f.read(len(MAGIC))
                if magic != MAGIC:
                    raise SystemExit(f"{path}: not an {MAGIC!r} file")
                dim = int.from_bytes(f.read(2), "little")
                if dimension is None:
                    dimension = dim
                    out.write(MAGIC)
                    out.write(dimension.to_bytes(2, "little"))
                elif dim != dimension:
                    raise SystemExit(f"{path}: dimension {dim} against {dimension}")
                dtype = record_dtype(dimension)
                size = os.path.getsize(path) - HEADER
                if size % dtype.itemsize:
                    raise SystemExit(f"{path}: {size} payload bytes is not whole records")
                count = size // dtype.itemsize
                game_max = shift
                chunk = max(1, (1 << 23) // dtype.itemsize)
                read_rows = 0
                while read_rows < count:
                    take = min(chunk, count - read_rows)
                    block = np.frombuffer(f.read(take * dtype.itemsize), dtype=dtype, count=take).copy()
                    if args.offset:
                        block["game"] = block["game"] + shift
                        game_max = max(game_max, int(block["game"].max(initial=0)))
                    out.write(block.tobytes())
                    read_rows += take
                if args.offset:
                    shift = game_max + 1
                rows += count
                print(f"{path}: merged {count} rows (shift {shift})", flush=True)
    print(f"{args.out}: {rows} rows, dimension {dimension}, ids from 0 to {shift - 1}", flush=True)


if __name__ == "__main__":
    main()
