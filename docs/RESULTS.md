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
