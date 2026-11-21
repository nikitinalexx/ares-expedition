# METHOD — the errors we paid for, and the rules that prevent them

This file exists because of one specific mistake, made repeatedly over weeks, that made a whole ledger of results say the opposite of
what it measured. Read it before designing an experiment. Every rule below is tied to a measurement in `docs/RESULTS.md`.

## 1. The error, named precisely

> **A treatment that changes BEHAVIOUR without changing the TRAINING DATA is a statement about the current player, not about the thing
> being tried.**

The instance that exposed it (20/11): we forced our phase-3 player to pick phase 1 and measured 44.25%, with a corpus that trains
25-33%. We recorded "forcing a plan does not work / that plan is bad". What it actually measured is "a player trained on one plan is
incompetent at another". The second statement is trivially true and says nothing about whether the other plan is good.

Why it is seductive: the number is real, the corpus is real, and the label ("these games were lost") is honest. Everything is honest
except the conclusion, because the games were played by an incompetent player of that plan.

## 2. Which of our own results were affected (and which were not)

Affected - they must never be quoted as evidence about the mechanism:

| row | what it actually measured |
|---|---|
| forcing a plan (`phaseBias`) -> 44.25%, its corpus trains 25-33% | a phase-3 player forced onto phase 1; never a phase-1 specialist |
| the mulligan learned from explored games -> 0 discards | random discards in games where discarding was never a plan |
| "heavy exploration -> corpus -> 21%" read as "phase diversity does not pay" | only that random single decisions destroy coherence |

Not affected, and this boundary matters for honesty: every search/target/volume/capacity experiment (2-round horizon -8, 8
determinisations -6.3, build and input playouts, the tree -1.5, margin vs 3-step TD, 5k vs 10k games, wide nets at 2k games) changed the
ESTIMATOR on the same trained player, so each is a clean statement about that player.

## 3. The rules

1. **No conclusion about a plan from games where the policy was not trained for it.** A plan is judged by a specialist that converged on
   it, nothing else. Conversely: a plan is not "bad" because our current player cannot play it.
2. **The value may not arbitrate exploration.** `V` and any search on it reject an untrained plan by construction - this is the fixed
   point, restated. Exploration at the plan level is value-blind; the value's job is to play well the plan it was trained for. (Measured
   consequence: "guided exploration" by search score gave 14%, and could not have done otherwise.)
3. **Gate on the DERIVATIVE, not the level.** A new plan's question is "is its curve still climbing" (44% -> ? after each iteration). A
   specialist that flattens below the champion is a real negative; one still rising after two iterations is not.
4. **Cost is per plan.** The value is plan-conditional, so each plan needs its own bootstrap (~2-5 iterations at 1.5-4 h). The portfolio
   ("how many plans can we own") is a budget question, not a hope.
5. **Saturation is per plan.** 500 -> 1 250 -> 5 000 games climbed 11.6% -> 30% -> 73% and stopped: that is one plan's saturation. 10 000
   games of the old plan say nothing about how much a new plan needs.

## 4. Checklist before any experiment

1. Does the treatment change the **data**, or only the **behaviour**? If only behaviour - the result is about the current player, and must
   be written as such.
2. Which player is the treatment trained for? If none, do not draw conclusions about the mechanism.
3. What is the **gate on the derivative**, and how many iterations does the mechanism get before it is judged?
4. Is the claimed effect larger than the effect of **alternating seeds**? If it is under 1.5 points, it needs 10 000 games.
5. Which existing measurement **contradicts** this hypothesis, and what would make me drop it?

## 5. Background facts these rules rest on (all measured)

- **The fixed point.** `V` estimates the futures of the current policy; the search ranks by `V`; the corpus contains only its states. So
  resolution, depth, volume and capacity amplify the same value (`RESULTS.md`, many rows); they do not change plans.
- **The value is policy-conditional.** My 200 MC -> +2.92 logits, the opponent's -> -2.20; the same +1 income is priced 6.5 / 9.0 / 12.1
  MC by three different corpora against the rules' ~15. There is no strategy-independent position value in this game, and no analytic
  valuation can replace one (microbe chains cannot be priced; the owner's steel example).
- **No two policies share a world.** One shifted choice changes every later draw, so "the same world for two candidates" does not exist;
  variance questions are answered by holding the policy and varying the world, differences between plans by averaging over many states.
- **10 000 games of one plan added nothing** (73.17% -> 72.72%): volume is not the lever, and the 5 000-game point is where one plan's
  corpus saturated.

## 6. Why this file is short

It contains only rules that were paid for with a wrong conclusion. Add to it when a new mistake is found - with the measurement that
exposed it, and with the boundary of what was actually shown.

## 6. The same error in a new costume: grading an alternative by an incompetent player

The behaviour-cell table of 20/11 ranked cells by the mean margin of the CURRENT player's games in them. It is the 44.25% mistake again: a
cell is a way of playing, and a player that cannot play it loses in it - so the cell "looks bad" and would be dismissed, while a
specialist trained for it could put it at the top. Worse, the descriptor is built from the score composition, which is an outcome, so the
cell assignment itself changes with competence.

**The rule:** no alternative is ranked by the results of a player whose competence at that alternative is the thing in question. Alternatives
are ranked by a specialist trained for each of them, and the axis of any comparison must be an ACTION defined before the outcome (a
commitment: first phase, first card), not an outcome-derived property.

Checklist addition: *is the quantity that ranks the alternatives produced by the same player whose competence is being tested?* If yes, the
ranking is a statement about the player, not about the alternatives.

## 7. An oracle's argmax is worthless without a split sample

The audit of 400 phase decisions: an end-of-game judge, asked to pick the best phase over 8 worlds, "gained" **+7.49 points** over the value's choice -
because it picked the maximum of eight noisy estimates. Made to choose on 4 worlds and paid on the other 4, the same judge gained **-0.26 +- 0.79**.

Rule: any oracle-based label or claim must select on one set of worlds and be evaluated on a disjoint set. An in-sample maximum is not evidence; it is
the winner's curse, and here it was 7.5 points of it.

## 8. An evaluator that continues with the same policy, against the same opponent, cannot judge that policy

Found by the owner's check (21/11), in the decision audit:

* **The rollout was self-play.** `makeSeatsPlayable` only converts human seats, so in an AGENT-vs-SMART comparison both rollout seats played OUR policy. The
  "end-of-game margin" the judge reported was therefore the self-play margin - exactly the quantity V was trained on. The audit then only proves that V
  agrees with itself: the fixed point restated, not a measurement.
* **The continuation is the policy.** For a decision whose payoff needs a different continuation (a plan), a one-step rollout cannot see it by
  construction.

Rule: before believing an oracle, name (a) who plays the other seat and (b) what continuation plays our seat. If the answer is "us" and "us", the oracle
can only confirm us. The audit now takes {@code -Dares.agent.search.judgeOpponent=SMART} so the other seat is the benchmark; the continuation question
stays open for plan decisions, and any claim there must say so.
