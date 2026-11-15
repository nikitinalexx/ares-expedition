# HANDOFF -- new AI for Expedition Ares

For the next assistant. Also read AGENTS.md, docs/AI_PLAN.md, docs/NIGHTLY.md, docs/EXPERIMENTS.md.
Talk to the owner in Russian. Code/identifiers in English. Branch: nn-pure-java-verify.
Owner commits; do not commit or push unless asked.

## Goal

Build a computer player stronger than NETWORK (prod) and NETWORK_V2.
Approach: policy over legal candidates. No hand-written bias. Reward = game outcome only.
Heroku/prod out of scope until local AI is strong.

## Machine

Windows, ~32 GB RAM, NVIDIA GPU (PyTorch training). D: ~590 GB free.
JDK 21+. Maven: -s .mvn/settings.xml. Python in ml/.venv (falls back to `py -3`; there is no venv on this machine and
torch is CPU-only 2.8.0). Train ~30s.
With the trained night7 model, one AGENT vs AGENT game is **58 ms** (2 seats, one thread, 510-560 decisions) and
`--mode compare` does **202 games/s vs RANDOM** at concurrency 24.

## Where the models are

`D:/ares/runs/<night>/handoff/<stamp>/best_model` — one folder per decision type (`model.json` + `weights.bin`),
plus `MODEL_INFO.json` (iteration, win rates) and `reference.json`. `D:/ares/data/<night>` holds **only datasets**.
Load directly with `--models <best_model>` when its `schemaHash` matches the current encoder (night7 it46 does:
`fbf1f802c289634d`, obsSize 760, hidden [256,128]).
Night 7: `D:/ares/runs/night7/handoff/night7_20261113_222800/best_model`
(it46, score 0.5564: 85.7% vs RANDOM, 25.9% vs SMART).

## State after Night 7 (13 hours, 120 iterations)

Best model: Night 7 `vs_random` it46.
- **85.7% vs RANDOM** (up from 42% in Night 5, 61.8% in Night 6)
- **25.9% vs SMART** (up from 4% in Nights 1-4, 7% in Night 5-6)
- **0.5% vs NETWORK** (avg points: 67.6 vs 104.72)

## Key Diagnosis: The 68-Point Ceiling
- The agent currently averages **~68 points** per game.
- Against RANDOM (scores ~50 pts), 68 points wins 86% of games. RANDOM is solved.
- After iteration 46, win share vs RANDOM reached 84%. The agent stopped growing because beating RANDOM by 1 point or 50 points gives the exact same reward (`outcome = 1.0`). There is zero gradient signal pushing for higher engine output.
- Against SMART (scores ~82 pts), 68 points loses ~74% of games.
- Against NETWORK (scores ~105 pts), 68 points loses 99% of games.
- **Solution**: Curriculum stage 2 -- train against SMART. In games vs SMART, only plays that produce >82 points receive `outcome = 1.0`, directly forcing the neural policy to build stronger engines and optimize terraforming tempo.

## Night 8 -- Ready to Run
Config (`ml/nightly_config.json`):
- Experiment 1: `vs_smart` (`AGENT:SMART:1`, 10000 games/it)
- Experiment 2: `mixed_smart_random` (`AGENT:SMART:0.7,AGENT:RANDOM:0.3`, 12000 games/it)
- `selfplay.temperature`: 0
- `keep_data_iterations`: 1
- `rungs.keep`: 2 (both run round-robin all night until stopped)
- `plateau_patience`: null (disabled)

Command: `nightly run night8 --start-from night7`

## Search (family D) -- measured, not built

`docs/SEARCH.md` (instrument: `SelfPlayMain --mode bench`, code: `selfplay/RolloutBench`, `agent/DecisionProbe`).
- A decision point is replayed **exactly** (100% identical observation) if the action attempt is replayed from
  `DecisionProbe.turnStarted` with the recorded prefix; this holds with a random continuation policy as well (the
  prefix is forced, not re-sampled).
- With the trained night7 model: net **34 µs** per decision, one full rollout **25.6 ms** (~49 us per remaining
  decision), copy 6-10 us, engine work per decision 57 us. 39 / 150 / 259 rollouts per second on 1 / 4 / 8 threads.
- Full-rollout search in **training is rejected** (~900 CPU-h / 15k games); at **deployment it is affordable**
  (~46 ms per move on 8 threads); the trainable shape is 1-ply `Q(s,a) = V(apply(s,a))` (~0.1 ms per candidate,
  ~11 CPU-min / 15k games).
- Next: train `V(s)` on the observations already recorded, then compare policy vs 1-ply on held-out states.

## Inference cost (done 2026-11-14, before night 8)

`MlpPhaseModel` was 203 us per decision -- 65% of a game -- which made both deployment search and nightly throughput
3-6x worse than they needed to be. It is now **34 us** (5.9x), game 176 -> 58 ms, rollout 125 -> 25.6 ms, with no
change of semantics (`MlpPhaseModelTest` still agrees with the numpy reference to 1e-4; 86.9% vs RANDOM and 23.5% vs
SMART over 600 games each). Three changes, see `docs/SEARCH.md` Result 3: transposed weights (vectorisable SAXPY),
the observation's 90% zeros folded into a constant, empty candidate features and relu zeros skipped.

## 2026-11-15: SMART and NETWORK are benchmarks only; the score is a card table

Owner's rule, binding from now on: **SMART and NETWORK are benchmarks. Not teachers, not opponents in training, not a
source of data.** Imagine SMART does not exist - the only training partners are ourselves and RANDOM. A recorder that
translated SMART's turns into our decision format was written, tested and then **removed** on the owner's instruction
(the direction is dead: imitating SMART cannot beat SMART, let alone NETWORK).

Where that leaves us, measured today:

* `--mode scorecard` (new) reports the terms the engine scores. 300-400 games: agent **71.5** vs SMART **76.4**, with
  **+6.6** Tr and **+5.0** forests but **-9.3 printed card points** and -5.8 milestones+awards.
* SMART puts **27.4 cards** on the table per game against our **13.2**, worth 13.0 printed points against 3.2. The chain
  is engine -> discounts/income -> many cheap cards -> points, exactly as the owner describes it.
* `ml/value_sensitivity.py` (new) shows the critic cannot price a card: +3 printed points move V by **+0.008** (ten
  milestone points: +0.194). A margin-trained critic is no better (+0.003), and a critic trained on many-card games is
  better at predicting the winner (AUC 0.905) but still blind to cards.
* `mustBuild` (build 18 instead of 12 per game): win rate 38.2% -> **33.5%** on 2 000 games. Extra cards without an engine
  are paid at full price.
* Phase pick decided by search (new: the engine's own `PickPhaseProcessor` + `StateTransitionService`, opponent's pick
  determinized) works mechanically - all five phases appear - but every critic variant collapses the player (0.3%, 3.8%,
  32.8% vs SMART); the start of a phase is not a comparable position.

Encoder is now **805** values (the five score terms per player were missing, and a card candidate did not carry its own
printed points; the candidate width grew by 1 for every card decision). Old bundles convert with
`ml/pad_model.py --expected 805 --schemas <schemas.json>` (candidate columns are padded too now).

New/changed instruments: `--mode scorecard`, `ml/value_sensitivity.py`, `ml/card_usage.py`, `ml/explore_stats.py`,
`ml/check_player.py --no-references/--must-build/--phase-search/--phase-playout/--horizon/--samples/--phase-value-model`,
`-Dares.agent.explore=TYPE:q` (uniform exploration of the data generator), `-Dares.agent.valueModel.phase` (a separate
critic for the phase pick), `-Dares.agent.search.phasePlayout` (score a phase by playing the round out),
`train_policy_ce.py --objective pg/--behaviour/--adv-target margin`, `train_value.py` unchanged.

Training is self-play + RANDOM now: `ml/discover_config.json` (`AGENT:AGENT:0.75,AGENT:RANDOM:0.25`). Night `disc1`
runs it from `D:/ares/runs/policy/r11_cards` (the best playable bundle on the 805 encoder, ~37% vs SMART on 400 games).
Its first iteration: RANDOM 92.4%, SMART 33.9%.

The open problem is credit assignment over the engine->cards chain, and `docs/DISCOVERY.md` states it in numbers with the
two honest ways forward (scale of data; long-horizon planning, both already implemented as knobs).

## 2026-11-15, second half: verification first, then scale

Owner's direction: a network that sees no value in building is more likely a **bug** than a limit, so verify the machinery
before spending nights on training, and grow a test suite that catches regressions ("every bug becomes a test").

### What verification found and fixed

* **hidden information in the deployed search**: the symmetric score read the *opponent's full observation*, which
  contains their hand. `ObservationBuilder.buildPublic` zeroes the hand block and the search uses it for the opponent;
  `AgentDecisionAuditTest` fails if the public observation changes when the opponent's hand changes.
* **double payment measured on a shared copy**: one card's `payAgain` could improve the payout another card was measured
  on. One fresh copy per card now, and the test checks the feature is a single card, never the sum.
* **`WinPointsService` NPE** on a played resource card without a counter entry (the score is read on every decision, so
  this was a crash waiting to happen).
* **dead observation bits**: cards of the INFRASTRUCTURE expansion (`Sawmill`, `Pets`, the five hand cards) and the
  duplicated resource counters removed; encoder 795 -> 780. `ml/feature_audit.py` (activation rate per value over
  thousands of games) is the instrument; a test asserts no bit belongs to an unplayable card.
* **the score is ONE number per player** (owner's rule): `score:my_winning_points` / `score:opponent_winning_points`, the
  engine's own total; TR is not a field of its own. The V2 block keeps its legacy `win_points` because NETWORK_V2 is
  trained on it. `--mode scales` prints the divisor of every value (the V2 block is normalised by a legacy table).

### The obvious-move bench (`--mode sanity`, docs/SANITY.md)

Seven scenarios where the rules alone say what is better; runs through the live decision path, exit 1 on a hard failure,
and the nightly refuses to promote a model that fails one. It immediately found that the deployed player **refused a free
two-card draw** and **bought a standard ocean while holding `Deimos Down`**. Extending the value search to those menus
fixed the scenarios and cost 13 points of win rate, so the fix is two arithmetic rules instead (free blue action once per
phase; a standard project dominated by a card in hand), measured at no cost: sanity 6/6, 35.9% [33.8, 37.9] vs SMART.

### Afterstates: measured, rejected

`DecisionType.AFTERSTATE` records the state one action after every agent decision (same game result), so the critic can be
trained where the search reads. It works, and it costs ~4 points of win rate (36.5% -> 32.2%): the afterstate rows are
near-duplicates of the next decision row and dominate the stream. Kept behind `-Dares.agent.afterstates=true` for a
future *separate* critic.

### Running now

`ml/scale_config.json` (`scale1`): 40 000 games per iteration (self-play + RANDOM), PPO with the advantage of
`v_r15_outcome`, the two rules on, the sanity bench as a gate, chunks sampled across the iteration instead of the first N.
Started from `r15_parent` (the best bundle on the 780 encoder).

## 2026-11-15, third part: the search is out, the phase axis is measured, the latent plan is in

### Stage A: a rollout is not an improvement operator at this compute (negative, and it closes a branch)

`--mode rollouttest` with the honest design - the candidate is selected on one determinization of the hidden cards and
evaluated on another (`services/agent/Determinizer`, shared by the search and the audits) - on 60 `BUILD_CARD` positions
against the deployed player:

| | margin |
|---|---|
| the policy's choice | +8.3 |
| the best of 4 by one rollout per candidate | +5.1 |
| difference, out of sample | **-3.3 +/- 6.5** (24 positions better with the rollout, 35 with the policy) |

The in-sample number had been +12.3 +/- 8.1: the winner's curse in full. With a per-rollout noise of ~17 points and four
candidates, one sample per candidate is a choice made of noise; informative needs 10-20 samples per candidate (~40 s per
decision, ~8 min per game). So search stays where it was measured positive (1-ply + pairs on BUILD_CARD/DRAFT_CARDS),
and further search growth is out of scope. PLAN.md branches to the two things that measured positive: validated plans in
the generator, and scale.

### The phase axis, measured (`--mode phasedist`)

The recorded data has agent decisions only, so "what does NETWORK call?" needed an engine-side counter:
`services/PhaseCensus` (off by default) counted from `AiPickPhaseTurn` (the *decision*, not the turn processor, which sees
every turn twice), new mode `--mode phasedist`. 300 games NETWORK vs RANDOM, 100 NETWORK vs NETWORK, 100 SMART vs SMART:

| player | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| NETWORK | 34% | 39% | 4% | **2%** | 20% |
| SMART | 29% | 31% | 6% | 9% | 26% |
| RANDOM | 21% | 18% | 20% | 11% | 31% |
| **AGENT (r22_plan)** | **0%** | **0%** | **0%** | **49%** | **51%** |

NETWORK calls a build phase in ~73% of the rounds and alternates 1/2; our player never does. Not a claim about the value
of a phase (the engine is the same for both seats) but proof that the axis is unexplored by the policy - it was trained
on games in which phases 1/2 never appear, so no critic trained on them can price the phase.

### The latent plan: mechanically it works, and imitation of the nudges costs 7 points

`services/agent/PlanCodes`: four private bits per player (`Player.planCode`), random per computer seat at game creation
(`-Dares.agent.planRandom`), in the observation (last four values, zeroed in `buildPublic`), switching the generator's
exploration profile while recording (`-Dares.agent.planNudges`, `-Dares.agent.planNudgeRate`). Instrument:
`ml/plan_report.py` (per-code phases, builds, activations, spends, win rate, chi-square).

| run | data | model | divergence | vs SMART (2 000) |
|---|---|---|---|---|
| control | codes assigned, nudges OFF, 400 games | r22_plan (bits at zero weight) | none: builds spread 1.4, chi2 = 0 | - |
| budget control | 6 000 games, nudges OFF, 10 epochs CE | r25_ce_control | - | **37.8%** (same as the parent) |
| 3 epochs | 6 000 games, nudges ON | r23_plan_ce | chi2 = 199 (dof 45) | **33.1%** |
| 10 epochs | same | r24_plan_ce10 | chi2 = **2 367**, builds spread 2.5 | **30.8%** (code 0); 25-31% for codes 2/4/8/12 |

What this says: the channel carries a real, reproducible style (phases 1 at 29-34% for the build-phase codes against
15-17% for code 0, with the nudges off), and it costs 4.7-7.1 points to learn it, because the policy also learns the
*context* half of the nudge (code 0 now calls a build phase in 16% of the rounds, was 0%) and the shared trunk follows the
nudged games. The budget is not the explanation: the same 6 000 games and the same 10 epochs without nudges reproduce the
parent exactly.

Two bugs the instrument caught before any training run: the phase nudge bypassed `choose()` (so bit-8 games were recorded
with no phase decisions at all), and the first phase nudge was unconditional, which starved the economy and taught
"build phases are terrible" instead of "call a build phase with an empty hand" (now conditional on holding a green/blue-red
card, with `planNudgeRate` turning compulsion into preference).

### Running now

`r26_plan_anchor`: 6 000 games with `planNudgeRate=0.35` and CE 10 epochs from `r22_plan` with a **KL anchor** to the
parent on code-0 rows (`--anchor r22_plan --anchor-weight 3 --anchor-code 0`). The anchor is the trust region: a change in
behaviour that the plan code does not explain is paid for, so the deployed style should stay at 37.8% while the other codes
still diverge. Success = code 0 within ~1 point of 37.8% **and** chi-square in the thousands; failure = the anchor either
pins everything (no divergence) or nothing (the cost returns), and then the honest step is a bigger dataset per iteration
(20 000 games) before touching the mechanism again.

## 2026-11-15, fourth part: the critic is the lever (+3.4 points), the mechanisms are not

### The best player now

`r22_plan` + critic `v_mix3_margin_wide`, ranking the candidates of `BUILD_CARD`/`DRAFT_CARDS` (pairs on, symmetric score).
Measured at the 10 000-game resolution the effect needs:

| opponent | result |
|---|---|
| **SMART** | **42.05% +/- 1.0** (was 37.8% +/- 2 with the old critic) |
| RANDOM | 93.2% |
| NETWORK | 9.3% |

Profile: 12.9 cards per game, acceptance 0.566, phases 49% income / 51% draft, phase-3 spends 39.0%; sanity bench 8/9
obvious moves, 0 gate failures. The bundle is `r23_price` (encoder 786) and the critic `v_price1_margin_wide`; the extra
two values are the effective price of the best card in hand (see docs/OBSERVATION.md section J). So the gain is not a degenerate trick - the behaviour is the same, the card choice is
better.

The critic: 100 000 self-play games with mixed exploration (build 0.5, draft 0.3, standard 0.2, blue 0.2, phase 0.2),
target = margin, 512x256. The scaling is monotone in *data* and flat in *parameters*:

| critic | data | hidden | vs SMART |
|---|---|---|---|
| `v_r15_plan` | policy play only | 256,128 | 37.8% |
| `v_mix2_margin` | 20 000 | 256,128 | 38.5% +/- 0.6 |
| `v_mix3_margin_wide` | **100 000** | 512,256 | **41.18% +/- 0.9** |
| `v_mix3_margin_xl` | 100 000 | 1024,512 | 39.82% +/- 1.4 |

### What was measured and rejected today (all with the numbers in docs/EXPERIMENTS.md)

* **V as the player** (V decides act-vs-stop on every menu): 0.4-0.6% with a critic trained on random play, 0.1% with the
  policy critic, and the mechanism is visible in the data - 759 of 768 build opportunities declined, 0.45 cards per game.
  A state value is biased against any action that spends money now and pays ten rounds later; it explains the earlier -13
  and -8 point results too.
* **The same fix over a horizon** (stop scored by 2 rounds of policy playout): 19.5% - one rollout per candidate is the
  Stage A noise law again.
* **Latent plans** (per-game code, generator nudges, KL anchor to the parent): the channel works mechanically (chi-square
  2 367 across 16 codes, build-phase codes call phase 1 in 29-34% of rounds vs 15-17%), but it costs strength: 30.8%
  unanchored, 34.95% anchored, against 37.8% for the same budget without nudges.
* **The phase axis and the second build slot** (owner's observation: in phases 1/2 the picker builds twice and the agent's
  pass forfeits both): measured 0.6% of its decisions are two-slot states, because it picks phase 1/2 in 0% of rounds. The
  paired audits of the plans: call the phase -0.5 +/- 1.7, call it and build -0.0 +/- 1.6, build the metal-income cards
  -0.6 +/- 1.4. No single link of the chain pays when it is forced alone.
* **The steel/titanium discount**: it exists (services/DiscountService: 2 MC per steel income on a BUILDING-tag card, 3 per
  titanium on a SPACE one), but the player's mean steel income per decision is **0.12**, and the discount changes which card
  is cheapest in **1.0%** of rows - it does not own the income, so the chain cannot pay.

### Next, in order

1. **Give the critic the effective price.** `DiscountService.getDiscount` already computes, from the rules, what a card
   will actually cost this player. Put it in the observation (e.g. `my_best_effective_price_in_hand/50` and the discount
   available for it) so that the ranking sees "this card is cheap *because of my steel*" instead of having to infer a
   product of a tag and an income. This is the one feature the owner's chain needs and the encoder lacks. Cost: encoder
   change, regenerate the critic data (20 min per 100 000 games), retrain (20 min), evaluate at 6 000-10 000 games.
2. **Coverage generators with audits** (the arbiter stays the paired audit): the three plans measured today were blunt
   versions (force a link for 8-12 turns). The next family is *conditional* plans (call the build phase when a card is
   worth building by the critic; keep metal-income cards only when the discount is reachable), which is what the benchmark
   actually does.
3. **The critic's target**: margin beats outcome; the next candidate is a distance-to-end weighting (a row in the last five
   rounds is closer to the payoff than one in round 2), which is cheap to test with the same pipeline.
