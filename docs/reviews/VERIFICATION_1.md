# VERIFICATION 1 — checking CRITIQUE_1 claim by claim

Rule for this file: a claim is verified by a fact, a number, or a run — not by agreeing with it. Each item says what was checked, how, and what it
changed. The reviewer's file: `docs/reviews/CRITIQUE_1.md`. No reply is written yet, by the owner's instruction: first verify.

## A. Code facts (checked, no run needed)

### A1. "A nudged pick is recorded as if the policy chose it" — CONFIRMED

`AgentDecisionService.pickPhase` (lines 152-180): when the nudge fires, `index = buildPhaseWithWork(decision, player)` and then line 178 calls
`record(game, player, decision, index)` — the same call that records the policy's and the search's choices, with no flag. So the corpus cannot
distinguish a forced row from a chosen one.

**Consequence:** the policy's cross-entropy imitates the nudge at every nudged pick. At rate 1.0 that is a large share of the phase rows, and each
iteration starts from the previous policy (`--prev`), so the compulsion compounds. This is a plausible, mechanistic explanation of the drift
(income 5 → 17 → 0%, draft 5 → 5 → 22%) and of the fall at iteration 3 — without invoking plan-conditionality. **The reviewer's finding 2 stands.**

### A2. "Both seats are nudged, so the critics learn margins against a nudged mirror" — CONFIRMED

`GameService.createNewSimulation` sets the fixed code on **every** computer seat, and self-play has two computer seats. The probe corpora were
generated with `planCode=8` and `planNudges=true`, so both seats were nudged and the corpus records a mirror game. At evaluation the nudge is off and
the opponent is SMART — off-policy in both directions.

### A3. "The plan bits are in the observation regardless of the nudge flag" — CONFIRMED, and the evaluation did pass the code

`ObservationBuilder` writes the four `PlanCodes.code(player)` bits; `PlanCodes.code` does not depend on `nudges()`. So the bits are part of every
observation whatever the flags. **However**, the specialist evaluations were run with `-Dares.agent.planCode=8` (all compare commands in this
session), so the bit value at evaluation matches training. The reviewer's "possible input shift" does not apply to these runs — it applies to any
future evaluation that forgets the flag, and it is worth a test (an evaluation with and without the flag) because a silent mismatch would look like a
weak policy.

## B. Row 19 (the "plan-conditional value" evidence) — RECOMPUTED

Numbers used: p1 own 44.75% (n=400), p2 own 54.4% (n=400), p1 champion 76.5% (n=200), p2 champion 66.25% (n=200).

| statistic | value | se | z |
|---|---|---|---|
| critic main effect (champion vs own, averaged over policies) | **+21.8 points** | 2.85 | **7.6** |
| interaction (the p1→p2 change having opposite signs under the two critic sets) | +19.9 points | 5.71 (6.09 with +1.5 training noise on the own-critic points) | **3.5** (3.3) |

**Both are statistically real** — the reviewer's prediction that the interaction would shrink below 2σ is not supported by this arithmetic. **But the
reviewer's main reading is right and mine was wrong:** the dominant, clean effect is that **the champion's critics are better critics for these
policies** (+21.8 points, z≈7.6). My formula "+10 under its own critics, −10 under the champion's" described the interaction and ignored the main
effect, which is larger. The honest sentence is: *the specialists' policies score higher under the champion's critics; the specialists' own critics
are the weak part.*

**Why the interaction is not yet interpretable**, as the reviewer says: the four cells are not a clean factorial. "Own critics" means critics trained
on that iteration's corpus (loss 2.03 for iteration 1, 1.33-1.40 for iteration 2) — so critic quality, corpus, and training seed move together.
The interaction is real as a number and unexplained as a mechanism.

**Consequence for the plan:** the rule "a specialist ships as a stack with its own critics" is **not supported and probably wrong**. If the champion's
critics are better for the specialist's policy, the right combination is the specialist's *policy* with the best available *critics*, and the
specialist loop's critic training is the thing to fix (more data, on-policy labels), not to insist on.

## C. "A corpus inherits the strength of its generator" — CONFOUNDED WITH SIZE, as the reviewer says

RESULTS ~1516 compares: champion (5 000 games) → 73.17%; forced phase (`phaseBias` 44.25%) with **2 000** games → 25-33%; horizon-2 (65.08%) with
**1 000** games → 21.0%; heavy exploration (~50%) with **2 000** games → 21.1%. Every weak-generator corpus is 1 000-2 000 games, and LEDGER 10 says
the champion's own play at 1 250 games trains a **30%** player. So the table does not separate "weak generator" from "small corpus". **The rule must be
downgraded to a hypothesis until the size-matched control runs (P4).**

## D. What is still to be run (in the order the owner's instruction implies: verify before replying)

1. **Row-19 replication at 1 000 games** (running): p1 and p2 with the champion's critics. Decides whether 76.5% (the highest point in the project)
   survives. Also: p1 with and without `planCode=8`, to check A3.
2. **P4, the size-matched control** (no generation): subsample the champion's 10 000-game pack to 2 000 games, train the full stack, measure at
   1 000 games. If it lands at 25-35%, section C is confirmed and LEDGER 22/23 need a CORRECTED status.
3. **The oracle's split-half reliability** (needs the instrument; ~5 min of labels): run the end-of-game audit twice on the same menus and correlate.
   The reviewer's arithmetic says K=2 with a random continuation cannot rank anything; if the split-half r is low, `V0` and the oracle as costed are
   dead and the gate needs the options value with a common opponent instead.
4. **P1, the `Q^champ` phase oracle** (~3 h): 200 phase decisions, every legal phase played to the end with the champion's stack, K worlds, and the
   gain of the rollout argmax over the champion's own choice evaluated on fresh rollouts. This is the clean test of C1 and of whether the champion is
   one-step-optimal.
5. **P2, flagged nudges** (engine change + test + 1 h generation): regenerate one iteration with a `forced` flag and train the policy with forced rows
   masked. If it recovers ≥ 5 points, A1's mechanism is confirmed and the annealing was a workaround for a label bug.
