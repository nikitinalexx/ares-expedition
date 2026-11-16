"""How many candidates does a menu really have? The files pad every row to the type's maximum, and that padding is 76-96% of
the bytes for most decision types. This prints the distribution of the real count per type over a corpus."""
import glob
import os
import sys

import numpy as np

sys.path.insert(0, r'C:\Users\user\IdeaProjects\ares-expedition\ml')
import ares_data  # noqa: E402


def main(directory, per_type=6):
    seen = {}
    for path in glob.glob(os.path.join(directory, '*.ares')):
        name = os.path.basename(path).split('.')[0]
        if seen.get(name, {}).get('files', 0) >= per_type:
            continue
        _, rows = ares_data.read_decisions(path)
        if not len(rows):
            continue
        counts = rows['count']
        entry = seen.setdefault(name, {'files': 0, 'n': 0, 'max': 0, 'p999': 0, 'sum': 0.0})
        entry['files'] += 1
        entry['n'] += len(counts)
        entry['max'] = max(entry['max'], int(counts.max()))
        entry['p999'] = max(entry['p999'], int(np.percentile(counts, 99.9)))
        entry['sum'] += float(counts.mean()) * len(counts)
    print(f"{'type':22s} {'rows':>9s} {'mean':>6s} {'p99.9':>6s} {'max':>5s}")
    for name, e in sorted(seen.items()):
        print(f"{name:22s} {e['n']:9d} {e['sum'] / max(e['n'], 1):6.1f} {e['p999']:6d} {e['max']:5d}")


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'D:/ares/data/scale1090')
