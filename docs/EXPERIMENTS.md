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

### Why NETWORK is 34 points ahead (owner, 2026-11-14) — the theory everything is now measured against

NETWORK is not better at picking cards out of a menu. It plays a **conversion** game:

- It **almost never picks phase 4 (income)** — it lets the opponent call it. It plays 4 only when its income difference is
  large, judged the same way cards are judged: "if both collect, how does VALUE rank the two states?" A strong positive
  difference is the only reason to spend the pick on income.
- It plays **5 (draft) and 1/2 (build) most of the time**: draft the strong cards, then build them with every discount
  stacked. Late game, with high income and high discounts, it maximises 1/2 and builds non-stop.
- Every income round the *opponent* calls also pays NETWORK, and NETWORK has the money to turn that pay-out into a bigger
  board than the opponent can. The gap widens every round the weaker economy keeps calling 4 (snowball).
- A second viable engine: blue cards that make phase 3 dense (many actions, cards, resources, points) while the opponent's
  phase 3 has nothing useful.

Our agent does the opposite: it picks 4 or 5 in **97.5%** of the rounds, collects the income, and then **declines ~40% of
its build opportunities**. It hoards. That is consistent with every measurement above: search on the immediate afterstate
helps (the *ordering* of cards is fine), the absolute "build or stop" comparison fails (V was trained on a hoarding
policy, so it believes spending is bad), and forcing build phases is catastrophic *for this policy* — it cannot afford
them. The lever is not the menu, it is the conversion rate.

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

## Search (family D) — Step 0, 2026-11-13 (no night; `--mode bench`)

Numbers and method: `docs/SEARCH.md`.

| question | answer |
|---|---|
| is a mid-turn decision point replayable? | **yes, exactly** (100% identical observation on 1159 captures) by replaying the action attempt from `turnStarted` with the recorded prefix; 61% if re-entered at the mid-turn state; holds with a random continuation policy too |
| copy of the game state | 6–10 µs (p50/p95: 5.6 / 27 µs) |
| engine work between two decisions | 57 µs mean — the dominant cost, not the net |
| full rollout from a decision point | 8.7 ms (~18 µs per remaining decision); 627/s on 8 threads |
| full-rollout search in training | **rejected**: ~900 CPU-h per 15 000 games |
| full-rollout search at deployment | **affordable**: 25–75 ms per move |
| trainable search shape | 1-ply `Q(s,a)=V(apply(s,a))`, ~0.1 ms per candidate → ~11 CPU-min / 15k games |

Verdict: search is feasible, but only as **deployment-time** search or as **1-ply + a learned `V(s)`** in training.
`V(s)` is also the baseline the current one-outcome-per-game signal lacks. No search is built yet.

## Night 8 — result 2026-11-13/14 (62 iterations, 8.8 h): **negative**

The night-8 plan (train against SMART, `AGENT:SMART` and `AGENT:SMART:0.7 + AGENT:RANDOM:0.3`) did not produce a model
that beats the night-7 parent. Every iteration of both experiments scored *below* the start model, monotonically:

| it (vs_smart) | vs RANDOM | vs SMART | it (mixed) | vs RANDOM | vs SMART |
|---|---|---|---|---|---|
| start (night7 it46) | 86.7% | 23.8% | start (night7 it46) | 86.7% | 23.8% |
| 1 | 81.9% | 21.5% | 1 | 82.1% | 21.8% |
| 20 | 78.6% | 20.6% | 20 | 78.9% | 20.1% |
| 46 | 78.2% | 18.9% | 40 | 77.7% | 18.4% |

Diagnosis: the signal carried by one binary outcome per game, applied to all ~500 decisions of that game, has **state
information but no candidate-ranking direction**. A game the agent wins because the opponent is RANDOM says "spend on
cards that pay in 15 rounds" no more than "don't"; averaging over a night therefore converges to the mean of the game,
which is a weaker behaviour than the parent's.

Attempted fix, advantage weighting (`--adv-weight`: weight each row by `outcome - V(s)`, `V` fitted on a held-out 25% of
the *same* games): the damage per iteration is smaller (82.9 / 22.2 vs 78.6 / 20.6 at equal budget), but the parent is
still never beaten. Grid (`--adv-weight` 0 / 1 / 2 × `--freeze-trunk`): 82.85 / 22.20, 82.58 / 22.75, 77.4 / 19.15.

**Is the outcome signal at least *learnable*?** Yes — `V(s)` (760-dim observation → `P(win)`, 256-128 MLP, exported as
`value_mlp`) reaches **AUC 0.856** on the vs-SMART data (label mean 0.257) and **AUC 0.859** on the vs-RANDOM data
(label mean 0.84), held-out by game. It overfits after epoch 1 (val loss 0.30 → 0.52), so the export keeps the best
epoch. So the state *does* determine the result to a useful degree; the SBG loss, not the data, was the problem.

Rejected on the way: static per-card value tables. Card value is situational — AntiGravity in round 2 buys a dozen
projects, in round 17 two; a ridge fit of the final margin on the cards built (`ml/card_values.py`, 9912 player-games,
out-of-fold R² 0.128, coefficients −4.7…+6.5 points) is a correlation, not a value function, and the owner's rule is
that a coarse per-card estimate is unacceptable: it cannot say whether *this* card, *now*, is +5% or −5% to win.

## One-ply search on `V` — 2026-11-14 (the first thing that beat the parent)

Shape: at a BUILD_CARD decision, every candidate card is applied to a **copy** of the game (sync engine variants
`buildGreenProjectSync` / `buildBlueRedProjectSync`, inner inputs/payment answered by the policy), and the copy is scored
with `V(observation of the afterstate)`. The candidate with the best afterstate wins.

Fairness: the afterstate observation is built for the moving player only (`ObservationBuilder` is audited to contain no
hidden information — `target/card_tracking_audit.txt`: 197 of 209 lasting-effect cards recognised on both tables, 0 hand
identity gaps, 0 hand leaks, card resources encoded per card), and no rollout of the opponent's turn is made, so the
opponent's hand and the deck order cannot enter the score.

Two modes, and the difference matters:

| mode | rule | result (3000 games) |
|---|---|---|
| full: compare `V(s)` with `V(after build)` | search decides build **and** stop | **broken**: 84.5% chose stop, mean gap −2.0 logit, 60.6% vs RANDOM |
| rank (`-Dares.agent.search.rank=true`, default) | V only re-orders the cards the policy is willing to build | 87.08% RANDOM / 26.67% SMART |
| control: same code, random order of the same cards | proves the ordering, not the perturbation, carries the signal | **15.28%** vs SMART |

Why full mode fails: the afterstate of a build is a state V rarely sees in training (a position inside one action
attempt) and V systematically prefers not to spend; `V(s)` and `V(after build)` are not on one scale. Card against card
is.

**Frozen parent (night7 it46), 10 000 games, same command, search on/off:**

| configuration | vs SMART | vs RANDOM |
|---|---|---|
| plain policy (no search) | 24.46% [23.63, 25.31] | 84.77% [84.05, 85.46] |
| rank, `V(me afterstate)` | 26.03% [25.18, 26.90] (+1.57, p≈0.011) | 86.35% [85.66, 87.00] (+1.58, p≈0.001) |
| rank, **symmetric**: `V(me) − V(opponent)` on the same afterstate | 29.27% [28.38, 30.16] (+4.81) | 87.42% [86.75, 88.05] (+2.65) |
| symmetric + **draft** (`BUILD_CARD,DRAFT_CARDS`) | **31.39% [30.48, 32.30] (+6.93)** | **88.88% [88.25, 89.48] (+4.11)** |

Cost: 1.1–1.4 ms per searched decision without symmetry, 1.8 ms with it (4.3 candidates, copy + build + two forward
passes); 44–47% of the choices differ from the policy; 0 unusable candidates.

**Why symmetric is much better.** The copy of the position is public, so it can be described twice: as my state and as
the opponent's. `V(me) − V(opp)` cancels whatever part of `V`'s error is a property of the position rather than of the
player (drift, calibration of the round, "rich position" bias) and keeps the part that is a property of the difference.
The net's errors are not anti-symmetric (`V(me) + V(opp)` ≠ 1), and that asymmetry was exactly the noise in the ordering.
Same idea as the failed full mode, but with the opponent as the reference instead of the pre-move state.

What did **not** help (measured, so it is not tried again):

| variant | result | verdict |
|---|---|---|
| full mode: search also decides build/stop, `V(after) vs V(stop)` | 23.31% SMART / 77.32% RANDOM (worse than plain) | rejected — 32% of decisions become "stop"; a post-build position is off `V`'s distribution |
| re-ranking `PERFORM_BLUE_ACTION` on top (symmetric) | 28.78% / 87.02% | neutral to slightly negative |
| a second value net on the vs-RANDOM distribution (`v_random_it61`) | 26.67% / 86.1% (3000 games, one-sided) | no difference |
| a value net on both datasets (`v_mix`, AUC 0.902 — inflated, it separates the two curricula) | 26.04% / 85.41% | worse vs RANDOM |
| a wider net `512-256` (`v_smart_wide`, AUC 0.864) with symmetry | 28.61% / 87.86% | wash (2.3 ms vs 1.8 ms per decision) |
| **draft** re-ranked on top (`BUILD_CARD,DRAFT_CARDS`) | **31.39%** / **88.88%** | **kept: the biggest single gain after symmetry** |
| discard re-ranked on top (`BUILD_CARD,DISCARD_CARDS`) | 28.48% | no gain |
| selling re-ranked (`SELL_CARDS`, `SELL_CARDS_LAST_ROUND`) | 24.90% (−6.4) | harmful: V cannot rank "which card to give up for money" |
| mulligan re-ranked | — | not implemented: the discarded card is replaced by an unknown draw |

The draft works for a structural reason: the draft phase is picked in **half of all rounds**, the decision is "keep this
card of the reveal", and applying a candidate is just removing a card from the hand — no payment, no effect, no hidden
information. It is also the one place where the deck itself is chosen, so a better choice compounds through the whole
game. Selling fails for the mirror reason: the value of a card *in the hand* depends on the plan the policy has for it,
and V only knows the average plan.

One bug this exposed: a decision type the search did not implement was silently scored **as if it were a build** (a sell
went through the payment path, produced 4.7 million unusable candidates and a 13.07% win rate). The dispatcher now
returns "no opinion" for anything it does not implement, so an unsupported type can never be scored as a build again.

Next: the same search inside data generation (`ml/search_config.json`: `search_mixed` vs `plain_mixed`, identical
matchups, search is the only difference), so the policy imitates a better ranker instead of only being corrected at
deploy time.

## Search night (`search9`, 2026-11-14, 65 min) - the A/B is a tie, both arms lose to the parent

`ml/search_config.json`, two arms, same matchups (`AGENT:RANDOM:0.7,AGENT:SMART:0.3`), 12 000 games/iteration, search
**only** in the data generation of `search_mixed`. Both arms start from night7 it46 and are evaluated with search on, so
the difference is the data-generation change and nothing else. Both arms plateaued after 8 iterations (8 min each).

| model | vs RANDOM | vs SMART |
|---|---|---|
| it0 (the parent, two independent evals of the same folder) | 87.67 / 86.58 | 28.43 / 27.72 |
| `plain_mixed`, mean of it1..it8 | 85.0 | 25.1 |
| `search_mixed`, mean of it1..it8 | 85.5 | 25.6 |

Conclusion: replacing the taken card with the search's pick in the recorded data does **not** make the policy better,
even though that pick is worth +4.8 points at play time. Combined with night 8 (outcome-only signal) and the
advantage-weighting grid, this is the third independent rejection of *this* training objective: one binary outcome (or
one reordered action) per ~500 decisions, applied to the chosen candidate only, converges to a policy that reproduces
the *average* of the games instead of the best move. The training side, not the data, is what has to change next: the
candidate set needs a target for **every** candidate (distillation of the search's ranking), not only for the taken one.

Also measured the same day, all rejected (10 000 games each unless said otherwise):

| variant | vs SMART | vs RANDOM | verdict |
|---|---|---|---|
| ensemble of 3 value nets (vs-SMART CPU, vs-SMART GPU, vs-RANDOM) | 28.91% | 87.36% | wash; 2.5 ms/decision vs 1.8 |
| price calibration: add back `slope(MC) x MC spent` to the afterstate score | 28.82% | 86.92% | wash in rank mode; full mode still 76% vs RANDOM (400 games) |
| NETWORK benchmark, best configuration, 200 games | - | - | **7.5% [4.6, 12.0]**, 68.4 points against 102.3 — the target is far |

The last row is the strategic fact of the day: the agent plays ~68-69 points against SMART/RANDOM and ~68 against
NETWORK, while NETWORK scores 102 and the owner beats it. A card-ordering gain of 1-5 points of *win rate* against
SMART does not touch a **34-point gap in the level of play**, so the next lever is not another re-ranking variant but a
mechanism that can see further: deployment-time search with real rollouts (costed in `docs/SEARCH.md`, Result 2: 8.7 ms
per rollout, 259/s on 8 threads, ~70 ms per move for K=8), whose missing piece is a **fair determinization** of the
hidden cards - the rollout currently copies the real deck order, which is clairvoyance and is not allowed.

The price calibration was removed from the code: the stop/build decision is still the policy's, and every attempt to
hand it to `V` (full mode with or without the price correction) loses 9-15 points. The measurement that explains why,
now printed by the search itself: **41-42% of the decisions that offer a card, the policy declines to build** - the
headroom this search cannot reach, because `V` ranks cards well against each other and badly in absolute terms.

## Value audit — does `V` see what the encoder claims? (2026-11-14)
Instrument: `SelfPlayMain --mode audit` (`services/selfplay/ValueAudit.java`). It captures real positions, applies one
controlled change to a copy, and reports `dV` of the same score the search uses (`V(me) − V(opp)`, halved), with the
expected sign, the share of positions where the sign holds, the features that actually moved, and the result per round
band. Negative controls (the same change on the opponent) check the sign handling.

300 games, 3 770 positions, median round 16, median 4 cards on the table, opponent SMART:

| perturbation | expected | mean dV | P(sign) |
|---|---|---|---|
| mc +10 | > 0 | **+0.058** | 1.00 |
| mc −10 | < 0 | **−0.053** | 1.00 |
| plants +5 | > 0 | **+0.144** | 1.00 |
| heat +5 | > 0 | +0.073 | 1.00 |
| TR +1 | > 0 | +0.066 | 1.00 |
| forests +1 | > 0 | +0.123 | 1.00 |
| income mc / card / plants / heat +1 | > 0 | +0.114 / **+0.252** / +0.136 / +0.130 | 1.00 |
| extra point +1 | > 0 | +0.033 | 1.00 |
| hand +1 card | ≥ 0 | +0.085 | 0.81 |
| **opponent** mc +10 / plants +5 / plants income +1 | < 0 | **−0.058 / −0.144 / −0.136** | 1.00 (all) |
| income steel +1 | > 0 | **−0.023** | 0.37 |
| income titanium +1 | > 0 | **−0.069** | 0.21 |

The encoder is correct on every axis the audit can trace (`changedFeatures` shows the perturbation landing on `mc`,
`plants`, `opponent mc`, `steel_income`, ...), and the symmetric score reads both tables.

**Card value does decay over the game, exactly as the owner insisted it must.** The same card added to the table is
worth much more in round 3 than in round 20, and the decay is steeper for production than for cash:

| perturbation | rounds 0-4 | 5-8 | 9-12 | 13-16 | 17+ |
|---|---|---|---|---|---|
| `played +159/178` (BUILDING, ENERGY, SCIENCE) | +0.623 | +0.580 | +0.555 | +0.519 | +0.479 |
| `played +128/142/155/168` (PLANT green) | +0.305 | +0.291 | +0.280 | +0.258 | +0.213 |
| `played +162/186/337` | +0.197 | +0.175 | +0.161 | +0.143 | +0.119 |
| `+1 card income` | +0.270 | +0.264 | +0.258 | +0.250 | +0.236 |
| `+1 heat income` | +0.147 | +0.140 | +0.134 | +0.128 | +0.117 |
| `mc +10` | +0.052 | +0.055 | +0.057 | +0.060 | **+0.061** |
| `+1 resource on a card` | +0.07 … +0.08 | | | | ~+0.07 |

The last two rows are the timing argument in one line: production is worth less as the game runs out, cash is worth
slightly more. "One microbe" is also visible: adding a card **together with one unit of the resource it stores** is worth
+0.07 logit more than the card alone, on every resource card tested (P = 1.00 on most).

**The one anomaly is faithful to the data, and that is the bad news.** Metal income is negative at every round, so the
recorded games were checked directly (`night8/vs_smart it_0062`, 232 560 rows):

| state | share of rows | win rate | win rate without it |
|---|---|---|---|
| steel income > 0 | 21.9% | **18.2%** | 27.0% |
| titanium income > 0 | 9.6% | **13.2%** | 26.4% |

Same gap in every round band. Steel/titanium income is a **discount** (2 MC off every building, 3 MC off every space card
— `DiscountService`), and SMART prices titanium at 3 per point, so it is not the rules: it is how *our policy* plays the
metal line. `V` learned the habit of its teacher, which is the structural limit of every value model trained on
"did this policy win": it cannot prefer a move this policy never plays well. The rollout search inherits exactly that.

**Correction (same day): that anomaly is state-dependent, and `V` models it correctly.** The audit now splits every
perturbation by whether a given card is on my table. Metal income is worth more exactly when the card that spends it is
there — the sign flips:

| card on my table | titan income +1 without the card | with the card | difference |
|---|---|---|---|
| AntiGravityTechnology | −0.069 | **+0.011** | **+0.080** |
| WaterImportFromEuropa | −0.067 | −0.006 | +0.062 |
| OlympusConference | −0.074 | −0.012 | +0.062 |
| EarthCatapult | −0.071 | −0.009 | +0.062 |
| SolarPunk | −0.068 | −0.009 | +0.059 |
| Steelworks | −0.069 | −0.013 | +0.056 |
| IronWorks | −0.068 | −0.034 | +0.034 |
| AquiferPumping (steel) | −0.023 | −0.014 | +0.009 |

That is the owner's AquiferPumping example measured inside the model: the flag of the card and the metal income are both in
the observation, and `V` has learned the interaction. Blue cards are **not** smeared — `TableCardEffectsFeature` writes
**one flag per card for both players** (82 flags each: `AquiferPumping` at index 145, `op_AquiferPumping` in the opponent
block), verified by name and by perturbation. The earlier baseline ("metal is bad") is not a missing feature: it is the
price of a discount that has nothing to spend on yet.

## Phase choice — the decision the search never touches (2026-11-14)
From the same data (`PICK_PHASE` rows, 80 000 rows / 10 000 games):

| phase | picks | share | win rate |
|---|---|---|---|
| 1 build green | 239 | 0.3% | 10.3% |
| 2 build blue/red | 1 505 | 1.9% | 10.9% |
| 3 blue actions | 286 | 0.4% | 21.7% |
| 4 income | 38 704 | **48.4%** | 25.6% |
| 5 draft | 39 266 | **49.1%** | 25.8% |

The agent picks income or draft in **97.5%** of the rounds and almost never takes a build phase — which is where the
picker's extra build, the −6 discount and the extra income live. Note the rules: a phase resolves only if *someone*
picked it, so the agent's builds mostly happen because the opponent or the dummy picked phase 1/2.

That looked like a habit worth fixing, so it was measured instead of argued (`-Dares.agent.preferPhase=LIST` forces the
first legal phase of the list; 3000 games vs SMART, policy unchanged otherwise):

| forced phase preference | win rate vs SMART |
|---|---|
| none (the policy's own choice) | 24.5% |
| `4` (income only) / `5` (draft only) / `4,5` | 24.82% / 24.97% / 24.98% |
| `1,4` (take green builds) | **14.35%** |
| `3,4` (blue actions) | **7.30%** |
| `1,2` (both build phases) | **0.63%** |

Verdict: **the phase decision is saturated, not broken**. The policy's own choice is equivalent to "income or draft" and
any deviation costs 10-24 points — taking a build phase means giving up the income or the draft that feeds the whole
economy and the hand. The 10% win rate of the rare phase-1/2 picks in the recorded data is a selection effect (they
happen in lost positions), not a mistake. One hypothesis closed; effort goes elsewhere.

## Critic target: win or margin? (2026-11-14)

`ml/train_value.py --target margin` regresses the final score difference (margin/20, clipped) instead of the win: the score
is the game's own objective and carries *how dominant* a position was, which a binary win throws away (the "5 points all
game, then a huge economy in the last 10 rounds" player is invisible to a win label until the end).

Same 10 000 new-schema games, same split, 256-128, 4 epochs:

| target | AUC (on the win) | margin explained | play, search on, 2 000 games vs SMART |
|---|---|---|---|
| outcome (BCE) | 0.8545 | - | 30.05% |
| margin (MSE) | 0.8510 | 73% of the variance | 29.48% |
| the older vs-SMART net (760 columns, more data) | 0.856 | - | **32.3%** |

Verdict: the margin target does what it promises (it predicts the margin, not only the sign) but **does not rank candidates
better** - the play is the same within noise, and the older net still wins purely on data volume. The critic's target is
not the lever at this scale; data volume and the ranking signal are.

## Encoder v3.2: the phase-4 double payment (2026-11-14)

130 cards can pay again in phase 4, but the upgrade collects only ONE of them, so the state does not need 130 flags: it
carries the **best single double payment per dimension** (mc, plants, heat, cards, tr and the six incomes) for both
players - 22 columns, `ObservationBuilder.DOUBLE_PAYMENT_DIMENSIONS`. It is computed by actually applying `payAgain` to a
copy of the position for the few such cards a player owns, so dynamic payments (a card that pays science, or whose value
depends on steel) need no hand-written table.

Two properties are guarded by tests (`ObservationCompletenessTest`):

- the feature is the **maximum**, not the sum over the table (a numeric test with two pay-again cards);
- building an observation **never changes the game** - two cards (`CallistoPenalMines`, `AutomatedFactories`) draw a card
  inside `payAgain`, so the encoder gives them a copy of the game; without that, the encoder corrupted live positions and
  the numeric test disagreed with the encoder for exactly that reason.

Cost, measured: the observation went 52 -> 127 µs (one game copy per observation, not per card: per-card copies cost 142
µs) and a searched decision 0.9 -> 1.34 ms. The engine refuses a model trained on another observation size (no
compatibility shim); `ml/pad_model.py` converts old artifacts by appending zero columns and taking the new schema hash from
a freshly generated chunk. Verified end to end: the converted parent + converted V still play 32.3% vs SMART.

## Why the completeness audit missed rule predicates (2026-11-14)

`ObservationCompletenessTest` was a **hand-written list of facts**: money, heat, plants, incomes, TR, forests, phase
upgrades, picks, builds, hand, table cards, resources on cards, milestones, current temperature/oxygen/oceans, round.
It covered *values*. It could not cover **rule predicates** - things the engine reads to decide what is still legal:

- `planetAtTheStartOfThePhase`: `canIncreaseTemperature/Oxygen/Infrastructure` and every `onTemperatureChangedEffect`
  (Livestock!) use the planet **as it was when the phase started**, so a global that was open then can still be advanced
  after it is maxed, for the rest of the phase. That is the "hold Livestock, dump 300 MC into temperature, one animal per
  step, one VP each" plan - and the state could not tell "maxed and closed" from "maxed but this phase still pays".
- `isBuiltSpecialDesignLastTurn`: a card built with a special design ignores global requirements next turn.
- The ocean deck: nine fixed reward sets shuffled over nine slots. `Planet.revealOcean()` returns the **last opened**
  ocean once all nine are open, so every further purchase in that phase pays *that* reward (a 13 MC ocean that pays back
  4 MC is a 9 MC ocean, repeatedly). Only the count of oceans was encoded, not the last bonus.

The mechanical replacement for hand enumeration is a static coverage check: every accessor the rule code reads
(`states/`, `validation/`, `processors/`, `TerraformingService`, `DiscountService`, `CardValidationService`, …) versus every
accessor the encoder reads. 121 rule accessors, 76 encoder accessors, 71 in the gap. Most are plumbing (`getId`,
`getNextTurn`, `getPlayerUuid`), some are enforced upstream (card requirements are filtered before the decision), but it
lists exactly the class of thing the old audit could not see.

Added to the observation (encoder v3.3, 795 columns):

| feature | size | why |
|---|---|---|
| `planet:open_at_phase_start_{temperature,oxygen,infrastructure,oceans}` | 4 | the engine's own "can still advance this in the phase" |
| `player:{my,opponent}_special_design_last_turn` | 2 | requirements are ignored this turn |
| `oceans:last_opened_{cards,mc,plants}` | 3 | what every further ocean purchase pays once all nine are open |
| `oceans:collected_{cards,mc,plants}/40`, `oceans:revealed/9` | 4 | what has been taken, and whether the last one is open |

Tests: six in `ObservationCompletenessTest`, including the maximum-not-sum double payment, "building an observation never
changes the game", and the three ocean states the owner asked to distinguish explicitly - *last ocean not open* / *open
and still paying this phase* / *open and closed*.

## Does a better critic still buy strength? (2026-11-14, encoder v3.3)

| critic | data | AUC | play, search on, 2 000 games vs SMART |
|---|---|---|---|
| 760-column net (converted), the old best | night 8, fewer games | 0.856 | 31.4% (1 000 games) |
| 782-column net, before the ocean/phase features | 30 000 games | 0.8704 | 31.4% |
| **795-column net, with the rule features** | 30 000 games | **0.8793** | **31.4%** |

Three different critics, three AUCs, identical play. The extra accuracy does not convert: at ~0.87-0.88 AUC the re-ranking
call has reached its plateau (the full gain over the policy is ~+7 points and it was already there). Consequences:

1. Critic-improvement-for-its-own-sake is no longer a lever. The value net stays as the search leaf and as the critic of
   the actor, but chasing the last AUC points has no measurable payoff.
2. The remaining 30+ points against NETWORK are in the **policy's play**, not in the ranking of a menu: the search decides
   *which* of the offered cards to take, and the offered menu is produced by the policy's own economy.
3. So the next work is policy-side: distillation of the search ranking over all candidates (policy improvement without
   game-outcome noise), then the actor-critic loop on real trajectories.

Cost of this round: 30 000 games streamed at 33 games/s (15 min), critic trained in 15 min (1 epoch; epoch 2 overfits),
observation 106 us.

## Distilling the search ranking into the policy: tested, no gain (2026-11-14)

Pipeline built for this: `DecisionLogs` now writes a `RANKING.<chunk>.jsonl` sidecar with the search's value for **every
candidate** of every BUILD_CARD / DRAFT_CARDS decision (the handoff is `SearchScores`, a thread-local pair so no decision
type had to change), and `ml/distill.py` joins those lines to the recorded rows by (game, step), fine-tunes one head with
KL against the soft teacher distribution and exports a full model bundle.

Data: 15 000 games with the search on, 676 339 ranked decisions, 170 549 BUILD_CARD rows with a ranking.

| policy | search off, 2 000 games vs SMART |
|---|---|
| parent (padded to the current encoder) | 24.05% [22.2, 25.9] |
| parent + distilled BUILD_CARD head | 23.15% [21.4, 25.0] |

Distillation quality was not the problem: on held-out games the distilled head agrees with the teacher's top choice 71% of
the time and with its top-3 96%. The policy still does not get stronger.

Why, most likely: the search's edge is **local and narrow** (it fixes ~46% of build choices, worth ~+7 points *at play
time*), the other 29% of disagreements may be noise, and one head of fourteen changes nothing structural about the play -
the economy, the phase rhythm and the deck are decided by many heads together. This matches the two earlier nights: the
search's value is a *deployment-time compute* value, not a transferable policy signal at this critic quality.

Consequence for the plan: policy-side improvement has to come from an objective that changes behaviour **globally** (an
actor-critic loop on real trajectories and their outcome), not from imitating a narrow re-ranking. The search stays at
deployment (it is worth +7 there) and as the critic's consumer.

## Root cause of the training drift: the objective never compared candidates (2026-11-14, evening)

Every retrain in this project came out **worse than its parent** - night 8, the search night, and a fresh 30 000-game run
(24.05% -> 18.5% vs SMART, 84.8% -> 75% vs RANDOM, with the same warm start and only more games). The data was suspected;
it was not the data.

`train_all.py` builds its pool as `cand = rows["feat"][row, chosen]` - the features of the **taken** candidate only - and
trains `BCE(logit(chosen candidate), game outcome)`. So the head learns "how likely is a win if I take this card", for the
one card the old policy happened to take. The other candidates of the same decision are **never scored during training**,
although at play time the agent compares exactly those logits (`softmax` over the candidate set). Consequences:

- no training term can ever prefer candidate A over candidate B in the same state: the loss is a single scalar per
  decision, attached to the action the old policy chose. Its fixed point is "reproduce the old policy's choices", which is
  why more games only produced a more confident average;
- the logits of never-trained candidates are arbitrary, so the argmax at play time can pick noise. That is also why a
  proper **state** value function (V) re-ranking the same menu is worth +7 points while retraining the policy loses points.

Fix: `ml/train_policy_ce.py` trains the policy the way it is used - cross-entropy over the **candidate set** with the chosen
index as the label (masked padding, one head per decision type, shared observation projection, warm start from the parent
with zero-padded columns for the grown encoder, export in the existing bundle format). Outcome weighting is available but
off by default; the ranking itself is the signal.

## The fix works: candidate cross-entropy beats the parent (2026-11-14, evening)

`ml/train_policy_ce.py`: cross-entropy over the candidate set (label = the chosen index), one head per decision type,
shared observation projection, warm start from the parent with the new encoder columns zero-padded, export in the existing
bundle format. Data: 10 000 games of the 30 000-game set, 8 rows per game and type, 2 epochs, ~3 minutes on the GPU.

| player | search | vs SMART | vs RANDOM |
|---|---|---|---|
| parent (night 7 it46) | off | 24.05% | 84.77% |
| parent | build + draft re-ranking | 31.4% | 88.9% |
| **candidate-CE policy** | off | **29.65%** | **89.7%** |
| **candidate-CE policy** | build + draft re-ranking | **35.75% [33.7, 37.9]** | **92.3% [91.1, 93.4]** |

First training run in the whole project that improves on its parent, and the two mechanisms **add up**: +5.6 points from
learning the ranking objective, +6.1 more from searching on top of it.

Why it was possible: the old objective scored only the *taken* candidate against the game outcome, so no term in the loss
could prefer candidate A over candidate B in the same state; the fixed point was "reproduce the old policy", and the
never-trained candidates' logits were arbitrary at argmax time. Cross-entropy over the whole menu trains exactly the
function the engine uses (softmax over candidates): validation top-1 agreement with the recorded choice is 84% on
BUILD_CARD, 78% on DRAFT_CARDS, 96% on PICK_PHASE - the heads can now discriminate.

Next: iterate the loop (data from this player -> CE training -> evaluation), and keep the critic/search untouched so the
two effects stay separable.

## The loop compounds (2026-11-14, evening)

Iteration 1 trained on 10 000 games of the night-7 policy's play; iteration 2 generated 15 000 fresh games with the
iteration-1 player (policy + search), warmed-started from it and trained the same way (2 epochs, ~4 min).

| player | search | vs SMART | vs RANDOM | vs NETWORK | points vs NETWORK |
|---|---|---|---|---|---|
| parent (night 7 it46) | off | 24.05% | 84.77% | 7.5% (200 g) | 68.4 |
| parent + search | on | 31.4% | 88.9% | - | - |
| CE iteration 1 | off | 29.65% | 89.7% | - | - |
| CE iteration 1 | on | 35.75% | 92.3% | - | - |
| **CE iteration 2** | off | **34.5%** | **91.1%** | - | - |
| **CE iteration 2** | on | **39.4%** | **93.4%** | **10.75%** | **73.7** |

For the first time the training loop moves **up**, and the two mechanisms stay additive: the policy learns the menu (the
objective it is actually used with), the search re-ranks what the policy offers, and better play produces better data for
the next iteration. Top-1 agreement with the generating player's choice rose with the iterations too (SELL_CARDS_LAST_ROUND
0.87, STANDARD_ACTION 0.97, UNMI_RT 0.99).

Ready to launch as a night: `ml/ce_config.json` (trainer `train_policy_ce.py`, search on for data generation and for the
evaluation, 12 000 games per iteration, SMART:RANDOM 0.7:0.3):

    py -3 ml/nightly.py run --config ml/ce_config.json --name ce9 --start-from D:/ares/runs/policy/r4_ce2

## Acceptance profile: cards per game and phases picked (2026-11-14, owner's rule)

Win rate alone hid a degenerate player: `AGENT_V2` (r4_ce2) beat RANDOM 95% while the owner, playing it by hand, saw a
player that "picks phase 3 and buys standard projects, 0-2 cards per game". So every new network is now accepted on two
behavioural numbers first, and `ml/check_player.py` measures them automatically (it plays the games itself and prints a
verdict, exit code 1 on a degenerate profile):

| metric | r4_ce2 (no search) | what it means |
|---|---|---|
| cards built per game | **11.6** | healthy: it does build |
| phase picks | **4: 49%, 5: 51%** | only income and draft - never 1/2/3 |
| phase-3 decisions | pass 18%, standard projects **40%**, heat/plants 42% | it converts money into TR instead of keeping it for cards |
| win rate | 34.5% SMART / 94.8% RANDOM | |

Forcing other phases on this policy costs 15-33 points (`1,4` 19.9%, `3,4` 10.5%, `1,2` 1.2% against 34.5%), so the phase
mix is a **local optimum** of the current policy, not a free choice: it builds 11.6 cards only because the opponent or the
dummy calls the build phases. Making it a player that calls 1/2 itself is a *training* problem (what the data teaches
about those phases), not something an override can fix.

Phase-3 spends were also given to the search (full mode, `STANDARD_ACTION`), after fixing a dispatcher bug that made every
project unscorable ("1.0 candidates each", 99.6% stop): with the fix V compares pass against buying — and it **buys more**
than the policy (9.4% stop) and loses strength: 26.2% vs 34.5%. V is short-sighted here: a standard project raises TR and
oceans immediately, which it likes, while the money it costs is worth more later. So the standard-action search stays off;
the fix is kept (the dispatcher no longer swallows the score), the option stays as a documented negative result.

### Iteration 3 and the profile that does not move (2026-11-14, late)

| player | plain SMART | plain RANDOM | +search SMART | +search RANDOM |
|---|---|---|---|---|
| parent (night 7) | 24.05% | 84.77% | 31.4% | 88.9% |
| CE it1 | 29.65% | 89.7% | 35.75% | 92.3% |
| CE it2 | 34.5% | 91.1% | 39.4% | 93.4% |
| **CE it3** | **36.4%** | **92.2%** | 37.7% | 92.9% |

Strength climbs, the **style does not**: `ml/check_player.py` on it3 gives phases 4: 49% / 5: 51%, 11.3 cards per game,
phase-3 decisions: pass 17.5%, standard projects 39.6% - the same profile as it1 and it2. That is the loop reinforcing its
own behaviour: the data is generated by the player it trains.

Three obvious ways to change the style were measured and rejected: forcing phase picks (-15..-33 points), giving the
phase-3 spend decision to `V` (-8 points, and V *likes* spending because a standard project raises TR immediately), and
distilling the search ranking into a head (-1 point). What is left is not another menu-level trick: the player has to learn
a different *economy* (cards before TR), which is the actor-critic direction rather than imitation.

### Full rollouts for the phase-3 decision: rejected (2026-11-14, late)

The idea was to answer the decision V gets wrong ("pass or buy a standard project") by playing the rest of the game out
and using the real result instead of V. Machinery: the fair determinization already used by the horizon search, the
engine's own `processSimulation`, and `-Dares.agent.search.horizon=999` = play to the end.

| phase-3 policy | stop share | vs SMART (1 000 games) | cost |
|---|---|---|---|
| policy alone | 18% | 36.4% | - |
| search on V (full mode) | 9% | 26.2% | 1.3 ms/decision |
| full rollouts, 1 determinization | 45% | **19.1%** | 61 ms/decision |

Rejected. A single determinized playout is dominated by which hidden cards it deals, not by the difference between two
actions; and this player's economy *is* built on converting heat/plants/TR, so "pass more" costs it points. Averaging
several determinizations would cost 3-5x more for a comparison that the data says is the wrong direction for this player.

## CE night (`ce9`, 2026-11-14, 9 iterations): plateaus after one iteration

`ml/ce_config.json` (candidate cross-entropy, 12 000 games per iteration generated by the best model with the 1-ply search
on, SMART:RANDOM 0.7:0.3), started from `r5_ce3`:

| iteration | vs RANDOM | vs SMART | gain |
|---|---|---|---|
| start model (r5_ce3) | 91.9% | 36.6% | +0.643 |
| **it1** | **92.3%** | **37.8%** | **+0.647** (promoted) |
| it2 .. it9 | 91.1-92.2% | 33.7-35.0% | +0.62 .. +0.63 |

Plateaued at it1 (8 iterations without a better score), benchmark vs NETWORK: **9.0% [4.8, 16.2]** over 100 games.
Profile of it1 (owner's metrics): 11.3 cards per game, phases 4: 49% / 5: 51%, phase-3 pass 17.5% / standard projects 40%.
So the CE loop reaches its ceiling in **one** iteration and then only fluctuates around it - exactly the fixed point of
imitation: the data is generated by the player being trained.

One PPO step on that data (`ml/train_policy_ppo.py`: clipped ratio against the generating model, advantage `outcome - V(s)`,
entropy 0.01): 36.2% / 92.6%, profile unchanged (11.4 cards, phases 4/5, pass 17.7%). A single step is not the mechanism -
it needs many iterations, which is what `ml/ppo_config.json` now runs at night.

## Where the points go, and why the style does not move (2026-11-15)

`ml/score_breakdown.py` reads the last observation of every recorded game (both tables are in it) and prints the mean
score components. On the CE player's games against SMART:

| feature | agent | opponent | gap |
|---|---|---|---|
| win points | 0.44 | 0.42 | +0.02 |
| forests | 0.265 | 0.134 | +0.13 |
| mc income | 0.387 | 0.321 | +0.07 |
| cards played | 0.202 | 0.410 | **-0.21** |
| **blue cards** | **0.068** | **0.358** | **-0.29** |

The agent is not behind on terraforming points or income - it is behind on **cards, above all blue cards**: almost no
phase-3 engine. That is exactly the style the owner saw by hand, now measured.

Three attempts to break the style, all measured, all neutral-to-worse:

| variant | vs SMART | cards/game | phases | phase-3 spend |
|---|---|---|---|---|
| CE on all rows (it1, best) | 37.8% | 11.3 | 4/5 only | pass 17.5%, projects 40% |
| CE on WON games only (`--min-outcome 0.5`) | 34.1% | 11.1 | 4/5 only | pass 17.4%, projects 40.6% |
| PPO one step on the night's data | 36.2% | 11.4 | 4/5 only | pass 17.7%, projects 40.5% |
| PPO night (in progress) | 37.7% (it1) / 36.0% (it2) | - | - | - |

Every mechanism converges to the same behaviour, because every mechanism trains on games played by that same greedy
player. The data has no alternative lines with outcomes, so nothing in it can say "build the blue engine instead". The one
lever not yet pulled is **exploration while generating data** (temperature / entropy in self-play) plus many more RL
iterations - which is what the PPO night is for; if it plateaus as well, the next step is not another loss function but an
exploration policy and a much larger data/training budget.

## PPO night (`ppo1`, 2026-11-15, 24 iterations): +2 points, and the profile finally twitches

`ml/ppo_config.json` (clipped ratio against the generating model, advantage `outcome - V(s)`, entropy 0.01, lr 1e-4,
12 000 games per iteration, search on for data and eval), started from the CE night's best:

| player | vs RANDOM | vs SMART |
|---|---|---|
| CE night it1 (start) | 92.3% | 37.8% |
| **PPO it12 (best, plateau after 12)** | **93.2%** | **39.9%** |
| PPO it24 (last) | 92.9% | 37.3% |

So "many small clipped steps" does beat "one big CE step" - by ~2 points, and it stays at that level for a dozen
iterations instead of collapsing back. Benchmark vs NETWORK: 6.0% over 100 games (the CE night measured 9.0% over 100; with
100 games the interval is 3-13%, so they are not distinguishable - the honest statement is "roughly 6-9% vs NETWORK").

Profile of the PPO best (owner's metrics): **12.0 cards per game** (11.3 before), phases still 4/5 only, phase-3 pass
**21%** (17.5% before). The first movement of the style in the whole project, small but real.

Next launched from it12: `ml/ppo_explore_config.json` - the data-generating player samples at temperature 0.3, so the data
contains lines the greedy player would never play together with their outcomes, and the PPO ratio is computed against that
behaviour policy (`--behaviour-temperature 0.3`). If exploration can move the style (toward blue cards and phase 1/2),
this is where it has to happen.

## Two-ply build search (pairs), 2026-11-15: +1.3 points, cheap

The 1-ply search scores a card by the position right after building it. A card whose value is the chain it enables (one
microbe that unlocks the next card, a production card that pays for the next one) is badly served by that. So for the two
best cards by the single score, the search also scores "A then the best other card B" on the same copy and keeps the
better of the two readings (`-Dares.agent.search.pairs=true`). Every candidate is still compared through one quantity -
the value of the position after the builds.

10 000 games against SMART, PPO it12 + search:

| variant | vs SMART | cost |
|---|---|---|
| one-ply (build + draft) | 37.21% [36.26, 38.14] | 1.35 ms/decision |
| **one-ply + pairs** | **38.46% [37.51, 39.41]** | 1.50 ms/decision |

+1.26 points, z ≈ 1.8 (p ≈ 0.07) - small but positive at a cost that is invisible in play. Kept on. Measured on 2 000
games the difference is inside the noise (36.2% vs 37.1%), which is a useful reminder: the 3 000-game evaluation of the
nightly pipeline (±1.6 points) cannot resolve effects of this size.

## State of the player, 2026-11-15 morning: the ceiling of this setup

Four training runs in one night, all measured the same way (search on for data and for evaluation, 12 000 games per
iteration, plateau guard on):

| run | best iteration | vs RANDOM | vs SMART | vs NETWORK (100 g) |
|---|---|---|---|---|
| `ce9` candidate cross-entropy | it1 | 92.3% | 37.8% | 9.0% |
| `ppo1` PPO | it12 | 93.2% | 39.9% | 6.0% |
| `ppo2` PPO + exploration (temperature 0.3) | it1 | 93.2% | 39.8% | 9.0% |
| `ppo3` PPO + pairs in the data | it6 | 93.8% | 39.4% | 8.0% |

**They all land in the same place: ~37-40% against SMART, ~93% against RANDOM, ~8% against NETWORK.** On 10 000 games the
two best models are indistinguishable (37.46% and 37.28%), and even 10 000 games carry about ±1 point of seed-to-seed
noise, so the honest number for the best player is **37.5% ± 1 vs SMART** and it has not moved since PPO's first night.

What did move, and is kept:

- the **objective**: candidate cross-entropy instead of "will I win if I take this card" (24.05% -> 36.4% over three
  iterations, the single largest gain of the project);
- **PPO** on top of it: +2 points once, and it holds them for a dozen iterations instead of collapsing;
- the **1-ply symmetric search at play time**: +2-3 points on any policy, plus **+1.3** from two-ply pairs.

What does not move: the **style**. Twelve cards per game, phases 4/5 only, ~20% pass in phase 3, ~40% standard projects -
measured on every candidate (`ml/check_player.py`), and the score breakdown says the deficit against SMART is cards
(0.20 against 0.41) and above all **blue cards** (0.07 against 0.36). Every training variant converges to the same
behaviour because every variant trains on games of the same greedy player; exploration at 12 000 games per iteration is
too little to change that.

So the remaining work is not another loss function or another night of the same loop. It is one of:
1. a **better critic** (V trained on afterstates - the states the search actually asks about - instead of on decision
   states), which raises the ceiling of the only mechanism that has ever beaten a policy: the search;
2. a **much larger scale** (more games per iteration, more iterations, bigger nets) with the objective that already works;
3. an **architecture** that can plan (the current policy is memoryless: fourteen independent heads over one state).

## Discovering behaviour: exploration, and the phase decision made by search (2026-11-15)

Owner's rule, now binding: **SMART and NETWORK are benchmarks only** - never a teacher, an opponent in training or a data
source. The cloning attempt (a recorder that labelled SMART's turns as our decisions) was removed; `docs/DISCOVERY.md`
holds the reasoning and the loop that replaces it.

### Why the style is a fixed point, restated with data

Training on games of one greedy player cannot contain a line that player would not play, so neither the policy nor the
critic can learn that such a line is better. `-Dares.agent.explore=TYPE:q` makes the **data generator** draw that decision
uniformly at random with probability q; everything else is played by the current best player. 4 000 mirror games with
`PICK_PHASE 0.5, BUILD_CARD 0.35, STANDARD_ACTION 0.35, PERFORM_BLUE_ACTION 0.25` (`D:/ares/data/explore1`):

| phases picked | before | with exploration |
|---|---|---|
| 1 / 2 / 3 | 0 / 0 / 0 | 11% / 11% / 11% |
| 4 / 5 | 49% / 51% | 33% / 33% |

`ml/explore_stats.py` relates the final margin of a seat in those mirror games to *everything* the observation says about
how it played (8 000 seats, both seats are us, so the only difference is the exploration). Margin correlation, 95% half
width +-0.02:

| dimension | corr | dimension | corr |
|---|---|---|---|
| mc income | **+0.40** | cards played | +0.17 |
| animals on cards | +0.26 | green cards | +0.16 |
| forests | +0.25 | blue cards | +0.11 |
| plants income | +0.18 | steel income | -0.02 |
| heat income | +0.18 | titanium income | -0.08 |

Calling phase 3 with *random spends* is -0.05: the phase itself pays nothing, the engine behind it does. That is a
measurement, not an opinion, and it is the same conclusion the owner states from his own play: all cards are good when an
engine pays for them with discounts, so the target is the engine, not one colour.

### The phase decision, decided by search (new capability)

The agent never called phases 1/2/3 because the phase pick is pure policy - the search only re-ranked BUILD_CARD and
DRAFT_CARDS. It now scores PICK_PHASE too (`-Dares.agent.search.fullTypes=PICK_PHASE`):

* the afterstate is built by **the engine's own code**: `PickPhaseProcessor.processTurn` (round results, chosen/previous
  phase, the phase-3 double-repeat upgrade, Communications Streamlining and Nebu Labs MC, crysis) followed by
  `StateTransitionService.performStateTransferFromPhase(game, 0)`, exactly what `PickPhaseState` calls - so the build
  slots, income, draft, revealed cards and blue activations cannot drift away from the rules;
* the opponent's simultaneous pick is unknown, so it is drawn from that player's legal phases with the decision's seed:
  one draw per decision, the same for every candidate (common random numbers);
* V of the starting position of that phase is the score, so the pick is a one-step lookahead over strategies.

Measured on 300 games vs SMART, policy from `ppo1/it_0012`:

| phase critic | phases 1 / 2 / 3 / 4 / 5 | vs SMART | vs RANDOM |
|---|---|---|---|
| none (policy only) | 0 / 0 / 0 / 49 / 51 | 37.5% | 93% |
| `v_r3_outcome` | 6 / 18 / 51 / 12 / 12 | **0.3%** | 13% |
| `v_explore1_margin` (exploration + greedy data) | 2 / 7 / 6 / 48 / 37 | 32.8% | 87% |

So the strategic decision is now searchable, and the blocker is exactly the critic: both nets were trained on *greedy*
games, where the phase a player picked is not independent of the state it picked in (a player calls phase 3 when it has
actions to take), so V(start of phase 3) mixes "good positions that called phase 3" with "phase 3 is good". The critic
must be fit to the phase-start distribution on data where the pick is exogenous - which is what
`-Dares.agent.explore=PICK_PHASE:1.0` produces (6 000 mirror games, `D:/ares/data/explore_phase`). That critic
(`v_phase1_margin`), and a bounded rollout (play the round out, then read V) as a fallback, are the next two measurements.

### Rejected on the way

| attempt | result |
|---|---|
| exact-corrected REINFORCE on the phase head (uniform exploration, 36 000 rows, `train_policy_ce.py --objective pg`) | phase mix unchanged (4/5 only), val loss diverges: the outcome signal of a decision taken 12 times per game is far below the noise |
| one critic for both card ranking and the phase pick | the two afterstate distributions are different questions; one net answers a mixture of both |

## Where the points go, in points (2026-11-15): the card term

`--mode scorecard` (new, `ml/../selfplay/Scorecard`) plays AGENT against SMART and reports the terms the engine actually
scores. 400 games, best policy + search:

| term | agent | SMART | difference |
|---|---|---|---|
| terraforming rating | 37.8 | 31.2 | **+6.6** |
| forests | 13.9 | 8.9 | **+5.0** |
| printed points of played cards | 3.5 | 12.8 | **-9.3** |
| points from resources on cards | 4.6 | 6.0 | -1.3 |
| milestones | 3.1 | 5.0 | -1.9 |
| awards | 8.7 | 12.5 | **-3.9** |
| **total** | **71.5** | **76.4** | **-4.9** |

The player is not behind on terraforming or forests - it is ahead by 11.6 there - and loses the game on **card points**
(and the milestones and awards that a stronger card table collects by itself). So the target is not "build more cards":
`-Dares.agent.search.mustBuild=true` raises builds from 12.2 to 18.1 per game and costs **4.7 points of win rate**
(33.5% vs 38.2% on 2 000 games) - it converts credits into cards the player cannot use. The target is **which** cards,
i.e. their points and the engine they form.

Two encoder gaps were found from this and closed (encoder 795 -> 805, candidate width +1 for every card decision):

1. the observation had one number, `win_points`, for Tr + cards + milestones + awards together, so a value function
   could not tell a 3-point card from a terraforming point; the five terms are now written separately for both players;
2. the card candidates did not say how many points the card is worth (signature, colour, tags, price, active only); the
   printed points are now a candidate feature, so the build decision and its search can prefer a 3-point card.

## The critic cannot see a card's points either (2026-11-15)

`ml/value_sensitivity.py` perturbs one observation feature at a time in 6 218 real states and reads dV, so "does the
critic care?" is measured instead of assumed. For the value net that drives the search:

| perturbation | dV | per point |
|---|---|---|
| +10 milestone points | +0.194 | 0.019 |
| +10 resource points | +0.096 | 0.010 |
| +10 terraforming rating | +0.051 | 0.005 |
| **+3 printed card points** | **+0.008** | **0.003** |
| +0.5 (normalised) mc income | +0.185 | - |

A card worth 3 points moves the critic by 0.008 - about 15% of what ten milestone points move it, i.e. the critic is
blind to the very term the player loses the game on. Retraining the same net on the final **margin** instead of the win
did not help (+0.003 for three points): the marginal value of three points in the middle of a game is far below the
error of a learned value function, whatever the target. That is why the search - which ranks by V - never preferred
high-point cards, and it is a *measurement*, not a hypothesis.

Consequence for the objective: a card's points cannot be reached through V. They have to be reached through the head that
chooses the card, trained with an advantage measured **in points** (`train_policy_ce.py --adv-target margin`: the final
margin/20 minus the critic's own prediction, which is on the same scale) on games where the taken cards differ
(exploration data), with the card's printed points now among the candidate features. That is experiment `r11_cards`.

## SMART plays 27 cards a game; we play 13 (2026-11-15)

`--mode scorecard` now also reports the cards each side puts on the table (the played deck of each player, per game) and
the printed points they carry. 300 games, our best policy + search:

| | agent | SMART |
|---|---|---|
| cards on the table per game | **13.2** | **27.4** |
| printed points per game | 3.2 | 13.0 |
| printed points per card | 0.24 | 0.48 |

So the card term of the deficit is not "one or two good cards": the benchmark plays **twice as many cards**, and each of
them is worth twice as much. 27 cards in ~11 rounds is ~2.4 builds per round, which only the build phases (1, 2 and the
phase-3 leftovers) can supply - our player, calling phases 4 and 5 almost every round, has about one build slot per
round. That closes the circle with the owner's own description: an engine pays for the cards with discounts, and then
almost every card is worth building.

This also explains why forcing builds (`mustBuild`, 18 per game) lost: without the engine the extra cards are paid at
full price. The chain is engine -> discounts/income -> many cheap cards -> points, and neither half of it is visible to a
one-ply search whose critic prices a card's printed points at 0.008 (see the sensitivity table above).

## A better critic did not move the play (2026-11-15)

Coverage of many-card tables was added to the critic on purpose: 6 000 mirror games generated with `mustBuild` and
`preferPhase=1,2,5` (18-27 cards on the table, phases 1/2/5), then V trained on those plus the 12 000 normal games.

| critic | AUC | dV for +3 card points | cards per game | printed points | vs SMART |
|---|---|---|---|---|---|
| `v_r10_outcome` (normal games) | 0.887 | +0.008 | 13.2 | 3.2 | 37.2% (400 g) |
| `v_r12_outcome` (+ build-heavy games) | **0.905** | +0.010 | 13.4 | 3.3 | 32.3% (300 g) |

The critic became measurably better at predicting the winner and **not** measurably better at pricing a card, and the
player did not change: the fixed point is not the critic's accuracy, it is that three points are far below the resolution
of a value function fitted to game outcomes. Written down so nobody spends another night on critic accuracy alone.

## Afterstates, and two rules that fixed real pathologies (2026-11-15)

### The critic was asked about states it never saw - and training it on them does not help

The one-ply search reads V on the state right after a candidate is applied, while the recorded data only contained states
in which decisions are made. `DecisionType.AFTERSTATE` now writes that state too (one observation per agent action, same
game result, not capped like decisions). 6 000 games gave 384 000 afterstate rows, and the critic trained on decisions +
afterstates was measured on 2 000 games against SMART:

| critic | dV per point of `winning_points` | vs SMART |
|---|---|---|
| `v_r15_outcome` (decision states) | 0.0029 | **36.5%** [34.4, 38.5] |
| `v_r17_after` (decisions + afterstates) | 0.0029 | 32.2% [30.2, 34.3] |
| `v_r17_after_margin` (margin target, same data) | - | 29.4% [27.5, 31.7] |

So the distribution mismatch was real, but mixing the two distributions costs ~4 points of win rate: the afterstate rows
are near-duplicates of the next decision row (same game, same label), they dominate the stream, and the critic becomes
worse where decisions are made. Rejected; the stream stays behind `-Dares.agent.afterstates=true` for future experiments
(e.g. a *separate* critic for the search, trained only on afterstates).

### Two rules from the obvious-move bench (both arithmetic, not strategy)

1. **Free blue action** (`services/agent/GuaranteedActions.freeBlueAction`): an action that spends no credit, heat or plant,
   gives up no card and leaves at least one card or resource more is pressed without asking the network, once per phase
   (the extra activation a phase upgrade grants stays a real decision). The bench showed the policy refused a free
   two-card draw (`AiCentral`) and that letting the value search decide the whole blue menu costs 13 points.
2. **Dominated standard project**: a standard project that buys a global parameter for more credits than a card in hand
   buys the same parameter for is removed from the phase-3 menu (owner's arithmetic: `Deimos Down` gives three oceans for
   ~26 credits against 45 for three standard ones). This is the one cross-menu comparison our architecture can make
   cheaply, and it is the S6 scenario of the bench.

Both rules were measured: sanity 6/6 with no measurable cost (35.9% [33.8, 37.9] against 36.5% [34.4, 38.5]).

### Pipeline fixes that came out of it

* the policy trainers took the FIRST N chunk files; with a wide iteration that is "the first N thousand games", not a
  sample - chunks are now sampled (seeded) across the iteration;
* `--mode sanity` is a gate in the nightly: a model that fails an obvious-move scenario cannot be promoted whatever its
  win rate;
* `DecisionLogs` does not cap afterstates with the per-type decision cap (64/game instead of 8);
* `WinPointsService` no longer throws on a played resource card with no counter entry (the score is read on every
  decision, so this was a crash waiting to happen);
* the double-payment feature measures one card on a fresh copy per card; a shared copy let one card's `payAgain` change
  the payout another card was measured on.
