"""Learning curve from a train_network_v2_torch log: val BCE, AUC and the best checkpoint, as an SVG plus a table.

    py -3 ml/learning_curve.py --log D:/ares/tmp/net_sp_it3.log --out D:/ares/tmp/it3.svg

Why: the owner's question is whether the run is saturating, and the final hundredths of loss can be worth 1-2 points of
strength, so the curve has to be read together with the play measurement. The table prints, for 1% and 2% above the best
validation loss, how many checkpoints (and steps) it took to get there and how long the plateau lasted - "saturation at
step X" as a number instead of an impression.

The log may be UTF-16 (PowerShell `*>` writes UTF-16LE) or UTF-8; both are handled.
"""
import argparse
import re

import io


def read_lines(path):
    raw = open(path, "rb").read()
    if raw[:2] in (b"\xff\xfe", b"\xfe\xff") or (len(raw) > 1 and raw[1] == 0):
        return io.open(path, encoding="utf-16", errors="ignore").read().splitlines()
    return io.open(path, encoding="utf-8", errors="ignore").read().splitlines()


def parse(path):
    points = []
    for line in read_lines(path):
        match = re.match(r"step (\d+): train ([\d.]+)\s+val ([\d.]+)\s+auc ([\d.]+)", line.strip())
        if match:
            points.append((int(match.group(1)), float(match.group(2)), float(match.group(3)), float(match.group(4))))
    return points


def saturation(points):
    best = min(points, key=lambda p: p[2])
    best_val = best[2]
    out = {"best": best, "rows": []}
    for label, factor in (("1%", 1.01), ("2%", 1.02), ("5%", 1.05)):
        limit = best_val * factor
        reached = next((i for i, p in enumerate(points) if p[2] <= limit), None)
        out["rows"].append((label, reached, points[reached][0] if reached is not None else None,
                            len(points) - 1 - (reached or 0)))
    return out


def svg(points, out):
    width, height = 900, 520
    left, right, top, bottom = 70, 850, 30, 470
    steps = [p[0] for p in points]
    vals = [p[2] for p in points]
    aucs = [p[3] for p in points]
    vmin, vmax = min(vals), max(vals)
    amin, amax = min(aucs), max(aucs)
    span_x = max(steps) - min(steps) or 1
    span_v = (vmax - vmin) or 1
    span_a = (amax - amin) or 1

    def x(step):
        return left + (step - min(steps)) / span_x * (right - left)

    def y(value, lo, span):
        return bottom - (value - lo) / span * (bottom - top)

    best = min(points, key=lambda p: p[2])
    poly_v = " ".join(f"{x(s):.1f},{y(v, vmin, span_v):.1f}" for s, _, v, _ in points)
    poly_a = " ".join(f"{x(s):.1f},{y(a, amin, span_a):.1f}" for s, _, _, a in points)
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}">',
             '<rect width="100%" height="100%" fill="white"/>',
             f'<line x1="{left}" y1="{bottom}" x2="{right}" y2="{bottom}" stroke="#333"/>',
             f'<line x1="{left}" y1="{top}" x2="{left}" y2="{bottom}" stroke="#333"/>',
             f'<polyline fill="none" stroke="#c0392b" stroke-width="2" points="{poly_v}"/>',
             f'<polyline fill="none" stroke="#2980b9" stroke-width="2" points="{poly_a}"/>',
             f'<circle cx="{x(best[0]):.1f}" cy="{y(best[2], vmin, span_v):.1f}" r="5" fill="#c0392b"/>',
             f'<text x="{x(best[0]) + 8:.1f}" y="{y(best[2], vmin, span_v) - 8:.1f}" font-size="12" fill="#c0392b">'
             f'best val {best[2]:.4f} at step {best[0]}</text>',
             f'<text x="{left}" y="{top - 10}" font-size="13">val BCE (red, {vmin:.4f}-{vmax:.4f})  '
             f'AUC (blue, {amin:.4f}-{amax:.4f})</text>',
             f'<text x="{right - 60}" y="{bottom + 25}" font-size="12">step</text>']
    for i in range(6):
        step = min(steps) + i * span_x / 5
        parts.append(f'<text x="{x(step):.1f}" y="{bottom + 18}" font-size="11" text-anchor="middle">'
                     f'{int(step)}</text>')
    parts.append("</svg>")
    with open(out, "w", encoding="utf-8") as f:
        f.write("\n".join(parts))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--log", required=True)
    parser.add_argument("--out", default="")
    args = parser.parse_args()
    points = parse(args.log)
    if not points:
        raise SystemExit(f"{args.log}: no 'step N: ...' lines")
    info = saturation(points)
    best = info["best"]
    print(f"{args.log}: {len(points)} checkpoints, best val {best[2]:.4f} at step {best[0]} "
          f"(train {best[1]:.4f}, auc {best[3]:.4f})")
    print(f"{'within':>7} {'checkpoint':>10} {'step':>8} {'plateau checkpoints':>21}")
    for label, reached, step, plateau in info["rows"]:
        print(f"{label:>7} {reached if reached is not None else '-':>10} "
              f"{step if step is not None else '-':>8} {plateau:>21}")
    # A text curve, so the shape is visible without opening the SVG: val on the left, AUC on the right.
    print(f"{'step':>8} {'val':>7} {'auc':>6}  val curve (lower is better, * = best)")
    lo, hi = min(p[2] for p in points), max(p[2] for p in points)
    for step, train, val, auc in points:
        width = 40
        pos = 0 if hi == lo else int((val - lo) / (hi - lo) * (width - 1))
        bar = " " * pos + ("*" if (step, train, val, auc) == best else "o")
        print(f"{step:>8} {val:7.4f} {auc:6.4f}  |{bar}")
    if args.out:
        svg(points, args.out)
        print(f"wrote {args.out}")


if __name__ == "__main__":
    main()
