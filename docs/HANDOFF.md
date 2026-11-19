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
| the same, on the cap-fixed build (final) | **56.23% [54.5, 58.0]**, 0 failed | **34.53% [31.7, 37.5]** |

Also measured: RANDOM **92.80% [91.0, 94.2]** on 1000 games, and one iteration on only 7 998 games scored 45.1% [42.0, 48.2]
- i.e. shrinking the corpus to fit the night makes the player worse. 20 000 games per iteration is the floor.

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

### The four menus still decided by the policy alone (owner's list, 18/11)

`BUILD_PAYMENT`, `BUILD_INPUT`, `BLUE_INPUT`, `PICK_CORPORATION`. All four are applied by an existing call the live agent
already makes, so each is a matter of asking the search instead of the policy:

| menu | mean menu | how the afterstate is produced | status (3000 games vs SMART, bundle `1118-0058_13`) |
|---|---|---|---|
| BUILD_PAYMENT | 1.4 (p99 10) | `buildGreen/BlueRedProjectSync(copy, me, card, plan.payments(), input)`; the live build holds the card, so `choosePayment` is called from there | **done**: 57.85% [56.1, 59.6] against 56.23% [54.5, 58.0] |
| PICK_CORPORATION | 6.0 | `PickCorporationProcessor.processTurn(...)` is already synchronous; `CorporationChoice` carries the on-built input | **done**: 58.00% [56.2, 59.8]; Zetacell's forced discard is now settled (below) |
| both of the above | | | **59.08% [57.3, 60.8]**, 10 000-game confirmation running |
| BLUE_INPUT | 7.4 | `blueInputDecisions.fillRanked` hands each slot's partial input to the search, which applies the candidate, lets the policy fill the rest and performs the action on a copy | implemented, not yet measured |
| BUILD_INPUT | 5.1 | same shape, but the slots form a sequence (phase upgrade / tag / where the animal goes) | not implemented |

The corporation afterstate is now **settled**: Zetacell draws five and forces a discard of four, and the engine applies
that turn asynchronously during the corporation phase, so on a copy it never happened and V saw five extra cards and none
of the four given back. `OnePlySearch.settleQueuedDiscards` plays the discards out with the policy (the four it keeps are
not the critic's business yet; the point is that the position is the one Zetacell really leads to).

MayNi needed nothing: `BuildDto(GREEN_OR_BLUE, 12, 12)` is a build allowance, and the observation already encodes build
slots per colour (`ObservationBuilder.builds`), so V sees it. Every corporation with `onBuiltEffectApplicableToItself()`
(Austellar, Apollo, Exocorp, Hyperion, NebuLabs, Modpro, Sultira) has its legal inputs described in
`CorporationDecisions.templates`, so the corporation menu cannot throw for an unlisted card; the buffed corporations need
no input. Hyperion's template is `phaseUpgrades(4,5)` = the two upgrade slots of phase 3, which is what its own text says.

`BUILD_PAYMENT` is first because it is the cheapest (a build usually has one legal plan) and the only one that is literally
money: restructured/anaerobic/heat plans buy the same card for less.

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

### Test state and the one code fix the night needed

`mvn test`: 125 tests, 11 red, none a regression - 3 need an AFTERSTATE row the recipe does not record, 1 expects the old
1044-column mask, 7 are `MlpPhaseModelTest` fixtures trained on 780-value observations (the encoder is 1256). `docs/BUGS.md`
has the list.

`DecisionLogs` is a singleton and `abort()` kept its ranking sidecar open; now that every game writes a breakdown line into
that sidecar, the next recording renamed a path from the previous directory and threw. Fixed with `releaseRankings()` in
`abort()` and `startRecording()`; `DecisionLogsTest` is 9/9.

### Playing against it yourself (owner, 18/11)

The app reads the bundle from JVM properties, so the only thing a normal run needs is the VM options. In IntelliJ: Run ->
Edit Configurations -> VM options, or on the command line:

```
mvn -s .mvn/settings.xml spring-boot:run -Dspring-boot.run.jvmArguments="\
-Dares.agent.models=D:/ares/runs/policy/1118-0058_13 \
-Dares.agent.search=oneply -Dares.agent.search.symmetric=true -Dares.agent.search.pairs=true \
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN,UNMI_RT,COLLECT_INCOME \
-Dares.agent.valueModel=D:/ares/runs/value/v_1118-0058_13_s1,D:/ares/runs/value/v_1118-0058_13_s2,D:/ares/runs/value/v_1118-0058_13_s3 \
-Dares.agent.valueModel.phase=D:/ares/runs/value/v_1118-0058_13_s1"
```

Then pick the **AGENT** difficulty in the UI (`AGENT_V2` reads `-Dares.agent.models2`, a different set). Without
`-Dares.agent.models` the agent is uniform random - that is the documented default, so the options above are what make it
play as the trained bundle. Every menu is now under the search, so a decision costs ~15-20 ms; the UI stays responsive.

### Full coverage: 15 of 15 decision types

`MULLIGAN`, `UNMI_RT` and `COLLECT_INCOME` were the last three without a critic. What each needed:

- **MULLIGAN**: `MulliganTurnProcessor` does not only remove the cards - it deals as many new ones. So the afterstate of
  "throw this card away" is the same hand size with a random replacement, and that is what `applyOn` now does. The
  mulligan was declined in 100% of the recorded rows, which is exactly what a search that saw a smaller hand would decide.
- **UNMI_RT**: +1 TR for 6 MC, applied by the existing synchronous `unmiRtCorporationTurnSync`.
- **COLLECT_INCOME**: the phase-4 double produce; the turn is asynchronous, so the engine's `CollectIncomeTurnProcessor`
  is called directly on the copy.

### Coverage is not the same as deciding: `rankOnly` keeps the policy's "stop"

In `OnePlySearch.search` the default is `rankOnly = true`: the critic only **re-orders** the candidates, and when the menu
has a stop option the **policy still decides whether to act at all** (`if (policy < 0 || options.get(policy).id() == stop)
return policy;`). That is deliberate - a build's afterstate is off the distribution V was trained on - but it means that for a
menu whose policy always says "stop", adding it to `types` changes nothing:

- `MULLIGAN` is declined in 100% of the recorded rows, so a rank-only mulligan is a no-op;
- `SELL_CARDS` had the same collapse, which is why the earlier ablation found the sell menus worth ~0 *at play time* even
  though adding them moved the old bundle by 20 points (there the menus were the teacher, and the policy learned them).

The knob for those is `-Dares.agent.search.fullTypes=TYPE,...`: those types are scored against the stop candidate as well, so
the critic decides "act or not". The afterstate of a mulligan or a sale is a normal game state, so that comparison is fair;
it is the build menus where it is not. The measured list to try is `MULLIGAN`, `SELL_CARDS`, `UNMI_RT` - each one is a menu
where the policy's own stop is the known collapse.

### Anti-narrowing plan (owner's question, 2026-11-18)

The owner's own NETWORK was also trained over several self-play iterations and it **lost universality**: it converged to one
narrow line that beat its previous self on average. That is the failure mode this project calls "self-training decays
silently", and it has already been seen here in miniature: phases 20% -> 7% -> 2.2%, the mulligan declined in 100% of rows,
the in-phase sale at 100% stop, and the critic's card signal dropping to 0.074. What exists against it: RANDOM at 15% in the
generation pool, absolute benchmarks with intervals every iteration, the profile gate (builds, phase mix, standard projects),
the per-type coverage detector with its allowlist, the three-critic ensemble, and `rankOnly` (the critic re-orders a menu
instead of dictating one line). What is missing, in the order it should be built:

1. **Replay across iterations.** Training sees only the newest corpus, so the moment a style is abandoned the data that
   taught it is gone and nothing pulls back. Train each iteration on a mixture of the last K packs (the loop already keeps
   three). Needs `load_packed` in `ml/train_value.py` to accept a comma-separated list of packs and concatenate per type -
   every pack of one encoder has the same schema, so concatenation is valid. Falsifier: with replay, the drop of a profile
   dimension across iterations must be smaller than the same drop without it, at equal games.
2. **A strategy-diversity sensor** - **done** (`ml/strategy_mix.py`, 6 tests). A player-game is labelled by the phase it
   leaned on and the game channel its points came from ("phase2:tr", "phase5:resourceCards"), and the report prints the top
   profile's share and the effective number of profiles (exp of the entropy). Baseline on the 18.7k corpus: **17 profiles,
   top 29.2%, 5 above 5%, effective 6.27** - a mixed corpus, not a single plan. Read across iterations as a trend, not
   against a threshold (there is no known-good number). Clustering was rejected for this: k groups always partition into k
   groups, so an 80/20 corpus reported 29% with k=4.
3. **Acceptance on the worst benchmark, not the best.** SMART encodes the owner's heuristics, so a player can be tuned to
   beat that one style; today a run is accepted on `max(spread)` implicitly. Report and gate on
   `min(SMART, NETWORK, RANDOM)` alongside the profile gate.
4. **A cycle detector.** Every iteration, play the newest bundle against the bundle from two or three iterations back,
   head to head. A beats B, then A' beats A only on one line - that is a cycle, not progress, and it is invisible in the
   absolute numbers.
5. **Make act-vs-stop decidable, in this order** (prerequisite for the two dead menus: the mulligan discards 0 cards in
   18 000 games, the in-phase sale answers stop in 100%): train V on `AFTERSTATE` rows (`--include-afterstates`, the rows
   and the pack support already exist) and add one "this is an afterstate" bit to the observation, so `V(state)` and
   `V(after action)` share a scale; then, for menus that involve the hand, **the policy picks the card and the critic only
   gates act-vs-stop** (measured: letting the critic pick and gate gave 2.88% vs SMART, because its hand block is masked by
   design and it cannot see which card is junk). Falsifier: `fullTypes=MULLIGAN` must stop collapsing, the mulligan sensor
   must show a non-zero discard count, and the win rate must not drop; then 10 000 games.

Order: 1 and 2 immediately after the current retraining, then 3-4, then 5 (which also unlocks the act/stop gate that the
`fullTypes` experiment needs).

### Speed plan: where the three hours go, and what is measured first (owner, 18/11)

An iteration is **~3.1 h generation and ~15 min for everything else** (packing, three critics, policy, measurement):

| phase | time | share |
|---|---|---|
| 20 000 games | ~180 min at 1.8-2.6 games/s | ~93% |
| packing | ~1 min | <1% |
| three critics | 20 s each (longer with replay) | ~2% |
| policy CE | ~1.7 min | 1% |
| measurement (1000 SMART, 500 RANDOM, scorecard, profile) | ~12 min | 4% |

Inside one decision (~10 ms) - **measured**, not estimated (`SpeedBreakdownTest`, plus the in-game log):

| part | per candidate | share |
|---|---|---|
| copy the game | 8.7 us | 5% |
| build the observation (mine; the symmetric opponent view doubles it) | 81.6 us early game, 236 us mid game | 44-36% |
| three critics | 96.9 us early game, 139 x 3 = 417 us mid game | 52-64% |

So the old guess that the game copy dominates was wrong by two orders of magnitude: **the neural part is the largest term**,
the observation second, the copy irrelevant. The levers, in the order they are measured:

1. ~~A/B with `-Dares.agent.search.batch=true`~~ - **measured and rejected (18/11)**: 5x slower (49.1 against 9.4 us/row)
   and not bit-equal to the single-row path (a different float association order). `ValueBatchParityTest` pins both, so it
   cannot be re-enabled as an "optimisation". See `docs/RESULTS.md`.
1b. ~~Hand-unrolling the critic's matvec~~ - **measured and rejected (18/11)**: every variant is 2-3.5x slower and the
   j-unrolled ones are not bit-identical. `MlpValueModel.logit` already skips zero features, pre-accumulates the zero
   baseline and keeps the inner loop contiguous. CPU-side neural work is exhausted; do not retry.

1c. **Paired observation build** - the largest lever now measured: 62% of a candidate is one observation built twice
   (mine + the opponent's public view, `symmetric=true`). `buildPair(game, me, opponent)` builds both in one pass, sharing
   the seat-independent blocks, gated bit-exactly against the two separate builds. Do this before the GPU work.

2. **Batched inference service (branch `batcher-experiments`)**: adapted from the owner's `PolicyNNService` on `policy_ai`
   - a queue, one batcher thread, up to BATCH_ROWS rows per forward, 50 ms backfill, reusable flat arrays, futures, one
     ND4J/CUDA forward per critic per batch. The plugin point is `OnePlySearch.scoreBatch` (a `ValueScorer` interface), so
     no per-decision-type hacks. Tests: queue order/chunking/exception propagation, batch-vs-single argmax equality on 100k
     rows (bit-exactness is physically impossible across GEMV and GEMM kernels; the *observation* stays bit-exact).
3. **Incremental observation** (the big one): sections with declared data dependencies, recompute only the dirty ones,
   gated by a differential test that compares against the from-scratch build **bit-exactly** on real corpus decisions of all
   15 types. That gate is the whole point: a representation that silently loses a bit is worse than a slow one.

Expected ceiling, from the measured split: making the critics free (GPU batched across games) roughly halves the
per-candidate cost, and halving the observation on top of that gets to ~3.5x. A 1.7-2x generation speedup is the realistic
first target. Nothing here changes the player - it buys iterations per night.

One more contained idea, measured before the big work: with `symmetric=true` every candidate builds **two** observations
(mine and the opponent's public view), but most actions cannot change the opponent's public state at all - a private card
build, a sale, a mulligan. A declared "does this action touch public state" predicate per decision type would skip the
second build for the private ones, with the same bit-exact differential test as the incremental observation.

### The legacy encoder block: 33% of the columns, and a schema change to drop most of it

`ObservationBuilder` copies 1256 values: 839 columns of the current encoder plus **417 legacy columns** produced by
`baseEncoder.collectData(...)` only because NETWORK_V2 was trained on them. Inside the 417 are the milestone and award
features (40 + 18 columns) whose value was measured (dV 0.081 per unit of milestone progress, `ml/progress_check.py`), so the
block cannot simply be dropped.

Plan, in the order that costs least first:

1. **(running)** measure `-Dares.agent.search.symmetric=false`: if the opponent's public view is not worth its 31% of the
   per-candidate cost, deleting it is free. 3000 games against SMART on the champion bundle.
2. **Quality check, cheap**: train a critic with `train_value --drop-unprefixed` (masks every column without a `:`) on the
   current pack and compare AUC and `ml/card_value_check.py`. If the masked critic is as good, the legacy columns carry no
   signal for the critic, and the schema change is justified.
3. **Schema change** (only if 1 and 2 say yes): stop producing the legacy columns except milestones and awards - 1256 -> ~897
   (-29% columns, which also removes ~29% of the first layer's FLOPs, the layer that is 84% of the critic's work). Cost: every
   pack becomes incompatible, so it needs one full corpus regeneration plus retraining both the critics and the policy. That
   is a whole night, so it goes in only after 1 and 2.
4. **Independent of all of the above**: the opponent view can be built **once per menu** for the hand menus (mulligan, sale,
   discard, draft), where every candidate changes only private state - 31% saved on ~40% of the decisions, gated by a test
   that the public view is byte-identical across the candidates of those menus.

### Hall of fame: promotion against our own champions (owner's idea, 18/11)

`ml/hof.py` keeps a champion list with, per entry, the policy bundle, its critic ensemble **and its search configuration**
(a bundle without its menu list is meaningless - the list alone moved a bundle by 40 points). A candidate is promoted only
through three gates, cheapest first:

1. **Absolute**: SMART and RANDOM within 1.5 points of the champion. A candidate may not buy a head-to-head edge by giving up
   absolute strength.
2. **Head to head against the current champion**: 2000 games, at least 54%, and the interval must clear 50%. 2000 games give
   +-2.2 points, so 54% is separable.
3. **The pool**: against the last three champions, no loss below 45%. Duels are cached per unordered pair, so each is played
   once and a new iteration adds one row.

The point of gate 3 is the owner's NETWORK lesson: a candidate that beats yesterday's self can be a specialist. Beating the
recent pool is what "not specialising" looks like when only our own models may be opponents. SMART and NETWORK never enter a
duel as opponents; they are benchmarks only.

Mechanically a duel needs no engine change: `--mode compare --a AGENT --b AGENT_V2` reads `-Dares.agent.models` for one side
and `-Dares.agent.models2` for the other, so two policy bundles play each other with the same search flags
(`ml/hof.py duel`, verified end to end: 100 games, 0 failed). Until `-Dares.agent.valueModel2` exists both sides are judged by
the same critic ensemble, so a duel is a **policy** comparison; the entry records that.

Seven unit tests (`ml/tests/test_hof.py`) pin the gates: a real regression is refused, a small dip is allowed, a duel needs
both the estimate and the interval, a missing number fails closed, the pool requires the recent duels, and an existing
champion is never overwritten. Champion seeded: `1118-0058_13` (63.93 / 92.8).

Next for this thread: `-Dares.agent.valueModel2` so each side carries its own critics, then the league corpus
(`--matchups AGENT:AGENT:0.7,AGENT:AGENT_V2:0.15,AGENT:RANDOM:0.15`, where `models2` is a sampled old champion).

### Reading a night, and accepting a bundle

One ledger row (`D:/ares/tmp/night_ledger.json`) now carries everything needed to decide whether an iteration is progress:

| field | meaning |
|---|---|
| `score`, `ci` | SMART, `games` games, with the interval |
| `score_random`, `score_network` | the floor and the second absolute benchmark |
| `worst_benchmark` | `min` of the three - **this is the number the acceptance rule reads** |
| `builds_per_game`, `phases`, `card_points` | the profile gate: a player that stops building or lives in one phase is degenerate regardless of its win rate |
| `stop_share` | how often the policy declines a build offer (a collapse shows here first) |
| `types`, `value_scale`, `generation_symmetric`, `record_afterstates`, `critic_afterstates`, `policy_replay`, `policy_adv`, `explore` | the configuration, because a score without it cannot be compared |
| `policy`, `critic` | the bundle, for the rollback and for the hall of fame |

Acceptance order (cheapest first, `ml/hof.py` implements the last two):

1. **Profile gate**: builds, phase mix, printed card points. Degenerate -> reject without looking at the win rate.
2. **Absolute**: SMART, RANDOM, NETWORK each within 1.5 points of the champion (`worst_benchmark` is the one to quote).
3. **Head to head vs the champion**: 2000 games, at least 54%, interval clear of 50%.
4. **The pool**: against the last three champions, no loss below 45% (a specialist fails here even if it passed 3).

A candidate that passes 2-4 is promoted with `ml/hof.py promote`, and the promotion must be re-measured at 10 000 games if the
margin over the champion is under 1.5 points.

### Known follow-up: the parent menu still completes with the policy

`PERFORM_BLUE_ACTION`'s afterstate (`OnePlySearch.applyOn`) fills the blue inputs with `innerChoice`, i.e. the policy, and
`BUILD_CARD`'s afterstate (`applyBuild`) fills the build inputs and the payment plan the same way. Now that `BLUE_INPUT`,
`BUILD_INPUT` and `BUILD_PAYMENT` are ranked at execution time, the two are inconsistent: the parent compares candidates as
if the policy would finish them, and then the ranking finishes them differently. Making the parent use the ranked
completion is correct but multiplies the cost by the menu sizes (a parent menu of 5 actions times 3 slots times 5 options),
so it should be measured, not assumed - one variable, `docs/AUDIT.md`.

Blue inputs turn out to be interdependent exactly like build inputs: `RESEARCH_GRANT` asks a tag and then the tag's triggers
(Mars University, Viral Enhancers, Decomposers), and `FIBROUS COMPOSITE MATERIAL` asks for a microbe amount and only for
3 does it then ask for a phase upgrade. `rankBlueSlot` completes each candidate with the policy, so both are evaluated on
the whole action, but the ordering is greedy per slot.

## Night of 18/19 November: the mulligan is learned, not gated

**Best bundle before this night:** `policy/1118-0058_13` + `v_1118-0058_13_s1..s3`, **63.93% [62.2, 65.6] vs SMART**
(3000 games), 36.7% vs NETWORK, 92.8% vs RANDOM, profile clean. It is the HoF champion.

### What changed in the code (committed)

1. **One afterstate dispatcher.** `OnePlySearch` had two (`applyOn` for the batch path, a switch inside `score()` for the live
   path, which is the one that runs because `batch` is off by default). MULLIGAN, UNMI_RT and COLLECT_INCOME were missing from
   the live one, so they scored as "no opinion" - the mulligan answered stop in 100% of rows even after it was "covered".
   `score()` now delegates to `applyOn`.
2. **The mulligan afterstate carries the engine's queue.** The hand is untouched during the loop; the whole set is replaced at
   stop. Step *k* is now `hand - (queued + candidate) + (k+1) sampled cards`, the stop baseline is
   `hand - queued + k sampled cards`, and the samples are drawn ONCE per decision (common random numbers) so the gap is the
   value of one card and not of two different draws. Before this, the loop discarded 6.58/9.10 cards of 10 and looked "free"
   in 95.9% of decisions.
3. **The corpus gate** (`night_loop.py`): before any training, the pack is read for the mulligan's per-game discard count and
   the share of games that sell. A corpus whose mulligan exploration never reached the rows, whose discard count is the same
   in every game, or whose sale exploration sells in >60% of games is skipped - and its exploration flag is DROPPED, so the
   next corpus is generated without the flag instead of repeating the block (a gate that only skips would grind the night).
   Tests: `ml/tests/test_corpus_gate.py`, `test_gate_drop.py`, `test_loop_guards.py` (18 tests).
4. **`--rows-per-type`** (12 tonight). The cap SAMPLES per type, so at the old 8 the stop row of the longest mulligans was
   dropped at random - the menu being taught was the one being distorted.

### The measured facts that set tonight's configuration

- A fixed mulligan count is not the answer: 0 -> 63.93%, 2 -> 63.75%, 5 -> 60.25% vs SMART (1000 games each).
- The critic cannot gate the mulligan in either formulation (95.9% "free" with the correct marginal afterstate, mean 9.94
  discards): its hand-identity block is masked, so it can only compare hand summaries, and most cards carry no printed points.
  Documented in `docs/RESULTS.md`; the entry stays in `ml/gap_allowlist.json` until a learned policy passes.
- Exploration reaches the menu: `MULLIGAN:0.9` gives **mean 5.55 discards** (max 11, stop in 26.2% of rows, 96.7% of games
  discard at least one) - the owner's "about half", produced by exploration, not by a constant.
- Ranking UNMI_RT and COLLECT_INCOME on the live path: 62.55% [59.5, 65.5] vs 63.93% - not a gain, so which menus are ranked
  stays a flag (`--types`), not a code path. Closed, do not repeat.
- The sale exploration is the measured killer of an iteration (89.7% of games selling -> 43.1%); a no-adv policy on that same
  corpus gave 43.7%, so it was the corpus, not the advantage weighting. **No sale exploration tonight.**

### The run

```
py -3 ml/night_loop.py --minutes 415 --games 5000 --threads 14 --critics 3 --patience 3 --max-flat 8   --abort-drop 0.06 --rows-per-type 12   --explore "BUILD_CARD:0.15,MULLIGAN:0.9"   --types BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN   --matchups "AGENT:AGENT:0.85,AGENT:RANDOM:0.15"   --seed-policy D:/ares/runs/policy/1118-0058_13   --seed-critic D:/ares/runs/value/v_1118-0058_13_s1,D:/ares/runs/value/v_1118-0058_13_s2,D:/ares/runs/value/v_1118-0058_13_s3   --generation-symmetric --record-afterstates --critic-afterstates   --policy-replay --replay-packs 2 --policy-adv --phase-critic --value-scale max99
```

Log: `D:/ares/tmp/night_1118b.log`. Ledger: `D:/ares/tmp/night_ledger.json` (every row now carries `menus`: the corpus's
mulligan count/spread and sale share, next to the score).

### What the morning must check, in this order

1. **Does the trained player mulligan by itself?** Generate 300 games with NO exploration on the new bundle
   (`--mode play`, `--matchups AGENT:AGENT:1`), pack it, `py -3 ml/mulligan_report.py --pack <pack>`: the per-game count must
   spread over several values and average near half the hand. A single spike means the policy learned a constant, which the
   owner's rule forbids.
2. **The sale must be rare or absent.** `py -3 ml/sale_report.py --pack <pack>`: a high share of games selling is the measured
   collapse, not a strategy.
3. **Profile** (`ml/check_player.py --model <bundle>`): cards built per game, phases, phase-3 standard-project share.
4. **Then** the win rate. An effect under 1.5 points counts only at 10 000 games.
5. Promotion only through the HoF gates (profile -> absolute worst benchmark -> duel 2000 games >= 54% -> pool).

## Morning report, 19 November: 63.9 -> 70.8 by playing the phase out; the mulligan is closed, with reasons

### The number

Champion before the night: `policy/1118-0058_13` + `v_1118-0058_13_s1..s3`, 63.93% [62.2, 65.6] vs SMART (3000 games).

**Now: 73.17% [72.3, 74.0] vs SMART** (10 000 games; 72.82% at 3000, 70.75% at 1000), from the night's own policy and card
critics plus the champion's phase critic and one flag. Against RANDOM: 98.6% (the champion was 92.8%):

```
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS,PICK_PHASE,STANDARD_ACTION,PERFORM_BLUE_ACTION,DISCARD_CARDS,SELL_CARDS,SELL_CARDS_LAST_ROUND,BUILD_PAYMENT,PICK_CORPORATION,BLUE_INPUT,BUILD_INPUT,MULLIGAN
-Dares.agent.search=oneply -Dares.agent.search.symmetric=true -Dares.agent.search.pairs=true
-Dares.agent.search.phasePlayout=true
-Dares.agent.valueModel=D:/ares/runs/value/v_1118-2332_17_s1,D:/ares/runs/value/v_1118-2332_17_s2,D:/ares/runs/value/v_1118-2332_17_s3
-Dares.agent.valueModel.phase=D:/ares/runs/value/v_1118-0058_13_s1
--models D:/ares/runs/policy/1118-2332_17
```

Profile (60 games, the same flags): builds **32.6/game**, build acceptance 76%, phases **8.7 / 32.0 / 38.3 / 4.7 / 16.3**,
standard projects in phase 3 **16.7%**, no warnings.

### What produced it, one step at a time

| change | vs SMART | games |
|---|---|---|
| champion as it was | 63.93% [62.2, 65.6] | 3000 |
| night critics + champion phase critic | 66.87% [65.2, 68.5] | 3000 |
| + the phase pick scored by a round rollout | 70.75% [67.9, 73.5] | 1000 |
| + the phase pick scored by a round rollout | 72.82% [71.2, 74.4] | 3000 |
| **+ the phase pick scored by a round rollout** | **73.17% [72.3, 74.0]** | **10 000** |

The 2x2 that found it (1000 games each): night critics + champion phase critic **66.80**, champion critics + night phase
critic **58.85**, both night **63.50**. The phase head of a freshly trained critic picks phases 2 and 3 in 88% of turns; the
champion's critic picks 26/21/23/15/15. The phase pick is the one decision whose action IS the phase, so mis-ranking it costs
points every round of every game.

Two variants show the *rule* matters more than the model: a phase-critic ensemble gives 65.05, and keeping the champion's card
critics gives 65.75. Playing the round out (`roundScore`, already implemented for phase picks) is what adds the last 4 points:
it restores the balance and pays 16% of picks into phase 5 (the draft), which the policy alone never chose (0% of its argmax).

A purpose-trained `PICK_PHASE` model does not replace it: `ml/train_phase.py` reaches val loss 0.554 against a 0.693 baseline
but `action_effect 0.0023` - it ignores which phase was chosen. The rollout works because it measures the consequence, not a
label.

**The caveat that decides how to ship it.** `roundScore` refuses to play when a seat is not a computer, because the engine
cannot drive a human's turns itself. So 72.82% is the **AI-vs-AI** number. In a game against a human the phase rollout returns
NaN, the candidate becomes "no opinion", and the phase falls back to the critic's V - the 66.87% player. The benchmark and the
human-facing player are two different players until that fallback is solved (drive the human seat with the policy inside the
rollout, or bound the rollout to a window that is safe to simulate). Say which one is being promoted.

**The obvious next lever** is the same recipe applied to the card menus: `-Dares.agent.search.horizon` +
`-Dares.agent.search.horizonStop` score a candidate and the stop over the same window of policy play. It was never measured
*after* the phase rollout changed the player; the gap it closes is the documented one ("an expensive action lowers V
immediately, money out, nothing in yet"). Given that the phase analogue was worth +3.9, this is where the next points are.

### The mulligan: three routes measured, all closed

The owner's rule: "discard the junk; sometimes there is a lot of it, sometimes little; on average about half".

1. **A fixed count is not the answer.** 0 -> 63.93%, 2 -> 63.75%, 5 -> **60.25%** (1000 games). Discarding a fixed number
   throws good cards away.
2. **The critic cannot gate it.** With the engine's replacement draw: 91.8% of removals "free", mean 6.58 discards. With the
   corrected marginal afterstate (accumulation + common random numbers): 95.9% "free", mean **9.94** of 10. The critic's
   hand-identity block is masked, so it can only compare hand summaries, and most hand cards carry no printed points.
3. **The policy learned "stop".** Trained on a corpus with `MULLIGAN:0.9` exploration (mean 5.14 discards per game, 13 distinct
   counts), the resulting player discards **0 cards in 100% of games** - correct learning: a *random* discard teaches only that
   random discards are bad, not which card is junk.
4. **The rollout is impossible at that stage.** `-Dares.agent.search.playout=MULLIGAN` (score a candidate by playing the round
   out) was implemented and fails structurally: the mulligan precedes the corporation pick, so every rollout must answer
   `PICK_CORPORATION` through the raw policy, which does not validate a corporation's on-built inputs. Measured: **4 of 12**
   games die with `Corporation PoliticalInfluence/HeatReflectiveGlass applies an on-built effect to itself, but the legal
   inputs...` against 0 of 12 without the rollout.

So the player keeps the whole hand. That is not "a working mulligan" in the owner's sense - it is the measured best of the
three available behaviours tonight, and the remaining route is a corpus where the *act* is explored while the card choice
stays informative (the policy head does see card identity; the label needs games where a plausible discard happened).

### The sale

Voluntary sales are **zero**: `SELL_CARDS` answers stop in 100% of rows in every corpus tonight. The sales that do happen are
the hand-limit trim (`SELL_CARDS_LAST_ROUND`, minimum = hand - 10): **74.5 cards per game for 223.6 MC** (3.5 cards and
10.5 MC per round, correlation with the final margin -0.095, i.e. neutral). That is the rule, not a habit: the player builds
32 cards a game and draws more than it can hold, so the only choice is *which* cards leave.

### The night loop was fixed, and it mattered

- **The corpus gate**: before any training the pack is read for the mulligan's per-game count and the sale share; a collapsed
  menu skips the iteration and its exploration flag is DROPPED (a gate that only skips would grind the night).
- **The auto exploration is one generation, not cumulative.** It used to stay in the spec forever: the detector added
  `COLLECT_INCOME:0.60` at 02:01 and iteration 18 fell to **53.05%**; the abort guard then stopped the night at 02:33.
- **`--phase-critic-model`** holds the phase pick on a fixed model while the card critics retrain (the finding above).
- **`--rows-per-type`**: the cap SAMPLES per type, so at 8 the stop row of the longest mulligans was dropped at random.
- **The profile check now passes the phase model**, so the acceptance gate measures the player it promotes.

### What is not done

- 3000/10 000-game confirmation of the 70.75% bundle, and the HoF duel against the champion (2000 games, >= 54%).
- `-Dares.agent.valueModel2` (per-side critics for HoF duels) is still not implemented.
- The mulligan route above (act explored, card choice informative).

## 19 November, second session: the mulligan, from the owner's description of how it is actually played

The owner's correction (19/11) settles what the decision is:

> the player sees the two offered corporations, mulligans away what fits neither of them, and only then - looking at the new
> cards - picks the corporation that fits the hand.

The engine agrees with the order: `PickCorporationsState.getPossibleTurns` returns `PICK_CORPORATION **and** MULLIGAN` together
when the mulligan is still available (choosing the corporation ends the opening, so not mulliganning is a legal choice).

Two things were missing, and both were structural, not thresholds:

1. **The afterstate never founded a corporation.** `applyMulligan` removed the cards and dealt the replacements, and the value
   was read on a position the engine never reaches - a player with no corporation. Now every mulligan afterstate founds the
   **best of the two offered corporations** for that hand (`applyOfferedCorporation`: apply on a probe, `settleQueuedDiscards`,
   take the higher V, apply it to the copy). The stop baseline founds one as well - otherwise the candidate carries the whole
   starting bonus and every discard looks free. That "best of the two" is exactly the owner's rule: a card that is useful to
   *either* corporation raises the maximum and is kept, a card useful to neither is free to discard.
2. **The critic could not see the hand.** The card critics are trained with `--drop-prefix cards:in_my_hand_`, so hand
   identities are zeroed and only the summary remains - that is why the gate called 95.9% of removals "free" and discarded
   9.94 of 10 cards. The mulligan now has its own model slot, `-Dares.agent.valueModel.mulligan=DIR`, and the first candidate
   for it is `v_1119_hand`: the same trainer on the same packs (2332_17 + 2332_18) **without** the mask, AUC 0.913, val loss
   0.991 against a 2.449 constant, 144k rows, 31 s. It is only asked at the mulligan, so the rest of the player (the 73%
   bundle) is untouched.

Instrumentation: `-Dares.agent.search.debugJunk=true` now prints how many mulligan afterstates founded a corporation next to the
gap distribution. Zero there means the two branches are not being explored at all and the numbers mean nothing.

Measurement order (one variable at a time): does the afterstate found a corporation at all -> does the gap distribution become
two-sided -> does the per-game discard count become partial and hand-dependent -> only then the win rate, against 73.17%.

## The mulligan works (2026-11-19, second session)

The owner's description of the decision is what made it possible, and it is now the code:

> look at the two offered corporations, throw away what fits neither, then - after seeing the new cards - pick the corporation
> that fits the hand.

`PickCorporationsState` offers `PICK_CORPORATION` **and** `MULLIGAN` together, so the two corporations are known while the
mulligan is decided. Three things were missing, and none of them was a threshold:

1. **The afterstate never founded a corporation** - it valued a position the engine never reaches. It now founds the best of
   the two offers *for that hand* (and the same for the other seat, so the rollout has a playable position). "Best of the two"
   is the owner's rule: a card useful to either corporation raises the maximum and stays, a card useful to neither is free.
2. **The critic could not see the hand** - the card critics are trained with `--drop-prefix cards:in_my_hand_`, so only the
   summary remained, and the gate discarded 8-10 cards. The mulligan has its own model slot and `v_1119_hand` is trained on
   the same packs without the mask (AUC 0.913).
3. **The determinizer produced illegal positions** - it pooled the opponent's corporations, the hand, the projects deck and
   the corporations deck into one shuffle, so a project card could sit in the "offered corporations" slot and every rollout
   died on `Corporation <a project> applies an on-built effect to itself`. It now shuffles one pool per card kind.

Then the threshold (`junkMargin`, a *required* advantage in milli-logits) is calibrated on the count, not guessed:

| threshold | discards per seat | vs SMART, 1000 games |
|---|---|---|
| off | 0 | 73.17% (10 000) |
| -100 | ~5.5 | 73.35% |
| 0 | ~5 | 73.70% |
| **50** | **4.00** | **75.50% [72.7, 78.1]** |
| 100 | 2.28 | 73.65% |

The count is hand-dependent, not a constant: at threshold 50, 13% of games discard nothing, the rest spread over 1-9 per seat
(`ml/mulligan_report.py`, which now counts per seat - a corpus records both seats, and the owner caught that the earlier
"discards per game" figures were double).

Flags of the bundle:

```
-Dares.agent.search.phasePlayout=true
-Dares.agent.search.playout=MULLIGAN
-Dares.agent.search.junkGate=MULLIGAN
-Dares.agent.search.junkMargin=50
-Dares.agent.valueModel.mulligan=D:/ares/runs/value/v_1119_hand
```

**The human seat is fixed (19/11, same day).** `roundScore` used to return NaN when any seat was not a computer, so against a
human the phase pick fell back to the one-action value and the mulligan to the policy - the shipped player was the weaker one.
The copy now gives a human seat `PlayerDifficulty.AGENT`, i.e. the rollout plays it with our own agent: the opponent is assumed
to be as strong as we are, which is the assumption the search already makes everywhere. The determinizer still shuffles the
human's hand, corporations and both decks first, so the search sees nothing it should not. AI-vs-AI is untouched (73.4%
[69.4, 77.1] at 500 games). This is the opponent model the adaptive player needs as well - one mechanism, two uses.

**The remaining structural problem (historical note).** `roundScore` refuses to play when a seat is not a computer, so both rollouts
(phase and mulligan) return NaN against a human and the player falls back to the critic's one-action values - i.e. the benchmark
player is stronger than the one a human meets. Fixing that means driving the human seat with a policy during the rollout (an
opponent model), which is the next task and is worth more than any knob: it is the difference between the measured player and
the shipped one.

## 19 November, third session: the collapse, and two specialists

The owner's reading of the 73% bundle at the table: "you collapsed into a phase-3 fighter". Measured, it is exact
(`docs/RESULTS.md`): phase 3 takes 33-42% of the picks against 8-10% for phase 1, upgrade 3.2 is bought in **100%** of games
against 2.5-5% for the phase-1 upgrades, and 10.7% of the phase-3 picks had neither a blue action nor the upgrade - the ones
that looked like habit.

Three things follow, and they are now instrumented rather than argued:

1. **The loop can see the collapse.** `night_loop.phase_flags` measures the CONCENTRATION of the phase mix (relative entropy
   below `--phase-entropy-min`, default 0.92) and names a phase under `--phase-share-min` (default 0.10). The measured fighter
   (9/26/40/9/16) is flagged; the champion's 26/21/23/15/15 is not. On a flag the next generation gets `PICK_PHASE:0.5`
   exploration for one generation. 7 tests.
2. **The mulligan has an A/B switch** (`-Dares.agent.search.mulliganCorp=false`): the same player without the corporation
   branch, so "the choice correlates with the two offers" can be measured instead of asserted.
3. **A phase handicap exists for diagnosis only** (`-Dares.agent.search.phaseBias=1:0.3`): it answers "what does the win rate
   do when phase 1 is played more", and it is not a fix - the owner's point, that a push breaks the scheme the player is
   optimised for, is the reason the specialists are built by *corpus* (exploration + retraining), not by handicap.

### The plan: two specialists, then an adaptive player

- **A - the phase-3 fighter**: `policy/1118-2332_17` + `v_1118-2332_17_s1..s3` + champion phase critic + the rollouts.
  Measured 73.17% vs SMART (10 000), 73.57% with the mulligan, 39.8% vs NETWORK, 98.6% vs RANDOM. **Frozen** - it is a real
  strategy and the pool must keep it.
- **B - the phase-1 fighter**: a corpus generated with `explore=BUILD_CARD:0.3,PICK_PHASE:0.5` so that the economy phases and
  the upgrade cards are actually *tried* in games whose outcomes teach what they are worth; then policy + critics, then the
  profile. Success is a DIFFERENT profile (phase 1 >= 25%, upgrade 3.2 < 50%), not a higher score against SMART alone.
- **Adaptive**: A and B play each other (the duel harness already runs two bundles), the corpus mixes both styles, one policy
  learns to pick the plan from the cards, and the acceptance is a duel against **both** specialists - the same gate the HoF
  uses. That is the owner's target: a player that can beat the phase-1 fighter and the phase-3 fighter.

### The opponent model (the human seat, and the rollouts)

`roundScore` refuses to play when a seat is not a computer, so against a human both rollouts return NaN and the shipped player
is the weaker one. The fix is an opponent model inside the rollout, and it should come from **our own pool** (A and B), not
from SMART or NETWORK: a corpus generated with a benchmark inside its rollouts would depend on the benchmark, which the owner's
rule forbids. The same mechanism gives the adaptive player its two opponents.

### How many games (owner's question, 19/11)

Measured rates on this machine with the current flags: **1.0 games/s** with both rollouts, 2.6 (8 menus) to 5 (3 menus)
without. Reason: the phase rollout plays a full round for each of ~5 phase candidates every round (~115 played rounds per
game), the mulligan rollout one round per candidate (~50). "6 games/s" was self-play without heavy ranking.

For evaluation the resolution is arithmetic: 1000 games -> +-3 points, 3000 -> +-1.7, 10 000 -> +-0.9, which is why an effect
under 1.5 points counts only at 10 000. For *training* there is no derived number: the evidence so far is that 1000-game
corpora produced degenerate bundles (43.1%) and 5000-game corpora 60-73%, and the instrument that settles it is a learning
curve (the same critic on 60 / 120 / 5000 game packs, val AUC and loss), which is running.

## 19 November, evening: what the day proved, and the two rules that close the data path

### The number

**73.17% [72.3, 74.0] vs SMART** (10 000 games), 98.6% vs RANDOM, 39.8% vs NETWORK. Champion before the day: 63.93%. The gain
came from the search, not from data:

| change | vs SMART |
|---|---|
| champion | 63.93% |
| night policy + night card critics + **the champion's phase critic** | 66.87% |
| **+ the phase pick scored by a round playout** | **72.82% (3000) / 73.17% (10 000)** |

The mulligan is now decided by the game (per card, the two offered corporations founded in the afterstate, the round played out,
a critic that sees the hand): mean **4.0 discards per seat**, 13% of games discard nothing - but it is **score-neutral at 3000
games** (73.57% [72.0, 75.1] against 73.17%). It is a working mechanism, not yet a point gain.

### The two rules that close the data path (each measured, each reproducible)

1. **A corpus inherits the strength of the player that played it.** A as it plays (73%) trains 73%. The horizon player (65%)
   trains 21%. The phase-biased player (44%) trains 25-33%. Fifty percent random phases trains 21%.
2. **One corpus = one policy.** A (73%) vs the previous champion (64%), a clean corpus, 50/50 games: the critic's val loss goes
   1.11 -> **1.97** and the student scores **31%** vs SMART (52% in a duel against that champion - a universal average, i.e.
   mediocre everywhere).

Mechanism: V(s) is one number per position. If two styles reach the same position with different continuations, V averages them;
the policy trains on the blur. A weakened generator produces positions whose continuations are weak, and the student learns them.

So: **plans cannot be grown from data here.** The levers that remain are the decision rule (search, value) on the *current best
player's own corpus* - the class that produced every gain today (`phasePlayout` +5.9, phase critic +3.3, `opponentWeight`
already optimal, `stopMargins` -3.6, `horizon` -8, wide critics -3.6 at this data size).

### Repairs made today (both were silent, both cost the night's time)

- **Rollouts recorded phantom decisions.** The generator wrote every hypothetical decision of every playout into the corpus
  (labelled with the real outcome) and the queue grew to 921k rows / 8 GB, killing the JVM every ~250 games. Fixed with one
  `record(...)` guard on `OnePlySearch.isBusy()`; pinned by `RolloutRecordingTest` (2 tests). Heap after: 0.6 GB.
- **The determinizer mixed card kinds** (corporations, hands, both decks in one shuffle), putting project cards into the
  "offered corporations" slot; fixed to one pool per kind. This is what made the mulligan rollout crash.
- **The generator chunks and resumes** on exit 6 (`should_restart`, 4 tests) instead of losing the corpus.
- The loop can now see a **collapsed plan** (phase concentration by entropy + an absent phase), and the phase pick can be held
  on a fixed model (`--phase-critic-model`), and the auto-exploration is one generation, not cumulative.

### What is left, in order

1. **The value's target and resolution** (running): seven-channel breakdown vs the single margin, same corpus, same policy.
2. **A real opponent response** in the search (a reply ply instead of the opponent's current value) - expensive, the only
   structural search change not yet tried.
3. **Capacity with >= 20 000 games** (postponed by the owner): the wide net lost 3.6 points at 2 000 games of data.
4. **Ship the 73% bundle against humans**: the rollout now plays a human seat with our own agent, so the interface player is the
   benchmark player (AI-vs-AI regression: 73.4% at 500 games).
