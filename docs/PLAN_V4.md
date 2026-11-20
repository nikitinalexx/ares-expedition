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

## 10. The honest case for and against this plan (owner's demand, 20/11)

### 10.1 Every previous approach changed the estimator, not the data distribution

| approach | what it changed | why it could not move the number |
|---|---|---|
| more corpus (5k -> 10k) | the size of one distribution | 73.17% -> 72.72%; the distribution was identical, so the value learned the same plan more precisely |
| deeper search (2 rounds, 8 determinisations, trees, playouts) | how the same V is queried | every variant 71-73%; the leaf is evaluated by a value conditioned on the same continuation |
| target shape (margin, channels, 3-step TD) | the label on the same states | 73.17% / 72.50%; a blend of two ~73% estimators stays at 73% |
| wider nets | capacity on the same data | 69.55% at 2 000 games: capacity without data is worse |
| action-level exploration | randomness on single decisions | the explored games are incoherent, so their labels teach "this is bad": forced plan 44.25%, its corpus trains 25-33%; the mulligan learned 0 discards |
| thrift instrument (saveAbove) | spending behaviour | money p95 unchanged at 15 MC: thrift does not create a production economy |
| imitation of SMART/NETWORK/human | the source of labels | forbidden by the owner's rule, and it would import a style rather than our strength |

One sentence covers the column: **none of them changed which states the corpus contains.** Everything was a better way to read the same
games.

### 10.2 The four measurements that say where the bottleneck actually is

1. The phase mix barely moves across corpora and bundles: 10/30/36/6/17. One plan, reproduced by every mechanism we built.
2. Force a different plan and it plays at 44.25%, and its corpus trains 25-33%: the plan space is not empty, it is unexplored and
   incompetent.
3. Mix two strong policies in one corpus and the value collapses (loss 1.11 -> 1.97, player 31%): one value cannot serve two plans. This is
   the measured form of "the value is plan-conditional" - and it means a population needs one value per plan, not one value for all.
4. 73.17% against SMART and 39.8% against NETWORK, with V's mean margin +0.94/+1.28 in games it loses two out of three: the value is
   calibrated for one opponent distribution and misprices another entirely.

Points 1-3 say the binding constraint is the plan space; point 4 says the second constraint is opponent/style coverage. Both are
**distribution** problems, and both are what coverage addresses. No estimator or search change addresses either.

### 10.3 Blocker by blocker: what this plan closes, and what it only hopes to close

| blocker | closed by | status |
|---|---|---|
| the corpus contains one plan | coverage iterations force different behaviours, per-cell corpora | mechanism is sound; risk = weak specialists (10.4) |
| exploring a plan is punished before it is learned | sandbox for cells whose plan the plain policy cannot execute | partial: only cells that need it, and the sandbox is one environment inside the cell, not the frame |
| one value cannot serve two plans | one corpus = one policy per cell | measured to be necessary; the cost is that selection becomes a separate problem |
| targets and weights depend on how many decisions a plan makes | round-grid labels, per-round loss normalisation | fully closed, arithmetic |
| style/opponent coverage (the 39%) | a league of our own specialists as opponents | weakest link: coverage is measured on OUR behaviour space, not on the opponent's |
| selection among plans | learned selector or a cell-conditioned policy | not solved by coverage; it is G2, and it is data-hungry |

### 10.4 The weakest joint, stated plainly

**Coverage without competence fills the archive with weak strategies.** A cell can be filled by exploration noise; if we then train on
those games, we imitate bad play - the exact trap of the forced-plan experiment (44.25%). The defence is the gate: a cell counts as
filled only when its specialist beats the incumbent INSIDE that cell, not when a game merely lands there. The consequence is honest:
some cells will never fill, and the archive will be smaller than the behaviour space. That is a result, not a failure of the gate.

The second weak joint is selection: choosing a plan from a hand and a board is the original problem in miniature, and at our data scale it
may be unsolvable by learning. If it is, the shape changes to "a book of players, one per opening family", which is still a real product
and still measurable.

### 10.5 Why this can cross 73% and why 39% is the harder number

For SMART: the mechanism is ordinary strength - more competent plans, each with its own value, and a selector that picks one. Every
component has a measured counterpart (plans exist, competence is the missing part, the value is plan-conditional). Nothing in it needs
SMART.

For NETWORK: our 39.8% is a style-calibration failure, and the honest mechanism is a league of our OWN specialists playing each other,
which is diversity without imitation. But the coverage is over our behaviour space: if NETWORK's plan is a plan our archive never
contains, coverage does not help against it. So the 39% number is the one this plan may NOT move, and it should be the last gate, not the
first promise.

### 10.6 What would make me drop the plan early

- G0 shows the archive already occupied by many cells: then diversity is not the binding constraint and this plan is aimed at the wrong
  target.
- G1 fails on three different cells: coverage is not reachable at our skill or our descriptors do not separate behaviours.
- G2 fails while G1 succeeds: coverage exists but selection (or strength of specialists) does not convert it into play.

Each of these is cheap and each has a defined consequence, which is the whole reason this plan is worth running instead of another knob.

## 11. Questions I would ask myself in the owner's place (20/11)

Grouped by what they can do to the plan: **P** = could change the plan, **H** = hygiene (must be true, cheap to check), **D** = diagnostic
that closes a specific gap.

1. **P - When are the points actually created?** If most of the final score is produced in the last 2-3 rounds (the owner's 90 -> 180
   jump), then long-horizon plan choice is secondary and end-game sequencing is primary, and the archive would optimise the wrong thing.
   Test: per-round score deltas by channel over the 10k corpus. Python only.
2. **P - How much of the margin is skill and how much is the deal?** Test: the same dealt world, two different policies (the paired
   determinisation we already have); if the paired difference is small relative to the total spread, decisions barely matter and no plan
   can move 73% much. Needs the engine, ~minutes.
3. **P - Does the behaviour space even have separate regions at high score?** If all strong games look alike in the descriptors, coverage
   chases ghosts. Test: descriptors of the 10k corpus against the final score. Python only.
4. **P - Is the draft the real seed of the plan, making the archive unnecessary?** The first picks commit the deck you will play; sampling
   first picks and then playing consistently might produce the diversity the archive is built to buy, at a fraction of the cost. Test:
   sample first picks, measure behaviour spread and win rate. This is a genuine alternative to the central mechanism and must be settled
   before building the archive.
5. **D - What exactly does NETWORK do that we do not?** Losses by score channel (TR / card VP / awards / milestones), plus its phase mix
   and build profile from the games we play against it. Benchmark analysis, not imitation: it tells us whether the gap is tempo or
   conversion, and therefore whether a plan-space attack is even aimed at the right gap.
6. **H - Are we capacity- or representation-limited at 10k?** Train a materially larger critic on the same pack: if validation loss improves
   and play does not, capacity is not the limit; if it does not improve, we are data/representation-bound. The earlier "wider nets are worse"
   was measured at 2 000 games, not at 10k.
7. **H - Is the margin the right objective for a WIN-RATE goal?** A 1-point win and a 50-point win count the same; the target is clipped at
   +/-6 logits. Compare margin vs clipped margin vs sign-with-margin-tiebreak on the same pack; the cost is a few trainings.
8. **H - Is the +5.9 of the phase playout real?** It carries the champion's structure. Re-run the ablation on the current corpus at 2 000+
   games; if it does not reproduce, our explanation of why we are at 73.17% is wrong even though the number is right.
9. **H - Is the corpus weighted by decision count?** (the owner's point) rows per round per game, share of the loss on blue-action rows.
   Python only; already in G0.
10. **D - What would the owner name as the three most frequent obvious mistakes of the player?** He watches games; the instruments do not
    see "wastes the last action of the round" or "buys a card it cannot pay to play". Measuring the frequency of those three is free
    diagnostic data, and it is not imitation: it describes our player, not his.
11. **H - Do we ever verify that a mechanism's gain survives a different seed set?** Attribution has been done on smaller runs than the
    73.17% measurement; the CI of the champion is +/-0.85, of the ablations often +/-3.

**Which of these can kill the plan:** 1, 2, 3 and 4. If points are made late (1), if deals dominate (2), if strong games are behaviourally
identical (3), or if the draft already supplies the diversity (4), then coverage is aimed at the wrong target - and all four are cheap
measurements on data we already own. That is why they belong BEFORE the first coverage iteration, not after.

### 11a. Correction: the skill-vs-deal test cannot be paired (owner, 20/11, second time)

"One and the same world for two policies" does not exist: one shifted choice changes every later draw, so the two branches do not share a
world after the first divergence. The test must therefore be built the other way round - **hold the POLICY, vary the WORLD**:

- take real states (say 100, spread over rounds 3/6/9), and from each one play the game to the end K times with the SAME policy and
  different determinisations of the hidden cards (no pairing is needed: we are measuring a spread, not a difference);
- the spread of the final margins is the world's contribution at that state, given our play;
- compare it with the spread of real margins of games sampled at the same round. If the two are comparable, the deal dominates and plan
  choice cannot move 73% much; if the world's spread is much smaller, decisions and plans are where the variance lives.

The earlier wording in this document used the paired framing and is wrong; the instrument is `judgeAudit`-like but its statistic is a
variance, not a paired difference.

## 12. Conclusions (owner, 20/11) - drawn from the answers to my own questions

1. **73% is not the architectural ceiling; it is the ceiling of one distribution.** Direct evidence from our own history: the same net,
   the same pipeline climbed **11.6% (500 games) -> 30% (1 250) -> 73% (5 000)** as the corpus became homogeneous and self-consistent. The
   architecture did not change across those three points; the data did. Therefore the question "can the same architecture and the same
   volume produce a phase-1 player instead of a phase-3 player" has an answer: **yes, by the same mechanism, applied to a corpus of
   phase-1 play.**
2. **The 44.25% "forced plan" result is confounded and must not be read as "phase 1 is bad".** It measured a player trained on one plan
   forced to pick another, with a corpus that trains 25-33%. It says our player cannot execute the other plan - which is trivially true
   and is exactly the missing competence. We have never trained a specialist ON the alternative plan.
3. **The unit of work is a specialist per first-round commitment, not an archive.** The archive stays as a *measurement* of coverage (how
   many behaviour regions we occupy), but the machinery that produces competence is the one we already have: self-play with the
   commitment forced for BOTH seats (a legal game - both players may pick the same phase), which keeps the corpus homogeneous (the
   measured requirement: two policies in one corpus -> loss 1.11 -> 1.97, player 31%), iterate like the bootstrap, and then compare
   specialists.
4. **Minimal forcing, maximal learning.** A specialist's identity is its first commitment (the round-1 phase pick and/or the first pick);
   everything after that is learned, so the plan is not a hand-written script but a seed the policy grows from.
5. **Order of work:** the three cheap premise measurements (points by round; behaviour regions of the 10k corpus; world-vs-skill variance
   by varying the world), then one specialist end-to-end (2-5 iterations, ~2-4 h each), then the comparison between two specialists. No
   archive infrastructure before a specialist has climbed.

## 13. What changed on the second deep review (owner, 20/11)

Six changes, each with a reason that comes from a measurement, not from taste.

1. **Force ONE seat and train on that seat's rows - do not force both.** The pack already stores `seat` (`train_value` orders the pool by
   `(game, seat, step)` and links rows within a seat), so the corpus can be homogeneous from the learner's point of view without teaching
   the opponent's seat a plan it will never play. The "force both seats" version would have produced a specialist trained against a
   mirrored opponent - a transfer gap disguised as a curriculum. Cost: record which seat was forced (derivable from the per-seat phase
   mix, which `mulligan_report.py` already does per seat; better to log it).
2. **Two-stage forcing, and the first stage is a probe, not a plan.** Stage 1: force the phase for the whole game (the hardest, most
   homogeneous corpus that exists) and ask one question - does the curve climb at all from 44%? This is the decisive test of "competence is
   learnable for a forced plan", and it is cheap. Stage 2: relax to the first commitment(s) only, which is the real plan. A probe result
   is never quoted as a plan result.
3. **The selector is Monte-Carlo, not learned.** At the first commitment, evaluate each specialist by its expected final margin over K
   determinised worlds (no pairing: an expectation over worlds, not a difference between two lines). Cost with 4 plans x 8 worlds x ~80 ms
   is ~2.5 s ONCE per game - affordable at play time, unlike per-move judging. A learned selector stays as the fallback if the MC
   estimate's variance is too large, and the measured "two policies in one corpus -> 31%" warns that a single model over plans is the
   riskier branch.
4. **The league is the answer to the 39%, and it needs only two specialists to start.** Each specialist trains on ITS OWN rows while
   playing the other one: diversity of opponents without imitation of anyone (both are ours). Gate: the worst benchmark (NETWORK) before
   and after, not just SMART.
5. **A budget and a stop rule, because cost is per plan.** Three specialists x up to three iterations is roughly two days of compute at
   the measured 1.5-4 h per iteration. At that point: review, with the curves in hand. Three iterations without a climb = the plan is
   recorded as unreachable at our skill, with the number.
6. **Forcing needs a unit test before a single game is generated.** The forced seat's phase distribution must be exactly the forced phase
   and the other seat's must not be. A silent failure of the forcing mechanism would recreate the original confound - the corpus would
   look homogeneous while measuring the old player - and that is precisely the error this plan exists to correct.

Unchanged, and deliberately: the three premise measurements come first (points by round; behaviour regions of the 10k corpus; world-vs-skill
variance by varying the world with the policy held); no analytic valuation; no named strategy; no deeper search; no corpus volume for the
sake of volume; no imitation.

## 14. The axis of the loop is a commitment, not an outcome cell (owner's correction, 20/11)

The owner's objection closes the last piece of the archive design: a behaviour cell built from the score composition is an OUTCOME, and a
player that cannot play the style loses in it - so the cell is graded by incompetence and would be dismissed exactly as the forced plan was.
The archive's margins therefore mean nothing about plans, and the cell assignment itself moves with competence.

What stays, and what changes:

* **Stays:** the inventory of observed behaviour ("what does our player actually do") - it is a description, and it is cheap.
* **Changes:** the axis of the loop is the **commitment** - the first-round phase and the first card taken. A commitment is an action: it is
  defined before the outcome, it can be forced in generation, and its specialist is graded by its own curve. Cells defined by actions cannot
  be contaminated by the player's competence in the way outcome cells are.
* **Gate unchanged in form, corrected in content:** a plan is judged by its specialist's curve and by the specialist's win rate against
  SMART, never by the mean margin of the champion's games that resemble it.

## 15. The axis must be an ENUMERATION, not my pick - and the seed must be allowed to anneal (owner, 20/11)

The owner's objection: "the plan was to let it explore all the strategies; why are you pushing it into phase 1 by hand? What if a phase-1 player
actually plays phase 5 a lot?" The distinction he is asking for is real, and it has two operational tests.

**Test 1 - is the axis enumerated or picked?** Forcing "phase 1" is legitimate only as ONE ARM of an enumeration of the game's own
commitments (5 phases, and later the corporation offer / the first card), where every arm gets the same treatment and the arms are compared by
their specialists' curves. Then no commitment is preferred by me; the loop is "run all arms, read which ones climb". Picking one arm because
I believe in it - which is what the current probe does - is a shove, and it may only be used to test the MECHANISM (does a
commitment-trained specialist climb at all), never to rank strategies.

**Test 2 - may the specialist leave the seed?** A shove means the specialist is clamped to the seed forever. A discovery means the seed is a
BOOTSTRAP CONDITION that the specialist is allowed to outgrow: the nudge rate must anneal across iterations (1.0 -> 0.5 -> 0.25 -> 0) and the
final measurement is made with the nudge OFF. If a "phase-1 specialist" ends up playing phase 5 a lot, that is the player voting, and it must
be allowed - the seed's only job was to make the first corpus coherent.

**The evidence that annealing is the right shape, already in hand:** p2's play-time profile is 24/23/18/4/31 - it was trained on a 65% build
corpus and plays 47% build with the draft at 31%. It is already outgrowing the seed. Clamping it would have hidden that.

**Consequence for the plan, stated plainly:** the current run answers "does the specialist loop climb under a forced commitment", and its
54.4% must never be quoted as "phase 1 is a 54% strategy". The ship-ready form is: enumerate the commitments, anneal the seed, measure each arm
with the nudge off, and let the arms' own curves decide which plans exist. The archive remains an inventory of what the specialists became,
not a ranking of what I guessed.

## 16. Mixed and dynamic strategies: specialists are the parts, the GATE is the player (owner, 20/11)

The owner's question: what if the ideal strategy lies at the intersection of phases 1-2-3, or is dynamic - "given my starting hand I decide whether to go
1, 2, 3 or 5"? And his own observation from NETWORK_V2: playing a move to the end (a whole phase) and evaluating the post-state with V, trained
initially on RANDOM data, produced exactly that behaviour - "I have several blue, I build blue; and since I already have many blue actions, I
build more blue actions", or "I have few blue, I have played green with income, I build more green". Is that a dynamic decision about which
specialist to become, and can we train it?

**Yes, and the architecture has a name: it is a mixture of options with a gate.** Three components, and only one of them is new to us:

1. **Specialists (options).** Each is a policy with its own value, trained on a corpus where its plan was actually played. This is what the
   corporation axis gives us, and what the probe already started.
2. **A gate (option selection).** At each plan point (the phase pick, a build decision), the player chooses WHICH option to continue with. This
   is exactly the owner's "play the phase to the end and evaluate the post-state": our `phasePlayout` is the same mechanism and it is the only
   thing that ever paid in our search (+5.9).
3. **A value per option** (the specialists' critics), because one value cannot serve several plans - measured directly: the same policy scores
   +10 points under its own critics and -10 under the champion's when the critics are swapped and nothing else changes.

**Why his V could do it and ours could not.** His NETWORK_V2 trained its value, at least initially, on RANDOM games. Random play visits states
our policy never reaches, so its errors are unbiased about those states; our value is trained only on our own games, so it is
policy-conditional and rejects any plan it has not played (the fixed point). This is consistent with what we measured from the other side: a
random-data critic in the RANKER role changed nothing (43.59% -> 42.98%), because there the shared slope cancels - but the GATE role compares
two plans from one state, where the slope does not cancel. So "add random-data value to the ensemble" was tested in the wrong role.

**Is the switching learned or gated?** Both are possible and they are different:
* the specialist's own policy already conditions on the state (hand, production, blue-action count, corporation) and can learn "many blue -> keep
  building blue" inside its own corpus - that is what the owner saw;
* the gate can be explicit and learned on gate decisions ("from this state, which option's rollout ends better"), which is cheaper and does not
  depend on one trunk carrying several styles (the latent-code experiment measured that tax at ~7 points).

**The acceptance test, so this is not another promise:** with at least two specialists, a gate beats the best single specialist, and it beats the
champion. And a direct diagnostic of the dynamic part: within one specialist's corpus, does the phase choice correlate with the state (blue count,
production on round 8)? If it does not, the policy is not learning the switching and the gate must be explicit.

**So: yes, this is plausibly the final form** - specialists as the parts, a gate as the player, a value per part, trained on outcomes, with an
unbiased component in the value so the gate can look at plans the policy does not yet play. The corporation axis is how we produce the parts
without authoring strategies; the gate is how they combine into a dynamic player.
