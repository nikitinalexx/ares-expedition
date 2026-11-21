"""Assemble the variant table from the artifacts: train_metrics.json per seed + the compare JSON per bundle.

    py -3 ml/report_variants.py v5k_b4096 v5k_b64 v5k_b10e1 ...

For every label it reads `<label>_s1..s3/train_metrics.json` (config, rows, games, val MSE/R2/AUC per seed, with the
spread across seeds) and `D:/ares/tmp/<label>_vs_network.json` when it exists. The point is that the table is derived
from files that were written by the runs themselves: a table typed by hand is how "batch 4096" survived months of
experiments that were never compared with anything.

Legacy labels whose measurement file has another name are listed in MEASUREMENTS below.
"""
import glob
import json
import os
import statistics
import sys

VALUE_ROOT = "D:/ares/runs/value"
TMP = "D:/ares/tmp"
MEASUREMENTS = {
    "v_1118-2332_17": "champ_b4096",
    "v_b64": "v_b64_10k",
}


def seeds_of(label):
    return sorted(glob.glob(os.path.join(VALUE_ROOT, f"{label}_s*")))


def mean(values):
    return sum(values) / len(values) if values else None


def read_metrics(label):
    rows = []
    for directory in seeds_of(label):
        path = os.path.join(directory, "train_metrics.json")
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as f:
            rows.append(json.load(f))
    return rows


def read_measurement(label):
    name = MEASUREMENTS.get(label, label)
    path = os.path.join(TMP, f"{name}_vs_network.json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main(labels):
    print("| variant | games | rows | batch | epochs | opt | best ep | val MSE | R2 | AUC | vs NETWORK |")
    print("|---|---|---|---|---|---|---|---|---|---|---|")
    for label in labels:
        metrics = read_metrics(label)
        if not metrics:
            print(f"| {label} | (no metrics) | | | | | | | | | |")
            continue
        config = metrics[0].get("config", {})
        mse = mean([m["best_val_loss"] for m in metrics])
        r2 = mean([m.get("val_r2") for m in metrics if m.get("val_r2") is not None])
        auc = mean([m["auc"] for m in metrics])
        spread = (statistics.stdev([m["best_val_loss"] for m in metrics]) if len(metrics) > 1 else 0.0)
        result = read_measurement(label)
        bench = (f"{100 * result['aScore']:.2f}% [{100 * result['aScoreCi95Low']:.1f}, "
                 f"{100 * result['aScoreCi95High']:.1f}] ({result['games']})") if result else "-"
        print(f"| {label} | {metrics[0]['games']} | {metrics[0]['rows_train']} | {config.get('batch', '?')} | "
              f"{config.get('epochs', '?')} | {config.get('optimizer', '?')} | {metrics[0]['best_epoch']} | "
              f"{mse:.4f} ±{spread:.4f} | {r2 if r2 is None else round(r2, 3)} | {auc:.4f} | {bench} |")


if __name__ == "__main__":
    main(sys.argv[1:])
