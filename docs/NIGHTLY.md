# Nightly pipeline

One command: self-play (Java) → train (Python) → eval. Several experiments per night, finished chunks only, stop/resume anytime.
Default trainer: `ml/train_all.py` (shared observation trunk, one head per `DecisionType`, folders `model/<TYPE>/`).

## Config (`ml/nightly_config.json`)

- `storage.run_root / data_root / temp_root` — durable store on `D:` (state/models/logs; finished chunks; child TMP).
- `storage.stage_root` — optional **SSD** path (e.g. `C:/ares/stage`): self-play writes here, finished `.ares` are moved to `data_root`. Respects the `C:` reserve. Omit to write straight to `data_root`.
- Decision files are ARES **v4**: observation and candidate features are float16 (v3 candidate float32 still readable). Halves the dominant part of each row so the disk is less often the bottleneck.
- `storage.reserves` — free space never to touch per volume; checked before stages and during self-play.
- `data_budget_gb` / `keep_data_iterations` — data cap and training window. **Models and evals are never deleted.**
  `data_budget_gb` is **our own cap on how much data THIS run may keep**, not free disk space — do not read a shrink as "disk is full": it fires just as readily at 15 GB used out of a 300 GB budget if a volume **reserve** (a different, per-volume number, e.g. `C:/` 30 GB) is the one actually violated. Rotation deletes data outside the window; if still short, deletes the **oldest** data of this run (window shrinks), never the iteration being written or the latest finished iteration of an active experiment. Each shrink now logs and records **which** constraint fired (a named volume's free space vs its reserve, or the data budget) — read that reason, not just the GB number. Shrinks appear in `REPORT.md` as **DATA WINDOW SHRUNK**, with the reason. `stopped_disk_low` only if even the minimum does not fit.
- `delete_data_of_other_runs` (default true): a **new** run deletes self-play data of earlier finished runs; their models/reports stay.
- `java.xmx`, `java.threads` (in-flight games). OOM → Java exit 6, stage restart, logged under **OUT OF MEMORY**.
- `hours` — **optional**. Default / omit / `null` / `0` = **run until you stop** (`nightly stop` or Ctrl+C) or the disk guard trips. No need to guess how long the machine is free; extra time is used automatically. A positive number is only if you want a timed budget. Experiments share a rung by **round-robin**. Early rungs without a timed budget advance after `min_iterations` each (default 1); the last rung runs until stop.
- `plateau_patience` / `plateau_min_iterations` (top-level, overridable per experiment) — a guard **independent of the rung schedule**: an experiment that has not beaten its own best score for `plateau_patience` of its own iterations in a row (after at least `plateau_min_iterations`, so 1-2 noisy evals do not kill it early) is marked **plateaued** and stops immediately — its time and data window go to the others right away, instead of it occupying a fixed share of the night for a result that is already in. Defaults: 5 / 3. Set `plateau_patience: 0` on an experiment (or at the top level) to disable it. This is what makes it safe to list **more** experiments by default: a loser frees its slice as soon as it is one, rather than everyone splitting the night by a fixed fraction regardless of how things turn out.
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

## Training I/O (why the train stage should not be disk-bound)

`train_all.py` / `train_phase.py` read every chunk file **once**, with one buffered `file.read()` per file
(`ares_data.read_decisions`), straight into an in-RAM pool split into train/val; every epoch after that trains from RAM
only — no further disk access, no second pass for validation. This was **not** always true: an earlier version used
`np.memmap` for this one-time ingestion and (in `train_phase.py`) literally re-read every chunk from disk on every
epoch. On a hard disk, `np.memmap` services a sequential scan as a stream of small page faults instead of one large
buffered read with normal OS read-ahead — the difference between the train stage finishing in a handful of seconds and
it taking most of a quarter hour with the disk pegged and the GPU idle, which is what the fix addresses (see
`docs/EXPERIMENTS.md`, Foundations §3 "I/O", and "Night 5" for the incident this came from). If a future trainer
re-introduces per-epoch disk reads, watch for the same symptom:
high disk, near-idle CPU/GPU, for a FIXED amount of iteration time regardless of model size.

## Reading results

- Morning pack: `…/handoff/<name>_<time>.zip` — `REPORT.md`, `best_model/`, logs, `journal.json`.
- `gain` / `score` = **mean**, over `eval.references`, of (model win rate − same agent with no model, i.e. uniform-random candidate). It is NOT a win rate by itself and NOT "vs RANDOM" specifically.
- `win_rate` (console log, `eval.json`) = the **mean raw win rate across all `eval.references` combined** — e.g. with `references: [RANDOM, SMART]` it is `(score_vs_RANDOM + score_vs_SMART) / 2`, which can look low even when the model is doing fine against RANDOM, simply because SMART is much harder. **Never read `win_rate` as "vs RANDOM"**: always check the per-reference breakdown next to it in the log line (`vs RANDOM=.. vs SMART=..`) or `eval.json`'s `vs` map, one entry per reference with its own `score`/`gain`/`gain_of_room`.
- `val_loss` vs `constant_val_loss`: below constant ⇒ generalises; above ⇒ memorising games.
- Hypotheses table + evidence-per-iteration in `REPORT.md` → human/AI conclusions in `docs/EXPERIMENTS.md`.

## Tests

- `python -m unittest discover -s ml/tests -v` (use `ml\.venv` when torch tests are needed).
- `RUN_E2E=1 python -m unittest ml.tests.test_e2e_real -v`
- Java: `mvn test`
