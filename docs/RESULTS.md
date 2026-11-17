# Results ledger (2026-11-15, all measured against SMART unless stated)

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
