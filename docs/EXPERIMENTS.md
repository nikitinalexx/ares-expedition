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

**3. I/O** — the train stage must be CPU/GPU-bound, not disk-bound: every chunk file is read once, buffered, into an
in-RAM pool; nothing re-reads from disk per epoch (night5 found and fixed a case where it did — see "Night 5" below).
Watch for the symptom even after this: high disk, near-idle CPU/GPU, for roughly the same wall time regardless of model
size — that is I/O, not training.

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

## Night 5 -- 2026-11-12 (stopped early by the owner, pipeline issues found)

**H-N1 is still open, not rejected** — the owner stopped the run after it7 because the pipeline itself had real problems, not because H-N1 looked bad. The fixes below are now in; resuming the same run (`nightly run night5 --start-from night4`, or fresh) is the natural next step before drawing any conclusion about H-N1.

What the owner reported, and what each one was:

1. **Train stage looked silent and HDD-bound** — log showed a selfplay commit at 17:23:19 and the next line (iteration promoted) at 17:41:47, an 18-minute gap with no progress lines in between; separately observed via Task Manager: disk ~99%, GPU ~2%, RAM only ~49% used on a 32 GB machine despite the pool fitting comfortably in RAM. Root cause (found by code reading, not by a fresh measurement on the night machine — confirming the exact before/after numbers there is the first thing to check on resume): `train_all.py`'s `build_pool` read every chunk file **twice** (once for the train split, once for validation) via `np.memmap`. For a one-time *sequential* scan of the whole file — which this is, nothing in it is random access — `np.memmap` on Windows services the scan as a stream of small page faults instead of one buffered read with normal OS read-ahead; `train_phase.py` (the `--independent` fallback) had the same problem but far worse, re-scanning the disk on **every epoch**, not just once. Fixed: both trainers now do ONE buffered `file.read()` per file (`ares_data.read_decisions`) and build train+val together in that single pass; everything after that is RAM-only. See `docs/NIGHTLY.md`, "Training I/O".
2. **Log numbers unreadable**: `gain +0.258, win_rate=0.262` — `win_rate` is the mean raw win rate averaged across *all* `eval.references` (RANDOM and SMART together here), not "vs RANDOM" as read; with SMART far harder than RANDOM, the blended number undersells (or oversells) progress against either one specifically. Fixed: the log line now prints the per-reference breakdown (`vs RANDOM=.. vs SMART=..`) next to the score, and `docs/NIGHTLY.md` spells out that `win_rate` is never to be read as a single opponent's rate.
3. **"DATA WINDOW SHRUNK ... (15.3 GB of 300 GB now)"** while ~400 GB was free on `D:` — read (reasonably) as nonsensical. `300 GB` is **our own self-imposed cap** on this run's data, never disk free space, and the message always printed that number regardless of what actually triggered the shrink. The real trigger here was almost certainly the `C:` reserve (the owner separately found `C:` nearly full and freed it to ~200 GB) — a volume `stage_root`/the pipeline writes to, unrelated to the 300 GB/400 GB numbers the owner was looking at. Fixed: the shrink message and the `REPORT.md` entry now name the actual violated constraint (a specific volume's free space vs its reserve, or the data budget) instead of always showing the budget pair. Also bumped the `C:` reserve 20→30 GB in the committed config as a small safety margin, since it is the OS drive.
4. **"Run three experiments for a fixed third of the night each, then just analyse"** — fair complaint about the *process*, not only this config (night5 itself only had one experiment). Successive halving (rungs) only compares experiments against each other at fixed checkpoints; nothing previously let an experiment that is *not even beating its own best score any more* stop itself early. Added: a **plateau guard**, independent of the rung schedule — `plateau_patience` consecutive non-improving iterations (after `plateau_min_iterations`, default 5/3, per-experiment override) marks an experiment "plateaued": it stops immediately, its data window is freed at once, and in a multi-experiment night its round-robin turns go to the others right away instead of waiting for a rung boundary. This is what makes scheduling *more* experiments by default reasonable — losers free their slice as soon as they are one, rather than every experiment getting a fixed fraction regardless of outcome. Tests: `ml/tests/test_nightly.py::PlateauGuard`.

None of this touches H-N1's actual mechanics (the AGENT:RANDOM matchup, the reward, the model). The 7 completed iterations of night5 (whatever their file-system state holds) are real data under the OLD, slower pipeline; resuming continues training the same model, now faster per iteration and with clearer logs, so a verdict on H-N1 is still pending.

## Night 5 -- 2026-11-12/13

Single experiment vs_random: AGENT:RANDOM:1, 30000 games/it, rows8, start-from night4 best.
Ran 15:09 -> 00:21 (7.67 active hours). Stopped by plateau guard after it23.

| it | vs RANDOM | vs SMART | note |
|---|---|---|---|
| 1 | 42.0% | 5.2% | start (from night4 rows8 it7) |
| 8 | 48.1% | 5.3% | |
| 16 | 52.5% | 7.0% | FIRST TIME OVER 50% |
| 17 | 50.9% | 5.9% | |
| 18 | 57.7% | 7.0% | BEST -- benchmark triggered |
| 20 | 53.2% | 8.6% | |
| 23 | 50.2% | 5.4% | plateau, stopped |

Benchmark (best model it18 vs NETWORK): 2.0% [0.6%, 7.0%] -- still near 0, expected at this skill level.

Verdict: H-N1 CONFIRMED. AGENT:RANDOM gives real climb. 50%+ vs RANDOM is a milestone.
Selfplay speed: 222-237 games/s (RANDOM has no net, twice as fast as AGENT:AGENT).

Problems:
- Training grew to 500-548s/it by it21-23 (pool = 54K games, 2 iterations). Too slow.
- Pipeline stopped at 00:21; plateau guard finished the experiment but no new one to continue.
- label_mean = 0.040 (warning: unbalanced labels). Investigate: in AGENT:RANDOM the label
  counts rows of AGENT decisions only but win label may be computed globally including RANDOM rows?
  Strength results look fine, but flag for investigation.

Night 6 fix: keep_data_iterations=1 (pool 15K, train ~50s), games_per_it=15000.
Command: nightly run night6 --start-from night5

## Night 6 -- 2026-11-13 (39 min, plateau guard killed it)

Single experiment vs_random (old config, selfplay not added yet). 15000 games/it, keep_data_it=1.
Ran 08:23 -> 09:02 (0.65 active hours). Plateau guard killed at it9.

| it | vs RANDOM | vs SMART |
|---|---|---|
| 1 | 46.1% | 5.9% |
| 2 | 55.1% | 5.8% |
| 4 | 61.8% (BEST) | 7.8% |
| 7 | 59.4% | 8.7% |
| 9 | 59.4% | 8.5% |

Verdict: continued climbing vs RANDOM. 61.8% is new best. BUT plateau guard with patience=5 killed
the experiment on normal eval noise (~5% swing in 2000 games). Real plateau or noise? Unclear.
Pipeline stopped because only 1 experiment.

Night 7 fix:
- plateau_patience: 15 (was 5) -- noise in 2000-game eval is ~5%, patience=5 is too trigger-happy
- plateau_min_iterations: 5 (was 3)
- 2 experiments: vs_random + selfplay (H-N2: self-play now meaningful at 60% vs RANDOM level)
- Round-robin: even if one plateaus, the other keeps running all night
Command: nightly run night7 --start-from night6

## Discovery on Self-Play Temperature (Tested after Night 6)

Investigated the 'label win share: 0.049' warning in self-play data.
Ran direct empirical tests with the Night 6 best model (it4, 61.8% vs RANDOM):
- temperature 0 (greedy): AGENT 121 : 72 RANDOM (62.25% win rate)
- temperature 0.5 (sampling): AGENT 10 : 189 RANDOM (5.25% win rate!)

Conclusion: Sampling with temperature 0.5 across 30+ sequential turns compounded error probabilities,
causing the agent during self-play to play 10x worse than random (losing 95% of games).
The network was training on 95% loss labels! Setting selfplay.temperature = 0 completely solves this.

## Night 7 -- Plan

- selfplay.temperature: 0
- plateau_patience: null (disabled: runs continuously all night, no 39-minute early exit)
- Two experiments running round-robin:
  1. vs_random: AGENT:RANDOM:1 with temp 0 (aiming for 70%+ with clean win labels)
  2. selfplay: AGENT:AGENT:1 with temp 0 (competitive self-play now that model is >60%)

Command: nightly run night7 --start-from night6

## Night 7 -- 2026-11-13 (13 hours, 120 iterations total)

Two experiments round-robin: vs_random (AGENT:RANDOM:1, 15000 games) + selfplay (AGENT:AGENT:1, 15000 games).
Selfplay temperature: 0. Start from night6.

Results:
| experiment | best it | vs RANDOM | vs SMART | vs NETWORK | score |
|---|---|---|---|---|---|
| vs_random | it46 | **85.7%** [83.6%, 87.6%] | **25.9%** [23.9%, 28.0%] | 0.5% (tested) | **0.5564** |
| selfplay | it42 | 79.7% [77.5%, 81.8%] | 22.1% [20.2%, 24.1%] | -- | 0.5068 |

Key Findings:
1. **temperature: 0 was the breakthrough**: agent jumped from 61.8% to 85.7% vs RANDOM, and from 7.8% to 25.9% vs SMART.
2. **The 68-point ceiling**: Agent consistently scores ~68 points. Against RANDOM (avg ~50 pts), this wins 86% of games.
   Against SMART (avg ~82 pts), this loses ~74% of games. Against NETWORK (avg ~105 pts), it loses 99% of games.
3. **Why it stopped growing around it46**: Win share vs RANDOM reached 84%. To beat RANDOM, 68 points is enough.
   There is zero gradient pressure to score 80-100 points when playing an opponent that scores 50.
4. vs_random clearly beat pure selfplay (0.5564 vs 0.5068).

## Night 8 -- Plan: Curriculum Stage 2 (Facing SMART)

Goal: Break past the 68-point ceiling by training against SMART (who scores ~82 pts). Only games where the agent
builds a stronger engine and scores >82 points receive outcome=1.0.

Two experiments round-robin:
- `vs_smart`: `AGENT:SMART:1` (10000 games/it, ~3.5 min/it) -- direct pressure to outscore SMART.
- `mixed_smart_random`: `AGENT:SMART:0.7,AGENT:RANDOM:0.3` (12000 games/it, ~3.5 min/it) -- 70% SMART + 30% RANDOM anchor.

Command: `nightly run night8 --start-from night7`
