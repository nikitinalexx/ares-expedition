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
11/26/16/42/4%, phase-3 pass 58%, standard projects 19%, no warning - but the player builds **24.6 cards per game** against a
healthy ~11: it overbuilds, and that is the next variable.

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
2. the overbuilding: 21.9 cards is still double the healthy ~11, and the search still reports a large positive V(build) -
   V(stop) margin; the exploration lever moved both numbers in the right direction, so it is the knob to keep turning.
