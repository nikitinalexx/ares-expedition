# Results ledger (2026-11-15, all measured against SMART unless stated)

> **Index first:** `docs/LEDGER.md` lists every experiment with its result, the conclusion we drew, and the STATUS of that conclusion
> (STANDS / CORRECTED / OPEN / no result), with a pointer to the section below. This file is the detailed record kept for audit; the current
> plan is `docs/PLAN.md`, older plans are in `docs/archive/`. A conclusion is overturned only by a measurement that satisfies `docs/METHOD.md`.


One place for every configuration measured during the day, with the number of games it was measured on. Read it with the
error bar in mind: **evaluation** noise is +/- 1 point at 10 000 games, but **critic training** variance is about +/- 3
points win rate (two critics with the same recipe on different 100k samples scored 42.98% and 39.38%, see
docs/EXPERIMENTS.md). The ensemble experiment exists because of that second band.

## The marginal rate (the honest accounting)

| period | change | what it was |
|---|---|---|
| until 15 Nov | 37.8% | the starting point of the session |
| 16 Nov, first half | 37.8 -> 42.98% | two one-off blindness repairs (the hand, the printed points) and the five-critic ensemble |
| 16 Nov, last ~24 h | 43.59 -> **44.00%** | tags and hand identities inside the ensemble: **+1 point, inside the +/- 1 band** |

At this rate the remaining distance to NETWORK (roughly 20 points of win rate against SMART) is not reachable by tuning:
`docs/FORECAST.md` states the kill criteria and the only two levers with a measured mechanism - data scale, and a class
change of the representation.

## What is deployed / best

| configuration | vs SMART | games |
|---|---|---|
| `r28_hand` (1090) + **six critics** (five remapped + `v_h_full`, trained with the tag and hand blocks) | **44.00% +/- 1.0** | 10 000 |
| `r28_hand` (1090) + five remapped critics (new blocks at zero weight) | 43.21% +/- 1.0 | 10 000 |
| `r25_rev2` (804) + **ensemble of five critics** (+ `v_ens_c`, `v_ens_d`) | 43.59% +/- 1.0 | 10 000 |
| `r25_rev2` + ensemble of three critics | 42.89% +/- 1.0 | 10 000 |
| `r23_price` (786) + `v_price2_margin_wide`, rank cards, pairs, symmetric | 42.98% +/- 1.0 | 10 000 |
| the same with `v_price1_margin_wide` | 42.05% +/- 1.0 | 10 000 |
| `r22_plan` + `v_mix3_margin_wide` | 41.18% +/- 0.9 | 10 000 |
| the same at 6 000 games | 40.36% +/- 1.4 | 6 000 |
| `r22_plan` + `v_mix2_margin` | 38.5% +/- 0.6 | 6 000 |
| `r22_plan` + `v_r15_plan` (the pipeline before today) | 37.8% +/- 2 | 2 000 |
| `r25_rev2` + `v_rev1_margin_wide` (one critic, 804) | 39.38% +/- 1.0 | 10 000 |
| policy alone, no search anywhere | 35.6% +/- 1.5 | 1 000 |

## Negative results, with the reason

| configuration | vs SMART | games | why it failed |
|---|---|---|---|
| V decides act-vs-stop on every menu, critic from random play | 0.4-0.6% | 1 000 | a state value is biased against spending now for a payoff ten rounds later: 759 of 768 build opportunities declined, 0.45 cards per game |
| V decides everything, policy critic | 0.1% | 1 000 | same bias |
| V re-ranks inside all menus (policy keeps act-vs-stop) | 5.8% / 0.85% | 1 000 | phases: the afterstate of a pick does not contain what the phase pays |
| V decides act-vs-stop with the coverage-trained critic | 36.0% | 1 000 | the bias again, smaller |
| `horizonStop` (stop scored over two rounds of policy playout) | 19.5% | 400 | one rollout per candidate = choosing by the ~17-point game noise |
| latent plans, code 0 (nudged data, no anchor) | 30.8% | 2 000 | imitating compulsion-generated games damages the base |
| latent plans, code 0, KL anchor to the parent | 34.95% | 2 000 | the anchor did not protect enough |
| the same budget without nudges (control) | 37.8% | 6 000 | the control: budget was not the problem |
| hand block zeroed inside the critic (same rows) | 27.5% | 3 000 | the block is worth ~12 points; removing it is a mistake |

## Paired plan audits (the arbiter of plans), 250-300 positions, 8-12 turns

| plan | difference | verdict |
|---|---|---|
| build income cards, press free actions | +1.7 +/- 1.4 | admitted (the only one) |
| press every blue action | -1.2 +/- 1.2 | rejected |
| call the build phase when the hand can pay | -0.5 +/- 1.7 | not paying |
| call the build phase **and** build the payable card | 0.0 +/- 1.6 | not paying |
| build the cards that raise steel/titanium income | -0.6 +/- 1.4 | not paying |

## Instruments built today (all in the repo, all reusable)

`ml/plan_report.py` (behaviour per latent code), `ml/slot_report.py` (how often a build phase offers two builds),
`ml/discount_report.py` (effective price and whether the player acts on it), `--mode phasedist` (which phases each
built-in bot calls), `--macro phases|phasesbuild|metals`, `--mode rollouttest` with an out-of-sample design,
`ml/train_value.py --names/--drop-prefix` (A/B one block of the representation on identical rows).

## The scale test (2026-11-16, 150 000 games) and what the pack changed

Two critics, same rows, same flags, one variable: the hand block zeroed (`--drop-prefix "cards:in_my_hand_"`).

| critic | hand block | best val loss | AUC | wall |
|---|---|---|---|---|
| `v_scale_full` | present | 0.2870 (epoch 2) | 0.8736 | 2406 s (raw chunks), 79.8 s (pack) |
| `v_scale_nohand` | zeroed | 0.2862 (epoch 2) | 0.8744 | 66.2 s (pack) |

At 150 000 games and 16 rows per game the hand identity block **does not improve the critic**: the difference is 0.0008
loss and 0.0008 AUC, inside the run-to-run noise of the same recipe. This does not overturn the play-strength result on the
3 000-game corpus (`hand block zeroed inside the critic`: 27.5%, worth ~12 points there) - different corpus, different
question. The owner's rule stands: information is not removed from the observation because of a local A/B.

Pipeline: the same 4-epoch run costs 79.8 s instead of 2406 s, because the corpus is packed once
(`docs/PIPELINE_IO.md`). The scale axis is therefore cheap now: a critic at 150k games is a two-minute experiment, not a
40-minute one.

## The hand, tested where it can matter (2026-11-16, night)

The first A/B (`--drop-prefix cards:in_my_hand_` over every row) found a difference of +0.0008 loss, inside the noise band.
The owner's objection was precise: a hand is held *in order to be played later, in order*, and that interaction can only
show up where the hand is playable - the first test averaged over rows where it is not. It also, it turned out, used a
biased normalisation sample.

Instrument: `ml/hand_ab.py`. Rows with at least two cards in hand and at least one affordable (`hand:affordable_cards >= 1`):
1 815 868 of 2 400 000 (mean hand 9.6 cards, 8.2 affordable), 15 002 held-out games. Paired: same rows, same seed, same
split; error bar = game-level bootstrap of the per-row squared-error difference (rows of one game are not independent).

| arm | best val loss (head stats) | best val loss (random stats) | AUC |
|---|---|---|---|
| with the 268 hand identity bits | 0.26156 | 0.26073 | 0.8866 / 0.8870 |
| the bits zeroed | 0.25928 | 0.25880 | 0.8880 / 0.8888 |

Difference (full - without) = **+0.00228 [+0.00090, +0.00368]** with the head statistics, **+0.00193 [+0.00059, +0.00329]**
with random ones. Both intervals exclude zero: on playable hands the identity block costs the critic about 0.7% relative
error and 0.0018 AUC.

Reading it correctly:

* the hand is **not** unimportant; the *critic* does not convert the identity of the hand cards into value accuracy. The
  train loss is also higher with the bits, so this is not classic overfitting: the extra 268 sparse inputs make the
  optimisation harder while the signal they carry is weak and interaction-dependent at our data volume.
* found on the way, a real defect: the standardisation sample was the **first 200 000 rows of the pool**, i.e. the opening
  of the first games, where a hand block is nearly empty. `--stats random` (now the default in `train_value.py`) samples the
  training rows uniformly and lowers the error of *both* arms by ~0.0008 for free.
* a one-ply afterstate value cannot express "play the income card now because the tag card is next turn": that is a plan,
  not a state. The place where the hand can pay is the **policy** (its label is literally "which card was taken, given this
  hand") and any multi-step search.

Decision: nothing is removed from the observation (owner's rule), and the deployed critic is **not** changed on this
evidence. A 0.002 value-accuracy difference is not a win in the ranking role, and only the ranking role pays; if the block
is to be masked at export, that must be measured as a ranking player (10 000 games), not as an AUC.

## Critic housekeeping the ablation needed (2026-11-16, night)

Two facts that block "just deploy the better critic", both found while setting up the block ablation:

1. **The encoder is 1256 wide now, the scale corpus and every critic on it are 1090.** The blue-table identity bits (166
   columns) were added after `D:/ares/data/scale1090` was generated. So `v_scale_full` / `v_scale_packed` are valid *critic
   science* on the 1090 representation but cannot be deployed: the engine would feed 1256 values. Every deployed critic has
   to be retrained on a 1256 corpus (the bootstrap corpus below is the first one).
2. **`--drop-prefix` was not deployable.** Zeroing a block in the training data leaves the first-layer weights of those
   columns at their random initial values, because their gradient is exactly zero. At play time the engine feeds the real
   (non-zero) values, so a "hand-free" critic would have added random noise in play - silently.

Fix: `train_value.mask_features` zeroes the weights and the normalisation of the dropped columns, and `main` now *refuses to
export* a model that reacts to them at all (invariance gate; the test run with 268 dropped columns measured 0.0). The
reported val loss / AUC / `reference.json` now describe the masked, deployable model. `ml/tests/test_train_value.py` pins the
contract.

With that in place a block ablation (`ml/critic_ablation.py`) is a real decision, not just a training-side A/B.

## The blue table is populated - the block is redundant, not empty (2026-11-16, night)

Correction of a claim made earlier in the same session. I had explained the blue-table ablation result with "random players
rarely build blue cards, so the block is nearly empty". The owner rejected that, and the corpus says he is right:

| measurement over `D:/ares/pack/boot1` (random play, 20 000 games) | value |
|---|---|
| rows with >= 1 blue card on MY table | **36.8%** (mean 0.49 cards) |
| rows with >= 1 blue card on the OPPONENT table | **43.8%** (mean 1.03) |
| (game, decision) samples where the decision maker has >= 1 blue card | **75.1%** |
| samples with >= 3 blue cards | 8.7% |

So the block is dense. The correct reading of the three-seed ablation (delta = loss without the block minus loss with it):

| block | seed 1 | seed 2 | seed 3 | CI excludes 0 |
|---|---|---|---|---|
| hand identity (268) | -0.00405 | -0.00236 | -0.00244 | **3 of 3** |
| blue table (166) | -0.00154 | -0.00163 | -0.00052 | 1 of 3 |
| milestones and awards (58) | +0.00882 | +0.00915 | +0.00767 | **3 of 3 (wanted)** |
| context switches (30) | +0.00050 | +0.00086 | +0.00044 | 0 of 3 (wanted, small) |

Adopted into the critic recipe: mask the hand identity (3 of 3, and it also cost 0.0019 on the 150k-game policy corpus).
NOT adopted yet: the blue table - the sign is consistent (all three seeds negative) but only one interval excludes zero, and
the owner's rule is that a critic claim needs an ensemble or three seeds, not a sign. It is re-tested on the policy-play
corpus, where blue cards are built by a player that chooses them rather than by chance.

Interpretation that survives both blocks: a state value reads the CONSEQUENCES of a card (incomes, tags, points, resources),
which are already in the observation. The identity bit says *which action this card offers*, and that is the policy's
business, not the critic's.

## Bootstrapping a policy from random play: two structural failures, both measured (2026-11-16, night)

With the candidate widths changed, no policy bundle from before can be loaded, so the first 1256 policy was trained from
scratch on 20 000 random games (`D:/ares/data/boot1`, 1.30 M rows, pack `C:/ares/pack/boot1`).

1. **Greedy decoding collapses to "stop".** The CE policy stops on **91.5%** of real BUILD_CARD rows (mean probability of
   stop 0.92) and builds **0.0 cards per game**; against RANDOM it wins 1.7%. Not under-training: "stop" is one generic
   option while every card is its own action, so without a value signal the generic option always wins the argmax.
2. **A signed advantage weight is unbounded below.** `--adv-value-model` multiplied the cross-entropy by the signed
   advantage and diverged: PICK_PHASE val loss **860**, train loss -20. Driving a taken action's probability to zero lowers
   such a loss without limit. Fixed with `--adv-weight-clip` (default 0.2..3.0): a bounded, positive reweighting. The same
   run is stable afterwards (PICK_PHASE val 1.3956, exactly the plain-CE level) and moves the stop share 91.5% -> 86.1% -
   better, still unusable alone.

What works: the **one-ply search with the critic decides build-vs-pass**, and the policy only ranks cards inside the menu.
`ml/check_player.py --search --value-model v_boot1_mask` gives **9.97 cards per game** (acceptance 0.29) from the same
policy that builds nothing by itself. That is the bootstrap that continues: generate with search + V, train the critic on
that distribution, then let the policy imitate the search's choices (expert iteration), which is self-play with our own
critic - no benchmark model is involved.

## Two infrastructure faults the bootstrap exposed (2026-11-16, night)

**1. A menu cap is a property of the player, not a constant.** `BlueActionDecisions` was sized for 12 candidates from a
corpus played by a weaker agent; a policy that builds 23 cards opens up to 14 blue actions, and the loud guard ("menu has N
candidates; the decision is sized for M") killed **8% of games** - `TOO_MANY_FAILURES` after 120 games. That guard is doing
its job: silent truncation would have trained on a mutilated menu. Re-measured maxima over the two 1256 corpora (mean /
max): BUILD_CARD 1.2-1.9 / 11, BLUE_INPUT 3.3-3.5 / 40, BUILD_INPUT 5.7-6.2 / 15, BUILD_PAYMENT 1.2-1.4 / 40,
PERFORM_BLUE_ACTION 1.3-2.0 / 11 (14 seen in the wild), SELL 2.5-4.8 / 21. Caps are now BuildCard 40, BlueAction 32,
CollectIncome 32, BuildInput 24; the pack strips padding, so a generous cap costs raw file size only.

**2. A model's identity is not a file's padding.** The full schema hash covers `maxCandidates` because a reader needs it to
decode a file - but the model never sees the padding. Raising a cap therefore made every policy bundle unloadable and the
bootstrap had to start over. `DecisionFile.Schema.modelHash()` now covers type + obsSize + candidateFeatures + the
observation layout, the export writes it (`ml/phase_model.model_hash`), and `NeuralPolicy.checkSchema` prefers it, falling
back to the strict check for older bundles. Cap tuning no longer invalidates a trained policy.

## The bootstrap loop, as a single command (2026-11-16, night)

`ml/bootstrap_loop.ps1 -Name boot4 -Games 20000 -Policy <dir> -Critic <dir>` runs one iteration: self-play with the search
deciding build-vs-pass, one pack pass with the candidate block, the bit-exact pack check, the critic (hand identity masked,
export gate), the policy by cross-entropy on the search's choices, then the acceptance instruments in order -
`ml/policy_argmax.py` (the share of BUILD_CARD rows where the argmax stops; a policy trained on uniform-random choices stops
on 91.5% and builds 0.0 cards per game), `check_player.py` (cards per game, phases, phase-3 spends), and only then a
1000-game compare against SMART.

State of the loop so far: `boot1` (random play) -> policy that stops on 91.5% and wins 1.7% vs RANDOM;
`boot2` (5 000 games with search + critic) -> a policy whose argmax builds on 42.5% of rows, **23.6 cards per game**,
16% vs RANDOM on its own, **3.2% +/- 1.1 vs SMART** with the search and its own critic. The engine and the loop are now
sound; what is missing is iterations, not machinery.


## The night of 16/17 November: the agent grew from its own games (2026-11-17, morning)

The candidate representation changed, so no policy bundle from before could be loaded and the agent had to be rebuilt from
nothing. `ml/night_loop.py` ran 18 iterations, each: 20 000 (then 40 000) games with the search deciding build-vs-pass and
the policy ranking the menu -> one pack pass with the candidate block -> the critic with the hand identity masked (export
gate proves the deployed model ignores the block) -> the policy by cross-entropy on the search's own choices -> the
acceptance instruments, then 1 000 games against SMART.

| iteration | vs SMART (1 000 games) | cards per game | argmax stop share | games |
|---|---|---|---|---|
| 1 | 5.10% | 26.8 | 28.8% | 20000 |
| 2 | 5.00% | 23.8 | 29.5% | 20000 |
| 3 | 5.60% | 24.8 | 29.9% | 20000 |
| 4 | 6.35% | 24.0 | 28.1% | 20000 |
| 5 | 6.40% | 25.0 | 29.0% | 20000 |
| 6 | 5.85% | 24.2 | 29.0% | 20000 |
| 7 | 7.10% | 24.3 | 29.0% | 20000 |
| 8 | 6.95% | 23.2 | 28.7% | 20000 |
| 9 | 6.90% | 24.8 | 28.6% | 20000 |
| 10 | 8.15% | 25.0 | 28.3% | 20000 |
| 11 | 8.50% | 22.3 | 26.7% | 20000 |
| 12 | 8.25% | 24.3 | 28.7% | 20000 |
| 13 | 7.60% | 23.3 | 29.1% | 20000 |
| 14 | 8.80% | 23.9 | 28.6% | 40000 |
| 15 | 9.65% | 23.7 | 28.9% | 40000 |
| 16 | 9.85% | 22.4 | 29.1% | 40000 |
| 17 | 8.30% | 23.8 | 28.2% | 40000 |
| 18 | 9.40% | 23.3 | 27.9% | 40000 |

**Final measurement, 10 000 games each (the headline, not an iteration number):**

| critic | vs SMART | 95% CI |
|---|---|---|
| one critic (`v_night16_mask`) | **8.63%** | [8.10, 9.20] |
| three critics averaged by the search | **10.28%** | [9.70, 10.89] |

The ensembles' intervals do not overlap: **+1.65 points** from averaging three nets, the historical lever, now reproduced on
the new encoder and a base that is 4x weaker than when it was found. The profile passes the owner's gate - phases
11/26/16/42/4%, phase-3 pass 58%, standard projects 19%, no warning.

**Correction (owner, 2026-11-17): there is no "healthy" card count, and the first reading of ours was backwards.** I had
written that 24.6 cards per game was "overbuilding against a healthy ~11". That 11 came from an old note, not a measurement,
and the measured comparison says the opposite (`--mode scorecard`, averages per game):

| player | cards per game | points | win rate |
|---|---|---|---|
| NETWORK (vs SMART) | **45.42** | 111.12 | 83.3% |
| SMART (vs NETWORK) | 34.41 | 74.26 | - |
| SMART (vs our agent) | 31.77 | 92.21 | - |
| our agent (night02) | **24.15** | 61.05 | 9.2% vs SMART |

We build **less than everyone** - half of SMART, a third of NETWORK - so "more cards" is a direction to move towards, not a
defect to remove, and NETWORK's profile is a clue rather than a standard: the numbers of a genuinely strong player are
unknown until one exists that beats NETWORK. What follows for the loop: the exploration knob (`-Dares.agent.explore`,
which randomises a share of build decisions and therefore adds declines) pushes this number *down*, so its effect has to be
read on cards per game and on both win rates together, never as "less building is healthier".

Start of the night: 3.2% +/- 1.1 (the random agent, with a search). Morning: **10.28% +/- 0.60**. No benchmark model was
used as a teacher, an opponent in training or a data source; SMART appears only in the measurements.

What the loop proved about the pipeline, not about the model:

* **it runs unattended**: 18 iterations in 6 hours, every stage logged, every stage either verified (bit-exact pack check,
  export invariance, 10/10 sanity) or refused;
* **the plateau is a recipe plateau**: 3.2 -> 9.85% over 15 iterations, then flat for three with a doubled corpus. The
  stagnation guard doubled the corpus once (iteration 14) and stopped the night at the second flat stretch instead of
  grinding - exactly what it was built to do;
* the drift guard never had to fire (argmax stop share stayed 27-30%, above the 18% floor) and the disk guard never fired
  (C: stayed above 120 GB, last two packs kept, raw corpora deleted after the bit-exact check).

### The second run: exploration on declines, and the red flag it exposed (2026-11-17, morning)

Two iterations with three critics, 40 000 games and `-Dares.agent.explore=BUILD_CARD:0.40` (the corpus keeps real declines
instead of drifting towards "build everything"): 9.50% then **11.40% [9.58, 13.52]** at 1 000 games, and cards per game fell
24.8 -> 21.9, so the lever does what it is supposed to.

**Final measurement of that bundle (policy `night02` + critics `v_night02_s1..s3`), 10 000 games: 10.89% [10.29, 11.51]
against SMART.** One step further than the first bundle (10.28% [9.70, 10.89]), so the two changes of the second run
(exploration on declines and the ensemble in the loop) together are worth ~+0.6 points.

**The red flag: the same bundle wins only 53.0% +/- 2.1 against RANDOM (2 000 games).** The old 44%-vs-SMART player won about
99% against RANDOM. A player that beats SMART 11% of the time and RANDOM 53% of the time is not weak - it is a *specialist*:
it learned what beats a mirror image of itself (85% of its training games are AGENT:AGENT) and does not convert a chaotic
opponent into wins. The profile gate passes (21.9 cards, phases mixed, phase-3 pass 58%, standard projects 19%), so this is
not the degenerate profile the owner warned about; it is a missing part of the objective.

That makes the next two variables clear and cheap to test, one at a time:
1. opponents in the training games: raise the RANDOM share and/or add a second AGENT variant, so the policy sees opponents
   that are not itself;
2. the build/decline balance - with the sign read from the table above: we build 24 cards where SMART builds 32-34 and
   NETWORK 45, so the open question is whether *more* building raises the score, and `-Dares.agent.explore` (which adds
   declines) is the knob that moves it.

## Extra card draw and blue cards, measured against SMART (2026-11-17)

The owner asked whether the game values the phase-5 extra draw (many blue cards give it) and why phase 2 (blue/red builds) fell
to 8% of our phase calls. Two instruments answer it from our own games: `ml/blue_report.py` counts the blue-card identities of
both tables (the observation carries both), `ml/hand_report.py` the hand size and how much of it is payable.

**Against SMART, 30 games with the best bundle:**

| | ours | SMART |
|---|---|---|
| blue cards on the table at the end | **7.57** | **9.83** |
| extra-draw blue cards | 2.40 (in 96.7% of games) | - |
| mean margin | -24.3 | - |

**In 40 000 self-play games:** blue cards on our table 7.28, on the opponent's 7.00; extra-draw cards 1.21 per game (73.2% of
games); correlation(blue cards, margin) **+0.230**; correlation(draw cards, margin) **-0.001** (mean margin +0.13 without a draw
card, -0.33 with one).

**Hand:** at its largest, mean **12.4** cards (median 12, 84.7% of games above 10), of which on average **5.4 are payable**; a
hand of 12+ goes with +1.88 mean margin against -4.31 for a hand of 9-11 (association), correlation(hand, margin) +0.152.

What this says, without turning any number into a norm:

* the player is not short of cards - it holds 12 and can pay for about five - so **extra draw is not the bottleneck**, which is
  consistent with the measured 0.000 correlation of the draw cards with the result;
* it IS short of **blue cards against SMART** (7.6 vs 9.8) and blue-card count is the quantity that correlates with the margin
  in our own games; that is the first measured support for the owner's reading of phase 2, and it is an association, not a
  proven cause;
* both instruments are in the repo and cheap (`blue_report.py`, `hand_report.py`), so the next phase change can be read on the
  quantity that matters (blue cards built, cards payable) rather than on the call frequency alone.
### The end-of-game card sale: real money, but dead money (2026-11-17)

The owner's point: excess cards are sold at the end, so a big hand is money. Checked in the code and measured.

* `SellExtraCardsState` offers `SELL_CARDS_LAST_ROUND` only when `player.getNextTurn() == null` and the hand is above
  `MAX_HAND_SIZE_LAST_ROUND = 10`; the price is 3 MC per card.
* Measured over 40 000 games (`ml/sale_report.py`): the last-round hand is 13.2 cards, so 3.2 are sold for **9.7 MC on
  average** (median 6, maximum 60), and the money raised correlates **+0.18** with the margin.
* But the turn is issued when there is no next turn left, and `ScoreBreakdown` has no MC term
  (TR + forests + card points + resource points + milestones + awards + extra): **the money cannot be spent and is not
  scored**. The +0.18 is the hand size standing in for the position, not the sale.
* Corrected in `ml/gap_allowlist.json`: the LAST_ROUND menu is a forced cleanup (benign), while the **in-game** `SELL_CARDS`
  menu is a real action that costs a turn and that the agent never takes - deprioritised by the owner, but not "automatic".

Consequence for the draw question: extra cards are not a money engine at the end, and the hand is already rich (12.4 cards,
5.4 payable), so the draw is not the bottleneck. The measured gap that matches a real deficit is blue cards against SMART
(7.6 vs 9.8) - association, not cause.
### Correction: the end-of-round sale is a real per-round income, and the gap is card QUALITY (2026-11-17)

The owner was right and my first reading was wrong. `StateTransitionService.performStateTransferFromPhase` enters
`SellExtraCardsState` after EVERY round whenever a player holds more than `MAX_HAND_SIZE_LAST_ROUND = 10`; the turn's name
("last round") is legacy. Measured over 40 000 games (`ml/sale_report.py`):

| | value |
|---|---|
| cards sold over a game | **36.8** |
| money raised over a game | **110.5 MC** (median 105, max 399) |
| per round | 1.84 cards -> 5.5 MC |
| games raising 61+ MC this way | 89.8% |

So the money is spendable and large - not dead. But it is also universal: correlation with the margin +0.076 and the bands are
flat, so the amount of sale income does not separate winners.

**What does separate them is what the money buys** (`--mode scorecard`, same 120-game runs):

| player | printed card points per game | total points | cards per game |
|---|---|---|---|
| our agent | **6.14** | 61.05 | 24.1 |
| SMART (vs us) | **16.43** | 92.21 | 31.8 |
| NETWORK | 18.20 | 111.12 | 45.4 |
| SMART (vs NETWORK) | 18.82 | 74.26 | 34.4 |

Our card is worth 0.25 printed points on average, SMART's 0.52 - half. The conveyor works (37 sold, 27 built, 12 in hand per
game) and the selection does not. `ml/night_loop.py` now records printed card points and cards built per iteration, so the next
lever is measured on the quantity that actually differs, not on the money.

### The card economy, measured end to end

1. a card in hand above 10 at the end of a round = 3 MC next round (`SellExtraCardsState`);
2. so draft and draw are a money engine, not dead weight - but everybody runs it;
3. the difference between a 60-point player and a 92-point one is not the size of the engine but the average value of what the
   engine buys: 0.25 vs 0.52 printed points per card.
### The input clamp was blinding the critic above ~58 MC (2026-11-17)

The owner's warning - "if the difference between 50 and 200 MC is lost anywhere, that is a catastrophe, and it holds for every
parameter" - was right, and it was the value model's own preprocessing.

`ValueNet` standardises every column and clamps it to mean +/- `CLIP` sd. With CLIP = 5 that cut the top of almost every
counter, because the encoder divides by 100 (money) or 10 (plants, forests) and the distributions are zero-inflated at the
start of a game, so the top end sits 9-27 sd away from the mean (`ml/feature_ranges.py`: mc 21.4 sd, forests 27.4, plants
11.7, win_points 9.0, mc_income 10.1, blue_cards_count 7.95, hand_cards_count 7.0).

The clamp affects the richest 0.5-0.6% of the rows (`ml/clip_report.py`) - which are the positions that decide games - and
inside them the response is exactly flat. Asked directly of the exported models (`ml/clip_probe.py`, mean V over 4 000 real
rows, money set to a fixed value):

| mc (encoded /100) | clip = 5 | clip = 20 |
|---|---|---|
| 0.40 | 0.54102 | 0.52100 |
| 0.63 | 0.55018 | 0.53536 |
| 0.80 | 0.55018 (**+0.00000**) | 0.54600 (+0.0106) |
| 1.20 | 0.55018 (**+0.00000**) | 0.57102 (+0.0250) |
| 2.00 | 0.55018 (**+0.00000**) | 0.62073 (+0.0497) |

So at clip 5 the critic cannot tell 80 MC from 200 MC at all; at clip 20 it rises monotonically over the whole observed range.

Actions taken:

* `--clip` is now a parameter of `train_value.py`, default **20** for the value model (the policy heads keep 5: their candidate
  features are one-hot bits, where the clamp is harmless);
* the value is threaded through the model, `predict`, the export and the invariance gate, and the export refuses to finish if
  the numpy path and the torch path disagree (it caught exactly that during the change);
* an A/B on 600 games is neutral on AUC (0.776 clip 5 vs 0.767 clip 20) and slightly better on loss (0.784 -> 0.768) - far too
  small to decide, so the next night runs the critics at clip 20 and the ranking role decides.

What this says about the rest of the representation: no other known defect of this class remains, but the sensor
(`feature_ranges.py` + `clip_report.py` + `clip_probe.py`) now runs on any feature, so the next one will be found by
measurement rather than by a conversation.
### Two corrections from the owner (2026-11-17, evening)

**1. The phase-5 draft count is NOT fixed** - I had answered that the reveal upgrade deals a fixed three cards. The engine's
`DraftCardsService.countCardsToTakeAndDraftByChosenPhase` says otherwise:

* choosing phase 5: see **5**, take **2**; upgrade `KEEP_EXTRA`: see 4, take 3; upgrade `SEE_EXTRA`: see **8**;
* plus the special effects of played cards (`countExtraCardsToTake` / `countExtraCardsToSee`): Tharsis, United Planetary
  Alliance and others add to both numbers;
* plus `LunaProjectOffice`: +1 see and +1 take for one turn, then it is spent.

So "how many cards I will see and take" is a sum of the phase upgrade, the played cards and a counter - a dynamic quantity, not
a bit. The observation carries the phase-upgrade bits and the blue-card identity bits, so the information is derivable; asked
directly (`ml/clip_probe.py` on the `cards:my_blue_UnitedPlanetaryAlliance` bit), V responds to that card by **+0.0164** at
clip 5 and **+0.0206** at clip 20 - the net is not blind to it. Whether the size of the response is right is a separate
question that only the ranking role can answer.

**2. 200 vs 300 MC is a big difference, and my clamp still lost it.** The owner: "that is about ten victory points, decisive at
the end". Measured on the exported models (`ml/clip_probe.py`, money set to a fixed value):

| mc (/100) | clip 20 | clip 40 |
|---|---|---|
| 1.00 (100 MC) | 0.57429 | 0.82267 |
| 2.00 (200 MC) | 0.63735 | 0.94175 |
| 2.50 (250 MC) | 0.64483 (+0.0075) | 0.96690 (+0.0252) |
| 3.00 (300 MC) | 0.64483 (**+0.0000**) | 0.98140 (+0.0145) |

At clip 20, 250 and 300 MC were identical to five decimals; at clip 40 all three are distinct. The default is now **40** for
the value model (`train_value --clip`, threaded through the model, `predict`, the export and the invariance gate).

Still open in the same class: the sparse resource counters (`ghg_microbe_count` 171 sd, `zoos_animal_count` 130 sd) are cut even
at 40 - they are near-always zero, so a mean/std scale inflates their tail. A robust scale (median/IQR or nonzero-values std)
is the next representation fix, and `ml/feature_ranges.py` lists every such column.
### Scaling the observation: the owner's max-normalisation, measured against the clamp (2026-11-17)

The owner's question: he normalised every parameter by its observed maximum (after ~100k random games), which keeps each value in
proportion to its own range, and asked what the difference is from the fixed clamp at 40 and which is better.

The mathematics first, because it settles half of it: a per-feature AFFINE map cannot change what the network can represent - the
first layer absorbs `a*x + b`. So "proportion" is preserved by both schemes, and the only real differences are (1) where the
representation SATURATES and (2) how well-conditioned the first layer's input is.

| scheme | saturation | scale of the inputs |
|---|---|---|
| `(x - mean) / std`, clamp 40 | at mean + 40 sd - for a rare counter 170 sd deep in the tail, i.e. flat | all features unit variance |
| `x / max` (the owner) | nowhere inside the observed range, extrapolates beyond | features keep their own size (a rare counter stays ~0.06) |
| `x / p99.9` | same, but one freak maximum cannot compress the working range | same |

Measured on the same 40 000-game pack, same recipe, three modes (`train_value.py --scale`):

| scale | best val loss | AUC |
|---|---|---|
| `mean` (standardise + clamp 40) | 0.5285 | 0.8705 |
| `max` (divide by the maximum, no clamp) | 0.5723 | 0.8559 |
| `max99` (divide by the 99.9th percentile, no clamp) | **0.5269** | **0.8729** |

So the owner's scheme is right in principle and the raw maximum is the weak version of it: one outlier (the observed maximum of
money in this corpus is ~7.7 in encoded units, i.e. far beyond the working range) compresses everything else. The 99.9th
percentile keeps the no-saturation property without that cost, and it is the best of the three here.

`--scale max99` is now the default. The clamp is exported as 1e6, not 0: the Java model computes `min(v, clip)`, so a zero would
flatten every input to zero and the exported net would answer a constant - a silent catastrophe avoided by construction.

Also worth recording as a measurement of the worst case: the probe on the max-scaled model keeps rising with money at 400 MC
(dV +0.043 from 300 to 400), where the clamped model is already 41 sd out at 400 MC. No money position is invisible any more.

## Targeted diagnostics: the critic can rank cards, the loop was drowning the signal (2026-11-17, evening)

Owner's question: how can the model fail to value victory points, when a point wins the game? Measured, then explained.

**The probes** (`ml/diagnostics.py`, `ml/card_value_check.py`, on the best critic at the time):

| probe | result |
|---|---|
| +1 victory point, rounds 1-4 / 17+ | dV +0.00584 / +0.00348 |
| one income unit in "victory points" the model's own units | 75 (mc), 54 (steel), 35 (titanium) |
| correlation(printed points of a blue card, dV of its bit) | **+0.074** |
| cards WITHOUT printed points vs WITH | +0.01292 vs **+0.01075** - scoring cards were valued *less* |
| AiCentral / AntiGravityTechnology / UnitedPlanetaryAlliance / MarsUniversity | +0.082 / +0.020 / +0.048 / **-0.010** |

So on the night corpora the critic had no usable sense of which card scores. The theory of why (and it is not the objective, which is
already E[final margin]): the data holds ONE card per position, so the marginal value of a card is confounded with the strength
of the position it was built in; the final margin is noisy (+/-20 points), which attenuates the slope of a small feature; and the
score column is collinear with TR, cards and forests, so it keeps almost none of the weight.

**The decisive comparison** - same recipe, two corpora:

| corpus | correlation(printed points, dV) |
|---|---|
| night pack (45% mirror, exploration on 8 decision types, 8 rows per type sampled) | **+0.074** |
| 3 000 clean games vs SMART, all rows, no build exploration | **+0.226** (decisions) / **+0.230** (with afterstates) |

So the critic *can* rank cards; the loop was drowning the signal with randomised builds. Afterstates add a little (val loss
0.888 -> 0.863, correlation +0.004) - they are not the lever by themselves, but they are the only data where two options differ
*only* by the card, so they stay on.

Also measured and worth keeping: on the clean corpus the critic's response to the score feature is tiny (dV per point 0.0003
against the 0.05 the margin target implies), i.e. the model is a *position-strength* estimator, not a calibrated score. That is
exactly why the ranking role works and the "V decides act-vs-stop" decider branch failed before - and it means the ranking
metric (card value), not the score slope, is what to optimise.

Next: a clean, large corpus (all rows, vs SMART, afterstates on, exploration only where a collateral collapse is detected), three
critics, policy by CE on it, then 10 000 games. The exploration shares are the variable that poisoned the previous night and they
are now a measured cost, not a free safety net.

### Where the loop stands after 12 iterations (2026-11-17, night)

`plateau_1117-2001.md` in one paragraph: 12 iterations, SMART 11.2-20.1% with 1000-game intervals of +/-2, RANDOM 64-81%,
printed card points **6.9-8.9 the whole time** against SMART's 18-19, phase mix swinging from 4:7% to 4:38%. Best 20.1%
(iteration 9, RANDOM 64%), best profile iteration 11 and 12 (phase 4 at 13-14%, phase 5 at 21-26%, RANDOM 77%).

What that means, without decoration:

* the loop does not converge: the spread between iterations is larger than any trend, so "18%" and "20%" are the same number
  measured twice. The causes are the 1000-game measurement (+/-2), the three-critic retrain, and the phase behaviour flipping
  between iterations.
* the one number that never moved is the one the diagnostics pointed at: **printed card points 8-9 against 18-19**. Every
  lever of the night (opponents, exploration, entropy, phase critic, ensemble, corpus doubling, clean data) touched it by less
  than a point.
* the phase mix is unstable at the *critic* level: a small change in the value net flips phase 4 between 7% and 38% of the
  calls. That is a variance problem in the phase head, not a strategy problem, and it is worth its own experiment.

The next single experiment, chosen because it attacks the flat metric directly: **give the critic the printed points of the
card**. The owner removed them from the candidate vector on the principle that one bit per card teaches everything; measured,
it does not: correlation(card value, printed points) is 0.19 on the cleanest corpus, and the search builds cards worth half of
what SMART builds. That is a schema change (one number per candidate), so it means regenerating the corpus - which the pipeline
now does in ~55 minutes per 40 000 games.

## Where the 20 points go, by the game's own channels (2026-11-18, night)

`--mode scorecard` on the best bundle, 200 games against SMART, prints the seven channels of `WinPointsService.ScoreBreakdown`:

| channel | ours | SMART | gap |
|---|---|---|---|
| printed card points | 9.32 | 18.83 | **-9.51** |
| terraforming rating | 31.78 | 37.53 | -5.75 |
| forests | 7.78 | 12.92 | -5.14 |
| milestones | 3.59 | 5.27 | -1.68 |
| awards | 9.91 | 11.25 | -1.34 |
| points on resources of cards | 9.96 | 7.00 | **+2.96** |
| extra | 0.0 | 0.0 | 0 |

Total 72.33 : 92.79 (19.5% of games won). Builds are 31.2 against 33.5 and cards in hand about the same, so **the game is
not lost on volume or on the engine**: the resource-card channel is the one we already win (+2.96, animals/microbes on
cards), and the whole deficit is printed points per built card (0.30 against 0.56), plus the ground game (TR + forests,
-10.9 between them).

That kills two hypotheses at once: (a) "we build too few cards" is false, (b) "the blue engine does not work" is false - it
works better than SMART's. What does not work is *which* cards are built and *whether* the ground game is played.

### The score-decomposition critic, measured and rejected

Seven channel heads (`ml/train_breakdown.py`) on the 40 000-game corpus `bd40`, per-channel scales, 4 epochs, 3.37M rows:

| channel | MSE | variance of the channel |
|---|---|---|
| terraforming rating | 92.2 | ~90 |
| forests | 24.6 | ~24 |
| printed card points | 21.3 | ~21 |
| resource cards | 80.0 | ~80 |
| milestones | 18.1 | ~18 |
| awards | 18.4 | ~19 |
| extra | 0.0 | 0.0 |

Every channel lands at its own variance - the heads predict the mean and nothing else - and the summed model gives margin
MSE 331 against 72 for a constant. Card-value correlation falls to 0.122 (margin target: 0.19) and `dV` per victory point
is 0.00019 (income is still worth ~124 points). Splitting the target into the seven channels does not create the signal; the
channels are each as unpredictable as the whole, because each is dominated by the rest of the game.

The structural conclusion: a model trained on *who won our games* cannot learn that points win, because in our games they
do not - economy does. That is the same silent-decay mechanism the coverage guard exists for, one level up: the target,
not the action, is collapsed.

## The critic was only ranking three menus; giving it the other two is where the 20% broke (2026-11-18, night)

Measured on the best bundle (`policy/1117-2001_11`, `v_1117-2001_11_s1..s3`), same flags, same 3000 games, the only change
being `-Dares.agent.search.types`:

| menus the critic ranks | win rate vs SMART (3000 games) |
|---|---|
| BUILD_CARD, DRAFT_CARDS, PICK_PHASE (the bundle as it was) | 19.55% [17.2, 22.1] (1000 games, earlier) |
| the same plus STANDARD_ACTION and PERFORM_BLUE_ACTION | **27.9% [26.3, 29.5]** |

The intervals do not overlap. Nothing was retrained; the critic simply got a vote on two menus it had never been asked
about, and those two are exactly where the channel breakdown says the points are lost: standard projects are the ground game
(terraforming rating, forests) and blue actions are the engine.

Why this works when the card ranking does not: V sees the *afterstate*, and an afterstate of `SP_OCEAN` or of a pressed
blue action differs from the current state in something immediate and structural - a terraforming step, an ocean, a forest,
a resource moved onto a card. The policy alone was choosing those with no lookahead at all (13 of 16 decision types were
policy-only). The card menus stay hard for V, because a card's worth shows up in points at the end of a game, not in the
next state.

### The corpus, measured before it was blamed

`ml/card_points_data.py` on 40 000 games (320 000 logged build decisions):

```
menus where a card with >= 2 printed points was offered:   8.9%
built a 0-point card:                                    82.0%
took the >=2-point card:  margin +8.54, win 62.3%
took another:             margin +8.82, win 62.6%      <- no difference
correlation(picked points, margin) = +0.042
winners built 1.03 printed points, losers 0.81
```

So a critic trained on our outcomes is *right* to ignore printed points: in our games they do not separate winners, and
they are barely reachable (8.9% of menus) because the draft does not take them. This is the silent-decay pattern at the
level of the training target: the corpus does not contain the fact, so no architecture and no number of seeds can learn it.
It also retires the "the network is too small / the data too thin" hypotheses - they were never the binding constraint.

### The same change, three menus further: 27.9% -> 47.6% (2026-11-18, night)

`OnePlySearch.applyOn` also applies the give-up menus: a candidate of `DISCARD_CARDS`, `SELL_CARDS` and
`SELL_CARDS_LAST_ROUND` is "this card leaves my hand", which is *exactly* what those decisions do (the money a sale pays
is the same for every candidate, so it cancels in the ranking - the critic only has to answer "which card do I miss least").
Adding those three types to `-Dares.agent.search.types`, nothing else changed:

| menus ranked by the critic | vs SMART, 3000 games |
|---|---|
| BUILD_CARD, DRAFT_CARDS, PICK_PHASE | 20.2% [18.8, 21.7] |
| + STANDARD_ACTION, PERFORM_BLUE_ACTION | 27.9% [26.3, 29.5] |
| + DISCARD_CARDS, SELL_CARDS, SELL_CARDS_LAST_ROUND | **47.6% [45.8, 49.4]** |

**Correction (owner, 18/11).** The mechanism written here first - "the policy always answered stop, so it never got the sale
money" - is wrong, and the owner is right about the rule: `SellExtraCardsState` offers only `SELL_CARDS_LAST_ROUND` when the
hand is over 10 at the end of a round (`Constants.MAX_HAND_SIZE_LAST_ROUND`), so the end-of-round sale is **mandatory** and
its money was always paid. What the menus changed is *which card leaves the hand* (`SELL_CARDS_LAST_ROUND` down to 10,
`SELL_CARDS` optionally during phases 1-4, 3 MC each) and *whether* to sell in-phase at all.

**Ablation, 1500 games each, same bundle, only the ranked list differs:**

| ranked menus | vs SMART |
|---|---|
| all eight | 56.23% (3000 games, [54.5, 58.0]) |
| without the end-of-round sale | 54.87% [52.3, 57.4] |
| without the in-phase sale | 56.60% [54.1, 59.1] |
| without either sale | 53.73% [51.2, 56.2] |

So the two sale menus are worth ~1-2 points *at play time with the current policy*, not 20. The 27.9% -> 47.6% step was
measured on the **old** weights: there the critic had to make those choices because the policy could not. Once the corpus
had been generated with the menus ranked, the policy learned the behaviour, and the search became nearly redundant for
them. That is the honest reading of the whole night: **the search is mostly a teacher - it manufactures the decisions the
policy is bad at, and the policy absorbs them in the next iteration.** It also means the play-time menu list can be trimmed
for speed later, at a measured cost of ~2 points.

What the three steps do have in common: every one of them is a menu the critic had never been asked about, and every one is
an *immediate* consequence (an ocean, a temperature step, a card out of the hand), not a promise of points twelve
generations later.

Channels with the eight menus (300 games): resource-card points 9.87 -> **15.97** (SMART 6.57), printed card points
8.93 -> 10.75 (SMART 17.12), TR 30.64 -> 33.90, forests 8.33 -> 10.01, total 72.25 -> 85.95 against SMART's 80.59, builds
34.0 against 32.9. The sale pays ~110 MC per game, the money becomes a 34-card game, and the resource-card engine - the one
channel we already beat SMART on - doubles. Against RANDOM the same bundle goes 77.2% -> 92.75% [91.0, 94.2].

Transfer, which is what rules out benchmark-specific denial: vs NETWORK, 1000 games, the same bundle with three menus wins
0.9% (9 games), with eight it wins **15.75% [13.6, 18.1]**.

### One loop iteration compounds it: 47.6% -> 57.45% vs SMART, 15.75% -> 32.35% vs NETWORK

Corpus of 18 772 games generated by the eight-menu search (the loop died on its own fixed 7200 s limit after writing them;
`docs/PIPELINE.md`). Three critics (`--target margin`, 512x256, AUC 0.913-0.917) and a policy CE, the loop's own commands:

| bundle | vs SMART (3000) | vs NETWORK (1000) |
|---|---|---|
| eight menus, old weights | 47.6% [45.8, 49.4] | 15.75% [13.6, 18.1] |
| + one iteration (`1118-0058_13`) | **57.45% [55.7, 59.2]** | **32.35% [29.5, 35.3]** |

Channels (300 games): resource cards 20.23 (SMART 7.03), forests 12.29 (8.54), printed points 11.67 (17.03), TR 29.2
(33.44), total 88.2 against 81.14, builds 33.2 against 31.9. The scorecard's 300-game win rate (49.67%) disagreed with the
3000-game compare (57.45%); at 1000 games the scorecard reads **55.9%** with a score of 91.11 against 79.40, so 300 games
are too few for the scorecard - 1000 is its floor.

### The own-score target learns the score and still ignores the cards (2026-11-18, 05:30)

`--target myscore` (the seat's own final score from the pack's `breakdown`) on the 18.7k-game corpus, 512x256, `max99`,
lr 3e-4, 4 epochs:

| metric | value |
|---|---|
| validation MSE | 0.0373 |
| constant predictor (target variance) | 0.0706 |
| AUC on the outcome (not the target) | 0.836 |
| correlation(card value, printed points) | **+0.089** |
| dV per victory point | 0.00012 |
| dV per victory point if the model simply read the points | 0.0111 (= 1/90) |

So the target is learnable (half the constant's error) and the model still prices a printed point at 1% of its value. With
the current score already an input column, predicting the final score is explained by "current score + round + economy";
the card identity adds almost nothing, and the network takes the easy path. This is the third target that fails to create a
card signal (outcome 0.19, seven channels 0.12, own score 0.09) and it should stop the "try another target" line of work.

### A data-integrity bug worth remembering

`RANKING.<chunk>.jsonl` writes the game id as the Java `long` it is, so **half of them are negative**; the pack stores the
same 64 bits as unsigned. `pack_dataset.py` joined them without masking, which matched *nothing*: the pack carried a
`breakdown` array of zeros and a critic trained on it learned a constant (mean 0.00, sd 0.00). It went unnoticed on the
40k corpus only because those ids happened to be positive. Fixed with `& 0xFFFFFFFFFFFFFFFF` on both sides; any future
sidecar join must do the same. `ml/card_points_data.py` also reads the pack, so a zeroed breakdown is visible there.

### Final numbers of the night (18/11, ~07:40, cap-fixed build)

Bundle `policy/1118-0058_13` + `v_1118-0058_13_s1..s3`, eight ranked menus:

| opponent | games | win rate | 95% CI | failed |
|---|---|---|---|---|
| SMART | 3000 | **56.23%** | [54.5, 58.0] | 0 |
| NETWORK | 1000 | **34.53%** | [31.7, 37.5] | 1 |
| RANDOM | 1000 | **92.80%** | [91.0, 94.2] | 0 |

Morning-to-morning, the same weights as the 19.55% bundle with three ranked menus: 20.2% -> 56.2% vs SMART, 0.9% -> 34.5%
vs NETWORK, 77.2% -> 92.8% vs RANDOM.

### A smaller corpus made it worse, and that is recorded here so it is not retried

One iteration on 7 998 games (same recipe, same eight menus, `BUILD_CARD:0.15` exploration) measured **45.1% [42.0, 48.2]**
against the 57.45% of the 18.7k-game iteration. Two iterations of the same recipe at different corpus sizes is the whole
difference, so: **do not shrink the corpus to fit a night budget.** 20 000 games per iteration is the floor for this player
(it takes ~2 h 10 m to generate at 2.6 games/s, `docs/PIPELINE.md`).

### Three defects the night found, all fixed

1. `pack_dataset.py` joined the `RANKING` sidecar on the game id as a signed Java `long` against the pack's unsigned
   representation, so half the games matched nothing and the score breakdown was an array of zeros. A critic trained on it
   learned a constant. Fixed with `& 0xFFFFFFFFFFFFFFFF` on both sides.
2. `HandCardDecisions.POOL = 40` (41 candidates with the stop option) overflowed: a player that sells every round keeps a
   hand large enough for 42 offers, and the decision threw `IllegalStateException` inside the generator. Raised to 64; the
   cap is excluded from the policy's model hash, so no bundle is invalidated. Re-measured: 56.23% with 0 failed games.
3. `night_loop.play()` capped a corpus at a fixed 7200 s and killed a 20 000-game generation at 18 772. The limit now
   scales with the game count.

## The tricky cards, checked one by one (owner's list, 2026-11-18)

The owner named the cards where a build's input is not independent per slot. All of them are confirmed against the engine,
and the slot order in `BuildInputDecisions.nextSlot` matters because earlier slots decide which later ones exist:

`tag -> phase upgrades -> resource targets -> cargo -> Decomposers -> ViralEnhancers -> MarsUniversity`

| card | what the engine does | verdict |
|---|---|---|
| Anaerobic Microorganisms | with >= 2 microbes on it, a payment plan for -10 appears (`BuildPaymentDecisions.canAnaerobic`); combinable with Restructured Resources (-5) for -15 | correct, now ranked by V |
| Decomposers | one slot per animal/microbe/plant tag of the built card; each slot is "add a microbe here" or "remove one to draw"; the second option exists only while `microbes_on_card + added > drawn` | correct, and it reads the partial input, so the tag chosen one slot earlier feeds it |
| Filter Feeders | `CardResourceService.addResources` gives the owner +1 animal whenever a microbe is added to a card | correct. **Rule changed on the owner's request: 1 VP per 2 animals, not per 3** (the buffed variant already printed 2) |
| Viral Enhancers | one slot per animal/microbe/plant tag: take a plant, or put a resource on a card that holds animals/microbes - never on the card being built | correct |
| Mars University | one slot per science tag; discard a card from hand (not the card being built) or stop; a discarded card with a PLANT **or DYNAMIC** tag draws two, otherwise one | correct |

Consequence for the ranking: a candidate is only comparable if the rest of the build is completed, so `rankBuildSlot` puts the
candidate into the partial input, completes the remaining slots with the policy (and, if the engine rejects that completion,
with the first option of every remaining slot), builds the card on a copy, and reads `V(afterstate)`. That is what makes
"chose the plant tag, so Viral Enhancers and Decomposers fire" visible to the value net.

## Engine tests for the complex cards (2026-11-18)

`src/test/java/com/terraforming/ares/ComplexCardsTest.java`, 11 tests, green. They check the *mechanics* every ranking above
them depends on, not a strategy:

| test | what it pins |
|---|---|
| Filter Feeders animals | one animal per addition of microbes to another card, and 4 animals = 2 VP (the owner's 1 VP per 2) |
| Filter Feeders singleton | card id 217 resolves to the single `FilterFeeders`; `BuffedFilterFeeders` is gone |
| Decomposers | "remove a microbe to draw" exists only while a microbe will be on the card |
| Viral Enhancers | the card being built **is** a legal target (`resourceCards(..., includeBuilding = true)`) |
| Mars University | the card being built cannot be discarded, and stop is always offered |
| Anaerobic Microorganisms | -10 needs two microbes; without a plant the -5 is not legal; both together make -15 |
| Fibrous Composite Material | a phase upgrade is asked only for three microbes |
| Research Grant | the tag is asked first, and the tag then fires Mars University |
| Power Infrastructure | every amount of heat up to the whole stock is offered and pays 1 MC each |
| Self Replicating Bacteria | five microbes must produce a build slot worth -25, or the microbes are wasted |
| Drone Assisted Construction | 2 MC, +2 when *my chosen* phase this round is upgraded (the opponent may be playing another phase) |
| Green Houses | heat pays plants at par, at most four per activation (the card's own `max`) |

Three of these corrected my own reading of the engine, and the corrections are part of the tests now:

- **Restructured Resources is paid with a plant, not with a green card** (`canUseRestructured` checks the special effect and
  `player.getPlants() >= 1`). So the -15 is two microbes plus one plant.
- **Viral Enhancers may put its resource on the card being built.** I had told the owner the opposite; the engine's
  `includeBuilding = true` says otherwise, and only Filter Feeders' text says "ANOTHER card".
- **Green Houses is capped at four heat per activation** by the card itself, so a large heat stock is dumped over several
  activations rather than one.

## `fullTypes` is not a way to make a collapsed stop menu work (2026-11-18)

The idea was: `MULLIGAN` and `SELL_CARDS` have a policy that always answers "stop", and in the default `rankOnly` mode the
critic only re-orders candidates while the *policy* keeps the act-or-stop decision - so those two menus are no-ops. The knob
is `-Dares.agent.search.fullTypes`, where the critic is compared against the stop candidate as well.

Measured: **2.88% [2.3, 3.5] against SMART, 3000 games, 0 failed** with `fullTypes=MULLIGAN,SELL_CARDS,UNMI_RT` on the
15-menu bundle that otherwise scores 63.9%. The failure is the one the code comment warned about: `V(state)` and
`V(after the action)` are not on one scale (an afterstate is a state between two decisions, and the value net was trained
on decision states), so "act vs stop" by raw V sells the whole hand and buys terraforming rating.

Conclusion: coverage has to stay `rankOnly`. A menu whose policy always stops cannot be fixed by ranking it - it needs the
**policy** to learn the decision, which needs a corpus that contains mulligans (and rows from exploration must be marked so
they do not become the policy's teaching labels).

## The UI: Easy / Normal / Hard / Ares (2026-11-18, owner's request)

`NETWORK_V2` ("Expert / AI v2") is no longer offered; `NETWORK` stays as Hard and the learned player with the search stays
as Ares. Ares is `AGENT_V2`, i.e. the second model set, so the live app must be started with `-Dares.agent.models=<bundle>`
(falls back to the same set) or `-Dares.agent.models2=<bundle>`, plus the search properties. The form now locks the settings
the player was trained on while Ares is selected: improved corporations, dummy hand, mulligan.

## A human played it (owner, 2026-11-18)

The owner played one game against the bundle `policy/1118-0058_13` + `v_1118-0058_13_s1..s3` through the UI (Ares =
`AGENT_V2`, all fifteen menus ranked) and called it "very decent". This is the qualitative check the acceptance rule asks
for besides the win rate - the earlier failure mode was a model that beat RANDOM 95% and played like a random card picker to
a human. What he did not report: nothing degenerate in the opening, no phase hoarding, no skipped mulligan complaints. The
mulligan is still declined by construction (the menu is a no-op in `rankOnly`), which is the next thing to make learnable.

## The first retrained iteration did not improve, and the phase mix narrowed (2026-11-18)

Iteration 13 of the loop (20 000 games, all fifteen menus, `BUILD_CARD:0.15`): **60.05% [57.0, 63.0] vs SMART** at 1000 games,
RANDOM 94.5%, builds 32.9, phases **1:4% 2:39% 3:44% 4:4% 5:9%**. The 3000-game number of the previous bundle was 63.93%
[62.2, 65.6], so the iterations overlap and the retraining did **not** move the player - while the phase mix did move: phase 1
fell from 26% to 4% and phase 4 from 15% to 4%. That is the narrowing the owner asked about, visible in the ledger before it
costs anything: the player found a phase-2/3 line and stopped using the rest.

The coverage guard flagged `MULLIGAN` after this corpus and wanted to add `MULLIGAN:0.60` exploration. Vetoed and recorded in
`ml/gap_allowlist.json` with the reason: exploring it is not safe until the act-vs-stop gate is calibrated (measured 2.88%
when the critic decides), and the critic cannot see the hand anyway. The guard reads the allowlist, so the next corpus does
not explore it.

## Replay across iterations, implemented (anti-narrowing, item 1)

`train_value.load_packed`, `train_policy_ce` and `night_loop` now accept a **comma-separated list of packs**: the newest one
plus `--replay-packs` older ones of the same encoder (default 2). Rationale: training on the newest corpus alone deletes the
data of every abandoned style, so nothing pulls the player back - the owner's NETWORK lost universality exactly this way.
Smoke test on two packs: 3 240 825 rows mixed, 76 000 sampled, regression beats its constant (val 2.22 against 3.26), export
artifacts written, AUC 0.865.

One variable against iteration 13: same seed bundle, same exploration, same game count - only the training mixture changes.

## A silent misalignment in the policy replay, caught and closed (2026-11-18, evening)

The first replay attempt at the policy stage crashed with `CUDA error: device-side assert - scatter gather kernel index
out of bounds`. The pack keeps two of its arrays as LAZY row views: `cands` (`PackedCandidates`) and `ids`
(`PackedIdView`). A numpy concatenation of two packs keeps the *first* pack's view while the per-row arrays (`chosen`,
`count`, `obs`) now carry the rows of both - every label then points at the wrong candidate set. On a single pack the
path is fine (`pol_single`, exit 0), which is how the cause was isolated.

`train_policy_ce` now **refuses** a comma list ("policy replay needs a merged pack") instead of guessing, and the loop
passes the replay set to the critic only (the critic's loader builds real numpy arrays, so its replay is safe: three
critics trained on two packs). The honest fix for the policy is a merged pack on disk (`ml/pack_merge.py`: append
`obs.f16` and the label arrays, shift the candidate offsets) - next.

This is exactly the failure the owner asked to design against: not a crash, but a *different representation* that silently
loses the correspondence between a row and its candidates.

## Bundle comparison after the replay attempt (2026-11-18, evening)

| bundle | corpus | critic replay | vs SMART 3000 | vs RANDOM 1000 |
|---|---|---|---|---|
| `1118-0058_13` (champion) | 18.7k, 8 menus | no | **63.93% [62.2, 65.6]** | 92.80% |
| `1118-1233_13` | 20k, 15 menus | no | 60.05% [57.0, 63.0] (1000 games) | 94.50% |
| `1118-1558_14` | 20k, 15 menus | yes (2 packs) | 61.57% [59.8, 63.3] | **94.15%** |

Three readings, all uncomfortable and all recorded:

1. **The 15-menu corpus did not improve SMART play.** Iteration 13 (60.05% at 1000) and the replay bundle (61.57% at 3000)
   both sit below the champion's 63.93%, and the intervals overlap only at the edge. The coverage work bought its points
   *at play time* (19.55% -> 64.22% with unchanged weights); turning it into training data has not paid yet.
2. **RANDOM went the other way** (92.8 -> 94.5/94.15). Lower against the hand-written benchmark and higher against a random
   one is what "less tuned to the SMART style, more general" looks like - which is the *stated purpose* of replay, but a
   purpose is not a result.
3. Under the owner's acceptance rule (**the worst benchmark decides**), the champion is still `1118-0058_13`: its worst
   number is 63.93 against SMART, the new bundles' worst is 61.57. Nothing is retired; the new ones stay as artifacts.

What this does **not** yet test: replay on the *policy* side, which is the side that carries the style. The policy still
trains on the newest corpus alone because a policy merge needs a merged pack (`ml/pack_merge.py`, refused rather than
guessed after the misalignment above). Until that exists, the anti-narrowing hypothesis is only half-tested, and the honest
statement is "critic replay alone does not move SMART play".

## `batch=true` is both slower and not equivalent (2026-11-18, measured)

`ValueBatchParityTest` (3 tests) compares `MlpValueModel.batchLogits` with the per-row `logit` on real observations plus
sparse/dense edge cases, with a zero tolerance (`Float.compare == 0`):

```
single  9.4 us/row
batched 49.1 us/row   (5x SLOWER)
one-row batch differs from logit: 1024637515 != 1024438334
```

Two conclusions, both now pinned by a test:

1. **Not equivalent, by construction.** `batchLogits` accumulates feature by feature (`aBase` + per-row deltas, skipping
   columns that are zero across the menu) while `logit` accumulates per row. Same mathematics, different float association
   order, so the low bits differ - and a ranker decides on differences that can be that small.
2. **Not an optimisation on CPU.** The feature-major loop does more work than a plain matvec per row.

So `-Dares.agent.search.batch=true` is rejected: it changes decisions and makes the games slower. It stays in the code only
as a reference, guarded by the parity test so nobody re-enables it believing it is free.

What this means for the "zero tolerance" rule: bit equality is achievable **inside one implementation** (the incremental
observation must reproduce the from-scratch build exactly), and against a float64 reference; it is not achievable between
two *different* implementations. A GPU path therefore needs its own reference check plus a measured count of how many real
menu decisions flip against the pure-Java path.

## The critic's matvec is already at the compiler's limit (2026-11-18, measured)

`MatvecVariantsBenchTest` compares the model's own forward pass with hand-unrolled variants on identical weights
(512x256 net, one observation, 2000 rounds, reused buffers):

```
current            191 us
unrolled over i, 4  496 us (0.39x)
unrolled over i, 8  424 us (0.45x)
unrolled over j, 4  681 us (0.28x)
unrolled over j, 8  492 us (0.39x)
```

Every variant is **slower**, and the j-unrolled ones are not even bit-identical (a different association order). Reading
`MlpValueModel.logit` explains it: the current loop already (a) pre-accumulates the contribution of the zero baseline
(`aBase`), (b) skips features whose raw value is zero, (c) keeps the inner loop contiguous over `w1T[i*h1 + j]`, and (d) skips
dead ReLU units in the second layer. That is the memory-optimal order; changing it loses more than unrolling gains.

Consequence for the speed work: **the neural term cannot be improved on CPU**. The two remaining levers are a batched
GPU/ND4J service (the owner's queue design) or a smaller network - and the latter is a quality decision that must be
measured, not assumed. The observation (44% of the per-candidate cost) remains a real CPU lever.

## The real per-candidate split with `symmetric=true` (2026-11-18, measured)

```
copy the game                 10.1 us   4%
my observation                86.3 us  31%
opponent's public view        86.3 us  31%
three critics                 98.5 us  35%
total                        281.3 us
```

The observation is built **twice per candidate** (my view and the opponent's public view, which is what
`-Dares.agent.search.symmetric=true` compares), so **62% of the per-candidate cost is one observation built twice**, and the
critic is only 35%. My earlier estimate ("neural is the largest term") was measured without the symmetric second build and
is therefore wrong for the configuration we actually run.

That makes the next speed step cheap and safe: a **paired build** (`buildPair(game, me, opponent)`) that produces both
views in one pass, sharing the blocks that do not depend on the seat (planet, score, round, context, dummy pool) and
building only the seat-specific ones twice. It is gated by the same bit-exact differential test as the incremental work:
for real decisions of all 15 types, the paired result must equal the two independent builds, bit for bit. If a block turns
out to depend on the seat in a way the sharing does not capture, the test says so instead of the player getting silently
different inputs.

Second candidate after that: with `symmetric=true` the opponent's public view is unchanged by actions that only touch
private state (mulligan, sale, discard, draft) *if* money and points are not in the public block - to be read from
`buildPublic` and measured, not assumed.

## The critic structurally cannot gate the hand menus (2026-11-18, measured)

Mechanism test: all fifteen menus ranked, `policyChoice=MULLIGAN,SELL_CARDS`, per-type stop margins
(`stopMargins=MULLIGAN:100,SELL_CARDS:200`), 200 games, then `ml/mulligan_report.py`:

```
MULLIGAN   rows 400  stop 100.0%  games that discarded 0.0%
SELL_CARDS rows 1600 stop 100.0%  games that discarded 0.0%
```

Nothing changed, and the reason is structural rather than a bug:

1. the critic's **hand-identity block is masked** (`--drop-prefix cards:in_my_hand_`), so it cannot tell a junk card from an
   engine card - it only sees summaries;
2. a mulligan **replaces** the card with a random draw, so the afterstate's summaries are a random sample of the deck: the
   expected gain of "throw this one away" is not positive for any particular card, which is why
   `V(best candidate) - V(stop)` never clears the threshold.

So the act-vs-stop comparison cannot be handed to this critic for the hand menus. The correct division is:

* **critic-gated** menus: builds, standard actions, blue actions, payments, inputs, corporations, phases - the candidate
  changes something in the state the critic can see;
* **policy-decided** menus: mulligan, in-phase sale, and the choice of *which* card to discard/draft/sell. The policy sees the
  hand identities, so it is the model that can tell junk from an engine. It has to **learn** that from outcomes, because
  today it answers "stop" in 100% of the rows and has therefore never seen what discarding leads to.

The route for those two menus is therefore data, not the critic: explore them in the corpus (`MULLIGAN:0.4`,
`SELL_CARDS:0.3`) and weight the policy's rows by `outcome - V(s)` (`night_loop --policy-adv`), so an explored discard that
ended well is reinforced and one that ended badly is suppressed. The sensor `ml/mulligan_report.py` is the acceptance
instrument: "did it start discarding, and how many".

Mechanically the split exists and is tested end to end: `-Dares.agent.search.policyChoice=<types>` keeps the policy's
candidate (the critic then only re-orders nothing for those menus), and `-Dares.agent.search.stopMargins=TYPE:N` gives the
critic an act-despite-stop override per type - the global `stopMargin` cannot be used, because a build's afterstate beats the
stop by ~1300 units and the same threshold made the player build every offered card (-4.7 points, measured earlier).

## The role swap for the hand menus is refuted (2026-11-18, measured)

`-Dares.agent.search.policyChoice=MULLIGAN,SELL_CARDS,SELL_CARDS_LAST_ROUND,DISCARD_CARDS,DRAFT_CARDS` (the policy picks the
card, the critic only gates the act), 3000 games against SMART, same bundle and critics as the champion:

| card picker on the five hand menus | vs SMART |
|---|---|
| the policy (it sees the hand identities) | 56.87% [55.1, 58.6] |
| the critic (hand block masked, sees summaries) | **63.93% [62.2, 65.6]** |

Seven points in favour of the model that *cannot* see the cards. The intuition ("give the choice to whoever holds the
information") was wrong: the critic's "which removal lowers my hand value least" is a better rule than the policy's learned
pick, presumably because the policy was trained on a corpus where those menus were never exercised properly (100% stop, so
almost no positive examples).

Consequence: the split is not "policy picks, critic gates". The critic keeps ranking every menu it can rank; the only dead
part is the *act-vs-stop* of the mulligan and the in-phase sale, and it has to be learned by the policy from explored,
outcome-weighted games - not moved to another model.

## Exploring the in-phase sale made the generator sell every round, and the player collapsed (2026-11-18)

The iteration that fell to **43.1% [40.1, 46.2] against SMART** changed five things at once (generation without `symmetric`,
`MULLIGAN:0.4` + `SELL_CARDS:0.3` exploration, AFTERSTATE rows, critic replay, `--policy-adv`). Two things were measured before
blaming anything:

1. **What the corpus looks like** (`ml/mulligan_report.py` on the new pack, 20 000 games):

```
MULLIGAN              stop 64.5%  games that discarded 55.6%  mean 1.01 cards  max 8
SELL_CARDS (in-phase) stop 75.5%  games that discarded 89.7%  mean 1.96 cards  max 7
SELL_CARDS_LAST_ROUND stop 29.6%  games that discarded 100%   mean 5.45 cards  max 8
```

Before the exploration the in-phase sale happened in **0%** of the games; with a 30% uniform exploration the generator sold in
**89.7%** of them. That is the owner's point exactly: selling mid-round is a rare, strong-player decision ("обычно ты
продаёшь посредине раунда очень мало"), and a generator that sells every round teaches a player that sells every round.

2. **The loop's own rule was too weak to stop it**: it logged `REGRESSION 1/2` and started another identical iteration. Fixed
with `--abort-drop 0.06` (one iteration 6+ points below the best ends the night, keeps every artifact and logs which knobs had
changed).

What this does **not** yet say: whether the 43% came from the corpus or from `--policy-adv` (advantage-weighted policy rows,
used for the first time). The isolating run is a policy trained on the same corpus with the champion's recipe (no
advantage weighting); and the next corpus drops the sale exploration entirely, keeping only the mulligan at 0.4, generated
with `symmetric=true` so the generator itself is not weakened.

The lesson recorded for the levers: **exploration is not a safety dial, it is a teaching signal.** A 30% uniform sale teaches
"always sell"; a useful exploration of that menu has to be far more conservative (or the menu learned from outcomes instead of
imitation).

## The mulligan cannot be decided by the critic, in either formulation (2026-11-18, measured)

The owner's model is "discard the junk": the number follows the hand (sometimes many junk cards, sometimes few), and the
average is about half. Three measurements pin what happens instead.

**1. A fixed number is not the answer.** Champion bundle, `-Dares.agent.mulligan.cards=N` (the critic still picks which cards),
1000 games against SMART:

| forced discards | vs SMART |
|---|---|
| 0 (the champion as it plays) | 63.93% [62.2, 65.6] |
| 2 | 63.75% [60.7, 66.7] |
| 5 | **60.25% [57.2, 63.2]** |

Two is neutral, five costs 3.7 points: discarding a fixed number throws away good cards exactly as the owner said.

**2. The first gate discarded almost everything - because the afterstate lost the accumulation, not because of the draw.**
`junkGate=MULLIGAN` with "this card replaced by a random draw" gave 91.8% of gated decisions "free", mean **6.58** discards of
10, clustered at 6-8 (i.e. at the recording cap). The first explanation offered - "a random card usually beats the worst card"
- is **wrong** (owner's correction, 18/11): a random card is worth about an average card, so it is neither good nor bad.
The real cause: the engine does not touch the hand during the mulligan loop. It collects the chosen cards into a pending set
and replaces the whole set only when the player says stop. Every step's afterstate was therefore built from the *full* hand, so
the already-queued cards stayed in the copy: step 5 looked exactly as profitable as step 1, and the loop ran until the
candidate list ended. **Lost accumulation.**

**3. Removing the draw did not fix it (same bug, same symptom).** With a deterministic afterstate ("the hand without this card")
the gate still fired in 91.6% of decisions with a *positive* mean gap (+348 milli-logit): mean **9.10** discards of 12. Hand
size shrinking is what V likes in our corpora (a large unbuilt hand correlates with losing), so this variant is biased too -
and it is also the wrong question: a mulligan does not shrink the hand, it replaces cards.

The corrected afterstate is per-step marginal: step *k* scores `hand - (queued + candidate) + (k+1) random cards`, and the
"stop" baseline is `hand - queued + k random cards` (what the engine does when the player stops). Both hands have the same
size, so the difference is the value of replacing *that one card* - which is exactly the owner's rule "discard the junk, the
count follows the hand". Whether V can express it is now a measurement, not an argument.

A structural finding from this thread: `OnePlySearch` has **two** afterstate dispatchers (`applyOn`, used by the batch path,
and a separate block inside `score()`, used by the normal path). The mulligan was added to the first only, so on the normal
path every mulligan candidate scored as "no opinion" and the agent answered stop in 100% of rows - the very collapse it was
supposed to fix. Both dispatchers now carry every type; a test that enumerates `DecisionType` and asserts both handle the same
set is the obvious next guard.

## One dispatcher: the duplication is gone, the extra menus are not free (2026-11-18, measured)

`OnePlySearch` used to apply a candidate in two places: `applyOn` (the batch path) and a switch inside `score()` (the live
path, because `batch` is off by default). The mulligan was added to the first only, which is why the menu answered stop in
100% of rows even after it was "covered"; `UNMI_RT` and `COLLECT_INCOME` were missing from the live one for the same reason.
`score()` now delegates to `applyOn` - one dispatcher, no second copy to forget.

That also made the two missing menus live. Measured on the champion bundle, 1000 games against SMART:

| live path | vs SMART |
|---|---|
| champion as it was (the two menus return "no opinion") | 63.93% [62.2, 65.6] |
| one dispatcher, the two menus ranked | **62.55% [59.5, 65.5]** |

The intervals overlap, so this is not evidence that ranking them hurts - but it is certainly not a gain, so which menus the
critic ranks stays a flag (`--types`), not a code path. The structural fix is kept; the experiment is closed as
"no effect, do not repeat".

## The mulligan exploration reaches the rows now (2026-11-18, measured)

`-Dares.agent.explore=MULLIGAN:0.9` on 60 games, recorded with `--max-rows-per-game-type 12`: **mean 5.55 discards** per game,
max 11, stop in 26.2% of rows, 96.7% of games discarded at least one card. That is the owner's "about half", produced by
exploration rather than by a constant. Note the row cap: it SAMPLES per type, so at the old cap of 8 the stop row of the
longest mulligans was dropped at random - the menu being taught was the one being distorted. `--rows-per-type 11+` for any
run that is supposed to learn the mulligan.

## The phase critic is worth 4-5 points, in both directions (2026-11-19, measured)

The nightly iterations retrain the policy and the critics, and the loop hands the phase pick to the iteration's first new
critic (`--phase-critic`). That turned out to be the whole difference between the champion and the night's bundles. A 2x2,
1000 games against SMART each:

| ranking critics | phase critic | vs SMART |
|---|---|---|
| champion (`v_1118-0058_13_s1..s3`) | champion (`s1`) | 63.93% [62.2, 65.6] |
| night (`v_1118-2332_17_s1..s3`) | night (`s1`) | 63.50% [60.5, 66.4] |
| **night** | **champion** | **66.80% [63.8, 69.7]** |
| champion | night | 58.85% [55.8, 61.9] |

The phase mix explains it: the night's critics pick phases 2 and 3 in **88%** of turns (iterations 16-18: 39/44, 45/43,
48/47), the champion's critic picks 26/21/23/15/15. The phase pick is the one decision where the phase itself is the action,
so a critic that mis-ranks it costs points in every round of every game - the same critic is fine as a card ranker.

`night_loop.py` now has `--phase-critic-model DIR`: hold the phase pick on a fixed model while the card critics are retrained.

**A purpose-trained phase model does not replace it.** `ml/train_phase.py --decision PICK_PHASE` on the champion-era pack
gives val loss 0.554 against a 0.693 baseline, but `action_effect 0.0023` - swapping which phase was chosen between rows
barely changes the loss, i.e. the model predicts the game from the state and ignores the candidate. The candidate features of
a phase pick carry almost nothing on their own; the champion's critic works because it scores the *afterstate* of the phase
(the extra build, the income, the draft), not the phase label.

## The mulligan cannot be played out from where it happens (2026-11-19, measured)

The remaining honest way to judge "is this card junk" is to let the game answer: play the round out with the policy and read
the value at the next pick (`-Dares.agent.search.playout=MULLIGAN`, implemented, `roundScore` as for the phase pick). It does
not work at the mulligan, for a structural reason:

- 12 games without the playout: 0 failed. 12 games with it: **4 failed**, 18 games earlier: 10 failed.
- The failures are `IllegalStateException: Corporation <X> applies an on-built effect to itself, but the legal inputs ...`
  for `PoliticalInfluence`, `HeatReflectiveGlass` and friends.
- Cause: the mulligan happens BEFORE the corporation is chosen, so every rollout from a mulligan afterstate must answer
  `PICK_CORPORATION`, and inside a rollout the decisions are answered by the raw policy (`innerChoice`), which does not
  validate a corporation's on-built inputs the way `chooseCorporation` does. The rollout then dies on the first corporation
  that needs them.

So the mulligan is not reachable by a rollout at its own stage; it can only be learned by the policy head from the corpus,
and for that the corpus must contain *sensible* discards (a random discard teaches only that random discards are bad - the
policy trained on 5.135 explored discards per game answers **stop 100%**, measured). The next step there is exploration that
forces the act while keeping the card choice informative, not uniform-random.

### Confirmed at 3000 games

The mixed bundle (`policy/1118-2332_17` + `v_1118-2332_17_s1..s3` for ranking + `v_1118-0058_13_s1` for the phase pick):

| bundle | games | vs SMART |
|---|---|---|
| champion `1118-0058_13` (all three roles) | 3000 | 63.93% [62.2, 65.6] |
| **night policy + night critics + champion phase critic** | 3000 | **66.87% [65.2, 68.5]** |

+2.9 points, with the intervals overlapping by 0.4 - so this is a real candidate, not yet a promotion. The promotion path is
the HoF gate: profile, absolute worst benchmark, then a 2000-game duel against the champion, then 10 000 games because the
margin is close to the 1.5-point rule.

### 70% against SMART: score the phase pick by playing the round out (2026-11-19)

The phase pick was the weak decision, so it was moved from "V after the phase's opening gift" to "play the round out with the
policy and read V at the next pick" (`-Dares.agent.search.phasePlayout=true`, the `roundScore` path that already existed for
phase picks). Same policy and critics as the 66.87% bundle, one flag:

| bundle / flag | games | vs SMART |
|---|---|---|
| champion `1118-0058_13` | 3000 | 63.93% [62.2, 65.6] |
| night policy + night critics + champion phase critic | 3000 | 66.87% [65.2, 68.5] |
| the same + `phasePlayout=true` | 1000 | 70.75% [67.9, 73.5] |
| the same + `phasePlayout=true` | 3000 | 72.82% [71.2, 74.4] |
| **the same + `phasePlayout=true`** | **10 000** | **73.17% [72.3, 74.0]** |

Two other phase variants, same bundle, 1000 games:

| variant | vs SMART |
|---|---|
| phase critic ensemble (champion s1+s2+s3) | 65.05% [62.0, 67.9] |
| night policy + champion card critics + champion phase critic | 65.75% [62.8, 68.6] |

So it is the *rule* (roll the round) and not the model that moves the number: averaging three critics is worse than the one
champion seed, and keeping the champion's card critics is not required.

The puzzle is solved by the engine, not by the model: **+9.2 points** over the champion (63.93 -> 73.17, 10 000 games, disjoint
intervals), with the player's profile untouched in the ways the owner checks. The profile of this bundle
(`ml/check_player.py`, 60 games): builds **32.6/game**, build acceptance 76%, phases
**8.7 / 32.0 / 38.3 / 4.7 / 16.3**, standard projects in phase 3 **16.7%**, no warnings. The nightly bundles that scored 60-63%
picked phases 2 and 3 in 88% of turns; the rollout restores the balance (the champion was 26/21/23/15/15) and pays 16% of the
money into phase 5 (the draft), which the policy alone never chose.

**Caveat that must not be lost.** `roundScore` refuses to play when a seat is not a computer
(`getPlayerUuidToPlayer().values().stream().anyMatch(player -> !player.isComputer())`), because the engine cannot drive a
human's turns itself. So 72.82% is the **AI-vs-AI** number: in a game against a human the phase playout returns NaN, the
candidate is "no opinion" and the phase falls back to the critic's V - i.e. the 66.87% behaviour. The benchmark player and the
human-facing player are not the same player until that fallback is solved (drive the human seat with the policy inside the
rollout, or evaluate the phase with a rollout over a window that is safe to simulate). Any promotion has to say which of the
two it promoted.

### The floor moved too

Against RANDOM, same bundle and flags, 1000 games: **98.60% [97.7, 99.2]** (the champion was 92.8%). A player that beats
RANDOM 98.6% of the time and SMART 72.8% is not a specialist that farms one opponent.

### The final bundle's own behaviour (120 games, no exploration)

`ml/mulligan_report.py` on a corpus generated by the 73% bundle:

| menu | discarded | per game |
|---|---|---|
| `MULLIGAN` | **0** in 100% of games | 0 |
| `SELL_CARDS` (voluntary) | **0** in 100% of games | 0 |
| `SELL_CARDS_LAST_ROUND` (the hand-limit trim) | 100% of games, mean **9.41** | 6:2% 7:8% 8:22% 9:18% 10:23% 11:20% 12:7% |

The trim is the one menu that discards a hand-dependent number, and it is the rule, not a choice. The mulligan discards nothing:
the three routes to choosing cards there are measured out (fixed count, critic gate, learned policy on explored games), and a
rollout is impossible at that stage. It stays open with a named next step, not with an invented fix.

## Correction (2026-11-19, owner's question): the mulligan counts were per GAME, not per seat

The owner asked how "10.66 of 12" is possible when the opening hand is 10 cards. It is not: **a corpus records both seats**
(`--mode play` with `AGENT:AGENT`), so every "discards per game" number in this file was about twice the real one, and
`ml/mulligan_report.py` counted it that way. The sensor now prints **discards per seat** and the seats per game, so the error
cannot recur.

Re-read per seat (`mean_discards_per_seat`, the same packs, no new games):

| configuration | discards per seat | seats per game in the corpus |
|---|---|---|
| the 73% bundle, no gate | **0.00** | 2.00 |
| `junkGate` + `valueModel.mulligan` (hand identity), no playout | **7.97** | 1.34 |
| `junkGate` + `playout=MULLIGAN` + hand identity | **4.77** | 1.77 |

So the gate without a rollout really does discard almost the whole hand (7.97 of 10), while with the rollout it discards
**about half** - the owner's description - which is also what the forced-count probe measured as the *worse* half: 5 forced
discards cost 3.7 points against 0 (1000 games). A plausible count is not a good mulligan; the choice inside it still has to be
right.

A second unit: the recording cap of 12 rows per type per game **truncates the stop rows** when both seats mulligan long
(`1.34` and `1.77` stops per game instead of 2), so per-seat means are slightly overestimated. Any study of this menu must run
`--rows-per-type 22` (two seats times eleven rows).

## The mulligan gate, calibrated (2026-11-19)

With the owner's rule implemented - the afterstate founds the best of the two offered corporations, the rollout plays the
round out, and the critic sees the hand (`valueModel.mulligan`) - the gate's threshold is a **required advantage**: discard
only when removing the card improves V by more than that many milli-logits. Measured on 60-game corpora of the 73% player,
`AGENT:AGENT`, `--rows-per-type 22` (two seats times eleven rows; at 12 the stop rows of long mulligans are truncated):

| required advantage | "free" removals | discards per seat | games that discard |
|---|---|---|---|
| 0 (no threshold) | ~93% | ~5 | ~98% |
| 50 | 87.6% | **4.00** | 86.7% |
| 100 | 74.3% | **2.28** | 70.0% |
| 150 | 72.9% | 2.08 | 61.7% |
| 200 | 53.6% | 1.04 | 41.7% |
| 300 | 48.3% | 0.85 | 28.3% |
| 700 | - | 0.10 | 2.5% |

The distribution is bimodal rather than a spike: at threshold 50, 13% of games discard nothing, most discard 7-9 (both seats
together) and 20% discard 8 per seat. That is a hand-dependent shape - a fixed number would be one bar - and it separates the
two kinds of opening the owner described: a hand that is fine, and a hand clogged with cards that fit neither corporation.

Why a threshold at all: the gate compares the best of ~10 noisy candidates against one baseline, and a maximum of ten noisy
estimates is biased upward (the measured mean gap at zero true effect is +0.25 logit), so a threshold of 0 discards almost
everything. The threshold is the selection-bias correction, and it is calibrated on the count, not guessed.

### The calibrated mulligan pays (2026-11-19, 1000 games each)

The threshold is not cosmetic: it is the difference between "keep everything" and a mulligan that pays.

| mulligan | discards per seat | vs SMART |
|---|---|---|
| off (the 73.17% bundle) | 0 | 73.17% [72.3, 74.0] (10 000) |
| threshold 100 | 2.28 | 73.65% [70.8, 76.3] |
| **threshold 50** | **4.00** | **75.50% [72.7, 78.1]** |

The direction is the owner's: fewer, timid discards barely move the score; the aggressive setting that discards about four of
ten - the junk that fits neither corporation - is the one that pays. The count is not a constant: at threshold 50, 13% of games
discard nothing and the rest spread over 1-9 per seat, because the gate answers per card.

This is the first time the mulligan has been decided by the game rather than by a constant: the afterstate founds the best of
the two offered corporations, the round is played out with the policy, and the card critic *with the hand identities*
(`v_1119_hand`) judges the difference.

**The same caveat as the phase rollout applies.** These are AI-vs-AI games: `roundScore` refuses to play when a seat is not a
computer, so against a human both rollouts return NaN and the player falls back to the critic's one-action value (no mulligan,
the phase by V). The benchmark player is now measurably stronger than the human-facing one; that gap is the next structural
task, not a knob.

### The mulligan at 3000 games: neutral, not proven (2026-11-19)

| mulligan | games | vs SMART |
|---|---|---|
| off | 10 000 | 73.17% [72.3, 74.0] |
| threshold 50 (4.0 per seat) | 1000 | 75.50% [72.7, 78.1] |
| threshold 50 (4.0 per seat) | **3000** | **73.57% [72.0, 75.1]** |

The +2.3 points at 1000 games did not survive at 3000: the intervals cover the baseline. So the mulligan is now *decided by the
game* (per card, with the two corporations founded in the afterstate, the round played out, and a critic that sees the hand) and
it discards a hand-dependent number - but it has **not** been shown to add points. Saying otherwise would be reading noise.

The count is real, though (`ml/mulligan_report.py`, exact flags of the bundle, `--rows-per-type 22`): 13% of games discard
nothing, the rest spread over 1-9 per seat, mean 4.00. A constant would be one bar.

### What the phase mix actually is: a phase-3 fighter with one blind spot (2026-11-19, 120 games of the 73% bundle)

The owner's reading is right, and it is measurable. Upgrade ownership per game (observation columns `table_phase_N_app_M`):

| phase | upgrade 1 | upgrade 2 | what the upgrades do |
|---|---|---|---|
| **1** | **2.5%** | **5.0%** | discount / a second green build - the economy path |
| 2 | 16.7% | 21.7% | one more blue/red project |
| 3 | 0.0% | **100%** | upgrade 1 = double activation, upgrade 2 = take 3 blue/red cards |
| 4 | 25.0% | 10.0% | extra MC / double production |
| 5 | 1.7% | 60.8% | keep more of the draft |

Phase picks: 1 at 8-10% of turns (stable across the whole game), 3 at 33-42%. Of 553 phase-3 picks, 57% had no blue action that
round - but 81% of those owned the upgrade that draws three blue/red cards, so the pick still bought something. **10.7%** of
phase-3 picks had neither an action nor the upgrade: those are the "by habit" picks the owner saw at the table.

So the player is a coherent **phase-3 fighter**: buy the draw upgrade, draft blue/red, build them in phase 2, activate them in
phase 3. That is a real strategy and it is the current best bundle - it must not be retrained away. The phase-1 economy path is
almost absent.

### Simultaneity: known in the past, modelled in the rollout, unknown for the current round

The observation carries the *previous* phase of both players and the dummy's remaining pool, not the picks of the same round
(they are simultaneous - nobody can know them). The simultaneity is instead inside the rollout: `phasePlayout` plays the round
with the policy, so the other seat and the dummy pick their phases in the simulation and the candidate is valued against that.
The approximation is that the other seat is played by *our* policy, not by SMART or NETWORK: an opponent model is the missing
piece, and it is the same piece the human seat needs.

## How many games: the critic saturates in discrimination long before calibration (2026-11-19)

The owner asked how one knows a corpus is big enough. Same trainer, same flags, three packs of the current encoder:

| corpus | games | train rows | val AUC | val loss |
|---|---|---|---|---|
| a 120-game pack | 120 | 1 712 | 0.923 | 3.70 |
| a 5 000-game pack | 5 000 | 71 968 | 0.906 | **1.11** |

Discrimination (AUC) is already at its ceiling with 120 games - the critic can tell a winning position from a losing one from
about a thousand rows. What keeps improving with data is **calibration**: the loss falls by a factor of three from 120 to 5 000
games. That matters because the search compares *values of the same position* (a candidate against the stop), which is a
difference of two calibrated numbers, not a ranking of distant states. It also explains why tiny corpora produced bundles that
were good at ordering cards and bad at "act or stop".

So the size of a corpus is not set by "can the critic learn", it is set by "how fine a difference must it resolve", and the
resolution of a *decision* is measured directly by the play, not by the loss.

## The rollout generator runs out of memory every ~250 games (2026-11-19, fixed in the pipeline)

With `phasePlayout` and `playout=MULLIGAN` the self-play JVM grows past any heap (tried 10, 13 and 18 GB) and exits with code 6
every few hundred games: two 2 000-game runs died at 93 and 252 games. The engine message says the pipeline is expected to
restart it after the finished chunks, and `night_loop.play` now does exactly that - 250-game chunks, `--count-chunks-from`, and a
restart on exit 0 or 6 while games are missing (`should_restart`, 4 tests). The corpus is the expensive artifact; throwing it
away because the JVM died is the one thing that must not happen.

The leak itself is still open: ~40 MB per game is retained by something in the rollout path. It is worth fixing in the engine
(the same rollouts are used at play time), but it no longer stops a night.

## Specialist B failed, and the reason is the recipe (2026-11-19, measured)

Attempt 1 at a phase-1 fighter: a 2000-game corpus from the 73% player with `PICK_PHASE:0.5` and `BUILD_CARD:0.3` exploration,
then three critics and a policy trained on it, then the same flags as A.

- The corpus was **diverse** as intended: phases 15/24/27/15/19 against the generator's 9/26/38/9/16, and its fingerprint says
  "phase-3 fighter; phase-4 fighter" (upgrade 3.2 in 98.9% of games).
- The player: **21.1% [18.7, 23.7] vs SMART** (A is 73.2%).
- The fingerprint of the new player: phases 10/32/37/7/14 with A's phase critic, 17/32/32/4/15 with its own - a small shift,
  not a different archetype.

So heavy exploration does not create a specialist, it destroys the corpus: with half the phase picks and a third of the builds
random, the games are mostly noise, and both the policy and the critics learn it. The same lesson as the 30% sale exploration
that took an iteration to 43.1%. A specialist must be grown from *competent* games whose **plan** differs, not from games with
random actions.

Attempt 2 (running): the same player, no phase exploration, and a temporary `phaseBias=1:0.6` during generation - the play is
still the search's, only the phase choice is pushed. That is a curriculum: the games stay good, the plan differs, and the bias
disappears at play time.

## The critic's learning curve, controlled (2026-11-19)

Nested packs from one corpus (1/2/4/8 chunks of 250 games), one trainer, one seed:

| chunk | games | train rows | AUC | val loss |
|---|---|---|---|---|
| 1 | 250 | 3 616 | 0.776 | 3.77 |
| 2 | 500 | 7 216 | 0.855 | 1.97 |
| 4 | 1 000 | 14 416 | 0.822 | 1.46 |
| 8 | 2 000 | 28 768 | 0.855 | **1.48** |

Calibration plateaus at 1 000-2 000 games; discrimination earlier still. A 5 000-game corpus is a 2.5x reserve for the critic,
not a requirement - the binding constraint is the *quality and diversity* of the games, which is exactly what attempt 1 above
broke by exploring too hard: 34 000 rows of noisy play made the player twice as bad.

## Why a phase-1 specialist cannot be forced into existence (2026-11-19, measured)

Three measurements settle it, and the owner's prediction ("a pure push in isolation will only hurt him, he is optimised for
his own scheme") was right:

| attempt | what changed | vs SMART |
|---|---|---|
| A as it plays | nothing | 73.17% [72.3, 74.0] |
| A with `phaseBias=1:0.6` | the phase CHOICE, nothing else | **44.25% [41.2, 47.3]** |
| B from a `PICK_PHASE:0.5` exploration corpus | the whole corpus | 21.1% |
| B from a `phaseBias=1:0.6` curriculum corpus (phases 37/17/20/13/14) | the corpus, competent play | 32.9% with A's phase critic, 25.3% with its own |

So the weakness is not the phase *choice*: forcing A to pick phase 1 costs 29 points, because **A cannot play phase 1 well** -
its builds, discounts and draft picks inside that plan are untutored. The curriculum corpus inherits exactly that, which is why B
is a different player (phases 15/29/18/15/23 - phase 3 down from 37%) and a much worse one.

What the owner's own game proved, then, is subtler and more important than "phase 1 is strong": he built the income/discount
engine and beat the AI *because the AI does not contest it* - the sweetest green cards came to him unopposed. In a two-player
game that is value created by the opponent's indifference, and a one-ply value net cannot see it: the card is worth the same
whether or not the other seat wants it.

Two consequences for the plan:

1. A phase-1 specialist must LEARN the plan (its play, not only its pick), which needs a corpus of good phase-1 play - and no
   such corpus exists yet. Forcing the pick produces bad phase-1 play, and training on it reproduces the bad play.
2. The delayed payoff of the economy plan is outside a one-round rollout: a discount or a second build pays over many rounds.
   The instrument for that is already in the engine (`-Dares.agent.search.horizon`, `horizonStop`) but it is global; what is
   needed is a per-type horizon so the PHASE decision can be played out over several rounds without paying for it on every
   card menu.

The adaptive player stays the goal, but its two ingredients have to be strong before mixing them means anything: a mix of a
73% fighter and a 25% fighter teaches the adaptive player that phase 1 loses.

## Correction: the opponent block IS load-bearing (2026-11-19)

Earlier today I wrote that the player "ignores the opponent" because a critic trained with every `op_` and hand column zeroed had
almost the same loss (1.11 -> 1.16) and AUC (0.906 -> 0.901). **The loss was the wrong instrument.** The same three blind
critics in the search, everything else identical, score **57.4% [54.3, 60.4]** against 73.17% - the opponent block is worth
**15.8 points**. The training loss barely notices because it is dominated by everything else that predicts the final margin; the
*decision* depends on the differences the block carries.

What remains true is narrower and still useful: the **phase choice** does not respond to the opponent's lean. Across the corpus,
phase 3 is picked 31% when the opponent has no blue cards, 40% at 0.1-0.3 and 38% at 0.4+; picking phase 4 does not rise when the
opponent's income is high (13% vs 10%). So the block is used, but not for the decision the owner pointed at.

Lesson for the plan: a probe on the *loss* is not evidence about behaviour. Any "the model does not use X" claim has to be made by
playing with X removed, not by training without it.

## H1 (delayed payoff) is half right and closed (2026-11-19)

Theory: the economy plan pays over many rounds, so a one-round rollout cannot see it and the phase pick falls back to what is
usable now. Test: `horizon=2` on PICK_PHASE only, with the stop played out over the same two rounds
(`horizonTypes=PICK_PHASE horizonStop=true`).

- **The plan moves exactly as predicted**: phases 16/20/24/18/22 against 10/30/35/8/18 - phase 1 and phase 4 up, phase 3 down.
- **The player gets worse**: 65.08% [61.2, 68.8] at 600 games against 73.17% [72.3, 74.0].

So the one-round window is not what holds the player back; a longer playout changes the valuation and degrades it (more variance
in the estimate, and the leaf is read in a state the policy reaches by playing two rounds, not one). The decision rule written in
the plan - keep it only if the win rate holds - closes H1 at the first honest test. The next hypothesis is H2 (the quality of the
value itself), which does not change the decision rule, only the numbers it compares.

## The generator's memory was the DECISION LOG, and it was polluting every corpus with phantom rows (2026-11-19)

The owner saw "OUT OF MEMORY" in the generation log. A class histogram of the live JVM (jcmd GC.class_histogram) settled it in one
look:

| retained | size |
|---|---|
| 4 607 040 `float[]` | **8.0 GB** |
| 3 685 272 `Candidate` | 88 MB |
| 921 046 `DecisionLogs$Pending` | 37 MB |
| 921 046 `Decision` | 29 MB |

So the memory was not the rollout's game copies: it was **921k decision-log rows waiting to be written**, each carrying an
observation vector. Cause: `AgentDecisionService.choose` records every decision it takes, and a rollout is played through the
full agent, so **every hypothetical decision of every playout was recorded as if the game had taken it**. The corpus was
therefore polluted with phantom rows (hundreds per game, all labelled with the real game's outcome), and the log queue grew
until the JVM died - every ~250 games, which is why the pipeline needed restarts.

Fix: `OnePlySearch.isBusy()` exposes "a hypothetical computation is in progress", and all recording in `AgentDecisionService`
goes through one `record(...)` helper that skips when it is set. Consequence: the corpora generated today *with rollouts*
(`p1cur`, `p1b`, `h1cur`) contain phantom rows, so specialist B's failure is confounded - the clean retest is in progress. The
73.17% measurement is not affected: recording does not change how the player decides, only what is written down.

## The rule that explains every failure today: a corpus inherits the strength of the player that played it

| generator | its own score | corpus it produced | player trained on it |
|---|---|---|---|
| A as it plays | 73.17% | 5 000 games | 73.17% (itself) |
| A + `phaseBias=1:0.6` (forced into phase 1) | 44.25% | 2 000 games | 32.9% / 25.3% (B) |
| A + `horizon=2` on the phase | 65.08% | 1 000 games, clean | **21.0%** (H1b) |
| A + `PICK_PHASE:0.5` exploration | ~50% (random phases) | 2 000 games | 21.1% |

The horizon corpus was regenerated *after* the phantom-row bug was fixed, so H1b is not a data artefact: the corpus is clean,
its fingerprint shows the horizon's plan (phases 15/24/24/17/20 against A's 10/30/35/8/18), and the player trained on it scores
**21.0%**. A weaker player's games teach a weaker player, at any corpus size.

Consequence for the plan: **self-play can only improve the decision RULE, not the plan, when the generator is the best player.**
What worked all day was exactly that: `phasePlayout` (+5.9), the phase critic (+3.3), the mulligan machinery (neutral), the
opponent weight (about to be measured). What failed was every attempt to grow a plan from a deliberately weakened generator.

That also re-opens H4 in a legal way: the hall of fame holds a *strong, genuinely different* player - the previous champion
`1118-0058_13` at 63.9% with a balanced phase mix (26/21/23/15/15). A corpus of **A vs the old champion** is interaction data
between two strong players of our own, with no benchmark in it.

## H5: the opponent's value is already weighted as well as it can be (2026-11-19)

`-Dares.agent.search.opponentWeight=w` makes the candidate score `(V(mine) - w * V(opponent)) / 2`. The default 1 is what
`symmetric=true` always did; 0 is blind to the opponent, 2 doubles the weight on denying him. 1000 games each:

| weight | vs SMART |
|---|---|
| 0 (blind) | 70.80% [67.9, 73.5] |
| **1 (default)** | **73.17% [72.3, 74.0]** (10 000) |
| 2 (deny twice) | 71.30% [68.4, 74.0] |

Both directions are worse, so the opponent term is load-bearing (removing it costs 2.4 points) and its weight is already at the
optimum: **the cheap form of "contest what he is building" is already in the search**, and pushing it further buys nothing.

The expensive form - knowing what the opponent *intends* to build, not merely how good his position is - needs games against a
genuinely different strong player, which is what the A-vs-old-champion corpus is for.

## H4: mixing two strong policies in one corpus trains the average, and the average is worse (2026-11-19)

The legal way to give the player a different strong opponent: 1000 games of **A (73.17%) vs the previous champion (63.93%)**,
`--models` A, `-Dares.agent.models2` the champion, `--matchups AGENT:AGENT_V2:1`, A's critics on both seats. The corpus is
clean (generated after the phantom-row fix), both players are ours, and no benchmark is in it. Fingerprint: phases 16/24/24/17/19
against A's 10/30/35/8/18 - two plans really are in the data.

Result: the policy trained on it scores **31.2% [28.4, 34.1]** against SMART.

Together with the earlier numbers this is one rule with two faces:

| corpus | whose decisions | trained player |
|---|---|---|
| A alone (5 000 games) | one strong policy | 73% (itself) |
| A weakened (horizon 65%, bias 44%, exploration) | one weak policy | 21-33% |
| **A + champion (both strong, different)** | **two strong policies** | **31%** |

So a corpus is not a bag of good decisions: the policy trained on it converges to something like the *average* of whoever played,
and an average of two different strong players is worse than either. The self-play loop improves only because the generator is
always the current best player and the changes are made on the **search/value side** - which is exactly the class that worked:
`phasePlayout` +5.9, the champion's phase critic +3.3, `opponentWeight` already optimal, `stopMargins` -3.6, `horizon` -8.

Consequence for the plan: stop generating corpora to grow plans. The remaining levers are (a) a better value on the *same* corpus
(capacity + data, postponed), and (b) a deeper search (an opponent-response ply instead of the opponent's current value).

### Why the mixed corpus is worse than "the average": the value fit degrades

The h4 corpus is not broken data - both seats are recorded, the rows are balanced (10 974 / 11 026), and the games are 50/50
(the champion's policy with A's critics is as strong as A, which is why the duel is interesting). What breaks is the *value*:

| corpus | critic AUC | val loss | player trained on it |
|---|---|---|---|
| A alone (5 000 games) | 0.906 | **1.11** | 73% |
| A + champion (1 000 games) | 0.872 | **1.97** | 31% |

The same position is reached by two different play styles whose continuations differ, so a single V has to average two
continuation distributions - nearly double the loss - and the policy trained on it inherits that blur. **A corpus must come from
one policy.** That is why the loop works at all (the generator is always the current best) and why every attempt to inject a
second style through data has failed today: exploration (21%), bias (25-33%), horizon (21%), and now a second strong policy (31%).
The gains have to come from the search and the value, not from the corpus.

## The value target matters most when the data is scarce (2026-11-19, last measurement of the day)

One fresh clean corpus of A (500 games, generated after every fix), one policy (A's), two critics trained on exactly that data:

| value target | vs SMART, 1000 games |
|---|---|
| the single final margin (what the loop uses) | **11.6% [9.7, 13.7]** |
| the game's seven-channel breakdown, exported as V = sum | **19.8% [17.4, 22.3]** |

Two things at once:

1. **The target matters a lot when the data is small**: the breakdown decomposition is worth +8.2 points from 500 games. It splits
   the credit per channel (terraforming, forests, printed points, resources, milestones, awards, extra), so a card that scores two
   printed points teaches something even in a game lost on tempo.
2. **Data size dominates for play**: the same policy with the night's critics (5 000 games + replay) scores 73.17%, while critics
   from 500 games score 11.6%. The learning curve measured on the *loss* plateaued at 1 000-2 000 games, but the *play* needs far
   more - the loss is not the instrument, again.

Next session's first experiment, with a number behind it: a clean 5 000-game corpus of A carrying the RANKING sidecar, then
breakdown vs margin at that size, 1000 games each.

## P2: playing the round out after a build changes nothing (2026-11-19)

The owner's hypothesis, stated precisely: a one-ply value should already see "money out, tags and income in", so why is depth
needed? If the myopia of the build decision were the bottleneck, scoring a build by playing the round out with the policy
(`playout=BUILD_CARD`, the same trick that gave +5.9 for the phase pick) would help.

| bundle | vs SMART |
|---|---|
| baseline (one-action afterstate for builds) | 73.17% [72.3, 74.0] (10 000) |
| **build scored by a played-out round** | **73.15% [70.3, 75.8]** (1 000) |

Identical, at twice the CPU cost. So the build decision's myopia is **not** the bottleneck: the round playout neither helps nor
hurts, which means the choice between building and not building (where `rankOnly` keeps the policy's stop) dominates whatever the
ranking of the cards could add.

A methodological note worth keeping: the 40-game smoke of this flag showed **32-8 (80%)**; the 1000-game measurement put it at
73.15%. Small samples flatter any change that touches many decisions.

Where that leaves the plan: the phase decision (the PLAN) is the only place where the search has produced a measured gain
(+5.9), and forcing or lengthening it fails (bias 44%, two-round horizon 65%). The value's accuracy is the remaining lever with
evidence behind it (breakdown +8.2 at 500 games), and its 5000-game corpus is generating.

## Our V is blind to a different plan - and this is measurable (2026-11-19, owner's question)

The owner asked: run our value over finished games against NETWORK - does it systematically mis-score the other side, because it
only ever saw phase-3 games? It does, and here is the number. 200 games of AGENT vs NETWORK (we win 37%, mean margin -13), our
three critics evaluated on every PICK_PHASE row (`ml/value_check.py`):

| | value |
|---|---|
| AUC(V -> win) | 0.866 overall, 0.894 / 0.839 per seat |
| **mean V** | **+0.94 (seat 0) and +1.28 (seat 1) logits** |
| mean margin on those rows | -13.1 and -4.3 points |
| "V > +0.8 and lost" | **1 188** of 3 114 confident rows |
| "V < -0.8 and won" | 5 |

So the value still *ranks* (AUC ~0.87) but its *absolute* calibration is wrong by about a whole logit in the optimistic direction:
in games we lose two out of three, it reports "winning" on both seats. That is not a bug in the network - `V(s)` is trained as
"who wins if both sides play our game", and against a different plan the continuation is different. The search then trusts an
over-optimistic value and keeps its plan while the other one wins.

Two consequences:

1. **The UI now shows exactly this number** (`GameController.winProbability` asks `OnePlySearch.winProbability`), so a human game
   will display the agent's own, over-optimistic estimate - the same bias the owner can now watch live.
2. **A value trained against a different strong plan should be less blind**, and the policy need not be touched at all: the
   earlier failure of the A-vs-champion corpus was the POLICY (it learned the average, 31%); the CRITIC trained on the same games
   may be exactly what blind-spot removal looks like. Measuring that now: A's policy + the A-vs-champion critics, against SMART
   and against NETWORK.

## Not blind to stockpiles: the value prices 200 MC, and the old NETWORK was the misleading one (2026-11-19)

The owner played a game in which he held 200 MC and 80 heat and converted them into ~90 points on the last turns; the AI's
displayed win chance went 85-90% -> 53% (at 90-90) -> 5% after the conversion. He asked whether our value is deaf to large
numbers (his own NETWORK divided by 100 without clipping and "always worked qualitatively").

Measured on our critics (three-model ensemble, one real state, only the feature changed):

| my MC | V (logit) | | opponent MC | V |
|---|---|---|---|---|
| 0 | +0.08 | | 0 | +0.50 |
| 100 | +1.77 | | 100 | -1.36 |
| **200** | **+2.92** | | **200** | **-2.20** |
| 400 | +4.64 | | 400 | -3.43 |

and opponent heat 80 -> -0.54. So the value is not deaf: it prices my stockpile at P~0.95 and the opponent's at P~0.10, and the
standardisation does not clip there (mean 0, max99 0.326, so 200 MC is z=6.1 against a clip of 40).

**What was deaf was the OLD NETWORK**, whose prediction the UI showed until today (`nnService ... ModelType.SECOND`) - the
85-90% while losing is its number, not our agent's. The UI now shows our own estimate, so the next game can be compared directly.

What remains true is the PLAY side: the agent does not stop the opponent from accumulating resources in the first place. That is a
plan question (it plays its own engine and never contests the stockpile), not a pricing bug - and it is the same "contest"
question the opponent weight could not answer.

## What the agent actually builds, by name (2026-11-19)

`--mode cards` now also writes `id -> name` (`cards_by_expansion.ids.json`); without it the corpus's card ids could not be
named (the names list was sorted, which drops them). On 1250 games of the current player (27 500 build decisions):

| card | built | per game |
|---|---|---|
| StandardTechnology | 357 | 0.29 |
| AntiGravityTechnology | 365 | 0.29 |
| Herbivores | 260 | 0.21 |
| **Fish** | 64 | **0.05** |
| MoholeArea | 7 | 0.006 |
| **VolcanicSoil** | 14 | **0.011** |

So it does build the converters - StandardTechnology is among its six most-built cards - but it never plays the snowball: Fish
appears once in twenty games. The reason is in the money distribution of its own states: **median 3 MC, 95th percentile 13 MC,
maximum ~60** (`mc` normalised by 100). A human who saves 200 MC and converts it in one turn lives entirely outside that
distribution, so the plan is neither valued nor played.

That is the honest answer to "why did I win with the economy engine": not that the value is blind to money (it prices my 200 MC at
+2.9 logit and the opponent's at -2.2), but that the player never produces the state in which the conversion exists to be valued.

## Guided exploration: closed (2026-11-19)

Value-guided exploration (`-Dares.agent.explore.guided`, softmax over the search's own scores instead of a uniform draw) produced
a corpus whose plan is genuinely more diverse - phases 14/25/29/14/18 against the generator's 10/30/35/8/18 - and critics trained
on it score **14.0% [12.0, 16.3]** against SMART. So softening the exploration was not enough: the games are still weaker than
the generator's, and a corpus inherits its generator's strength.

That closes the last data-side mechanism. Summary of the closed set: uniform exploration 21%, guided exploration 14%, phase bias
25-33%, horizon corpus 21%, two strong policies mixed 31%.

What is left is the search, and the owner's framing is the right one: not full MCTS (hidden information, chance, branching 10-40
per decision over ~500 decisions), but a **PIMC-style targeted deepening** - K determinisations, deeper only on the decisions that
change the plan (the phase), a policy prior to prune, V at the leaf. The measured asymmetry is the argument: one round on the
phase +5.9, two rounds -8, one round on a build 0. Search pays where the decision is a plan, not by depth.

## The value's target does not matter at this corpus size (2026-11-19, last measurement)

Same 1250-game corpus, same policy, two critics trained on exactly that data, 1000 games each:

| target | vs SMART |
|---|---|
| single margin | **30.6% [27.8, 33.5]** |
| seven-channel breakdown | **27.9% [25.2, 30.7]** |

So the +8.2 points the breakdown showed at 500 games was noise: at 1250 games both targets are ~30%, and with 5000-game critics
the same policy reaches 73%. **Corpus size dominates the target**, by more than 40 points. The breakdown remains a noise-reduction
idea (it cannot price MC or cost reducers anyway), but it is not the lever either.

## The plan preference lives in the VALUE, not in the search (2026-11-19, plan probe)

The cheapest way to ask "can the value prefer a different plan if it is allowed to play one?" is to commit the rollout to the
candidate phase: `-Dares.agent.search.planProbe=true` makes the played-out round keep our seat on that phase at the next pick, so
the comparison is "commit to this plan" instead of "play one round as usual".

Neither moves the plan: baseline 10/30/36/6/17, with the probe 8/34/38/3/17. Even when the rollout is FORCED to play the other
line, the value still prefers phase 3. So the preference is inside V, not in the search structure - which is why every structural
change (horizon, samples, build playout, plan probe) moves nothing, and why a bigger tree would only execute the same preference
more precisely.

Everything measured today therefore points at one thing: the preference comes from the corpus (a corpus inherits its generator's
strength, one corpus = one policy), and the corpus size is the dominant quality factor (500 games -> 11%, 1250 -> 30%, 5000 -> 73%).
The next high-information experiment is not a new search but **much more of the same data**: one night of 20 000 games with the
current pipeline, then the same measurements.

## Night of 19/20: 10 000 games, a better critic, and the same player (2026-11-20)

The 10 000-game corpus (the day's bundle generating itself with the phase rollout, 40 chunks, 46.7 GB):

| quantity | before (5 000 games) | after (10 000 games) |
|---|---|---|
| critic AUC | 0.906 | **0.934** |
| critic val loss | 1.11 | **1.07** |
| vs SMART | 73.17% [72.3, 74.0] (10 000) | **72.72% [69.9, 75.4]** (1 000) |
| vs NETWORK | 39.8% [37.7, 42.0] (2 000) | 42.0% [37.8, 46.4] (500) |
| phase mix | 10/30/36/6/17 | 10/31/40/4/16 |
| builds, standard projects in phase 3 | 32.6, 16.7% | 32.4, 15.7% |

So the critic got measurably better and the player did not move. The corpus-size effect saturates between 1 250 games (30%) and
5 000 games (73%); doubling again adds nothing to the play. The phase mix does not shift either - 10x more of the same data from
the same policy is 10x more of the same plan.

## The tree does not beat the flat search either (2026-11-20)

`-Dares.agent.search.tree=PICK_PHASE -Dares.agent.search.tree.budget=32` - the PUCT allocation with the policy's prior
(`NeuralPolicy.probabilities`), 32 samples per phase decision, each with its own determinisation: **71.65% [68.8, 74.4]** against
73.17%. Unit-tested selection rule (`TreeSelectionTest`, 4 tests), but at play time it is the same plateau.

That closes the last structural idea the measurements allowed. Everything at play time lands between 71 and 73:

| change | vs SMART |
|---|---|
| champion (one ply, phase by a played-out round) | **73.17%** |
| tree on the phase (budget 32) | 71.65% |
| tree/horizon variants, samples=8, build playout, input playout | 65-73.15% |

The explanation is now uniform and measured three ways (2-round horizon, 8 determinisations, plan probe, tree): **the preference is
inside V, V's preference comes from the corpus, and the corpus comes from the player.** More search on the same V cannot leave
that loop, and more of the same data does not move it either.

## The stockpile cannot be created by spending behaviour (2026-11-20, save instrument closed)

The owner's observation: the player never reaches 200 MC, so it never learns the conversion line. Instrument:
`-Dares.agent.generation.saveAbove=25` - during generation a build is declined while the player holds more than 25 MC, so the
games should enter stockpile states.

It does not work, and the reason is structural: the money distribution does not move. p95 **0.15 (15 MC)** in both corpora, the
maximum even lower than usual (0.54 against 0.71). Declining builds only redirects the money into other spends; the player's
*income* (~10-20 MC a round) never produces a 200 MC stockpile. A human's 200 MC is the product of a production economy, not of
thrift: production cards raise the income, the income accumulates, the stockpile converts.

So the missing plan is "build the production economy", and the player does not build it (phase 4 = 4% of picks, the phase-4
upgrade in 28%/14% of games). That is a plan, and plans live in the value - exactly the loop we could not break by data or search.

## Where the night leaves the project (2026-11-20, 05:40)

| bundle | vs SMART | vs NETWORK |
|---|---|---|
| champion (5 000-game critics, phase playout) | **73.17%** [72.3, 74.0] (10 000) | 39.8% [37.7, 42.0] (2 000) |
| 10 000-game bundle (better critic: AUC 0.934, loss 1.07) | 72.72% [69.9, 75.4] (1 000) | 42.0% [37.8, 46.4] (500) |
| the tree (PUCT + prior, budget 32) | 71.65% [68.8, 74.4] (1 000) | - |

Four independent mechanisms now say the same thing - more search on the same value does not move the plan, and more of the same
data does not move the value's preference: a 2-round horizon (-8), 8 determinisations (-6.3), the plan probe (no shift), the tree
(-1.5). The remaining levers are not knobs: a plan-conditioned policy set (which needs a corpus of good alternative play that does
not exist), a full AlphaZero with values trained on search returns (days, unproven), or throughput work so a 50-100k-game loop is
affordable and the plan is given a chance to appear on its own.

## TD critics in the flat search: 72.50%, i.e. nothing - and that was the prediction (2026-11-20, morning)

Three TD critics (`--td-bootstrap` on the day's margin critic, 3 steps, exact per-step score reward, exact final margin on the last
rows): AUC 0.905 / 0.904 / 0.903, loss 1.577 / 1.578 / 1.574 - the same quality as the margin critics (0.934 / 1.07) on a
different target.

| search | critics | vs SMART |
|---|---|---|
| flat one ply (champion setup) | margin | 73.17% (10 000) |
| flat one ply, identical flags | TD | **72.50% [69.7, 75.2]** (1 000) |

Why this is the expected outcome and not a failure of the target: a ranker compares two afterstates of ONE position, so a shared
slope (the calibration TD fixes) cancels out. The 16/11 decider measurement already said the same: a well-calibrated critic moved
the ranker by nothing (43.59% -> 42.98%).

The open half of the prediction is the line tree, where values are SUMMED along a path and a shared slope no longer cancels. It is
being measured against the same flags with margin critics (400 games per arm).

## The line tree's cost is real, not a depth-cap bug (2026-11-20, morning)

The first smoke ran 40 games in 1 007 s (0.03 games/s, 30x the flat search). Diagnosis with `-Dares.agent.search.debug=1`:

- the cap DOES fire: `[TREE] iteration i: our decisions taken 4` - the driver answers exactly `depth` of our decisions per line;
- the price is the simulation between them: to reach 4 of our decisions the engine resolves ~150 decisions of both seats (the
  opponent's policy play, blue actions, discards, builds).

So a line tree is a genuine search and costs what a genuine search costs; it is affordable at play time (0.03 games/s is still
faster than a human) and unaffordable for corpus generation, which is why throughput work (S4 in `PLAN_V3.md`) stays behind the
tree proving itself.

**The confound was found before the result was believed.** The first tree run had TD critics trained on 10 000 games and margin
critics from 5 000, so a TD win could have been a data win - and the day already knows extra data does nothing (10 000 -> 72.72%).
The run was stopped and a MATCHED control trained: identical packs (`1119-2204_22`, `1118-2332_17`, `1118-2332_18`), identical
seeds 1..3, identical recipe, only `--td-bootstrap` absent.

| critics (same data, same seeds) | AUC | val loss |
|---|---|---|
| matched margin `v_m10k_s1..s3` | 0.933 / 0.931 / 0.931 | 1.073 / 1.116 / 1.093 |
| TD `v_td_s1..s3` | 0.905 / 0.904 / 0.903 | 1.577 / 1.578 / 1.574 |

The TD critic orders final outcomes WORSE (it does not fit the final margin by construction - it fits the local change plus a
bootstrap). That is expected and it is why the test is not "which AUC is higher": the question is whether the tree can rank lines
with each of them. Both arms now run with the matched pair (400 games each, same seeds, so the games are pairable).

## V is policy-conditional, not "biased towards blue cards" (2026-11-20, owner's example)

The owner's two examples, recorded verbatim in substance because they name the failure exactly:

1. **Production, no points yet.** A player builds 5 steel income and 5 titanium income and spends a lot of money. His opponent
   builds blue actions and presses them, collecting money and points. Our value calls the first position much worse and the second
   one "genius - lots of blue buildings, lots of points". The first player is the owner, and he beats the "phase-3 brawler" with one
   hand.
2. **AssetLiquidation.** After four rounds TR is down to 0, but there are many green cards. The owner would win; our value says he
   will lose.

**What is actually happening.** `V(x)` is not the value of the position. It is `E[final margin | x, OUR POLICY keeps playing]` - a
*policy-conditional* value. In example 1 the production position loses **under our continuation**, because our player will spend
next round's money on one more blue card and convert none of the income. So V is right about our play and wrong about the position.
That is not a bias towards blue cards in the features; it is the conditioning.

Two measurements already in this document say the same thing from the other side:

- V **sees** the delayed state: my 200 MC -> +2.92 logits, the opponent's 200 MC -> -2.20, his 80 heat -> -0.54. No blind spot in
  the input.
- Against NETWORK, V's mean margin is **+0.94/+1.28** in games it loses two out of three: a value fitted to our own policy's games
  answers "what would WE do from here", not "what is this position worth".

**Two consequences that close most of what we tried:**

- **Depth cannot fix it.** Every deeper search evaluates a leaf with the same V, i.e. asks the same question about a state our policy
  cannot play out. This is why 2 rounds (-8), 8 determinisations (-6.3), a build playout (0), an input playout (-1.5) and the line
  tree all landed at or below 73.17%: not noise, a different question.
- **Horizon cannot fix it either, and this is arithmetic:** a production card pays over 10-15 rounds. Our horizons were 1 round, 2
  rounds, 3-4 decisions. A horizon shorter than the payoff's time scale cannot see the payoff at any value quality. The only horizon
  that contains the delayed value is the end of the game.
- **TD cannot fix it.** `reward + V(next)` carries the bias into the label through the bootstrap and then converges to the same
  preference with less variance - a more confident version of the same mistake.

**What is left, stated as a measurement rather than a hope.** The only judge that carries no policy-conditional bias is the game's own
final score. Its problem is variance (the owner's 90 -> 180 last round), and the answer to variance is *pairing*: both candidates
played from the same determinised world, so the variance of the world's initial assignment is common. **How far that pairing actually
holds is measured below and it is much weaker than "the same world"** - the owner's objection of 20/11.

### Pairing does not survive the first divergence (owner's objection, 20/11)

The owner's point, in his words: one decision can change who draws first, so every later draw shifts by one and almost every card the
two branches see differs. A shared seed and a "similar world" therefore buy nothing.

**He is right, and the code says so.** `Determinizer.shuffle` (agent search) fixes the hidden assignment at the ROOT with a seed, so
all candidates of one menu start from the same world. But every shuffle generated DURING the game is unseeded:
`ShuffleService.shuffle` uses `ThreadLocalRandom.current()` (line 18), and that is what builds the tier decks (`CardService` 134-160),
the dummy hand (`MarsGame.updateDummyHand` 338) and the draft rows. Draws themselves are pops from a deck, so a different number of
pops at a different time changes which card each branch receives even without a re-shuffle.

Consequences, and they change the design:

- **Per-decision judging is not defended by pairing.** After a few rounds the branches are effectively independent, so the difference
  between two candidates carries roughly the variance of two independent games. The judge may not decide a single move on that.
- **Averaging over states is the same trick at a coarser level, and it does work.** The ruler question is "on average, does the plan V
  prefers end with more points?" With a per-state difference sd of ~20 points and 100 states, the standard error of the mean is
  ~20 / sqrt(100) = 2 points - enough to see a 3-5 point effect - and it does not require pairing at all. Only the *per-state* use
  requires it.
- **Noisy but unbiased labels are acceptable for training V, and only for that.** A critic's regression target tolerates noise (it
  averages out over thousands of rows); what it cannot tolerate is bias. So the judge's legitimate product is the label set, not the
  move.
- **If real pairing is ever needed** (only after the ruler shows V is wrong), the fix is not a better seed but logical indexing:
  seed each random event by its identity in the world (round, event kind, slot) instead of by its position in a stream. A single
  seeded stream does not fix it either - the k-th shuffle of one branch is the draft of the other. That is a refactor of
  `ShuffleService`/`CardService`/`MarsGame` plus the copy construction, and it is not paid for until the ruler says there is
  something to gain.
 And there is a hard precondition: if the
continuation is the plain policy, the judge says "production loses" and is right, because the continuation does not convert it -
measured as a forced plan at 44.25% whose corpus trains 25-33%. So the judge is only informative together with a continuation that
can execute the plan.

**The open question this creates, and it is now the first thing to measure:** does V's plan preference agree with the paired
end-of-game judge's preference on the same states? On 100 real phase decisions (~5 minutes of compute: 4 candidates x 8 paired worlds
x ~80 ms per policy game) this gives a number where we only have an argument today. If they agree, V is not the problem and the plan
is wrong; if they disagree, the bias is real, measured, and the judge becomes the label source V is missing.

### The decision count is a function of the strategy - and our target ignored that (owner, 20/11)

The owner's observation: a blue-action/Helion player makes ~500 decisions a game, a green-card player ~100. Any target or loss
measured per DECISION ROW is therefore not strategy-invariant - the same game gets a different decay, and the decision-rich style
contributes several times more rows to the critic's loss than the decision-poor one. Two separate defects follow, and neither was
ever controlled:

1. **The target.** A trace/λ-return counted in rows weights the terminal outcome by `λ^(number of rows)`, which is a quantity of the
   plan. Measured on the round grid it is not: with λ = 0.9 and ≤ 15 rounds, the terminal keeps ~23% of its weight from round 1,
   independent of how many blue actions the plan presses.
2. **The loss.** Rows are not equally informative: a round in which 20 blue actions are pressed contributes 20 rows of the same
   position, a quiet round contributes 2. If the critic is trained unweighted, the blue-heavy style dominates the gradient by that
   ratio. **This is a candidate mechanism for the very preference the owner described** ("V thinks the blue player is a genius"),
   and it is measurable in one pass over the pack: rows per round per game, and the share of the loss carried by blue-action rows.

Neither is a knob: both are errors in how the data is defined. The fix for (1) is arithmetic (round grid, exact scores at round
boundaries); the fix for (2) is a per-round normalisation, i.e. one sample per round per game, which is also a smaller corpus for
the same information.

### There is no strategy-independent position value (owner, 20/11)

The owner's objection to any analytic valuation, recorded because it closes a whole family of ideas: card chains (microbes -> animals ->
points, a chain that ends in a 20 MC forest) cannot be priced per resource, and the price of the same resource differs by player - "in
your world steel income is worth 5 MC, in my NETWORK's world 10".

Our own measurement already agrees, from `ml/price_report.py`: +1 income is priced **6.5 MC** by the policy's corpus, **9.0** by
forced-build play, **12.1** by random play, against the rules' ~**15**. Three prices for one resource, each correct for its own player.

Consequence, and it is a reframing rather than a doubt about the data: the value is conditional on the plan that will spend the
resources, not on the state. Our `V` is conditioned on exactly one continuation - our own policy - so it can describe one plan and
cannot compare plans, which is the fixed point. Any analytic proxy would be a fourth price, roughly 10x coarser than the chains it
pretends to price, and it would fail in the same direction as the bias it was meant to remove.

### The confound that produced a month of "closed" results (owner, 20/11)

Re-reading the 44.25% forced-plan measurement exposed a defect that is not specific to it: **a treatment that changes behaviour without
changing the training data is a statement about the current player, not about the thing being tried.** The player was trained on one
plan; forcing another plan onto it measures its incompetence at the second plan, and the corpus then honestly labels those games as
losses. Applied to the ledger, three rows are confounded in exactly this way:

| row | what it actually measured |
|---|---|
| forcing a plan (`phaseBias`) -> 44.25%, corpus 25-33% | a phase-3 player forced to pick phase 1. Never a phase-1 specialist |
| the mulligan learned from explored games -> 0 discards | random discards in games where discards were never a plan; the label "this game was lost" is honest and useless |
| heavy action-level exploration -> corpus -> 21% | that random single decisions destroy coherence - true, but it was also read as "phase diversity does not pay", which it does not show |

Rows that stay valid, and this matters for honesty: the search and target experiments (2-round horizon -8, 8 determinisations -6.3, build
/input playout 0/-1.5, tree -1.5, margin vs 3-step TD) changed the estimator on the SAME trained player, so they are clean measurements of
"more search on this value". The wide-net and data-volume results are clean too. The confound is limited to "force a behaviour onto a
policy trained for another behaviour" - and that is precisely the class of experiment that would have discovered other plans.

**Operational rules that follow.**

1. **No conclusion about a plan from games where the policy was not trained for it.** A plan is judged only by a specialist that converged
   on it. This is the rule the plan's specialist loop is built on.
2. **The value may not arbitrate exploration.** `V` and any search on it will reject an untrained plan by construction - that is the fixed
   point restated. Exploration at the plan level must be value-blind; the value's role is to play well the plan it was trained for.
3. **The gate moves from the level to the derivative.** For a new plan the question is not "is it at 73.17%" but "is its curve still
   climbing" (44% -> ? after each iteration). A specialist that flattens below the champion is a real negative result; one that is still
   rising is not.
4. **Cost is per plan, not per project.** Since the value is plan-conditional, each plan needs its own bootstrap (2-5 iterations at
   ~1.5-4 h), i.e. roughly a day of compute per specialist. The portfolio question ("how many plans can we afford to own") becomes a real,
   computable budget instead of a hope.
5. **Saturation is per plan.** 500 -> 1 250 -> 5 000 games climbed 11.6% -> 30% -> 73% and then stopped: that is the saturation of ONE
   plan's corpus. A new plan starts its own curve, and the fact that 10 000 games of the old plan added nothing says nothing about how
   many games a new plan needs.

## When the points are created (premise 1, 2026-11-20, 10 000-game pack)

Instrument: `ml/points_by_round.py` + `ml/round_scores.py`, 19 unit tests (`ml/tests/test_round_scores.py`). The round grid is the
PICK_PHASE row of each seat in each round (it happens exactly once per round), with the live score from the observation columns
`score:my_winning_points` / `score:opponent_winning_points`.

**A discovery on the way, and it matters for every future measurement:** the pack's `breakdown` field is the FINAL score of the game,
replicated on every row - not the score at that row. A "points per round" chart built from it shows 100% of the points in round 0, which
is an artefact. The live score is in the observation.

| quantity | value (18 498 game-seats) |
|---|---|
| rounds per seat | mean 11.1, median 10, p10 8, p90 14 |
| final score | mean 84.5, sd 24.6 |
| points made during the rounds | mean 50.9 |
| **points settled at the end** | **mean 33.6 = 39.6% of the final score** (median 37.5%, p90 57.1%) |

Points created per round, counted from the end: last round (settlement) 33.6, -1: 10.9, -2: 9.1, -3: 7.0, -4: 5.3, -5: 3.8. Counted from
the start, the average rises monotonically: 1.2, 1.2, 1.9, 2.9, 4.1, 5.8, 8.1, 10.3, 12.7, 14.7, 16.5 - the economy compounds, as a
production-driven game should.

**Reading.** Both readings the plan considered are now separated: the score is *settled* late (40% of it) but *created* by the position
over ~11 rounds, with the per-round contribution roughly doubling every three rounds. So "the last round decides" is a statement about
settlement, not about decisions, and a whole-game plan is the right object - the end-game-sequence variant is not needed. The owner's
90 -> 180 jump is consistent with the 40% settlement share.

## Does the behaviour space have separate regions? (premise 2, same pack)

Instrument: `ml/behaviour_regions.py`. Descriptor: the share of the final score from terraforming (TR + forests) and from card points
(printed + resource cards), 4 bins each, from the game's own channels.

| cell (tr_bin, card_bin) | n | share | mean score | mean margin |
|---|---|---|---|---|
| 1, 1 | 9 054 | 48.9% | 92.1 | +16.1 |
| 2, 1 | 3 996 | 21.6% | 84.4 | +1.7 |
| 2, 0 | 3 824 | 20.7% | 66.1 | -17.8 |
| 1, 2 | 1 010 | 5.5% | 100.1 | +16.5 |
| 3, 0 | 310 | 1.7% | 51.7 | -36.8 |
| 0, 2 | 177 | 1.0% | 84.8 | -5.5 |

8 of 16 cells are occupied; the top quartile by score spreads over **5 of the 8** cells, with 64.7% in the single most populated one; the
sd of mean margins across cells is 19.4 points. So the space is not one-dimensional and the cells are not equivalent - coverage has
something to cover.

**Caveat, stated because it limits the claim:** the descriptor is built from the score composition, which is itself an outcome (a losing
player collects fewer card points). So part of the separation is endogenous; the honest reading is "strong games are composed in at least
two different ways (terraforming-heavy and card-heavy)", not "we have found N strategies".

## The probe corpus: a forced plan, and the forcing verified before training (2026-11-20)

Purpose: the first point of the specialist curve. Not "does phase 1 win" - that question is confounded and will stay confounded until a
specialist is trained on it - but "can a forced plan produce a corpus at all, and does the corpus actually contain that plan".

Generation: the champion bundle (policy `1118-2332_17`, its three critics, the phase and mulligan critics) with
`-Dares.agent.planCode=8 -Dares.agent.planNudges=true -Dares.agent.planNudgeRate=1.0`, both seats, self-play, 5 000 games, 14 threads,
3 621 s (~1.4 games/s), 20 chunks, 8.9 GB raw, 456 116 rows before de-duplication, pack 1.1 GiB (5.6 s).

**Forcing verified on the corpus itself** (`ml/plan_report.py`), because a silent failure here would recreate exactly the confound the
plan exists to remove:

| corpus | phase 1 | phase 2 | phase 3 | phase 4 | phase 5 |
|---|---|---|---|---|---|
| champion (10k corpus) | 10% | 30% | 36% | 6% | 17% |
| probe code 8 (5k) | **49%** | **15%** | 25% | 5% | 5% |

Build phases go from 10% to 64% of the picks; acceptance 0.72, drafts 8.0, blue actions 5.87 per game. The plan is in the data.

Next: a specialist policy trained on this corpus from the champion (`train_policy_ce --prev`), critics trained on the same corpus, and a
1 000-game measurement against SMART. That number is the first point of the curve; the gate is whether the curve climbs, not where it
starts.

## Specialist iteration 1: the first point of the curve, and a test that measured the wrong thing (2026-11-20)

Pipeline: probe corpus (5 000 games, plan code 8, nudges 1.0) -> specialist policy (`train_policy_ce --prev champion`, 27.8 s) -> critics
on the same corpus (AUC 0.896/0.900/0.899, loss ~2.04) -> 100 then 400 games against SMART, search as in the champion.

| measurement | result |
|---|---|
| specialist vs SMART, 100 games | 42.5% [33.3, 52.3] |
| specialist vs SMART, 400 games | **44.75% [40.0, 49.0]** |

44.75% is, to within noise, the old forced-plan number (44.25%). So iteration 1 does not yet separate "the plan is bad" from "the player is
incompetent": it is the first point, and the gate is the derivative.

**Profile, and the one that matters for the design:** trained on a corpus with 64% build phases, the specialist picks build phases only
**44%** (phase 1: 18% against the champion's 10%, phase 2: 26%), and phase 4 (income) rises to **24%** (champion 6%, training corpus 5%).
The policy learned part of the plan; the play-time search re-ranked the phase back towards income.

**A test that measured the wrong thing, recorded so it is not repeated.** Setting `phasePlayout=false` for the specialist gave
**20.0% [15.0, 26.1]** (200 games), and the tempting reading is "the plan policy alone is weak". It is wrong: with phasePlayout off,
PICK_PHASE is still in `search.types`, so the phase is decided by ONE-STEP V - not by the policy. The champion's +5.9 was measured by
replacing exactly that one-step V with a played-out round, so 20% is a measurement of one-step V for the phase, not of the policy's plan.
The correct test of "the plan belongs to the policy" is to REMOVE PICK_PHASE from `search.types`, and it is the next measurement.

**Design consequence.** For a specialist, the phase decision is the plan commitment and must come from its own policy; the search should
rank tactics (builds, drafts, inputs), not the phase. Otherwise a generic value re-decides the plan every round, which is the fixed point
wearing a specialist's clothes.

### The correct test of "the phase belongs to the policy": 22.5%, and what it means

With PICK_PHASE removed from `search.types` (so the POLICY picks the phase) and everything else identical: **22.5% [17.3, 28.8]** at 200
games, against 44.75% [40.0, 49.0] when the search re-decides the phase by a played-out round. The earlier 20.0% (one-step V on the
phase) and this 22.5% are, to within noise, the same - so the specialist's *own* build plan is worth ~22%, and the 44.75% is the search
overriding it.

Two readings, and they are not exclusive:

1. **The build plan we forced is weak in this game.** The nudge at rate 1.0 produced 64% build phases, which is an extreme preference; a
   competent build player may need income and draft phases too. That is stage 2 (relax the forcing) and it is a parameter of the corpus,
   not of the player.
2. **The policy learned the plan only partly** (44% build at play against 64% in its corpus), so the 22.5% is "a half-learned extreme
   plan", not "the best build play".

For the derivative gate this matters: iteration 2 must be generated by the specialist (with the nudge, so the corpus keeps the plan) and
measured with the champion's flags, and the comparison is against 44.75%. Two consecutive points at the same level is already the signal
that the loop does not climb for this plan, whatever the reason - and then stage 2 (nudge rate 0.35-0.5) is the next question, not
another 5 000 games at 1.0.

## Specialist curve: 44.75% -> 54.4%, and the value is plan-conditional measured directly (2026-11-20)

Iteration 2: the iteration-1 specialist generated 5 000 games (nudge 1.0, code 8), a new policy was trained from it (`--prev p1`) and new
critics on the same corpus (AUC 0.893-0.901, loss 1.33-1.40 against iteration 1's 2.03 - the corpus's play is less degenerate).

| stack | vs SMART |
|---|---|
| iteration 1: p1 + critics of corpus 1 (400 games) | 44.75% [40.0, 49.0] |
| **iteration 2: p2 + critics of corpus 2 (400 games)** | **54.4% [49.5, 59.3]** |
| p1 + the CHAMPION's critics (200 games) | 76.5% [70.2, 81.8] |
| p2 + the CHAMPION's critics (200 games) | 66.25% [59.4, 72.4] |

The derivative is positive: +9.6 points on the matched stack, with the intervals barely touching. This is the first mechanism in weeks that
climbs rather than landing on 71-73%.

**The cross-measurement is the direct proof of the central thesis.** With the champion's value, p1 (a half-learned plan) measures 76.5% and
p2 (a more plan-locked policy) drops to 66.25%. The same policy is worth +10 points under its own critics and -10 under the champion's:
a value trained on one plan misprices another plan's moves. That is "the value is plan-conditional", measured on the same policies with only
the critics swapped - and it is why a specialist must be shipped as a stack (policy + its own critics), not as a policy dropped into the
champion's search.

**Profile, play time, nudges OFF (100 games):** p1 18/26/23/24/9 (phases 1-5); p2 24/23/18/4/31. Build phases 47%, and the plan drifts
towards the draft phase. The plan is migrating as the specialist learns, not converging to the nudge's 65% - which is the expected
behaviour of a preference that the policy can outvote, and the reason stage 2 will relax the rate.

**Next:** iteration 3 with p2 as the generator. The gate is unchanged: it must climb again. If it lands at 54 +/- noise, the curve has
plateaued below the champion and the honest next question is the nudge rate (0.35-0.5) or closing this plan, not a fourth 5 000-game run.

### Correction (owner, 20/11): a behaviour cell cannot be graded by the player who cannot play it

The premise-2 table above is wrong as an EVALUATION, and the error is the same one the whole plan exists to avoid. The cells were graded by
the mean margin of the games the CURRENT player played in them. But a cell is a way of playing; if the player cannot play microbes, its
games that land in a microbe-shaped cell are bad *because the player is bad at it*, not because the cell is bad. A player who could play
microbes would take microbe cards, pick the phases for them and manage them, and the cell would then be at the top - with a different
player.

Two consequences, and the second is structural:

1. **The measurement is descriptive, not evaluative.** "8 of 16 cells occupied, strong games spread over 5" describes the champion's own
   distribution. It says nothing about which plans are strong. The mean margins per cell must not be quoted as evidence about plans, and
   they never enter a gate.
2. **The axis itself is endogenous.** The descriptor is built from the composition of the final score, which is an outcome: a weak player
   produces a different composition than a strong one, so the cell assignment changes with competence. An outcome-defined cell cannot rank
   plans, by construction.

**What replaces it.** The axis of the loop is the **commitment** (the first-round plan: which phase the seat commits to, which card it
takes first), because a commitment is an ACTION - exogenous, defined before the outcome, and it can be forced in generation and then graded
by the specialist's own curve. The outcome cells stay only as an inventory of observed behaviour ("what does our player actually do"),
never as a ranking.

**The rule this produces, and it goes into METHOD.md:** no alternative may be ranked by the results of a player whose competence at that
alternative is the thing in question.

## Which axis is enumerable in this game? Corporations: 25, already balanced (2026-11-20)

The owner's question - "why five arms, you said 16 cells?" - exposed that I was mixing two different spaces, and neither of them is the right
one:

| number | what it is | status |
|---|---|---|
| 16 | 4x4 bins of an OUTCOME descriptor (terraforming share x card-point share) | retired as a ranking axis: it grades plans by a player who cannot play them, and the descriptor is endogenous |
| 5 | the legal phases; "prefer this phase when legal" | a cheap ACTION axis, good only as a mechanism probe |
| **25** | **the corporations actually played in the 5 000-game probe corpus** | **the axis the game already enumerates and the corpus already balances** |

Measured on the probe corpus (5 000 games, both seats): 25 distinct corporations played, median **414** games each (max 786, min 1; only two
below 100). So a per-corporation natural experiment already exists in the data, with no forcing at all: a corporation is dealt and chosen
before any strategy is played, and it determines which strategies are even possible (a microbe plan needs microbe-enabling cards).

Consequence: the coverage axis is the corporation, specialists are trained from the existing corpus (no shove), and the phase preference is
then part of what each specialist discovers. The five phase arms stay as a mechanism probe; the 16 outcome cells stay as an inventory.

### Iteration 3 corpus: the plan drifts further, and income disappears (2026-11-20)

Generated by p2 with the same nudge (code 8, rate 1.0), 5 000 games, 3 909 s. Phase shares by corpus:

| corpus | 1 | 2 | 3 | 4 income | 5 draft |
|---|---|---|---|---|---|
| iteration 1 (champion forced) | 49% | 15% | 25% | 5% | 5% |
| iteration 2 (p1 forced) | 50% | 15% | 13% | 17% | 5% |
| iteration 3 (p2 forced) | 50% | 14% | 12% | **0%** | **22%** |

The nudge pins phase 1 at 50% (it is forced whenever legal, and the rule forbids repeating it), and the free slots move: action phase 25% -> 12%,
income 5% -> 17% -> 0%, draft 5% -> 5% -> 22%. Income disappearing entirely is a degeneracy signal to watch: without the income phase the economy
depends on the dummy's choices and on card income alone. If iteration 3's specialist degrades its profile, this is the first place to look, and it
is the kind of collapse the annealing schedule (rate -> 0.5) is meant to prevent.

### Iteration 3 result and the correction about income (2026-11-20)

p3 (trained on the iteration-3 corpus) + its critics, 400 games vs SMART: **44.6% [39.8, 49.5]**. The curve is therefore

**44.75% -> 54.4% -> 44.6%**: not a climb, an oscillation, and the middle point is real (its lower bound 49.5 touches the third point's upper
bound 49.5).

The corpus explains part of it: the iteration-3 corpus has phase 4 (income) at **0%** and the draft at 22%, and its critics are worse (loss
1.61-1.65 against 1.33-1.40 in iteration 2). So quality degraded, and per the plan this is the point where the SEED must anneal (rate 1.0 -> 0.5)
rather than where a fourth 5 000-game run at 1.0 is justified.

**But the "0% income" must not be read as degeneration, and the owner's correction of 20/11 is why** (`docs/GAME_RULES.md`, "Phase 4 is a SHARED
phase"):

* phase 4 resolves for everyone, so picking it buys a small extra for the picker while a build phase buys an extra build or a discount;
* with a dominating income the marginal value of one more income collection is low, and the opponent who spends their pick on it "buries
  themselves" - so declining phase 4 can be the strong move;
* consequently "does not pick phase 4" - and also "picks phase 1 a lot" - are NOT degeneracy signals. The profile gate judges outcomes and
  build/standard-project behaviour, not the phase mix.

**And the play-time profile confirms the corpus artefact:** p3 with the nudge OFF picks 17/25/14/**29**/15 - income is back to 29% at play time
while the corpus that trained it had 0%. The 0% belongs to the forced generator, not to the player.

## Two cheap screens before the next hour of compute (2026-11-20)

Instrument: `ml/corpus_audit.py` + 6 unit tests (`ml/tests/test_corpus_audit.py`). Both read labels (the corporation screen reads the raw
PICK_CORPORATION rows, which carry the margin), so they run in minutes.

### Row weighting (the owner's decision-count observation, measured)

On the 5 000-game probe pack: 225 011 (game, seat, round) groups, 430 918 rows, **1.92 rows per round** (p10 1, median 1, p90 4, max 13),
p90/p10 = **4.0**, and the busiest **decile of rounds carries 26% of all rows** (uniform would be 10%). So the critic's loss over-weights the
decision-rich style by about 2.6x in the tail.

Caveat that matters: this corpus was generated with `--max-rows-per-game-type 8`, which caps each decision type per game. Without the cap the
skew would be larger, NOT smaller - the cap compresses exactly the tail this screen measures. So the 4x is a lower bound for this corpus, and the
per-round normalisation in the training target stays justified.

### Corporation screen: 2.4% of the margin's variance

25 corporations, 10 000 seats, mean margins from -13.3 to +12.4 but within-corporation sd of 41-48: **eta squared = 2.4%**. The corporation
barely explains the observed outcome.

**What this does and does not say.** It does not say the corporation is irrelevant: the choice is endogenous (our player picked it) and a player
who cannot exploit a corporation's economy gets nothing from it - the same confound as the behaviour cells. What it does say is that, at the
current skill, **a specialist per corporation is not justified by the data**, so the plan's step 2 (25 specialists, ~19 h per iteration) is not
where the next hour should go. The only axis with a measured climb remains the commitment; the next candidate axis after that is the starting
HAND (which is what actually determines which strategies are possible), and it needs clustering by the cards' own tags rather than by my choice.

### Annealing corpus (rate 0.5): the plan softens, the drift to draft continues (2026-11-20)

Generated by p2 (its own critics) with the nudge at **rate 0.5**: 5 000 games, 3 927 s, 465 733 rows.

| corpus (nudge rate) | 1 | 2 | 3 | 4 income | 5 draft |
|---|---|---|---|---|---|
| iteration 1 (1.0) | 49% | 15% | 25% | 5% | 5% |
| iteration 2 (1.0) | 50% | 15% | 13% | 17% | 5% |
| iteration 3 (1.0) | 50% | 14% | 12% | 0% | 22% |
| **annealing (0.5)** | **38%** | **19%** | 15% | 3% | 24% |

The build share falls from 65% to 57% as expected when the nudge is halved, and the drift towards the draft (24%) survives - so the draft is the
player's own preference, not the seed's. Whether that helps is the measurement: 400 games against SMART, compared with 54.4% (the best point so far)
and 44.6% (the last point at rate 1.0).

### Annealing result: 52.5% - the level holds when the compulsion is halved (2026-11-20)

The annealed specialist (corpus at nudge rate 0.5, trained from p2, with its own critics: AUC 0.906-0.909, loss 1.62-1.65) scores **52.5% [47.6,
57.3]** against SMART over 400 games.

| point | nudge rate | vs SMART |
|---|---|---|
| iteration 1 | 1.0 | 44.75% [40.0, 49.0] |
| iteration 2 | 1.0 | 54.4% [49.5, 59.3] |
| iteration 3 | 1.0 | 44.6% [39.8, 49.5] |
| **annealing** | **0.5** | **52.5% [47.6, 57.3]** |

So the oscillation at rate 1.0 (44.75 -> 54.4 -> 44.6) collapses to the good level when the compulsion is halved: the drift into a degenerate corner
(phase 1 pinned at 50%, income at 0%) is what the full-rate nudge caused, not the mechanism. The annealed point is statistically
indistinguishable from the best point at rate 1.0, which is the expected shape if 0.5 is close to the right pressure.

Still **20 points below the champion**: this is a second plan at ~53%, not a replacement for the 73.17%. Its use is the gate and the league, so the
next step is not more annealing of the same plan but the **enumeration of other commitments**, without which there is no gate to test.

### Correction (owner, 20/11): the phase axis cannot create a distinct plan - the champion already mixes phases

The champion's own phase mix is **10/30/36/6/17**: phase 3 (blue actions) is already its largest share and phase 2 (blue/red builds) the second. So
"arm phase 3" does not test a different commitment - it forces a MORE BIASED version of the player we already have, and its corpus will be a variant
of the champion's own play. The same holds for every phase: the champion occupies all five, with a bias, and its search re-decides the phase every
round.

The general form: **a commitment axis that the current player already covers cannot produce a new plan.** The five phase arms are therefore not five
strategies; they are five settings of one bias, and the phase-3 run was stopped before generating (it would have answered a question we already know
the answer to).

**What the axis must be instead:** the plan's CONTENT - which economy gets built (microbes, animals, energy, steel/titanium, plants, card draw). That
is determined by the cards, and the player does not cover it: it builds Herbivores 0.21 and Fish 0.05 per game (the microbial/animal economies are
almost absent), so those cells are genuinely empty. A hand/tag-based commitment is therefore the honest axis, and it is also what the owner described
from the start: "given my starting hand I decide which specialist to become".

## The NETWORK gap is tempo and events, not conversion (scorecard, 2026-11-20)

Instrument: `--mode scorecard --a AGENT(p1) --b NETWORK --games 1000`, which counts the cards each seat actually PLAYED
(`player.getPlayed().getCards()`), so it is free of the per-type recording cap that distorts corpus counts. Colours come from the card packages
(`cards/green|blue|red`, 348 classes matched to the 318 ids).

| per game | p1 | NETWORK |
|---|---|---|
| cards played | 34.8 | **42.1** |
| green | 13.2 (37.8%) | **19.3 (45.8%)** |
| blue | **17.5 (50.2%)** | 14.2 (33.8%) |
| **red (events)** | **4.2 (12.0%)** | **8.6 (20.4%)** |
| points | 90.3 | **96.9** |
| win rate | **40.7%** | - |

Score channels (mean per game):

| channel | p1 | NETWORK |
|---|---|---|
| terraforming rating | 29.3 | **34.9** |
| forests | 9.9 | **11.2** |
| printed card points | 12.5 | **17.5** |
| **resource cards** (animals/microbes) | **25.9** | 15.8 |
| milestones | 3.4 | **5.9** |
| awards | 9.4 | **11.7** |

**Reading.** p1 is the conversion player: it wins the resource-card channel by **10 points** (the Fish/animal economy the owner saw in his game).
NETWORK is the tempo player: twice the events, more green, and it wins terraforming rating (+5.6), printed points (+5.0), milestones (+2.5) and
awards (+2.3). So the gap to NETWORK is **not** conversion - p1 already beats it there - it is **tempo and one-shot effects**.

**Consequence for the plan:** the next practice regime is now obvious and exogenous: a *red/event* preference (a tag nudge on the draft and build
menus). It is a card type, not a strategy of mine, and it targets exactly the channel where the agent is four cards per game short. The same
instrument measures the result, and the opponent is the benchmark.

### Caveats on how much data the policy trainer actually reads (20/11)

Two defaults in `ml/train_policy_ce.py` silently shrink the corpus, and they apply to every "5 000-game specialist" reported here:

* `--rows-per-game 8` keeps at most 8 rows per decision type per game;
* `--max-chunks-per-type 10` picks a random subset of 10 chunk files per type. A 5 000-game corpus is 20 chunks, so **the policy trains on about
  half of it** (the subset is seeded, so two runs with the same seed use the same chunks).

The comparison between p1 and the clean control is still fair (same defaults, same seed), but "trained on 5 000 games" is wrong as stated: it is
"generated 5 000, trained on ~2 500". Raising `--max-chunks-per-type` to 20 and testing it is a separate, cheap experiment.



## The lever is practice, not plans (the clean-fine-tune control, 2026-11-20)

The decisive control: the champion's policy fine-tuned on 5 000 **unperturbed** games of its own play (plan code 0, the same recipe, the champion's
critics, 5 000 games against SMART).

| run | vs SMART |
|---|---|
| champion | 73.17% [72.3, 74.0] (10 000) |
| p1: fine-tune on the **nudged** corpus (plan code 8) | 74.88% [73.66, 76.10] (5 000) |
| **clean fine-tune: fine-tune on the champion's own play** | **75.16% [73.94, 76.23] (5 000)** |

The two fine-tunes are **identical within noise** (0.28 points apart). So the phase perturbation contributed **nothing**: what helps is
**one more round of fine-tuning on 5 000 fresh self-play games** (+2 points, z ~ 2.7 against the champion, and reproducible in two independent
runs).

**This settles the reviewer's strongest objection.** His warning was that we diagnosed an ordinary approximate-policy-iteration problem as a
plan-level pathology and then built the cure on the components the data said were weakest. The control says exactly that: no plan machinery is
needed for the +2; practice is.

**What it does not settle.** Both runs trained on ~2 500 of the 5 000 games (the trainer's defaults, see the caveat above), and neither was
measured against NETWORK. The +2 may be the first step of a long curve or a one-off; only iteration 2 answers that.

**The obvious loop, and it is cheap:** generate 5 000 games with the current best -> fine-tune the champion's policy on them -> measure at 5 000
against SMART (and 1 000 against NETWORK) -> keep if better -> repeat. ~2 h per iteration. That is the plan now; the specialist/gate/two-target
machinery is shelved until the practice loop stops paying.

## The phase-3 blind spot, measured (2026-11-20, owner's observation)

The owner played the new champion and reported: it called phase 3 while it had one activatable blue card and no phase-3 upgrade, against three on the
other side - "it does not understand that I can act in phase 3 too". Phase 3 is a **shared** phase (like phase 4): calling it lets both players
activate.

Measured on 4 000 phase decisions of the champion's corpus (4 chunks, plan code 0), counting the blue cards in play from the observation
(`blue_cards_count`, `op_blue_cards_count`, both stored divided by 25):

| situation | n | P(phase 3) |
|---|---|---|
| my blue cards > opponent's | 3 291 | 39.5% |
| equal | 1 509 | 31.7% |
| my blue cards < opponent's | 3 200 | 34.3% |
| **no reveal-cards upgrade, mine = 0, opponent >= 2** | **31** | **35.5%** |
| no upgrade, mine < opponent's | 833 | 26.7% |
| no upgrade, mine > opponent's | 550 | 34.9% |

So the decision is only weakly sensitive to who benefits from the shared phase: 5-8 points between "I have more" and "I have less". In the exact case
the owner described it still calls phase 3 about a third of the time.

Two facts that matter for the interpretation, one of which corrects an earlier claim of mine. The reveal-cards upgrade is bought by **88.2%** of
game-seats, so it is usually available - but it yields only **~1.3-1.4 cards** (it reveals three and keeps blue/red; the rest are discarded,
`docs/GAME_RULES.md`), not three. So "the upgrade justifies calling phase 3 while behind" is a much weaker argument than I first wrote.

The information is present in the observation (`blue_cards_count`, `op_blue_cards_count`, `ctx:opponent_activated_blue_cards`), so this is not a data
problem: it is either a value that does not weigh the shared gain, or a playout that does not carry the opponent's activations to the leaf. The
diagnostic that separates them: on the "no upgrade, mine = 0, opponent >= 2" states, compare the end-of-game margin that the phase playout gives
phase 3 against its alternatives.

## The "activatable blue cards" feature does not exist, and the docstring says it does (owner, 20/11)

The owner's objection, verified in code: `blue_cards_count` does not distinguish a passive blue card from one with an action, and the opponent's
*activated* counter is meaningless against a human because the AI presses first.

* **`blue_cards_count` is all blue cards played.** `PlayerIncomesFeature.encode` (line 51): `player.getPlayed().getCards()...filter(color == BLUE).count()`.
  It is the NETWORK_V2 vector's original meaning, deliberately not redefined.
* **There is no activatable count anywhere.** `names_now.json` contains no such column; the only "activation" features are
  `ctx:my/opponent_extra_blue_activations` and `ctx:my/opponent_activated_blue_cards`, which are counts of actions already performed.
* **The `ObservationBuilder` docstring (lines 139-144) claims the opposite** - "the number of activatable blue cards each side has" and even the
  reasoning that motivated it ("an engine of three activatable cards makes the next activatable card worth more, because phase 3 then fires a whole
  chain"). That feature was never implemented: only the identity bits were.
* **What does exist:** 83 identity bits per side for the blue cards on the table (`ObservationBuilder` 562-570, public, kept by `buildPublic`). That is
  the signal the owner asked for, and it is there - but nothing tells the model which of those 83 cards have an action, so it has to learn it from 83
  correlated bits.
* **`ctx:opponent_activated_blue_cards` is order-dependent.** It is the size of `player.getActivatedBlueCards()`, a deck of actions already taken.
  Against a human the AI decides before the human presses, so the opponent's value is 0 by construction.

**Consequence.** My earlier sentence "the information is in the observation, so this is a value problem, not a feature problem" was half wrong: the
*aggregate* the search would need does not exist, and the phase-3 blind spot was measured with all-blues instead of activatable ones. The fix is a
small, rule-derived feature: per side, the count of played blue cards that have an action (and their resource load). Then the phase-3 diagnostic is
re-run on the real quantity.

## The practice loop converges after one step (iteration 2, 2026-11-20)

| iteration | vs SMART (5 000) | vs NETWORK (1 000) |
|---|---|---|
| champion | 73.17% [72.3, 74.0] (10 000) | 39.8% [37.7, 42.0] (2 000) |
| fine-tune 1 (clean 5 000) | 75.16% [73.94, 76.23] | - |
| **fine-tune 2 (clean 5 000, whole corpus)** | **74.53% [73.30, 75.75]** | **43.25% [40.2, 46.4]** |

So the +2 is **one step, not a curve**: the second iteration is 0.6 points below the first, well inside noise. The NETWORK number creeps up
39.8 -> 41.8 -> 43.25 over the two iterations, but each step is inside its own interval.

**The health panel says the same thing, and it is the stronger evidence:** iteration 2's corpus is *behaviourally identical* to iteration 1's
(builds 3.233 vs 3.255, acceptance 0.808 vs 0.814, phase entropy 0.532 vs 0.531, colour shares 34/52/13 vs 34/52/14, distinct cards 3.232 vs 3.253,
HHI 0.388 vs 0.388, adaptation to the corporation 0.040 vs 0.040 - no collapse, no change). The second fine-tune converged to the same player.

**Consequences.** The practice lever is real but bounded: it buys ~+2 points once and then stops, and it does not add diversity either. The
half-corpus default was not the binding constraint (iteration 2 used the whole corpus and did not beat iteration 1). The next lever has to be a
different one: the value (the reviewer's main effect) or the concrete NETWORK gap (twice the red/event cards), and any nudge used for that needs the
forced-row flag before it can be trusted.

### Throughput after two semantics-preserving changes: 1.07 -> 1.44 games/s (+35%), 20/11

| step | games/s (500 games) | observation CPU per game | critics CPU per game |
|---|---|---|---|
| baseline (practice2 run, 5 000 games) | 1.07 | 7.20 s | 1.50 s |
| streams removed from the observation path | 1.31 | 4.63 s | 1.50 s |
| + flat `Card[] byId` in `CardService` | **1.44** | **3.74 s** | 1.47 s |

Both changes are semantics-preserving: the streams were replaced by the same loops with the card instances resolved once, and the flat table is
checked against the original lookup for **every id from 0 to 11 000** (`CardServiceTest`, 4 tests). The critics' cost is unchanged, as it should be,
which is a useful control that the measurement is honest rather than a different workload.

What remains on the table would cost quality and is rejected by the owner: dropping the public observation (31% of the observation cost) and pruning
the candidate menus by the prior (1.3-1.6x on the big menus). No further optimization is planned unless it is provably decision-neutral.
