# Nightly pipeline

One command: self-play (Java) → train (Python) → eval. Several experiments per night, finished chunks only, stop/resume anytime.
Default trainer: `ml/train_all.py` (shared observation trunk, one head per `DecisionType`, folders `model/<TYPE>/`).

## Config (`ml/nightly_config.json`)

- `storage.run_root / data_root / temp_root` — durable store on `D:` (state/models/logs; finished chunks; child TMP).
- `storage.stage_root` — optional **SSD** path (e.g. `C:/ares/stage`): self-play writes here, finished `.ares` are moved to `data_root`. Respects the `C:` reserve. Omit to write straight to `data_root`.
- Decision files are ARES **v4**: observation and candidate features are float16 (v3 candidate float32 still readable). Halves the dominant part of each row so the disk is less often the bottleneck.
- `storage.reserves` — free space never to touch per volume; checked before stages and during self-play.
- `data_budget_gb` / `keep_data_iterations` — data cap and training window. **Models and evals are never deleted.**
  Rotation deletes data outside the window; if still over budget/reserve, deletes the **oldest** data of this run (window shrinks), never the iteration being written or the latest finished iteration of an active experiment. Shrinks appear in `REPORT.md` as **DATA WINDOW SHRUNK**. `stopped_disk_low` only if even the minimum does not fit.
- `delete_data_of_other_runs` (default true): a **new** run deletes self-play data of earlier finished runs; their models/reports stay.
- `java.xmx`, `java.threads` (in-flight games). OOM → Java exit 6, stage restart, logged under **OUT OF MEMORY**.
- `hours` — **optional**. Default / omit / `null` / `0` = **run until you stop** (`nightly stop` or Ctrl+C) or the disk guard trips. No need to guess how long the machine is free; extra time is used automatically. A positive number is only if you want a timed budget. Experiments share a rung by **round-robin**. Early rungs without a timed budget advance after `min_iterations` each (default 1); the last rung runs until stop.
- `low_priority: false` on a dedicated night machine.
- Each experiment: `hypothesis`, optional `train_args`, optional `temperature` (else `selfplay.temperature`).
- `benchmark.references` — slow opponents (NETWORK…) once at the end, best model only. Gated by `min_win_rate` vs `gate_refs` (default: skip until ≥50% wins vs RANDOM or SMART). Until then every NETWORK game is wasted time — the skill gap is huge.

## Commands (repo root)

| Action | Command |
|---|---|
| Preflight + tiny real iteration | `nightly check` |
| Start or resume | `nightly run night4` |
| Status | `nightly status night4` |
| Stop (packs result) | `nightly stop night4` or Ctrl+C |
| Pack manually | `nightly pack night4` |
| New night from earlier best model | `nightly run night4 --start-from night3` |

`run` runs `check` first and refuses to start if anything is broken.

## Between nights

- Each night has its own folder. Default start: no model (uniform agent).
- `--start-from <run|model dir>` (new runs only) or `start_model` in config: copy to `it_0000`, first self-play, warm start, evaluated; replaced only if beaten.
- Earlier runs’ self-play data are deleted on new start; only the model is carried. Conclusions: `docs/EXPERIMENTS.md`.

## Stop / resume

- Stop finishes the current chunk and packs. Same `run` continues.
- By default there is no time budget — stop when you want the morning pack.
- Optional timed budget used up: clear `hours` / `--hours 0` and `run` again to continue until stop, or raise hours.
- Kill / power loss: run again; unfinished chunk is lost, `.done` stages are not redone.
- One lock per run (`run.lock`).

## Layout

```
D:/ares/runs/<name>/   state, logs, exp/<exp>/it_*/{model,eval}, handoff/
D:/ares/data/<name>/   data/<exp>/it_*/*.ares
D:/ares/tmp/<name>/    child TMP
```

Chunks: `*.ares.tmp` → `*.ares` when complete. Bad chunks → `quarantine/`.

## Reading results

- Morning pack: `…/handoff/<name>_<time>.zip` — `REPORT.md`, `best_model/`, logs, `journal.json`.
- `gain` = improvement over AGENT without a model. `score` = mean gain over eval references.
- `val_loss` vs `constant_val_loss`: below constant ⇒ generalises; above ⇒ memorising games.
- Hypotheses table + evidence-per-iteration in `REPORT.md` → human/AI conclusions in `docs/EXPERIMENTS.md`.

## Tests

- `python -m unittest discover -s ml/tests -v` (use `ml\.venv` when torch tests are needed).
- `RUN_E2E=1 python -m unittest ml.tests.test_e2e_real -v`
- Java: `mvn test`
