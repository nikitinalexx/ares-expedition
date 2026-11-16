"""Why does the bootstrap policy build nothing? Look at what its argmax does on real BUILD_CARD rows.

Greedy decoding of a policy trained on uniform-random choices has a known failure: every card is its own action, so the
learned probability of each single card stays lower than the probability of "stop", and argmax then always stops. If that is
what happened, the fix is not more training - it is either sampling while generating data, or a policy whose argmax has a
reason to build (the value-based advantage).
"""
import json
import os
import sys

import numpy as np
import torch

sys.path.insert(0, 'ml')
import ares_data  # noqa: E402
from train_policy_ce import Head, load_exported  # noqa: E402

model_dir = sys.argv[1] if len(sys.argv) > 1 else 'D:/ares/runs/policy/boot1'
pack = sys.argv[2] if len(sys.argv) > 2 else 'C:/ares/pack/boot1'
decision = sys.argv[3] if len(sys.argv) > 3 else 'BUILD_CARD'

manifest = ares_data.open_pack(pack)
data = ares_data.open_packed_type(pack, decision, manifest)
meta, params = load_exported(os.path.join(model_dir, decision))
hidden = meta['hidden']
model = Head(manifest['obsSize'], data['candFeatures'], hidden)
state = {
    'obs_layer.weight': torch.as_tensor(params['w_obs']), 'obs_layer.bias': torch.as_tensor(params['b_obs']),
    'cand_layer.weight': torch.as_tensor(params['w_cand']), 'mid.weight': torch.as_tensor(params['w_mid']),
    'mid.bias': torch.as_tensor(params['b_mid']), 'out.weight': torch.as_tensor(params['w_out']).reshape(1, -1),
    'out.bias': torch.as_tensor(params['b_out']).reshape(1),
}
model.load_state_dict({k: v for k, v in state.items()}, strict=False)
model.eval()

rows = np.arange(0, min(len(data['obs']), 40000), 7)
stop_wins = 0
build_wins = 0
stop_prob = []
bias_rows = 0
with torch.no_grad():
    for start in range(0, len(rows), 512):
        block = rows[start:start + 512]
        obs = torch.from_numpy(np.asarray(data['obs'][block]))
        cands = torch.from_numpy(data['cands'][block])
        count = torch.from_numpy(np.asarray(data['count'][block]).astype(np.int64))
        logits = model(obs, cands)
        mask = torch.arange(cands.shape[1])[None, :] < count[:, None]
        logits = logits.masked_fill(~mask, -1e9)
        best = logits.argmax(dim=1).numpy()
        ids = data['ids'][block]
        for i, b in enumerate(best):
            if int(ids[i][b]) == 0xFFFF:
                stop_wins += 1
            else:
                build_wins += 1
        probabilities = torch.softmax(logits, dim=1)
        for i in range(len(block)):
            k = int(count[i])
            # the stop candidate is the one with the STOP id; its probability mass is the "never build" mass
            stop = [j for j in range(k) if int(ids[i][j]) == 0xFFFF]
            if stop:
                stop_prob.append(float(probabilities[i][stop[0]]))
                bias_rows += 1

total = stop_wins + build_wins
print(json.dumps({
    'decision': decision,
    'rows': total,
    'argmax_stop': stop_wins,
    'argmax_stop_share': round(stop_wins / max(total, 1), 4),
    'argmax_build_share': round(build_wins / max(total, 1), 4),
    'mean_probability_of_stop': round(float(np.mean(stop_prob)), 4) if stop_prob else None,
    'median_probability_of_stop': round(float(np.median(stop_prob)), 4) if stop_prob else None,
}, indent=1))
