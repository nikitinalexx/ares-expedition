"""The phase mix of a raw corpus or a pack, by the phase id the player actually chose.

The previous version of ml/phase_value.py looped over range(5) while the ids are 1..5, so every column was shifted and one
phase (5) was never counted. Ids are read from the data here, not assumed.
"""
import glob
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ares_data  # noqa: E402


def mix_from_pack(pack):
    manifest = ares_data.open_pack(pack)
    data = ares_data.open_packed_type(pack, 'PICK_PHASE', manifest)
    chosen = np.asarray(data['chosen'])
    ids = data['ids']
    phase = np.empty(len(chosen), dtype=np.int32)
    for start in range(0, len(chosen), 4096):
        block = np.arange(start, min(start + 4096, len(chosen)))
        rows = ids[block]
        phase[block] = rows[np.arange(len(block)), chosen[block]]
    return phase


def mix_from_raw(directory, chunks=8):
    phases = []
    files = sorted(glob.glob(os.path.join(directory, 'PICK_PHASE.*.ares')))[:chunks]
    for path in files:
        _, rows = ares_data.read_decisions(path)
        if not len(rows):
            continue
        chosen = rows['chosen'].astype(np.int64)
        ids = rows['ids']
        phases.append(ids[np.arange(len(rows)), chosen])
    return np.concatenate(phases) if phases else np.empty(0, dtype=np.int32)


def show(label, phase):
    if not len(phase):
        print(f'{label:28s} (no rows)')
        return
    values, counts = np.unique(phase, return_counts=True)
    shares = dict(zip(values.tolist(), (counts / counts.sum()).round(4).tolist()))
    print(f'{label:28s} n={len(phase):7d}  ' + '  '.join(f'{k}:{v * 100:5.1f}%' for k, v in shares.items()))


if __name__ == '__main__':
    for argument in sys.argv[1:]:
        if os.path.isdir(argument) and glob.glob(os.path.join(argument, 'manifest.json')):
            show(os.path.basename(argument), mix_from_pack(argument))
        elif os.path.isdir(argument):
            show(os.path.basename(argument) + ' (raw)', mix_from_raw(argument))
        else:
            print(f'{argument}: not found')
