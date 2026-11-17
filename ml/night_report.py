"""The night in one screen: what each iteration did, how the score moved, and whether it stopped growing."""
import json
import os
import sys

path = sys.argv[1] if len(sys.argv) > 1 else 'D:/ares/tmp/night_ledger.json'
if not os.path.exists(path):
    print('no iteration finished yet')
    raise SystemExit(0)
rows = json.load(open(path, encoding='utf-8'))
if not rows:
    print('the ledger is empty')
    raise SystemExit(0)

print(f"{'it':>3s} {'vs SMART':>8s} {'ci95':>17s} {'vs RANDOM':>10s} {'ci95':>17s} {'builds':>7s} {'stop':>6s} {'games':>7s} {'explore':>16s}")
print('-' * 86)
for r in rows:
    ci = r.get('ci') or [0, 0]
    rnd = r.get('ci_random') or [0, 0]
    print(f"{r['iteration']:3d} {(r['score'] or 0) * 100:7.2f}% "
          f"[{((ci[0] or 0) * 100):5.2f}, {((ci[1] or 0) * 100):5.2f}] "
          f"{(r.get('score_random') or 0) * 100:9.2f}% "
          f"[{((rnd[0] or 0) * 100):5.2f}, {((rnd[1] or 0) * 100):5.2f}] "
          f"{(r.get('builds_per_game') or 0):7.2f} {(r.get('stop_share') or 0) * 100:5.1f}% "
          f"{r['games']:7d} {str(r.get('explore')):>16s}")

best = max(rows, key=lambda r: r.get('score') or 0)
last = rows[-1]
print()
print(f"best: iteration {best['iteration']} -> {(best['score'] or 0) * 100:.2f}% "
      f"(builds {best.get('builds_per_game')}, argmax stop {(best.get('stop_share') or 0) * 100:.1f}%)")
print(f"last: iteration {last['iteration']} -> {(last['score'] or 0) * 100:.2f}%")
scores = [(r.get('score') or 0) for r in rows]
if len(scores) >= 4:
    print(f"trend over the last 4: {[round(s * 100, 2) for s in scores[-4:]]}")
if len(scores) >= 6 and max(scores[-3:]) <= max(scores[:-3]) + 0.005:
    print('WARNING: no improvement in the last 3 iterations')
