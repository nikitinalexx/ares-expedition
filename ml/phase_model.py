"""Model of the PICK_PHASE decision: definition, export to the files the Java agent reads, and an independent numpy forward pass.

Architecture (a Q-network: "how likely do I win if I pick THIS candidate in THIS situation"):
    z      = clip((obs - mean) / std, -clip, clip)                 data-driven standardisation, stored in the model
    a      = W_obs z + b_obs                                       once per decision
    h1_k   = relu(a + W_cand c_k)                                  for every candidate k
    h2_k   = relu(W_mid h1_k + b_mid)
    logit_k = w_out . h2_k + b_out                                 win probability = sigmoid(logit_k)
The policy of the agent is softmax(logit / temperature) over the legal candidates (temperature 0 = the best one).

Files written by export_model (the contract with MlpPhaseModel.java):
    model.json    {"kind","version","decisionType","schemaHash","obsSize","candFeatures","hidden":[h1,h2],"clip","weightsCount","weightsSha256"}
    weights.bin   float32 little endian, in this order (every matrix row-major as [out, in]):
                  mean[obs], std[obs], W_obs[h1,obs], b_obs[h1], W_cand[h1,cand], W_mid[h2,h1], b_mid[h2], w_out[h2], b_out[1]
    reference.json  a few inputs with the outputs of the exporting framework (for equivalence tests)
"""
import hashlib
import json
import os

import numpy as np

FORMAT_VERSION = 1
KIND = "phase_mlp"


def weight_layout(obs_size, cand_features, hidden):
    h1, h2 = hidden
    return [("mean", (obs_size,)), ("std", (obs_size,)), ("w_obs", (h1, obs_size)), ("b_obs", (h1,)), ("w_cand", (h1, cand_features)),
            ("w_mid", (h2, h1)), ("b_mid", (h2,)), ("w_out", (h2,)), ("b_out", (1,))]


def numpy_forward(params, clip, obs, cands):
    """Independent implementation of the forward pass. obs [obs_size], cands [K, cand_features] -> logits [K]."""
    z = np.clip((obs - params["mean"]) / params["std"], -clip, clip)
    a = params["w_obs"] @ z + params["b_obs"]
    h1 = np.maximum(a[None, :] + cands @ params["w_cand"].T, 0.0)
    h2 = np.maximum(h1 @ params["w_mid"].T + params["b_mid"], 0.0)
    return h2 @ params["w_out"] + params["b_out"][0]


def load_exported(model_dir):
    """Reads the exported files with numpy only. Returns (meta, params)."""
    with open(os.path.join(model_dir, "model.json"), encoding="utf-8") as f:
        meta = json.load(f)
    raw = open(os.path.join(model_dir, "weights.bin"), "rb").read()
    if hashlib.sha256(raw).hexdigest() != meta["weightsSha256"]:
        raise ValueError("weights.bin does not match model.json (damaged or half written)")
    flat = np.frombuffer(raw, dtype="<f4")
    layout = weight_layout(meta["obsSize"], meta["candFeatures"], meta["hidden"])
    if flat.size != sum(int(np.prod(shape)) for _, shape in layout) or flat.size != meta["weightsCount"]:
        raise ValueError("weights.bin has an unexpected size")
    params, pos = {}, 0
    for name, shape in layout:
        n = int(np.prod(shape))
        params[name] = flat[pos:pos + n].reshape(shape).astype(np.float64)
        pos += n
    return meta, params


def atomic_write(path, data, mode="wb"):
    tmp = path + ".tmp"
    with open(tmp, mode) as f:
        f.write(data)
        f.flush()
        os.fsync(f.fileno())
    os.replace(tmp, path)


def export_from_arrays(out_dir, schema_hash, obs_size, cand_features, hidden, clip, params, reference=None):
    """Writes model.json / weights.bin from numpy arrays (names as in weight_layout)."""
    os.makedirs(out_dir, exist_ok=True)
    layout = weight_layout(obs_size, cand_features, hidden)
    chunks = []
    for name, shape in layout:
        arr = np.asarray(params[name], dtype="<f4")
        if arr.shape != tuple(shape):
            raise ValueError(f"{name}: expected shape {shape}, got {arr.shape}")
        if not np.isfinite(arr).all():
            raise ValueError(f"{name} contains NaN/Inf: the training diverged, the model is not exported")
        chunks.append(arr.reshape(-1).tobytes())
    raw = b"".join(chunks)
    meta = {"kind": KIND, "version": FORMAT_VERSION, "decisionType": "PICK_PHASE", "schemaHash": schema_hash, "obsSize": obs_size,
            "candFeatures": cand_features, "hidden": list(hidden), "clip": clip, "weightsCount": len(raw) // 4,
            "weightsSha256": hashlib.sha256(raw).hexdigest()}
    atomic_write(os.path.join(out_dir, "weights.bin"), raw)
    atomic_write(os.path.join(out_dir, "model.json"), json.dumps(meta, indent=1), "w")
    if reference is not None:
        atomic_write(os.path.join(out_dir, "reference.json"), json.dumps(reference), "w")
    return meta
