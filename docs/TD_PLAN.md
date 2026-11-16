# Plan: verify the value function properly (TD, and the policy-conditional loop)

Written 2026-11-16 after the owner rejected two of my explanations. Both experiments below exist because the *principle*
is not in doubt any more: NETWORK compared 11 projections by value, with no hand-written rules, and built 27 cards a game.
What is in doubt is my critic, and each hypothesis is paired with a measurement that can kill it cheaply.

## Where the critic stands (measured, `--mode audit`, 600 positions, `v_rev1_margin_wide`)

Every direction is right, the symmetric reading is consistent (`V(me) + V(opp) = 0.97`), and the calibration looks sane -
**except one exchange rate**: `+1 income = 6.5 MC` where the rules make it worth ~15 MC at the median round. That single
number explains the hoarding (12.9 cards against SMART's 27) better than any story about noise, because with that price a
12-MC card never pays.

## H1 - TD on the game's own score fixes the price of an income stream

`ml/train_value.py --td-bootstrap DIR --td-steps N` trains V on the **remaining margin** `R(s) = final - current`, using the
exact score increment between consecutive decisions: `R(s_t) = (M(s_t+1) - M(s_t)) + R(s_t+1)`, with `R = 0` after the game
ends. The score is visible in every state, so the reward carries no noise, and the terminal label is used exactly where it
is exact.

Why it should move the income price: a Monte-Carlo critic must infer the whole future from a distant random outcome; a TD
critic is told the near-term score gain exactly and only has to estimate what *remains*. An income card's price and the
income it buys are then compared on the same scale, at every round.

**Falsifier (checked first, costs minutes):** the audit's `income mcIncome +1` row does not move from ~6.5 MC toward
~12-16 MC. If it does not move, TD has not changed the quantity that matters and the ranking result is irrelevant.
**Then:** single-critic win rate (5 000 games, ranking role) against 39.4% for the best Monte-Carlo critic; and the
**decider** test (V chooses build vs pass, `fullTypes=BUILD_CARD`), 3 000 games, against the 36.0% that configuration scored
with a Monte-Carlo critic. That 0.4-6% collapse earlier was measured with a critic whose income price was wrong, so it is not
evidence against the mechanism.

## H2 - the value of an income stream depends on the policy that follows it (the loop)

If H1 fails, the second explanation is the loop: in the games our critic sees, the player does not convert money into cards,
so income genuinely does not win, so the critic prices it at 6.5 MC, so the player keeps hoarding. NETWORK's critic was
trained on games in which the player *did* build.

Data: `-Dares.agent.buildAlways=true` (generator only, never decline an affordable build) + `-Dares.agent.afterstates=true`,
50 000 games. Two critics from it: (a) decision rows only, (b) decision + afterstate rows - the second one is trained on the
states the comparison actually reads, which is what `projectPhasePick` + `assumeProjectIsBuilt` produced for NETWORK.

**Falsifier:** the audit's income price stays below ~8 MC for both critics, or the decider configuration stays below 40% at
3 000 games. If the price moves but the decider does not, the failure is in the comparison (projections), not in the price.

## H3 - the projections the comparison reads (only if H1/H2 both fail)

Rebuild the decider exactly as NETWORK did: for each build decision, project the phase (`projectPhasePick`), build the card,
recurse one level into the next best decision, and compare the leaves. Our current decider applies the card to a copy and
reads the value immediately - a state the critic has rarely seen. This is a Java change of ~100 lines behind a flag
(`-Dares.agent.search.projectBuilds=true`), and its falsifier is the same decider measurement.

## Protocol for every candidate (no exceptions)

1. audit exchange rates (income, TR, points) - cheap, decides whether the critic is worth measuring at all;
2. single critic, ranking role, 5 000 games;
3. decider role, 3 000 games;
4. only if something is positive: ensemble, 10 000 games, profile (`ml/check_player.py`), sanity bench;
5. every claim about a critic carries an ensemble or three seeds, because a single critic's win rate has a +/- 3 point band
   (measured: 39.38% vs 42.98% for the same recipe on two samples).
