# Systematic consolidation (2026-11-16)

Purpose: one place where the knowledge is written down, so that the next step follows from the evidence instead of from
whoever said what last. Every claim is a measurement in this repository unless marked ESTIMATE.

## A. The facts, with their source

| # | fact | number | source |
|---|---|---|---|
| 1 | NETWORK beats our best player | 90/10 (our 9.3%) | `--mode compare` |
| 2 | our best player beats SMART | 43.59% +/- 1.0 (10 000) | same |
| 3 | policy alone (no search) | 35.6% +/- 1.5 | same |
| 4 | ranking cards by a policy-trained critic | +5…+8 points over the policy | rows 2-3 |
| 5 | a single critic's training variance | +/- 3 points (42.98 vs 39.38, same recipe) | two 10 000-game runs |
| 6 | ensemble 1 / 3 / 5 critics | 39.38 / 42.89 / 43.59% | same |
| 7 | score gap vs SMART | cards -9.3, milestones+awards -5.8, TR +6.6, forests +5.0 | `--mode scorecard` |
| 8 | cards built per game | 12.9 vs SMART's 27 | `ml/check_player.py` |
| 9 | critic's price of +1 income | 6.5 MC (policy data) vs 12.1 MC (random data); rules ~15 | `--mode audit` |
| 10 | critic's price of +1 income by round | flat (policy data) vs 14.0 -> 10.4 (random data) | same |
| 11 | critic's symmetry V(me)+V(opp) | 0.97 (policy) vs 0.997 (random) | same |
| 12 | the hand summary inside the critic | 39.4% with, 27.5% without (identical rows) | `--drop-prefix hand:` |
| 13 | ranking with the random-data critic | 22.8% (right price, poor ranking) | 3 000 games |
| 14 | decider (V chooses build vs pass) | 36.0% (policy critic), 18.9% (random critic, with projection), 37.3% (ensemble + projection) | 3 000-game runs |
| 15 | phasedist: NETWORK calls phases 1/2 in | 73% of rounds; our player 0% | `--mode phasedist` |
| 16 | two-slot build decisions in our data | 0.6% of rows (7.6% with exploration) | `ml/slot_report.py` |
| 17 | paired plan audits (n=250-300) | income +1.7 +/- 1.4; free blue -1.2; phases -0.5; phases+build 0.0; metals -0.6 | `--mode macroaudit` |
| 18 | rollout per candidate (out of sample) | -3.3 +/- 6.5 points | `--mode rollouttest` |
| 19 | TD target after the link bug was fixed | not measured yet (the first run mixed seats) | - |

## B. The mechanisms, and what theory says about each

**1. Value-based decision with one-step projections (NETWORK's loop).**
Theory: a one-step lookahead is policy improvement over the base policy *iff* the value ranks the successors correctly. The
decision itself has zero variance given V; what matters is V's **slope** along the dimensions on which the candidates
differ (money, income, cards, points). Level errors cancel between two successors of the same position.
Status: this is the only mechanism that has ever produced a strong player here (fact 1). Our implementation of the same idea
is rank-only (fact 4) because the full decider fails (fact 14) - see D.

**2. Monte-Carlo value from the final outcome.**
Theory: unbiased for V^π, but the label's variance is the game's (sd ~17 points) and, more importantly, the *slope* it learns
is the slope **of the data's behaviour**: in games where money is never converted, income really does not win. This is a
confounding problem, not a variance problem.
Status: measured - fact 9/10 (6.5 MC flat) vs the same representation trained on random play (12.1 MC with the right round
profile). The representation can express the correct slope; the data decides it.

**3. TD on the exact intermediate reward.**
Theory: TD-Gammon's setting (a single terminal label, self-play). Our game is *better* than backgammon here: the score is
visible every state, so the reward is exact and dense; only the tail has to be bootstrapped. TD trades Monte-Carlo variance
for bootstrap bias; with the exact reward the trade is favourable, and it is the standard answer to "one terminal label".
Status: first implementation was **invalid** (fact 19): it linked rows of different seats (the score is per decision maker) and
non-adjacent steps (rows are capped 8 per game). Fixed: same seat, step + 1; elsewhere the exact `G - M` target. Not yet
measured.

**4. Policy imitation (candidate cross-entropy).**
Theory: bounds the player by the generator's quality; with exploration data the labels contain the exploration (measured:
CE on exploration data costs 1.5 points). Useful for speed and for distillation of a *validated* teacher, not for
improvement.
Status: facts 3, 4, and the CE result.

**5. PPO with outcome advantage.**
Theory: on-policy gradient with a baseline; the per-decision advantage has to be estimated from a game outcome whose sd is
~17 points, while the effect of one decision is 1-3 points. To resolve a 1-point effect the variance demands ~10^4 games per
update. At our scale this is a plateau machine.
Status: four nights at 37-40% before today's critic work.

**6. Ensembles.** Theory: averaging k models reduces the error in the *decision* (ranking) as ~1/sqrt(k) if the members'
errors are independent. Measured +3.5 for 1 -> 3; +0.7 for 3 -> 5; worse beyond (facts 5, 6).

**7. Paired plan audit.** Theory: the only affordable causal test at our resolution; common random numbers cancel the game's
noise for the part the plan causes. Measured resolution ~1.4 points at n=300 (fact 17). It is the arbiter of plans.

**8. Latent plans / style population.** Theory: exploration in behaviour space, selection by outcome. Measured negative
(30.8% unanchored, 34.95% anchored) because imitating compulsion-generated play damages the base. Kept as an instrument, not
as a plan.

## C. Implementation bugs found (each with the instrument that caught it)

| bug | consequence | caught by |
|---|---|---|
| the hand was not in the observation (only 5 specific cards) | a state value could not see cheap cards it held; "build or wait" blind | owner's question, then `--drop-prefix hand:` (12 points) |
| the printed points of a card were documented and never written | the card menus could not see what a card scores | audit of the layout against the code, now a test |
| the phase comparison read a meaningless afterstate | phase search collapsed (0.3-3.8%) | `--mode phasedist` + code reading |
| candidate slot inserted before `stop`, then zero-padded at the end | stop's weight shifted; -3.8 points | parity run after the remap |
| TD linked rows of different seats and non-adjacent steps | target anti-correlated with the outcome (AUC 0.458) | AUC collapse + code reading |
| the sanity gate reported an unbuildable position as a model failure | blocked promotion in 1 of 5 runs | repeated gate runs |
| datasets are bound to the encoder hash; six old ones (450 GB) | disk full twice, one training export destroyed at the last step | the failure itself |

## D. Why NETWORK's decider worked and ours does not - hypotheses and their cheap tests

| # | hypothesis | test | cost |
|---|---|---|---|
| H-a | its value was trained on the states the comparison reads (projections) | `projectBuilds` on/off with the same critic: measured +1.3 (37.3 vs 36.0), not enough | done |
| H-b | the projection **recurses** (2-3 builds) so the compared states differ by realized effects | add one recursion level to `projectToNextDecision` | 3 h |
| H-c | asymmetry: projections only for me, so "act" is systematically favoured | already the case in our "stop vs act" comparison | untouched |
| H-d | the data: random first, then 1-2 self-play iterations (owner) | run exactly that loop: play with the random-data value, record, retrain, repeat | 1 day |
| H-e | our leaf after `applyBuild` is a mid-turn state (builds list changed, turn unfinished) that the encoder renders differently from any decision state | compare `build(decision)` against `build(after an action)` on the same position: which columns differ | 1 h |
| H-f | the projection should also project the **opponent's** action (owner added this in phase 3) | a second variant that plays both seats before reading V | 3 h |

## E. The single track we follow (no branching until a step is decided)

**Step 1 (now).** Fix TD (done) and measure it on the full-row random dataset: audit (price and round profile) then ranking
and decider at 3 000 games. Decision: does TD keep the correct price *and* improve ranking over 22.8%?

**Step 2.** If the TD critic is at least as good as the policy critic on price and ranking, run the owner's loop: play with
it (decider + projections), record 20 000 games, retrain the value on those games, repeat once. Measure after each
iteration: audit price, rank-only win rate, decider win rate, cards per game.
**Falsifier:** if after two iterations the ranking is below 43.59% *and* cards per game stay below 16, the loop does not
converge at our scale, and the conclusion is written down as such.

**Step 3 (only if Step 2 fails).** H-e and H-b: the leaf representation (what the encoder renders after an action) and one
level of recursion. These are the two remaining differences between our comparison and NETWORK's, both cheap to test.

**Step 4 (only if Step 3 fails).** The representation A/B: `--drop-unprefixed` (the 417 legacy values) and, if it helps,
replace them with engine aggregates. This is a class change, and the last big lever we have measured a reason for.

## F. What we will not do

* no new mechanism without a falsifier written first;
* no single-critic claim (ensembles or three seeds);
* no named plan outside the paired audit;
* no more "add a feature and hope": every block is A/B-able and must earn its place;
* no direction change per message: this document is the queue, and each step is decided by the criterion written above.

## G. The two roles of a critic need different things (this explains four earlier results)

**Ranker** ("which card of this menu"): only the *order* of cards matters. Any shift or slope shared by the candidates
cancels in the difference. Measured confirmation: adding the correctly priced random critic to the five-critic ensemble moved
the price of income from ~6.5 to 8.2 MC and the win rate from 43.59% to 42.98% +/- 1.4 - i.e. nothing, exactly as theory
says it should, because the stop decision is not the critic's in that role.

**Decider** ("build or pass"): needs *calibrated slopes* - the price of money against the price of what the card gives. This
is where every failed experiment lived, with a consistent reason:

| experiment | result | why, given the price law |
|---|---|---|
| `mustBuild` (always build) | -4.7 points | the policy-critic's price said building is bad; forcing it ignored *when* |
| the stopMargin dial | 41.4-43.2 vs 43.6 | the gap it thresholds is computed with the wrong price |
| decider with the random-data critic | 18.9% | right price, but its card *ordering* is poor (22.8% as a ranker) |
| decider with projections, policy critic | 37.3% | good ordering, wrong price |

So a decider needs **both**: a correct price *and* a good ordering. Neither of our critics has both - which is precisely what
the owner's loop is meant to produce (a correctly priced value whose play is then good enough to teach the ordering).

**The policy's stop head** is the third consumer: it imitates the behaviour in the data, and in our data that behaviour
hoards. CE cannot fix it (it reproduces the labels); only data from a player that spends correctly can.

This is the roadmap in one line: *the ranker is close to its ceiling (~44%); the decider and the policy's stop head need the
loop, and the loop needs a value with a correct price - which the random-data critic already has.*
