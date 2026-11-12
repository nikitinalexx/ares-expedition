# Agent experiment journal

Each night must confirm or rule something out. Read this before planning the next night.
One variable per experiment. Raw numbers live in the morning pack (`journal.json`, `REPORT.md`); **verdicts** go here.

Metrics:
- `gain` / `score` — improvement over AGENT without a model (mean over `eval.references`).
- Per-ref **win rate** (`aScore`) vs RANDOM/SMART — the real climb ladder.
- `val_loss` vs `constant_val_loss` — below constant ⇒ generalises.
- NETWORK — rare `benchmark` only, never a training source. **Do not run it until win rate vs RANDOM or SMART is ≥50%** (config: `benchmark.min_win_rate`). 40% vs RANDOM with 0% vs NETWORK is normal: the skill gap is enormous; a strong player beats random ~always. If NETWORK is still ~0 after the gate opens, raise the bar (60%, 70%…) and ignore NETWORK until the fast yardsticks move again.

Speed (planning): eval AGENT+model vs RANDOM ≈ 75 games/s (2×2000 ≈ 1–2 min); self-play ≈ 7–20 games/s (disk-bound); train on ~16k games ≈ 30 min.

## Foundations

**1. Data quality** — observation (no hidden info), candidates, label noise; who plays; games in the window; rows/game/type; shuffle; label balance; freshness vs current policy.

**2. Training quality** — architecture, regularisation, early stopping — on top of good data.

Owner/AI statements are **hypotheses** until a night measures them.

## Rules of evidence

- Confirmed = night (or controlled compare) with numbers and intervals.
- One variable vs the night’s control.
- Rejected results are recorded the same way.

## Hypotheses

| id | claim | source | status | how |
|---|---|---|---|---|
| H-D1 | Rows of one game in a row cause memorisation; mixing over many games fixes it | owner (older nets) | night2: weak yes on val (~0.04), strength indistinguishable under soft cap ≤32 | default: mixed pool |
| H-D2 | Effective volume is #games, not #rows | owner | night2: directionally supported | evidence table |
| H-D3 | > few rows/game/type add little (near-duplicates) | AI from H-D2 | night3: val better at 8 than 32; strength slightly lower than control peak — weak support | `--rows-per-game` |
| H-T1 | Frequent early stopping beats fixed epochs | night1 | used; best epoch 1–6 of 50 | keep |
| H-T2 | Weight decay 1e-3 helps | AI | night2: **rejected** (worse val) | keep 1e-5 |
| H-D4 | Games vs RANDOM/SMART are better training data than self-play | AI | **shelved** (weak teachers) | — |
| H-D5 | Self-play temperature 0.5 dirty labels; 0.2 is cleaner/stronger | owner→AI | night3: **not supported** (temp_low 0.192 < control 0.238; start≈0.17) | keep 0.5 for now |
| H-E1 | 300 games/opponent ranks experiments | — | night2: **rejected** (identical setups 0.03/0.05/0.13) | ≥2000 |
| H-P1 | A weak non-random policy that passes / sells / skips builds loses tempo to RANDOM’s constant small gains; NETWORK stays useful because VALUE makes it almost always act and optimise | owner (careful: not an axiom) | open — fits night3 0% vs NETWORK while ~44% vs RANDOM | measure action mix (build/blue/pass/sell) vs RANDOM/NETWORK; only then design a fix |

## Data sources (working strategy)

Only AGENT decisions are recorded. External players are **yardsticks**, not teachers. Bootstrap: iteration 1 without a model; then best own model in self-play (`AGENT:AGENT`).

## Confirmed

- Outcome self-play gives a signal (night1).
- Eval only vs SMART/NETWORK is blind early (night1) → RANDOM in eval; NETWORK* in benchmark.
- Night1 feeding memorised games (9/9 val above constant).
- Night2 settings remove memorisation (23/23 val below constant); strength vs RANDOM ~35%, some wins vs SMART.
- Eval at 300 games/opponent is too noisy to rank (night2, H-E1).
- Start-model noise at 2000 games ≈ ±0.01 (night3: 0.173 / 0.174 / 0.167).

## Working theory

Generalisation appears with few rows/game/type + early stop; game count in the window helps val. Strong weight decay does not. Lower self-play temperature did not help (night3).

**Climbing is stuck:** night3 best was control it1 (warm-start from night2); later iterations did not beat it. ~44% vs RANDOM is progress but far from “beats random”; NETWORK 0% at that level is expected, not a crisis. Focus eval on RANDOM/SMART win rate; NETWORK only after the 50% gate.

Disk was the self-play bottleneck (~12 KB/row): observation already float16, but **padded candidate features were float32**. ARES **v4** stores candidates as float16 too (~half the dominant payload). Optional `stage_root` on SSD writes during self-play, then commits `.ares` to HDD. Scheduler: round-robin until stop (no hours guessing).

---

## Night 1 — 2026-11-10/11

Signal vs RANDOM 7.2% vs 0.7% baseline. Eval vs SMART/NETWORK blind. All trainings memorised (val > constant).

## Night 2 — 2026-11-11

Stopped early on data budget (fixed later). Best: sequential_feed it8, RANDOM 35.5% [30;41], SMART 4.3%. All val gaps negative. H-D1 weak; H-T2 no; H-E1 no; H-D2 directional.

## Night 3 — 2026-11-11/12 (`night3`)

`--start-from night2`, 8h active budget, eval 2000/opponent, experiments control / temp_low / cap8.
Ran 19:10→02:30 (**finished** with ~40 min leftover): fixed per-experiment slots + no continue-after-rungs. **Fixed in code after the night.** Window shrunk 4× (disk). Benchmark ran.

| experiment | its | scores | best vs RANDOM / SMART | val−const |
|---|---|---|---|---|
| control | 4 | .238 .186 .169 .220 | **0.439 / 0.046** (it1) | −0.20 → −0.22 |
| temp_low (T=0.2) | 3 | .176 .172 .192 | 0.361 / 0.031 | −0.21 → −0.22 |
| cap8 (rows=8) | 4 | .176 .196 .209 .187 | 0.398 / 0.029 | −0.23 → −0.23 |

Start model (night2 best) scored ≈0.17 three times. Benchmark best (control it1): NETWORK **0.0**, NETWORK_V2 **0.055**.

Verdicts: H-D5 not supported; H-D3 weak support on val; **self-play climb failed** (best = first iteration); agent still useless vs NETWORK → H-P1 next.

## Night 4 — plan

Goal: use the whole free window; fewer parallel experiments (disk); start measuring H-P1; keep climbing from night3 best.

- No `hours` — runs until stop (scheduler fix after night3).
- One rung, 2 experiments, 7000 games/it, eval 2000, `--start-from night3`.
- Disk: `keep_data_iterations: 2`; 2 experiments so peak fits ~150 GB better.

| experiment | hypothesis | difference |
|---|---|---|
| control | continuing from night3 best improves gain over iterations when given a full shared budget | night3 train settings |
| rows8 | H-D3 as default: write+train ≤8 rows/game/type saves disk without losing strength vs control | `max_rows_per_game_type: 8` + `--rows-per-game 8` |

Separately (not a night ablation): dump action frequencies (build / blue / standard / sell / pass-like) for AGENT model vs RANDOM vs NETWORK on a fixed set of states — evidence for/against H-P1 before changing the objective.

## Night 4 -- 2026-11-12 (stopped early)

Two experiments round-robin: control (AGENT:AGENT, rows32) + rows8 (AGENT:AGENT, rows8). 7000 games/it, start-from night3 best.

| experiment | its | best vs RANDOM / SMART |
|---|---|---|
| control | 10 | 40.8% (it4) / 4.4% |
| rows8 | 9 | 45.7% (it7) / 5.5% -- best result so far |

Throughput: selfplay ~60s/7000 games, train ~34s, eval ~41s = ~2.5 min/iteration. CPU 82% on neural forward.

Verdicts: H-D3 confirmed as default (rows8 it7 = best ever, adopted as global default). AGENT:AGENT does not climb: two weak copies produce noisy win labels; at <50% vs RANDOM the winner is essentially random noise.

H-N1 added: switch to AGENT:RANDOM opponent to get a real outcome signal (see Hypotheses table).

## Night 5 -- plan

Goal: test H-N1.

- Experiment: vs_random -- matchups AGENT:RANDOM:1, 30000 games/it, rows8, start-from night4 best (rows8 it7, 45.7% vs RANDOM).
- Rationale: when AGENT beats RANDOM those decisions are genuinely better-than-random; losing decisions are real mistakes. Positive feedback loop as win rate rises.
- Disk budget 300 GB (400 GB free, 30 GB reserve). keep_data_iterations 2.
- Iteration time: ~4 min selfplay + 34s train + 41s eval = ~5-6 min. Expect ~100 iterations overnight.
- Success: win rate vs RANDOM reaches >50% within 20 iterations.
- If not: H-N1 rejected; revisit strategy (filtered imitation / curriculum / auxiliary signals).

Command: nightly run night5 --start-from night4
