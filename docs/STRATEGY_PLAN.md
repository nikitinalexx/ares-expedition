# Why the player cannot find a strategy, and the plan that can

Owner's question (2026-11-15): can this system *in theory* discover the strategies a human found - build a blue engine,
call phase 3 and activate it every round, steel/titanium economies, phases 1/2/5 - or is it only hoping?

## What a strategy is, in this game

A strategy is a **commitment over several rounds with a delayed payoff**:

```
build blue cards now  ->  call phase 3 every round  ->  activate them (money, cards, resources)
                      ->  more income  ->  more cards  ->  more points  ->  wins
```

Nothing in that chain pays at the moment it is started. A single decision looks neutral or even bad, and the payoff of the
whole chain shows up ten rounds later.

## The three conditions for learning it

1. **The data must contain the strategy.** If no recorded game has an engine, then no critic and no policy can know what an
   engine is worth: the counterfactual outcomes do not exist in the data. Our player never builds blue cards, so the value
   net has never seen a game where an engine paid off (`ml/feature_audit.py`: the card bits of the engine cards never fire).
2. **The evaluation must happen AFTER the payoff.** Comparing alternatives at the moment they are taken hides delayed
   payoffs behind immediate ones. This is exactly why the owner's NETWORK worked and why my first phase search did not:
   * NETWORK rolled the phase out and read VALUE at the **end of the phase**;
   * my one-ply phase search compared the **start** of each phase, where phase 1/2 have already handed over a build slot
     while income and the draft have not paid anything yet. That comparison is structurally biased, and it collapsed the
     player (measured: 0.3% - 32.8% against SMART).
3. **What the planner finds must be transferred to the fast player.** A rollout planner is too slow to play every game; the
   policy has to be trained on the planner's choices (the policy-improvement step), otherwise the deployed player cannot
   use it.

## Why the mechanisms I tried cannot do it (measured, not guessed)

| mechanism | why it cannot discover an engine |
|---|---|
| PPO / CE from game outcomes | the advantage of one decision in the chain is 1-3 points against a game noise of sd 17; the strategy is a *conjunction* of decisions, each of which looks neutral - the noise never reinforces the whole chain. Four nights of PPO plateaued at 37-40% vs SMART |
| one-ply value search on the phase | biased as above: it compares phase openings, not phase outcomes |
| exploration at temperature / candidate noise | produces near-copies of the same style, the games still contain no engine |
| hand-written rules ("build with discounts") | do not generalise, and the owner explicitly rejects them |

## The plan (each link measurable, each with a falsifier)

### Step 1 - data that contains an engine

Generator profile: `-Dares.agent.preferPhase=2,3 -Dares.agent.search.mustBuild=true -Dares.agent.pressBlueActions=true`
(build aggressive, call blue-phase, press every blue action including the extra activation). The resulting games contain
"many blue cards, activated every round" together with their outcomes.

*Metric:* cards and blue cards on the table, phases picked, and the share of games where a blue action was pressed.
*Falsifier:* the games still do not contain a table with several activated blue cards.

### Step 2 - a critic that knows what an engine is worth

Train V on those games (plus the normal ones). The claim to test is exactly the owner's: *"VALUE learned that big
combinations of blue actions are powerful"*.

*Metric:* a value audit on constructed positions - V(many activatable blue cards + money) vs V(same position without
them), and V(after activating one) vs V(before). Both must rise.
*Falsifier:* V does not separate an engine position from a plain one, in which case the search has nothing to see and the
data still is not enough (more engine games, then).

### Step 3 - the phase decision by rollout, read at the end of the phase

For every legal phase: copy the game, apply the phase choice with the engine's own transition, play the phase out with the
policy, read V at its end, pick the best. This is the mechanism that sees "I hold five blue cards: phase 3 pays now".

*Metric:* the bench scenarios "phase 3 with a blue table" and "activation instead of stop"; the phase mix in self-play
(phase 3 share must move off zero).
*Falsifier:* with an engine-aware critic the planner still refuses phase 3 on an engine position.

### Step 4 - transfer to the fast player

Record the planner's choices (phase, activation-or-stop) over a few hundred games, train the fast heads to imitate them
(candidate cross-entropy, our own search as the teacher - no outside teacher), then continue with PPO on the games the
resulting player generates.

*Metric:* the fast player's profile (`ml/check_player.py`: cards, phases, phase-3 spend) and strength against SMART on
10 000 games.
*Falsifier:* the distilled head plays differently from the planner (agreement measured on held-out decisions).

## What is still missing before step 3 can be judged

The rollout planner is implemented (`-Dares.agent.search.phasePlayout`, and `horizon` for the card search) and it is
affordable for the *deployed* player (~0.5 s per phase decision, ~12 decisions per game). It was rejected earlier for two
honest reasons that this plan removes: it ran with a critic blind to engines (step 2 fixes that) and with the symmetric
reading of an opponent whose hand was visible (fixed: `buildPublic`).

## How this relates to the owner's NETWORK

NETWORK did steps 3 and 4 inside one hand-written system: it rolled out the hypothetical phases, evaluated the end of the
phase with a value net, and greedily chose the action that raised VALUE most. That design did not produce one strategy: it
opened a *space* of coherent playstyles - blue engines realised in phase 3, steel and titanium economies, plant/heat
conversions, phases 1/2/5 build-and-draft loops - and the same mechanism picked whichever of them the position favoured,
because the rollout played each variant to its end and the value compared the ends. "Build blue cards and realise them in
phase 3" is one example of that space, not the target.

This plan is the same mechanism, with the parts that NETWORK had hard-coded learned from our own games instead: the
evaluator (V), the plan preference (from the rollout audits) and the policy (distilled from the search). The deliverable is
therefore not "an engine player" but a player that can *choose among* the styles the position allows - which is the only
thing that beats a benchmark specialised in one of them.

## What the first attempt at step 1 measured, and why it was wrong (2026-11-15)

Generator profile: `preferPhase=2,3`, `mustBuild`, press every blue action. 4 000 games, both seats our player:

| | engine profile | normal games |
|---|---|---|
| blue cards on the table (end) | **0.45** | 0.06 |
| cards on the table (end) | 0.42 | 0.15 |
| winning points (end) | **57.0** | 71.5 |

The profile does create engines (7x more blue cards) and it plays **worse** by 14 points. Training a critic on it would
teach "engine = bad", which is the opposite of the goal. The reason is structural: pressing *every* blue action is not the
same as playing an engine well, and our player has no idea which actions pay.

And a critic trained on our own games prices an engine at nothing, as measured by `--mode engineaudit`: adding four
activatable blue cards to a real position **lowers** the value by 0.50 logit, pressing them raises it by 0.30. Expected -
and it is the reason a search cannot prefer an engine.

## The chicken-and-egg, stated exactly

The value of an option depends on the policy that will realise it. A blue card pays only if someone calls phase 3 and
presses the actions afterwards; our policy does not, so a paired rollout with the plain policy honestly answers "the card
is worth ~0". The owner's NETWORK escaped this because the *heuristic* pressed the blue actions and only the value was
learned. Without copying heuristics the escape has to be an experiment: **force the plan inside a rollout and compare the
outcome**.

`--mode macroaudit` does exactly that, on real positions, with common random numbers:

* A: play the position to the end with the normal policy;
* B: the same copy, but for the next N turns a probe forces every blue action (and prefers non-green builds);
* the paired difference B - A is the causal effect of *committing to the engine for those turns*, not an opinion.

If B is systematically better in positions with blue cards, then (i) the engine is real for this player, (ii) the paired
deltas are labels the value and the policy can be trained on, and (iii) the rollout planner has something to prefer. If B
is not better, then the engine as our player would realise it is not worth it, and the honest conclusion is that the gap
against SMART is not "missing engine" but "the whole play is too weak for an engine to pay" - a different problem, with a
different plan.

## Where the "filigree" value comes from (owner's note)

The owner's value estimated the difference between "build this blue card", "build a red card and get two oceans" and so
on, including tempo and the *marginal* effect of another blue card when the engine already runs. Two consequences for this
plan:

1. the observation must let the value see the engine (it does: cards on the table by class, blue count, activations,
   incomes, both score totals), and the *marginal* structure is learnable only from games where both states occur - i.e.
   the data must contain engines at several sizes;
2. a 3-point difference is 0.17 sigma of the score feature and moves the critic by ~0.005 of win probability, which is
   below its error on a single state. The value can only resolve such differences if the *comparison* is made on the same
   state with the same noise - which is what the paired macro/rollout design does (common random numbers), and what a
   plain one-ply read of two different afterstates does not.

## The theoretical core: which improvement operator can find a plan

Four mechanisms, one criterion: can it evaluate a plan that the current player never executes?

| mechanism | evaluates plans it never runs? | measured outcome |
|---|---|---|
| PPO / CE on game outcomes of the current policy | no: it only sees the lines the policy plays | plateaued at 37-40% over four nights |
| a value net on the same games | no: an engine state never occurs, so V interpolates over a hole | `--mode engineaudit`: +4 blue cards lower V by 0.50 logit; pressing them raises it by 0.30 |
| forcing the plan inside the player | yes, but the plan is executed by a policy that does not know it | the forced engine profile scores 57.0 against 71.5 for normal play, so the data teaches "engine = bad" |
| **the simulator: execute the plan on a copy and measure** | **yes, without a teacher** | `--mode macroaudit`, paired rollouts with common random numbers |

The last one is the operator the owner's NETWORK had for the phase decision: roll the hypothetical phase out, read VALUE at the
end of it. Generalised to a plan of several rounds it is *model-based policy improvement*: a plan is judged by playing it,
not by a critic's guess, and the only assumption is that the simulator is the game (it is).

### What has to be true for it to work, and how it is checked

1. **A short plan executed by our own player must be at least neutral.** Otherwise no plan pays and there is nothing to
   learn - the honest conclusion then is "the play is too weak for a plan to pay", not "another training loop".
   Checked by `--mode macroaudit`: the paired difference of "play the plan for K turns" against "play normally" on the same
   copy. Noise per pair is ~17 points, so a 3-4 point plan needs ~500 positions to be resolved (that run is going).
2. **The planner must be able to see the difference.** A rollout to the end of the game costs ~0.5-1 s; a phase decision
   with 5 candidates is ~5 s, ~12 decisions per game, so a few hundred labelled games is one night. That is the budget of
   step 3, and it is why the planner is a *labelling* instrument and the fast player is distilled from it.
3. **The distillation must be faithful.** Measured by agreement between the distilled head and the planner on held-out
   decisions (the tooling exists: candidate cross-entropy reports top-1/top-3 on a validation split).

### Order of work, corrected

1. `macroaudit` at n = 500 for the two honest plans (press only free actions; build for income). One of them must show a
   positive paired difference with a CI excluding zero.
2. If it does: use the planner (rollout over phase candidates and over act/stop in the blue menu) to label a few hundred
   games, distill the phase and activation heads, and measure the profile (`ml/check_player.py`) and strength (10 000 games).
3. If nothing is positive: the ceiling is the execution quality of the plain player; then the plan is to strengthen it
   (data scale, architecture) *before* any strategy work, and that is a different, cheaper-to-argue claim than "learn an
   engine".

## The paired-plan test, resolved (2026-11-15)

`--mode macroaudit`: the same position played twice on the same copy, once normally and once with a plan forced for eight
turns, paired by construction (same deck, same dummy, same opponent).

| plan forced for 8 turns | n | difference | better with the plan |
|---|---|---|---|
| press every blue action, build any non-green card | 24 | -3.8 +/- 6.4 | 11 vs 12 |
| press only free actions, build any non-green card | **500** | **-1.2 +/- 1.2** | 214 vs 234 |
| build only cards that pay income, press free actions | 24 | -0.1 +/- 6.5 | 12 vs 9 |

The resolved answer is negative: **committing to the engine plan does not pay for this player**. The confidence interval
of the best of them is [-2.4, 0.0], i.e. neutral at best. Combined with the generator measurement (the forced engine
profile scores 57.0 against 71.5 for normal play) the picture is consistent: the *executor* cannot realise an engine, so no
plan, critic or policy can learn that an engine is good from it.

That is the falsifier written into this plan, and it points at the next operator: not a macro plan executed by a policy
that does not understand it, but **a rollout of the individual move** - the simulator chooses among the candidates itself
(`--mode rollouttest`): for each candidate the position is replayed to the end and the outcomes are paired. If the
rollout's best candidate beats the policy's choice on the same positions, planning is an improvement operator for this
player and its choices are the labels for distillation. If it does not, the ceiling is the plain policy and strategy work
must wait for a stronger one.
