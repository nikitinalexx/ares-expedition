# PLAN V4 — the theory of change: break the fixed point by separating conversion from adversarial play

Written 20/11 at the owner's instruction: stop testing theories one by one and state a plan that is theoretically capable of
working, with the reason it can work and the reason each earlier attempt could not. Everything below is either a measurement
already in `RESULTS.md` or an explicit hypothesis with a gate that can kill it cheaply.

## 1. The object

We maximise one number: the final score difference against a strong opponent, at a fixed compute budget. Nothing else is a goal -
not AUC, not cards per game, not the phase mix. Those are instruments that tell us whether the number is being chased for the right
reason.

## 2. Why the loop cannot leave 73.17% (the fixed point, stated exactly)

Let `π_k` be the policy, `V_k(s) = E[u | s, a ~ π_k from s]` the value, `u` the final score difference, and `D_k` the corpus of games
`π_k` played against itself. Our loop is:

```
V_k    = regress u on the states of D_k
search = argmax_a  V_k(afterstate(s, a))      (+ noise, determinisations, deeper lines)
π_k+1  = imitate the search's choices
D_k+1  = games of π_k+1 vs π_k+1
```

Three facts, all measured, and together they prove the loop is closed at the current point:

1. `V_k` is an estimate of `π_k`'s futures. It is not an estimate of the position; it is an estimate of the position *as our player
   plays it onwards* (the owner's two examples: 5 steel + 5 titanium income, and AssetLiquidation with TR 0 and many green cards).
   The features are not blind - V prices a 200 MC stockpile at +2.92 logits - the conditioning is the problem.
2. The search's ranking of an action is `V_k` of its afterstate. So the search cannot prefer an action whose value is only realised by
   a continuation `π_k` does not play: it scores it as "our player, in this position, later" - which is bad. Hence search improves the
   choice *within* the current class of plans and cannot leave it. Measured five ways: 2-round horizon -8, 8 determinisations -6.3,
   build playout 0, input playout -1.5, the line tree -1.5, all at or below 73.17%.
3. `D_k` contains only states `π_k` reaches, and the label of a state is the outcome of `π_k`'s continuation. So the corpus cannot
   teach the value of a plan our player does not execute: the exploring games, when forced, are weak (44.25%, and their corpus trains
   25-33%), which the critic honestly learns as "this plan loses".

**Corollary (this is what closes the last month of work).** Better resolution (TD, more data, wider nets, thrift instrument), more
depth (rounds, determinisations, trees) and more compute inside the current loop are *amplifiers*, not escapes: they make the same
conditional value more precise. 10 000 games of the same policy is 10 000 games of the same plan; a tree sums the same bias.

## 3. The precondition for any escape

To learn that a plan is good we need data in which that plan was played **competently**; competent play cannot come from `π_k` (by
§2.2) and may not come from another player (owner's rule: no imitation of SMART, NETWORK or human games, no distillation). So the
competence must be *learned*, and the only place it can be learned is a task whose optimum is reachable by our own improvement
operator. Enumerating the possibilities:

| source of competence | status |
|---|---|
| deeper search (see the payoff, 10-15 rounds ahead) | infeasible: ~500 decisions a game, 40-wide menus |
| another agent as teacher | forbidden by the owner's rule, and it would import a style, not our strength |
| hand-written rules ("buy production first") | forbidden: measured to produce 44.25% and a corpus that trains 25-33% |
| **a reduced game where the missing skill is learnable without an adversary** | **untested, and the only one left** |

## 3b. Why an analytic valuation is not an option (owner's objection, 20/11) — and what replaces it

The proposal "compute the position's potential arithmetically" is dead, and the owner's argument is decisive on two independent grounds:

1. **Combinatorics.** The value lives in chains: a microbe card, a card that consumes microbes, a card that pays per microbe, a forest that
   comes out of it. Pricing each microbe at 3 MC prices none of that. The number of such chains is exactly why this game is interesting.
2. **The price is contextual.** The owner's own sentence: "in your player's world steel income is worth 5 MC; in my NETWORK's world it is
   worth 10". That is not an opinion - it is already measured here (`ml/price_report.py`): the same +1 income is priced 6.5 MC by the
   policy's corpus, 9.0 by forced-build play, 12.1 by random play, against the rules' ~15. Three different prices for one resource, and
   each is right for its own player.

**So there is no strategy-independent position value in this game.** The only value that exists is "this position, for the player who
will use it" - i.e. a value conditional on a PLAN, not on a state alone. Which reframes everything measured so far: our `V` is
conditioned on exactly ONE continuation (our current policy), so it can describe one plan and cannot compare plans. The fixed point is a
*plan* fixed point, and the owner's "V thinks the blue player is a genius" is V measuring the plan it knows.

**What replaces the arithmetic:** not a better price, and not a strategy I choose. The owner's question of 20/11 is the right one:
"what about a microbe strategy, or a mixed 1-2-3, or 4-5? How does your plan reveal strategies instead of looking for the ones you
imagine exist?" Any plan that NAMES a strategy ("the economic plan") is my imagination with a schedule. So the mechanism must be one
that does not need the strategies named.

## 4. The mechanism: discovery by coverage (an archive of behaviours)

The only class of mechanisms that finds strategies without naming them is **diversity pressure with an archive**: quality-diversity /
MAP-Elites, and at scale the AlphaStar league. The principle is one line: the objective of an iteration is not "beat the champion", it
is **"fill a behaviour cell that is empty"** - and an empty cell can only be filled by playing *differently*, which is exactly what a
single self-play loop never does.

- **Descriptions come from the game, not from me.** A game is placed in the archive by numbers the engine already produces: the share of
  the final points that came from TR / from card VP / from production-driven conversions, the phase-mix vector, the resource profile at
  round 8, the cards actually built. Coarse (2-3 dimensions), and derived from the game's own scoring structure - the same channels
  `ml/specialist_report.py` already fingerprints.
- **A cell is a behaviour mode, not a personality.** In this game the strategy is a map from the dealt cards to a plan, so one policy can
  land in several cells across games, and the archive stores "which policy produced a game in this cell", not a single labelled style.
- **The honest caveat, stated because it is the known weakness of the method:** the descriptors shape what can be discovered. The defence
  is that they come from the game's scoring/phase structure rather than from my idea of a strategy, they stay coarse, and a strategy
  counts as real only if it survives a second, differently chosen coarse descriptor set.

### 4.1 The loop

1. **Inventory (a measurement, not a build).** Take the existing 10 000-game corpus, place every game in the archive, and count the
   occupied cells. This is the honest answer to "how many strategies did we actually find in a month" - probably one to three - and it is
   the baseline every later iteration is measured against.
2. **Coverage iteration.** Choose the least-covered cell by a fixed rule (round-robin over cells below a target count: no cell is
   privileged), generate games with pressure aimed at that cell, keep the games that land in it, and train that cell's policy on them
   (`one corpus = one policy`, which we measured: two policies in one corpus -> loss 1.11 -> 1.97, player 31%).
3. **Competence where it is needed.** A cell whose plan cannot be executed by the plain policy (the microbe chain, the economy) is trained
   in the sandbox - same rules, same clock, same card pool, non-adaptive opponent - because that is the only setting where the deviation
   is not punished before it is learned (the forced-plan measurement: 44.25%, corpus 25-33%).
4. **Selection.** A general player must pick a cell from a hand and a board. Two candidate answers, measured against each other, not
   chosen by taste: (a) a learned selector over cells, trained on which cell won from which opening; (b) one policy conditioned on the
   cell id, trained on all cell corpora - which is also the direct test of whether conditioning survives at our data scale, the question
   left open by the "mixing destroys value" measurement.
5. **Duel.** The selector against the champion, 1 000 games, then NETWORK, then the 10 000-game rule if it is within 1.5 points.

## 5. Why coverage breaks the fixed point

The fixed point exists because the objective is "improve the current policy" and the only evaluator is that policy's value. Coverage
changes the objective: a cell is empty precisely because the current behaviour never produces it, so the iteration is forced to produce a
different plan - and then to learn *that* plan's value, which is the object the price measurement says is the real one (6.5 / 9.0 / 12.1
MC for the same income in three worlds). Nothing in the loop requires me to guess which strategies exist; it requires only that the
archive reports what is missing.

## 6. Gates

| gate | measurement | rule |
|---|---|---|
| **G0. Inventory** (hours, Python only) | occupied cells in the 10k corpus; plus the row-weighting audit from the decision-count observation (rows per round, share of the loss on blue-action rows) | no gate - it is the baseline, and it also says whether our critic has been trained on a style-weighted sample |
| **G1. A cell can be filled** (1-3 iterations, ~2-4 h each) | the target cell's occupancy, and the specialist's score *inside that cell* against the current player's score in the same cell, profile gate first | filled and better inside the cell -> the mechanism works; three iterations without filling -> the cell is recorded as unreachable at our skill, with the number |
| **G2. Coverage pays** (1 000 games) | the selector (or the conditioned policy) against the champion | not above 73.17% -> coverage without selection is worthless; closed, and that is a real answer |
| **G3. The duel** | 1 000 -> 10 000 against SMART, NETWORK | the usual rule |

## 7. How this can fail, and what each failure means

- **Descriptor artefacts**: cells fill, but the "strategies" are just noise in the descriptors. Caught by the second descriptor set.
- **Specialists are individually weaker**: expected; G2 is about the selector, not about the parts.
- **Selection fails** (conditioning does not survive mixing at our data scale): then the answer is one champion per opening family, and
  the project's shape changes from "one player" to "a book of players". Still a real product, and measurable.
- **Compute**: each iteration is 1.5-4 h of generation at ~1 game/s; filling tens of cells is weeks, not days. This is the honest size of
  the plan, and it is smaller than the AlphaZero route it replaces.
- **Night 7's precedent** (training against RANDOM stalled at ~68 points) applies to the sandbox step only, and the difference is named:
  win-bit objective with a saturated reward, no critic, no margin, no search. Inside a cell, the sandbox is a tool, not the frame.

## 8. What is deliberately not in this plan

- No analytic valuation (section 3b) and no named strategy, including "the economic plan".
- No corpus volume beyond what a cell needs; no deeper search; no target-shape variants.
- No imitation of SMART, NETWORK or human games.
- No rescaling of a mechanism that failed its gate.

## 9. Why this is a plan and not another experiment

It requires no guess about which strategies exist: it requires an archive, a fixed coverage rule, and a cell-by-cell gate. Its first step
is a measurement that is worth having even if the rest fails (how many behaviours do we actually have, and is our critic trained on a
style-weighted sample). Its failure modes are all measurable, and each one changes the shape of the project in a defined way instead of
inviting "one more knob".
