"""Writes the small deterministic model that the Java tests use to prove that Java computes exactly what Python exported.

    python ml/make_test_fixture.py --schema-hash <hash of the current encoder> --out src/test/resources/phase_model_fixture

The schema hash is printed by the Java test `MlpPhaseModelTest.fixtureIsFresh` when it is out of date (the encoder changed).
The reference outputs come from numpy_forward, an implementation independent of PyTorch and of the Java code.
"""
import argparse
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from phase_model import export_from_arrays, numpy_forward  # noqa: E402


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--schema-hash", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--obs-size", type=int, default=915)
    parser.add_argument("--cand-features", type=int, default=6)
    args = parser.parse_args()

    rng = np.random.default_rng(12345)
    obs_size, cand_features, hidden, clip = args.obs_size, args.cand_features, [8, 4], 5.0
    params = {
        "mean": rng.normal(0.2, 0.1, obs_size), "std": rng.uniform(0.05, 1.5, obs_size),
        "w_obs": rng.normal(0, 0.3, (hidden[0], obs_size)), "b_obs": rng.normal(0, 0.1, hidden[0]),
        "w_cand": rng.normal(0, 0.8, (hidden[0], cand_features)),
        "w_mid": rng.normal(0, 0.5, (hidden[1], hidden[0])), "b_mid": rng.normal(0, 0.1, hidden[1]),
        "w_out": rng.normal(0, 0.8, hidden[1]), "b_out": np.array([0.1]),
    }
    params = {k: np.asarray(v, dtype=np.float32).astype(np.float64) for k, v in params.items()}  # what the file will hold
    samples = []
    for _ in range(24):
        obs = np.clip(rng.normal(0.3, 0.6, obs_size), -3, 6)  # includes values that hit the clip
        k = int(rng.integers(1, 6))
        cands = np.zeros((k, cand_features))
        cands[np.arange(k), rng.integers(0, 5, k)] = 1.0
        cands[:, 5] = rng.integers(0, 2, k)
        obs32, cands32 = obs.astype(np.float32), cands.astype(np.float32)
        logits = numpy_forward(params, clip, obs32.astype(np.float64), cands32.astype(np.float64))
        samples.append({"obs": obs32.tolist(), "cands": cands32.tolist(), "logits": [float(x) for x in logits]})
    export_from_arrays(args.out, args.schema_hash, obs_size, cand_features, hidden, clip, params, samples)
    print(f"fixture written to {args.out}")


if __name__ == "__main__":
    main()
