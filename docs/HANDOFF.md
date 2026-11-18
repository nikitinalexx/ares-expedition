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

## 2026-11-16, night: the critic is a distribution, and the encoder now carries the hand

### Where the player stands

| configuration | vs SMART |
|---|---|
| `r25_rev2` (804) + **ensemble of three critics** (`v_rev1`, `v_ens_a`, `v_ens_b`) | **42.89% +/- 1.0** (10 000 games) |
| same player, any single one of those critics | 38.7% / 39.05% / 39.38% |
| `r23_price` (786) + the best single critic of the previous encoder | 42.98% +/- 1.0 (10 000) |

The three-point spread inside the *same recipe* is the finding of the night: a single critic is a sample, not a number. The
search averages an ensemble for free (`-Dares.agent.valueModel=a,b,c`; three forward passes per candidate, ~0.4 ms), so
every future run carries an ensemble. `docs/RESULTS.md` has the whole ledger with this caveat written at the top.

### What the new representation adds (and what it is worth)

* `HAND_SUMMARY_SIZE = 20` in the observation (cards, per colour, per tag, sum of nominal and *effective* price, cheapest
  effective price, how many are affordable). A/B on identical rows, the block zeroed before training: **39.4% with it,
  27.5% without** - it is the most informative block the critic had all day.
* the printed victory points of a card in the candidate vector (documented since the beginning, never written). It is the
  LAST slot on purpose: candidate features only ever grow at the end, so a converted model can take a zero weight
  (`ml/remap_model.py --pad-candidates`). Placing it before `stop` shifted stop's weight and cost 3.8 points - caught by
  the parity run, now impossible to repeat silently.
* policy heads retrained on the new encoder (`r26_ce`, CE 6 epochs on 30 000 games) - see the next entry for the number.

### The gate is trustworthy now

`--mode sanity` reported "1 gate failure" in one run of five with no model change: the bench had failed to *sample* a
position for a scenario ("no position in states [PERFORM_BLUE_ACTION]") and reported that as a gate failure. A missing
position is an infrastructure problem: it is retried three times and then reported as a warning, never as a gate failure.
Four consecutive runs: 0 gate failures.

### Tests that now guard the representation (`ObservationCompletenessTest`, 9 tests)

facts (hand included) must move the observation; every decision's menu must be exactly what the rules allow; a card
candidate must carry its printed points and price (recomputed from the card); every `hand:`/`plan:` column must be zero in
the public view; the double payment must be one card's payment, not a sum (with a card pair chosen to have different
payments, which removed a flake); building an observation must not change the game.

### Next

1. two more critics on fresh mixes -> ensemble of five (variance falls as 1/sqrt(k)); same player, same cost.
2. the nightly (`ml/rev_config.json`) on top of the ensemble, so the policy is trained on data the better critic ranks.
3. the remaining big lever stays the *card flow*: 12.9 cards per game against SMART's 27. The phase axis (NETWORK calls 1/2
   in 73% of the rounds, this player in 0%) and the second build slot are still unexplored by the policy's own data; the
   blunt plans for them measured 0.0 +/- 1.6 and -0.5 +/- 1.7, so the next attempt there has to be a *conditional* plan.

## 2026-11-16, morning: where the player stands, and what the night taught

### The best measured player

`D:/ares/runs/best.json` has the exact configuration. In short: policy `r25_rev2` (encoder 804) + an **ensemble of five
critics** (`v_rev1_margin_wide`, `v_ens_a..d`), ranking the candidates of `BUILD_CARD`/`DRAFT_CARDS` with pairs and the
symmetric score.

| | measured |
|---|---|
| vs SMART | **43.59% +/- 1.0** (10 000 games) |
| the same pipeline at the start of the session | 37.8% +/- 2 (2 000 games) |
| profile | 12.9 cards per game, acceptance 0.57, phases 49% income / 51% draft, phase-3 spends 39% |
| sanity bench | 9/9 obvious moves, 0 gate failures |

### What actually produced the six points

1. **The critic sees the hand** (20-value summary: cards, per colour, per tag, nominal and *effective* price, cheapest,
   affordable). Direct A/B on identical rows with the block zeroed: 39.4% -> 27.5%. The observation described the table,
   the money and five specific hand cards - the state value could not see that it held cheap cards, so "build now or wait"
   was judged blind. Found by the owner's question, not by an audit.
2. **The card's printed points** in the candidate vector (documented since the beginning, never written).
3. **The ensemble**: one critic is a sample, not a number. Same recipe, different 100k sample: 42.98% vs 39.38%. Ensembles of
   1/3/5: 39.38 / 42.89 / **43.59**. Five forward passes per candidate (~0.7 ms) is nothing at this scale.
4. Everything else was measured and *rejected*: V deciding act-vs-stop (0.4-0.6%), the horizon fix (19.5%), latent plans
   (30.8/34.95%), the build/stop dial (41.4-43.2 against 43.6), the phase and metal plans (0.0/-0.5/-0.6 +/- 1.4-1.7), CE on
   exploration data (41.4 against 42.9), an afterstate-trained critic inside the ensemble (42.84 against 43.59).

### Instruments that did not exist this morning

`ml/plan_report.py`, `ml/slot_report.py`, `ml/discount_report.py`, `--mode phasedist`, `--macro phases|phasesbuild|metals`,
`--mode rollouttest` with an out-of-sample design, `ml/train_value.py --names/--drop-prefix` (A/B a block of the
representation on identical rows), `--include-afterstates`, `-Dares.agent.search.stopMargin`, `ml/remap_model.py
--pad-candidates`, and nine tests in `ObservationCompletenessTest` that would have caught the hand blindness on day one.

### What is still missing, in order of expected value

1. **A quality signal for a single card.** The trainable label we have is the *state* value (final margin of the game we
   played); the label a card needs is the counterfactual ("what would this game have been worth had I taken this card"), and
   a rollout gives it only with 10-20 samples per candidate (~8 minutes per game). Everything that tries to answer "should I
   build this card" without that label has failed (mustBuild -4.7, the stopMargin dial, the phase/metal plans).
2. **The policy's card flow.** 12.9 cards against SMART's 27 is the largest term of the score gap (cards -9.3). The policy's
   stop head declines ~43% of builds and CE on exploration data only teaches it the generator's noise. The honest route is
   PPO with the ensemble's advantage (the nightly) or distillation of an admitted plan - not more CE.
3. **The phase axis** (NETWORK calls 1/2 in 73% of the rounds, this player in 0%) - only a *conditional* plan can pay there;
   the blunt ones measured zero.

### Protocol changes that come out of the night

* Every critic is a distribution: report ensembles (or three seeds), never a single net.
* Every block of the representation is A/B-able with `--drop-prefix`; a block that does not earn its place is deleted.
* A dataset is bound to the encoder (schema hash) and is deleted when the encoder changes - six old ones (450 GB) filled the
  disk twice.
* The sanity bench treats a position it cannot sample as an infrastructure warning, not as a model failure.

## 2026-11-16, second half: the price law, the two roles, the tag block

### What is measured and settled

1. **A critic's price of an income stream is set by how much its data spends** (`ml/price_report.py` on `--mode audit`):
   policy play 6.5 MC (flat), forced-build play 9.0 MC, uniform random play **12.1 MC with the right round profile**
   (14.0 -> 10.4), the rules ~15 MC. Random games are noisy about everything except the big effects, so their critic ranks
   badly (22.8%) while the policy critic ranks well (39-44%) with a wrong price.
2. **The two roles need different things**: a *ranker* needs only the order of the candidates (a shared slope cancels, and
   the stop decision is the policy's), a *decider* needs calibrated slopes, i.e. the price. Measured: adding the correctly
   priced random critic to the five-critic ensemble moved the price 6.5 -> 8.2 MC and the win rate 43.59% -> 42.98% +/- 1.4,
   i.e. nothing - as theory says for a ranker. Every failed decider experiment now has a reason: `mustBuild` (-4.7),
   the stopMargin dial (41.4-43.2), the random-data decider (18.9%, bad order), the policy-critic decider (37.3%, wrong
   price).
3. **TD in the current form does not solve the price**: with the seat/step link bug fixed (same seat, step + 1, exact
   `G - M` where the chain ends) the TD critic prices +1 income at 7.7 MC (9.3 / 7.5 / 6.4 by round) - better than the
   policy critic's flat 6.5, worse than the random critic's 12.1. Not adopted; the `tr +1` row reads as 0.3 MC because a
   remaining-margin value *subtracts* points already banked (convention, not a bug).
4. **What the legacy representation covers**: 82 per-card identity bits out of 349 project cards (blue 102, green 192,
   red 55), so tags for ~267 cards are invisible to the model unless they are counted. The owner's point that
   "the number of activatable cards is already implied by the identity bits" is right (a linear layer sums them), and that
   pair will be dropped at the next encoder change.

### Encoder 828 (in progress)

`TAG_SIZE = 2 x 11 + 2`: tag counts on the table per side and the activatable-card count per side (the last two to be
dropped). Tests: 9/9. Parity check with zero weights and an A/B on identical rows (`--drop-prefix tags:`) are running; the
verdict uses `ml/verify_critic.ps1` (price, ranking, decider, profile).

**Cost of an encoder change, measured:** every dataset is bound to the encoder's schema hash, so an encoder change discards
the data of the current cycle (the 50 000-game `tags1` set and the 20 000-game `randplay1` loop data both had to be
regenerated). Rule: batch encoder changes, then run one cycle.

### The track still stands

`docs/SYSTEMATIC.md`, section E: (1) TD - measured, not adopted; (2) the owner's loop (random -> 1-2 self-play iterations)
with a critic that keeps the price >= 10 MC while the ranking clears 39%; (3) the leaf representation and one recursion
level in the projection; (4) the legacy-block A/B.

## 2026-11-16, evening: state after the long session

### The best player

`r28_hand` (encoder **1090**) + **six critics**: `v_rev1_hand`, `v_ens_a_hand`, `v_ens_b_hand`, `v_ens_c_hand`,
`v_ens_d_hand`, `v_h_full`. Ranking the candidates of `BUILD_CARD`/`DRAFT_CARDS`, pairs on, symmetric score.
`D:/ares/runs/best.json` has the exact flags; `ml/rev_config.json` is the same for the nightly.

| | measured |
|---|---|
| vs SMART | **44.00% +/- 1.0** (10 000 games) |
| vs SMART, the same five critics without the block-trained member | 43.21% +/- 1.0 |
| profile | 13.5 cards per game, acceptance 0.61, phases 49/51, phase-3 spends 38% |
| sanity bench | **10/10**, 0 gate failures |

### The encoder (1090 values) and the tests

New since the previous handoff: tag counts on the table per side (22), hand identity bits for every playable card (268,
generated from `--mode cards` by `ml/gen_playable_cards.py`), the printed points of a card in the candidate vector, and the
*effective price* of the best card in hand. Removed as dead or redundant: the Luna Project Office and Project Inspection
counters (EXPERIMENTAL, 0.00000% of 200 000 rows) and the activatable-card counts (a linear function of the identity bits).

`ObservationCompletenessTest` (9 tests) now covers: the hand facts, every decision's menu, the printed points and price of a
candidate (recomputed from the card), every private column zero in the public view (this one caught a real hand leak), and
the double payment as one card's payment. `SanityBench` has a new gate: **the phase-4 double production is never declined**
(`GuaranteedActions.doubleIncome`, wired before the policy).

### What was measured and closed today

* **The price law**: the price a critic puts on +1 income is set by how much its data spends - 6.5 MC (our play, flat),
  9.0 (forced builds), **12.1 (uniform random, correct round profile)**, and rules say ~15. A critic trained on random data
  prices it right and ranks badly (22.8%); the mixture (ranker + decider + exploration) gives 9.6 MC and still ranks only
  37.9%.
* **The two roles**: a ranker needs the *ordering* (a shared slope cancels, the stop decision is the policy's), a decider
  needs the *price*. Every failed decider experiment now has a reason: 36.0% (policy critic), 37.3% (ensemble +
  projections), 33.3% (best-priced critic, no projections).
* **The better price does not help the ranker**: adding the best-priced critic to the ensemble took 44.00% to 42.86%.
* **TD on the remaining margin**: first version invalid (seats mixed, non-adjacent steps), fixed version prices income at
  7.7 MC - better than the policy critic, worse than random. Not adopted.
* **Batching**: implemented, measured *slower* (3.3 vs 5.2 games/s at 4 threads) and it exposed a scale bug (Vx1000 vs raw)
  that made pairs outrank batch scores; the path stays behind a flag. Throughput of the normal path: **14.9 games/s** at 12
  threads (100 000 games in ~1.9 h); the projection configuration is ~30x slower by construction.
* **A wrong claim corrected**: the double-payment block is alive (91.8% of rows); my "0.00000%" was an audit threshold
  artefact (values are divided by 10). A class-level cache for it was tried and reverted - the probe could not prove that
  `payAgain` ignores the table, and the test that compares the feature with the best single card caught a cached sum.

### Next, in order

1. the running test: the best-priced critic **with projections** (the closest reconstruction of NETWORK's mechanism),
   1 500 games;
2. if the decider still loses to the ranker: the ranker is the deployment, and the next lever is more data for its ensemble
   members (the hand bits are rare events and need volume), plus the loop at 100 000 games per iteration rather than 4-12k;
3. keep every new block behind the A/B protocol (`--drop-prefix`) and every new rule behind a sanity gate.

## STOPPED HERE (2026-11-16, power cut) - superseded by the section after it

> The tree is compiled and tested now, and the pending jobs were redone from the pack. Keep this block only as the record of
> what the power cut interrupted; the live state is in "2026-11-16, evening (after the power cut)" below.

Everything is committed. No process was left running; two jobs were killed mid-flight and must be redone or discarded:

| job | state | what to do |
|---|---|---|
| `v_scale_nohand` (critic on 150 000 games, hand block zeroed) | **killed during training**, the folder is incomplete | delete `D:/ares/runs/value/v_scale_nohand` and rerun the command in the experiment log |
| `v_scale_full` (critic on 150 000 games) | trained, AUC 0.8676, not yet audited or measured | run the audit and the ranking test |

Dataset present on disk: `D:/ares/data/scale1090` (150 000 games, 12.08 M rows, encoder **1090**) - the scale corpus for the
critic. Disk: ~280 GB free.

### The working tree state (uncompiled!)

The last commit contains **uncompiled** changes to the candidate representation:

* `CardFeatures`: candidate = `one-hot(268) + stop`; the 64-bit hash, colour, tags, price, points are gone;
* `CorporationsDecisions`: `one-hot(35)` + the chosen inputs; `CardSignature` deleted;
* `ObservationBuilder`: blue-card identity bits on both tables (`BLUE_TABLE_SIZE`), on top of the 1090 encoder;
* `ml/gen_playable_cards.py` now generates `ALL`, `BLUE`, `CORPORATIONS`.

Because candidate widths changed, **every policy head must be retrained** (the value models are unaffected: they read the
observation only). Until that is done, `--mode play`/`compare` cannot load the old policy bundle `r28_hand` with the new
classes. The bootstrap path: compile, `--mode names/schemas`, then generate a small corpus with **no `--models`** (the agent
falls back to uniform random, which still records rows with the new candidate vectors), CE a policy from scratch, and iterate.

### What is measured and stands

* best player: `r28_hand` + six critics = **44.00% +/- 1.0** vs SMART (10 000 games), sanity **10/10**;
* the price law, the two roles, the closed decider branch, the ensemble variance - in `docs/RESULTS.md`,
  `docs/EXPERIMENTS.md`, `docs/SYSTEMATIC.md`;
* the single rule added today: the phase-4 double production is never declined (`GuaranteedActions.doubleIncome`), with its
  sanity gate.

### The next three actions, in order

1. compile + `mvn test` (the candidate change touches every card menu) and run `--mode sanity`;
2. audit + rank `v_scale_full` (does 3x data improve the price or the ranking?);
3. bootstrap a policy on the new candidate vectors (random-play corpus, CE), then evaluate the six-critic ensemble.

## 2026-11-16, evening (after the power cut): the pipeline stops reading the HDD twice

Owner's rule for this session: *"all processes must be efficient, not just slightly reduce the data volume"* - the previous
run's bottleneck was the disk again (`v_scale_full` read 119.7 GiB per epoch off the HDD). Measured, then fixed.

### What was measured

* `D:` is a 2 TB Seagate HDD: **106-113 MB/s** through one `read()`, but only **48 MB/s** through `np.memmap` (a memmap
  faults one page at a time).
* `C:` is a 465 GB Samsung 980 PRO SSD with 164 GB free.
* In the 119.7 GiB corpus, **76-96% of a row is the candidate block padded to the menu maximum** (BUILD_INPUT: 7 real
  candidates padded to 200; BUILD_CARD: 3.7 padded to 81). A value model never reads a byte of it.
* `v_scale_full`: 2406 s for 4 epochs, best AUC 0.8676.

### What was built

* `ml/pack_dataset.py` - one sequential pass writes per decision type: `obs.f16` + `lab.npz` + a cross-type
  de-duplication mask computed **once** (it used to be a 12-million-key Python set rebuilt on every start). With
  `--candidates` it writes the real candidates only.
* `ml/pack_check.py` - the gate: proves the pack is bit-exact to the raw chunks (obs, every label, every candidate, ids,
  zero padding). Run before training on any pack.
* `train_value.py --packed`, `train_phase.py --packed`, `train_all.py --packed`, `train_policy_ce.py --packed`, and the
  nightly now packs each data window automatically (`ensure_pack`) and falls back to raw chunks if the pack fails.
* `docs/PIPELINE_IO.md` - the numbers, the format and the rules (one pass, only used bytes, no re-reads).

### Result on the same corpus, same flags

| | `v_scale_full` (raw chunks) | `v_scale_packed` (pack) |
|---|---|---|
| wall time, 4 epochs | **2406 s** (40 min) | **79.8 s** |
| best AUC (margin target, 2.4 M rows, 16/game) | 0.8676 | 0.8736 (same seed, the caps differ slightly) |
| disk read by the trainer | 4 x 119.7 GiB (HDD) | 24.5 GiB once from the SSD, then 4 990 MB into RAM in 5.6 s |
| pack build (once) | - | 1500 s, 24.5 GiB, 1 047 208 duplicate rows dropped |

`mvn test` is green (18/18: `ObservationCompletenessTest`, `OpeningDecisionsTest`), the tree compiles, and `CardSignature`
is gone: card candidates are `one-hot(268) + stop`, corporation candidates `one-hot(35) + chosen inputs`.

### The hand question, answered with a number (2026-11-16, later)

The owner objected to "the hand gives the critic nothing": a hand is held to be played later, in order, and the first A/B
averaged over rows where it cannot be played. `ml/hand_ab.py` repeats it on playable hands only (>= 2 cards, >= 1
affordable), paired by rows/seed/split, error bar by a game-level bootstrap. Result: the 268 identity bits **hurt** the
critic - +0.00193 loss [+0.00059, +0.00329], AUC 0.8870 vs 0.8888. The same run exposed a real defect: the standardisation
sample was the first 200k rows of the pool (the opening, where the hand block is empty), so `train_value.py` now samples the
training rows uniformly (`--stats random`, the default) and both arms get ~0.0008 better for free.

Conclusion for design: the critic is the wrong place to convert "which cards I hold" into strength. A one-ply afterstate
value cannot express "play the income card now because the tag card is next". The hand belongs to the **policy** (its label
is literally the card taken, given the hand) and to any multi-step search. Nothing was removed from the observation, and the
deployed critic is unchanged until a ranking-role test (10 000 games) says otherwise.

### The critic, changed on measurement (2026-11-16, night)

Owner's permission: "change the critic as you see fit". Two changes, both with a reason and a gate:

* **A dropped block is now dead in the exported model.** `--drop-prefix` used to zero the block only in the training rows;
  the first-layer weights of those columns never receive a gradient and keep their random initial values, and the engine
  feeds the real values at play time - a silent noise injector. `train_value.mask_features` zeroes the weights and the
  normalisation, `main` refuses to export a model that reacts to the block (measured 0.0 on 268 dropped columns), and
  `ml/tests/test_train_value.py` pins the contract. Without this, no block ablation could ever become a deployment.
* **The encoder is 1256 wide, every scale critic is 1090.** The blue-table identity bits (83 per side) were added after the
  150k-game corpus. Those critics are critic science, not deployable models. Everything must be retrained on a 1256 corpus.

The first 1256 corpus is being generated now: `D:/ares/data/boot1`, 20 000 games, **no `--models`** (uniform random), which
is the policy bootstrap. Next: pack it with `--candidates`, `ml/critic_ablation.py` on it (which of the eight blocks pays
on the current representation), `train_policy_ce.py --packed` for the bootstrap policy, then self-play with that policy and a
new critic; only then is the ranking-role measurement (10 000 games) possible.

### Where the bootstrap stands (2026-11-16, late)

Done and committed:

* `C:/ares/pack/boot1` - 20 000 random games, encoder **1256**, packed with candidates, verified bit-exact;
* `D:/ares/runs/value/v_boot1_mask` - the first deployable 1256 critic (hand identity masked, 268 columns, invariance gate
  0.0, AUC 0.8839);
* `D:/ares/runs/policy/boot1` (plain CE) and `boot1_adv` (bounded advantage reweighting) - both mechanically fine, both
  worthless alone: the greedy argmax stops on 91.5% / 86.1% of BUILD_CARD rows and builds 0.0 cards per game;
* `ml/check_player.py --search --value-model v_boot1_mask` on that same policy: **9.97 cards per game**, acceptance 0.29.
  The search decides build-vs-pass, the policy only ranks. That is the mechanism the bootstrap continues with.

Running: `D:/ares/data/boot2` - 5 000 games played with exactly that configuration (search on BUILD_CARD/DRAFT_CARDS over
`v_boot1_mask`, `boot1_adv` elsewhere).

### Morning of 17 November: the agent is rebuilt, and it works

| | value |
|---|---|
| start of the night (random policy + search) | 3.2% +/- 1.1 vs SMART |
| end of the night, best iteration (1 000 games) | 9.85% |
| **final bundle, 10 000 games** | **10.28% +/- 0.60** (`policy/night16` + three critics `v_night16_s1..s3`) |
| the same policy with one critic, 10 000 games | 8.63% [8.10, 9.20] |
| profile (acceptance gate) | phases 11/26/16/42/4%, phase-3 pass 58%, std projects 19%, **24.6 cards per game** |

Everything is in `docs/RESULTS.md` ("The night of 16/17 November"), `ml/night_loop.py`, `ml/night_report.py`,
`ml/ensemble_check.py`.

### What is left, in order of expected value

1. **The build/decline balance, with the sign measured instead of assumed.** Card counts per game (scorecard averages):
   NETWORK 45.4, SMART 34.4 (vs NETWORK) / 31.8 (vs us), our agent 24.2. We build the least, so "more cards" is the
   direction, not a defect; there is no healthy band, and NETWORK is a clue, not a standard (owner, 2026-11-17).
   `-Dares.agent.explore` randomises a share of build decisions and therefore adds declines - it must be read on cards per
   game AND both win rates together.
2. The blue-table re-test on policy-play data (3 seeds) - it was negative in sign on all three seeds of the random corpus
   but the interval excluded zero in only one.
3. Data scale on the new encoder: the corpus of the night is 40 000 games; the old critic wanted 150 000. The pack makes a
   150k-game critic a 2-minute run once the games exist (about an hour of self-play at the measured 24 games/s).
4. The producer still writes candidate padding: 27.6 GB per 20 000 games of which 4.2 GB is observation
   (`docs/PIPELINE_IO.md`). Emitting the compact form from the simulator is the next efficiency item, not a model item.

### Bootstrap recipe (candidate widths changed, no policy can be loaded)

```powershell
# 1. a small corpus with NO --models: the agent falls back to uniform random, and the rows carry the new vectors
$cp = (Get-Content target\cp.txt -Raw).Trim()
& $java -Xmx10g -cp "target/classes;$cp" com.terraforming.ares.selfplay.SelfPlayMain --mode play `
    --out D:/ares/data/boot1 --games 20000 --threads 14 --games-per-chunk 3000 `
    --matchups "AGENT:AGENT:0.85,AGENT:RANDOM:0.15" --max-rows-per-game-type 8
# 2. one sequential pass, candidates without padding
py -3 ml/pack_dataset.py --data D:/ares/data/boot1 --out C:/ares/pack/boot1 --candidates
py -3 ml/pack_check.py --pack C:/ares/pack/boot1 --data D:/ares/data/boot1 --limit-chunks 2
# 3. CE from scratch, then self-play with the new policy and repeat
py -3 ml/train_policy_ce.py --packed C:/ares/pack/boot1 --out D:/ares/runs/policy/boot1 --epochs 3
```

Note on `--mode sanity`: it runs **without a policy model** right now (the old bundle no longer loads after the candidate
change), so gates that require one specific candidate are coin flips. The last run was 8/10 with one gate failure
("one-credit card draw: stop"), which is the random agent, not a regression.

## Night of 17/18 November: the score-decomposition experiment (owner: no more 20%)

Owner's instruction before going to sleep: verify whether the score-channel theory gives an effect; if it does not, dig into why
the network does not learn, check every two hours, and do not spend five two-hour runs doing the same thing.

### What is running

`train_breakdown.py` on a fresh 40 000-game corpus (`C:/ares/data/bd40`) whose RANKING sidecar now carries the game's own
`ScoreBreakdown` per seat. The critic predicts the seven-term difference `d = b[me] - b[opp]` (terraforming rating, forests,
printed card points, points from resources on cards, milestones, awards, extra), and the ranking value is `V = sum(d)`, which
is exactly the margin: verified on 40 packed game pairs. The seven heads are collapsed into one output at export
(`--export`), so the Java loader and the deployment path are untouched.

Why this and not an auxiliary target per feature: the margin is the sum of these seven channels, so they are not a hand-picked
list - they are the game's own definition of victory. A card that adds two printed points raises its channel by two in every
game, won or lost, so the channel is learnable even where the outcome is dominated by tempo.

### Decision tree (each step has a falsifier; do not repeat a failed step)

1. **Breakdown critic measured**: card-value correlation (target > 0.2; it was 0.19 with the margin target and 0.074 on the
   poisoned night data), `dV per victory point` (target near 0.05; it was 0.0003), and 10 000 games against SMART.
   - score > 22% -> adopt, continue iterations with the breakdown critic;
   - card correlation > 0.25 but score flat -> the critic is fine and the **policy** is the bottleneck: next lever is the
     policy's imitation (search choices, advantage weighting), not the critic;
   - correlation still <= 0.2 -> the target is not the problem: next hypotheses in order are (a) volume/capacity (150k games,
     1024 wide, dropout), (b) the observation does not carry "what this card will do" (the owner's "blue engine" point).
2. **If the critic stays blind to scoring**: test the opposite direction - the *search's stop rule*. Measured earlier:
   `mean V(best build) - V(stop) = +262 logit` and the agent builds 24-30 cards per game; if the build-vs-pass comparison is
   systematically off, no card ranking can fix the score.
3. **Fallback that needs no critic at all**: `ml/card_advantage.py` (per-card advantage from our own games, split-half stable)
   as a CE reweighting of the card choice - it is a statistical table of our own games, so it must be measured against the rule
   that forbids hand-written strategy before it is used.

### Housekeeping for the morning

- the ledger of the last loop run: `D:/ares/tmp/night_ledger.json`, the plateau report `plateau_1117-2001.md`;
- best measured bundle so far: 19.55-20.1% vs SMART, 77% vs RANDOM (`policy/1117-2001_11`, `v_1117-2001_11_s1..s3`);
- disk after cleanup: ~100 GB free on C:, ~350 GB on D:; only one pack is kept.

## Morning report, 18 November: the 20% was a blind spot of the search, not a property of the network

### The number

**19.55% -> 47.6% [45.8, 49.4] vs SMART, 3000 games.** Against RANDOM: 77.2% -> **92.75% [91.0, 94.2]**, 1000 games.
Nothing was retrained; every weight is the bundle of 17/11 evening (`policy/1117-2001_11`,
`v_1117-2001_11_s1..s3`). The only change is *which menus the critic ranks*.

| menus the critic ranks | vs SMART, 3000 games |
|---|---|
| BUILD_CARD, DRAFT_CARDS, PICK_PHASE (the bundle as it was) | 20.2% [18.8, 21.7] |
| + STANDARD_ACTION, PERFORM_BLUE_ACTION | 27.9% [26.3, 29.5] |
| + DISCARD_CARDS, SELL_CARDS, SELL_CARDS_LAST_ROUND | **47.6% [45.8, 49.4]** |

`OnePlySearch.applyOn` already applied all five added types to a game copy. 13 of 16 decision types were decided by the
policy with no lookahead; the five that can be applied are the ones that were missing, and each is an *immediate*
consequence - an ocean, a temperature step, a card out of the hand - not a promise of points in twelve generations.

Exact flags:

```
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND
-Dares.agent.search=oneply -Dares.agent.search.symmetric=true -Dares.agent.search.pairs=true
-Dares.agent.valueModel=<the three critics> -Dares.agent.valueModel.phase=<the first>
```

### Where the points come from, by the game's own channels (300 games)

| channel | before | after | SMART |
|---|---|---|---|
| resource cards (animals, microbes) | 9.87 | **15.97** | 6.57 |
| printed card points | 8.93 | 10.75 | 17.12 |
| terraforming rating | 30.64 | 33.90 | 33.11 |
| forests | 8.33 | 10.01 | 9.11 |
| milestones / awards | 4.19 / 10.29 | 4.45 / 10.87 | 4.42 / 10.26 |
| **total** | 72.25 | **85.95** | 80.59 |

Builds 34.0 against 32.9. So the engine does work once it is paid for: the sell menus were the money. `SELL_CARDS` had the
policy answering "stop" in 100% of the rows (a known, allowlisted collapse in `ml/action_coverage.py`), and the end-of-round
sale is worth ~110 MC per game; the critic only has to answer "which card do I miss least", which it can read off the hand
summary. The money became a 34-card game and a live resource-card engine.

Profile gate (`ml/check_player.py`): builds 27-30 per game, phases 26/21/23/15/15%, standard projects in phase 3 = 16.6%.
Not degenerate; it got better against RANDOM too, so it is not a denier of one scripted opponent.

**Transfer to the stronger benchmark, 1000 games, same bundle:** vs NETWORK `0.9%` with three menus -> **`15.75%
[13.6, 18.1]`** with eight. A 17x jump on an opponent that was never the one being denied, which rules out "it learned to
spoil SMART's script". This is the number to quote for strength; SMART is now the easy half.

### Iteration on the new menus: 47.6% -> 57.45% vs SMART, 15.75% -> 32.35% vs NETWORK

The night loop's first attempt died on its own fixed 7200 s generation limit with 18 772 of 20 000 games written (eight
ranked menus generate at 2.6 games/s, `docs/PIPELINE.md`). The corpus was packed as it stood and the iteration was run by
hand (`pack_dataset -> pack_check -> 3 x train_value --target margin -> train_policy_ce`), the same commands the loop uses:

| bundle | vs SMART (3000) | vs NETWORK (1000) |
|---|---|---|
| `policy/1117-2001_11` + `v_1117-2001_11_s1..s3` (old search config) | 20.2% [18.8, 21.7] | 0.9% |
| same weights, eight menus | 47.6% [45.8, 49.4] | 15.75% [13.6, 18.1] |
| `policy/1118-0058_13` + `v_1118-0058_13_s1..s3` (one iteration on 18.7k games) | **57.45% [55.7, 59.2]** | **32.35% [29.5, 35.3]** |

Critic AUC on the new corpus 0.913-0.917 (was 0.895). Channels, 300 games: resource cards **20.23** against SMART's 7.03,
forests 12.29 against 8.54, printed points 11.67 against 17.03, TR 29.2 against 33.44, total 88.2 against 81.14, builds
33.2 against 31.9. The engine channel is now three times SMART's; printed card points are the only channel still behind.

Resolved: the scorecard's 49.67% was a 300-game artefact. At 1000 games it reads **55.9%** and a score of 91.11 against
79.40, which agrees with the compare interval. Use 1000 games as the scorecard's minimum, not 300.

### The bundle to use, and the exact flags

```
policy:  D:/ares/runs/policy/1118-0058_13
critics: D:/ares/runs/value/v_1118-0058_13_s1,D:/ares/runs/value/v_1118-0058_13_s2,D:/ares/runs/value/v_1118-0058_13_s3

-Dares.agent.search=oneply
-Dares.agent.search.symmetric=true
-Dares.agent.search.pairs=true
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND
-Dares.agent.valueModel=<the three critics>
-Dares.agent.valueModel.phase=D:/ares/runs/value/v_1118-0058_13_s1
```

The previous bundle (`policy/1117-2001_11` + `v_1117-2001_11_s1..s3`) is kept and still scores 47.6% with the same flags, so
a regression there is a one-line rollback. Do not quote a scorecard run under 1000 games: at 300 games it read 49.67%
where 1000 games read 55.9% and 3000 games read 57.45%.

### Why the card menus are still the next lever, and what the data says about them

`ml/card_points_data.py` on 40 000 games (320 000 build decisions):

```
menus offering a card with >= 2 printed points:  8.9%
took it:     margin +8.54, win 62.3%
took other:  margin +8.82, win 62.6%    <- no difference
correlation(picked points, margin): +0.042
```

A critic trained on the outcome is *correct* to price printed points at zero: the corpus does not contain the fact, and
point cards are barely reachable because the draft does not take them. This is the silent-decay pattern one level up - the
training target is collapsed, not the network. Volume and capacity were never the binding constraint; do not spend a night
on 150k games before this is fixed.

The seven-channel critic was the first attempt at this and is rejected in `docs/RESULTS.md` (each channel lands at its own
variance). The untested ones, cheapest first:

1. **Consolidate the wide menus in the loop** - one iteration with the eight types above, so the policy learns these
   choices instead of only imitating the narrow search. This is what the night run after this report does.
2. ~~`train_value.py --target myscore`~~ - **measured and rejected (18/11, 05:30)**: it learns the score (val MSE 0.0373
   against 0.0706 for a constant) and still prices a printed point at 0.00012, i.e. 1% of its value; correlation with
   printed points 0.089. Three targets have now failed to create a card signal (outcome 0.19, seven channels 0.12, own
   score 0.09). Stop trying targets.
3. **Exploration on DRAFT_CARDS** - 8.9% reachability is the measured reason; the generator's own rule is that a collapsed
   option is fixed by exploring that type, not by more seeds.

### Housekeeping

- bench ledger: `D:/ares/tmp/night_ledger.json`; plateau report of the previous run `D:/ares/tmp/plateau_1117-2001.md`.
- `ml/train_breakdown.py` (seven channel heads) is kept as a rejected experiment, not part of the pipeline.
- `night_loop.py` ledger rows now record `types` and `value_scale`, because those two now change the meaning of a score.
