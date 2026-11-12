# Plan: new computer player

Goal: an AI stronger than **NETWORK**, that finds synergies itself and is not stuck in one exploitable strategy.
Constraints: no hand-written behaviour bias; reward = game outcome; nights are long self-play→train→eval loops on the owner’s machine; Java writes data, Python trains. Heroku out of scope for now.

Principle: **do not repeat the owner’s value+projection approach**. This doc is a hypothesis plan with go/no-go criteria.

## Owner decisions that shape the plan

1. Train only the **full** agent. Isolated single-decision optima break when the rest of the policy changes. First build all decisions + a complete observation; nights train the whole thing. Single-decision loops remain infrastructure checks only.
2. Observation must be complete and leak-free (`ObservationCompletenessTest`, `CardTrackingAuditTest`). Round-context blind spots and a few indistinguishable cards are tracked as encoder TODOs.

## What measurements showed

| Fact | Note |
|---|---|
| RANDOM ~360 games/s; SMART ~70; V2 CPU ~0.7; V2 GPU ~2.5 | GPU helps V2 only ~3.6× — projections/encoding dominate |
| V2 move choice = project each candidate, score with value net | `network2`, `CardProjectionService` |
| Phase choice assumes opponent always picks phase 4 | exploitable |
| V2 not stronger than NETWORK in fair compares | `/simulations/compare/models` |
| Engine had rare rule bugs | `docs/BUGS.md` |

**Consequence:** behaviour (how moves are chosen) matters more than net size. Enumerating all moves is a speed ceiling of the **approach**, not of the hardware.

## Player decisions

See `docs/GAME_RULES.md`. Corporation + mulligan → **simultaneous phase pick** → build / blue actions / draft / sell → endgame. Phase pick is a matrix game; pure `argmax` + fixed opponent model is exploitable.

## Approach families

| | Family | Role |
|---|---|---|
| **A** | Policy over **engine legal moves** (+ value head) | **Primary** — fast games, no hand projections |
| B | Value + full enumeration (owner line) | Yardstick / fallback |
| C | Offline Q from logs | Possible add-on |
| D | Search on top of A (top-k 1-ply, phase matrix) | Stage 2.5 — measure |
| E/F | Evolution / pure NETWORK imitation | Rejected / init only |

A does **not** project every candidate through the engine. The engine is the referee (legal moves + apply).

## Architecture (A) — current implementation sketch

- Observation v3: table/hand features (copies removed) + round context (`ObservationBuilder`).
- Unified `decide(observation, candidates) → index`; one Q-head per `DecisionType`, shared trunk (`train_all.py` / `MlpPhaseModel`).
- Reward: win/draw/loss only. Aux heads allowed later if they do not change the reward.
- Search later: top-k 1-ply / phase matrix — measure cost vs strength (stage 2.5).

## Training loop

Bottleneck is iterations of play→train, not GPU flops. Nightly automates that (`docs/NIGHTLY.md`). Promote checkpoints only when eval gain beats the previous on enough games. Encoder/schema hash must reject mismatched data.

## Testing rule

Before training on a new piece: label perspective, hidden-info leak, schema hash, move generator soundness/completeness, observation completeness. Mutation-check tests when practical.

## Status

All non-crisis decisions + observation + nightly multi-decision training exist. Strength vs NETWORK is still ~0% (see `docs/EXPERIMENTS.md`). Next leverage is data/behaviour diagnosis (e.g. H-P1 passivity), not a new architecture family.
