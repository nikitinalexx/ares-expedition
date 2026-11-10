"""PLACEHOLDER trainer: proves that the pipeline plumbing works, it does NOT learn a useful policy.

It reads the finished chunks (streaming, never the whole data in the RAM), computes how often each phase was chosen and what
the average result was, and writes a small model file. The real trainer must follow the same contract:

    python ml/train_stub.py --data DIR1,DIR2,... --out MODEL_DIR [--prev PREVIOUS_BEST_MODEL_DIR]

Contract for every trainer used by the night pipeline:
  * read chunks with ares_data.open_memmap / ares_data.chunk_paths (data are float16, convert per batch);
  * check the schema hash in the header against the one the model was built for;
  * write checkpoints into MODEL_DIR atomically (write to a temp name, then os.replace) so that a kill never leaves a broken file;
  * write the file DONE as the very last step: a model without DONE is never trusted;
  * exit with a non-zero code and a clear message on any problem.
"""
import argparse
import json
import os
import sys
import time

import numpy as np

from ares_data import chunk_paths, open_memmap, validate


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True, help="comma separated data directories")
    parser.add_argument("--out", required=True)
    parser.add_argument("--prev", default="")
    args = parser.parse_args()

    started = time.time()
    files = []
    for directory in [d for d in args.data.split(",") if d]:
        files += chunk_paths(directory, "PICK_PHASE")
    if not files:
        print("no finished chunks in " + args.data, file=sys.stderr)
        return 2

    chosen_count = np.zeros(6)
    outcome_sum = np.zeros(6)
    rows_total = 0
    schema_hash = None
    for path in files:
        schema, rows = open_memmap(path, expected_hash=schema_hash)
        schema_hash = schema["hash"]
        problems = validate(schema, rows[: min(len(rows), 20000)])
        if problems:
            print(f"{path}: {problems}", file=sys.stderr)
            return 3
        # stream in blocks: only a block is in the RAM
        for start in range(0, len(rows), 50_000):
            block = np.asarray(rows[start:start + 50_000])
            phases = block["ids"][np.arange(len(block)), block["chosen"]].astype(int)
            np.add.at(chosen_count, phases, 1)
            np.add.at(outcome_sum, phases, block["outcome"])
        rows_total += len(rows)

    mean_outcome = np.divide(outcome_sum, chosen_count, out=np.full(6, 0.5), where=chosen_count > 0)
    model = {"kind": "phase_table_stub", "schemaHash": schema_hash, "prefer": mean_outcome[1:].tolist(),
             "counts": chosen_count[1:].tolist(), "previous": args.prev or None}
    os.makedirs(args.out, exist_ok=True)
    tmp = os.path.join(args.out, "model.json.tmp")
    with open(tmp, "w") as f:
        json.dump(model, f)
        f.flush()
        os.fsync(f.fileno())
    os.replace(tmp, os.path.join(args.out, "model.json"))
    with open(os.path.join(args.out, "train_metrics.json"), "w") as f:
        json.dump({"rows": rows_total, "chunks": len(files), "seconds": round(time.time() - started, 2)}, f)
    with open(os.path.join(args.out, "DONE"), "w") as f:
        f.write("done")
    print(f"trained on {rows_total} rows from {len(files)} chunks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
