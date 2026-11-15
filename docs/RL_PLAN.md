# Learning architecture — the honest assessment (2026-11-14)

The mission, restated because it is the thing that decides every choice below: **the strongest possible fair player, built without
learning from NETWORK** (it may only be benchmarked), with full freedom over architecture, objective, representation and
search. Not "a better copy of SMART". Not architecture for its own sake.

This document exists because the project drifted: the recent work was increasingly a set of hand-written rules (phase
priority, "always build", per-card value tables). Those are what SMART already is. They measure hypotheses; they must not
become the player.

## What the evidence says (all of it is measured, in `docs/EXPERIMENTS.md`)

| approach | result | what it proves |
|---|---|---|
| supervise the policy on its own games, weight by the binary outcome | **degrades**, every iteration, 3 independent nights | the objective's fixed point is the *mean of the generating behaviour*, not a stronger player |
| advantage weighting (`outcome − V(s)`) | less damage, still never beats the parent | the returned credit is one scalar per game; it cannot rank ~500 decisions inside the game |
| `V(s)` on the same data | AUC 0.86, and its **relative** card ranking is worth +6.9 points at deploy | the state *does* determine the result; the model is usable — as a **ranking**, not as a level |
| `V` audited with controlled perturbations | economy monotone and round-aware, but *"metal income is bad"* — faithfully | `V` learned the generating policy's habits, including the wrong ones |
| 1-ply rollouts with a `V` leaf | no gain over the immediate afterstate | a leaf is only as good as the value it uses |
| phase choice forced to build / income / draft | build phases catastrophic for this policy | a rule cannot repair an economy that cannot afford the rule |

Consequences, stated plainly:

1. **Supervised imitation of a self-play generator is a dead end here.** It cannot exceed its generator, and its fixed
   point is the generator's average behaviour. Every night of compute spent on it is spent going sideways.
2. **`V` is not the problem in absolute terms, the target is.** `V` is trained on "did this policy win", so it encodes the
   policy's habits. Any method that maximizes `V` at inference (search, RL with `V` as a reward) inherits them.
3. **The game's own score is the objective.** The player who wins is the one with more points, and the point differential
   is available every round from the engine. It is the one signal in this project that does not come from a policy, a
   heuristic or NETWORK.

## The architecture I recommend

**Approximate policy iteration with a value critic trained on the terminal outcome.** The dense signal is the *learned
value*, never the score, because in this game the score is not monotone with winning — measured on one night (27 261
positions at round ~10, base win rate 25.8%):

| grouping at round 10 | low | mid | high |
|---|---|---|---|
| victory points | 21.9% | 24.5% | 33.0% |
| income (MC+cards+plants+heat) | **14.8%** | 26.5% | **36.2%** |

| state at round 10 | win rate |
|---|---|
| few points + strong economy | **31.0%** |
| many points + weak economy | **16.6%** |

So a reward of "victory points gained this round" would systematically punish the patient engine that wins the game. **The
score may only enter as the terminal result**, and everything dense must come from a value function trained on that
terminal result — which is exactly how a value net "understands tempo": +1 card income is worth 8× a victory point to our
current `V` (+0.25 vs +0.033 logit in the audit), because that is what the outcomes say.

- **Return.** Terminal only: the win (or the final margin, which is the same quantity at the end of the game). No
  per-round points, no shaping constants.
- **Critic.** `V(s)` trained on that terminal return, on **full trajectories** of the current policy, with the margin as
  a continuous target (more gradient than a binary label, no early-game point signal), evaluated by AUC and calibration
  by game. The critic is the ceiling of everything else and is improved deliberately: more rows (no 8-per-game cap),
  wider nets (the GPU is idle today), fresh on-policy data every iteration.
- **Policy improvement.** The 1-ply symmetric search *is* the improvement operator: for every decision, score every
  candidate with the critic, turn the scores into a soft distribution, and train the policy to match it — **all
  candidates, every row**, not the single action taken. This is value-based policy iteration with zero outcome noise in
  the target (the +6.9 points at inference is the measured lower bound of what that ranking contains).
- **Exploration.** Temperature/entropy while generating self-play, greedy at evaluation, so the state distribution of the
  data does not freeze around the current habits.
- **Deployment.** Policy + the same search (policy plus search is strictly better than the policy, measured).

### Why this is not the AWR that failed

The failed experiment weighted *behaviour cloning* of the taken action by `outcome − V(s)`: one scalar per game, applied
to ~500 independent rows, with the same action target the policy already chose. Here the target is the critic's ranking
**within one state**, so the signal is a vector over candidates, it carries no game-outcome noise, and it can point at a
card the policy never picks. The critic is the only thing trained on outcomes, and it is trained as a predictor, not as a
weight on the policy's gradient.

### Why this is not another heuristic

The rule set is empty. The only knowledge in it is "more points than the opponent wins" (the rules) and "prefer what the
value of the position prefers" (the definition of policy iteration). If the behaviour NETWORK discovered — draft, build,
compound, call income rarely — is truly strong, this loop has to *discover* it; encoding it would be building SMART.

### Artefacts: no compatibility shims

The current player (night 7 it46 + search) is the strongest thing we have — 84.8% vs RANDOM, 24.5% vs SMART, 31.4% with
search — and a fresh model trained for half an hour on RANDOM is nowhere near it (the night 2/3 trajectory needed ~1.5-2
hours of iterations to reach 46-60% vs RANDOM). Its preference for income and draft is *not* a defect at its own level:
forcing any other phase costs 10-24 points (measured). So it stays as the benchmark and the warm start.

But it buys no code complexity: there are **no compatibility shims**. A model belongs to the encoder it was trained on and
the loaders refuse anything else loudly (`the model was trained on … the current encoder produces …`). If the encoder
changes, the models are retrained and the old ones stay on disk as artefacts with their recorded numbers. The encoder was
re-verified rather than extended: blue cards are already individually encoded for both players (82 flags each), and the
"pays again" greens do not need 130 global flags — the one place that needs to know which production a card re-pays is the
phase-4 replay decision itself, so the information belongs in that decision's candidate features.

### Why it is feasible here

Self-play is cheap (200 games/s at concurrency 16), the encoder is audited, the GPU does the learner, and the evaluation
harness resolves ±1 point at 10 000 games. The bottleneck is the learner and the critic, not the game.

### What would falsify it

1. Held-out AUC of the critic does not improve as it gets more rows and wider nets → the representation or the labels are
   the limit, and the work moves to the encoder (the audit already names the coarseness: card identity is not encoded,
   only colour+tags).
2. Distilled policy does not beat its parent over ~10 iterations while the critic's AUC does improve → the ranking is
   informative but not transferable through the heads; then the heads (one per decision type, shared trunk) are the
   problem.
3. Both improve but the win rate does not → the objective is fine and the search/rollout depth is the next lever.

## Build order

1. **Complete the encoder (small, and it must come first).** Blue cards are already individually encoded for both players
   (82 flags each, verified), but **green card identity is not**: the 12 "pays again" green projects (the ones phase 4 can
   replay — `DiversifiedInterests`, `Grass`, `Lichen`, `MassConverter`, `MicroMills`, `Moss`, `QuantumExtractor`,
   `SolarTrapping`, `Sponsors`, `HeatReflectiveGlass`, `PerfluorocarbonProduction`, `Tourism`) are indistinguishable in the
   state — the audit gives all of them the *same* dV (+0.2904) and their flags never appear in `changedFeatures`. Add 12
   flags per player (24 columns, observation 760 → 784), zero-initialise the new input weights so the warm start from
   night 7 is bit-identical at step 0, and verify with the audit that the two cards now score differently. The old data
   can still be used by zero-padding it, so the change costs one data pass, not a retrain from zero.
2. **Full trajectories.** Self-play writes *all* decisions (`--max-rows-per-game-type 0`) with seat, round, step and the
   final result. Verify a trajectory can be rebuilt in order and that its rows share one terminal result.
3. **Critic v2**: `ml/train_value.py` on those rows with the **margin** as target, wider nets, per-game split, calibration
   by round band. Compare AUC against the current 0.86 — this alone upgrades the search leaf.
4. **Ranking recorder + soft-target distillation**: during self-play with search enabled, record the symmetric score of
   every candidate of BUILD_CARD / DRAFT_CARDS; add the distillation loss to the policy trainer.
5. **One iteration end to end**, evaluated on the same 10 000-game harness (policy with and without search), then iterate
   on the critic and the target, never on rules.

## What is kept, what is dropped

| kept | dropped |
|---|---|
| the audited observation encoder (760 values, no hidden information) | supervised "imitate the taken action, weighted by the outcome" as the learning core |
| the value net as critic and as the search leaf | the 8-rows-per-game data sampling |
| 1-ply symmetric search at inference (compute, not a rule) | **the per-round score differential as a reward (misleading, measured above)** |
| the nightly pipeline, chunk format, eval harness | diagnostic flags as a product: `preferPhase`, `mustBuild`, phase rules, card tables |
| night 7's policy as a warm start | any hand-written priority order between phases or cards |

## Data volume: streaming training (2026-11-14)

A night of self-play with **every** decision recorded does not fit in memory: 30 000 games produced 7.66 M rows and 58 GB
(`D:/ares/data/r2_full`, 17 min of self-play at concurrency 32, observation 98 us). `ml/train_value.py --stream` therefore
reads one chunk file at a time: normalisation comes from a sample of files, the validation set is a **held-out set of
whole chunks** (no game sits on both sides), and an epoch is one pass over the remaining files in random order, shuffling
the rows *inside* each file before batching. Peak memory is one chunk plus the model.

Why the intra-file shuffle: rows are stored grouped by game, and training on consecutive records of one game is how a
model memorises that game (owner's warning). The validation by whole chunks would show it as rising val loss, but there is
no reason to create the problem.
