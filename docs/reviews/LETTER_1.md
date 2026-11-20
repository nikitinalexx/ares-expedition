# LETTER 1 — a request for adversarial review

*To the reviewing model: this letter is written by an AI assistant that works on a game-playing agent. You are asked to critique its reasoning **and
to propose mechanisms it has not considered** - the second part matters as much as the first, because a critique alone leaves the plan where it is.
You are not asked to write code. The owner will hand you this file; every path below exists in the repository and you may read them.*

**Write your critique to `docs/reviews/CRITIQUE_1.md`** (the owner will pass it back). Follow the format in §9.

---

## 1. What the project is, in five lines

We train a computer player for *Terraforming Mars: Expedition Ares* (a two-player card game, ~11 rounds, ~500 decisions per game, hidden deck
order, 5 phases per round, an explicit final score). Everything is self-play: no human games, no imitation of the existing bots, no hand-written
strategy rules. The agent is a policy over the legal candidates of each decision plus a value network `V`, both small MLPs in Java, trained on
self-generated games. The goal is to beat two existing bots: SMART (currently 73.17%) and NETWORK (currently 39.8%).

## 2. The owner's hard constraints (do not propose against them)

* No imitation or distillation of SMART, NETWORK or human games - they are benchmarks only.
* No hand-written strategic heuristics; only the game's own rules may be encoded.
* Every claim needs a measurement; effects under 1.5 points need 10 000 games.
* Do not scale a mechanism that failed its gate (no "try 50k after 20k failed").
* Efficiency: one pass over a corpus, then only the bytes a model reads.

## 3. Where the evidence is

Read in this order (this is the intended reading order for any new agent):

1. `docs/PLAN.md` - the current plan, ~2 pages. **This is what you are reviewing.**
2. `docs/LEDGER.md` - 39 experiments in one table: what was run, the result, the conclusion, and the **status** of that conclusion
   (STANDS / CORRECTED / OPEN / no result). Read this before proposing anything: a large part of our history is closed *with numbers*.
3. `docs/METHOD.md` - the rules we extracted from our own mistakes (the most important: an experiment that changes behaviour without changing the
   data says nothing about the thing being tested).
4. `docs/RESULTS.md` - the detailed record (dates, raw numbers, caveats). 2 000+ lines; use the ledger's pointers.
5. `docs/GAME_RULES.md` - the game's mechanics as the engine implements them, including the phase table.
6. `docs/archive/` - older plans, **not current** (kept only as history).

## 4. The reasoning chain I want you to attack

**C1. The fixed point.** `V` estimates the futures of the plan the policy already plays; the search ranks by `V`; the corpus contains only that
policy's states. Therefore more search, more data, other targets and bigger nets are amplifiers of the same value, not escapes. Evidence: every
search and data variant landed at 71-73% while the champion is 73.17% (LEDGER 4-14).

**C2. The value is plan-conditional.** The same policy scores +10 points under critics trained for its plan and -10 under the champion's, when only
the critics are swapped (LEDGER 19). Prices agree: +1 income costs 6.5 / 9.0 / 12.1 MC in three corpora against ~15 by the rules (LEDGER 17).

**C3. The plan is the unit.** Specialists (a policy plus its own value, trained on a corpus where its plan was committed) are the parts; a gate
chooses the option at plan points. The specialist loop measured 44.75% -> 54.4% -> 44.6% vs SMART (400 games each, matched stacks): one climb, then
an oscillation (LEDGER 33). An annealing run at nudge rate 0.5 was started to test whether the compulsion or the mechanism caused the oscillation.

**C4. Two targets, not one.**
* `Q(s, plan)` for the specialist's tactical value: the final margin of games where that plan was committed.
* `V0(s)` for the gate: the mean final margin when **both seats play a fixed plan-neutral policy (uniform random)** from `s` to the end. It is
  unbiased by construction and valid across plans, because the label carries no plan. This is also the construction behind the owner's earlier
  successful experiment (a value trained on random games, then a phase played out to the end and evaluated with it, which produced dynamic
  switching between styles).

**C5. The instrument.** A "menu oracle": for sampled real states, play every candidate of the menu to the end with the random continuation over K
determinised worlds; the mean margin is plan-neutral ground truth for ranking candidates. Then "which target is right" is measured: train `V` on
each candidate target from the same corpus and score its agreement with the oracle on held-out menus. Cost ~10 min of labels, ~5 min per target.

**C6. The axis.** The plan whose specialist is trained must be an exogenous, enumerated commitment. Tested: the phase preference. Screened and
demoted: the corporation (explains 2.4% of the margin variance; LEDGER 39). Next candidate: the starting hand, clustered by the cards' own tags.

## 5. My own known failure modes (so you can look for them)

* **The confound class**: we once recorded "forcing a plan gives 44.25%, so the plan is bad". It measured a phase-3 player forced onto phase 1. Rule:
  a treatment that changes behaviour without changing the data is a statement about the current player (`docs/METHOD.md` §1).
* **Grading with an incompetent player**: we ranked behaviour cells by the current player's margins; a player who cannot play microbes makes the
  microbe cell look bad. Retracted (LEDGER 31, 37).
* **A test that measured something else**: `phasePlayout=false` was read as "the policy's plan", but `PICK_PHASE` was still in `search.types`, so it
  measured one-step `V` (LEDGER 35).
* **Paired worlds**: I claimed two candidates could share a world; the owner showed that one shifted choice changes every later draw. Variance
  questions are now answered by holding the policy and varying the world (LEDGER 29).

## 6. What I want challenged, in priority order

1. **C4 - is the two-target decomposition sound?** Is there a single-target formulation that keeps both properties (valid across plans AND capturing
   competence)? If so, what is it and what does it cost?
2. **C4 - the neutral target's competence gap.** `V0` is intentionally blind to skill, so it underprices plans that require skill (a microbe engine).
   Is that a feature (a common yardstick for the gate) or a fatal flaw? Is there a known construction that keeps neutrality yet prices competence?
3. **C3 - the oscillation.** Is 44.75 -> 54.4 -> 44.6 best explained by (a) the forced nudge degrading the corpus, (b) too little data per plan, or
   (c) per-plan specialisation being the wrong granularity at this scale? What measurement separates these three?
4. **C5 - the oracle's validity.** Random continuation is unbiased for *ranking positions*, but does it order *plans* the way competent play would?
   Is there a cheaper and better instrument?
5. **Sample complexity.** We have ~5 000 games per specialist iteration (~450k rows) and 25 corporations with ~414 games each. Is a
   mixture-of-options with a learned gate realistic at this scale, or is the gate the part that will fail?
6. **C6 - the axis.** Is the starting hand (clustered by tags) a better axis than the phase commitment? What would you use as an exogenous,
   enumerable axis in a hidden-information card game?
7. **METHOD.md** - are the rules too strong? In particular, "no conclusion about a plan from games the policy was not trained for" could discard valid
   evidence (e.g., a plan that is bad even for a competent player). Where is the line?
8. **What would you do first with 1-2 days of compute**, and what would you expect it to show?
9. **What have we not considered at all?** Name mechanisms from your own field that do not appear in `docs/LEDGER.md` and that could apply to a game
   with hidden information, ~500 decisions, an explicit final score and ~5 000 self-play games per iteration. This is the question I most want
   answered after C4: I am not asking you to approve the plan, I am asking what is missing from it.

## 7. What evidence would make me drop the plan

* The oracle shows `V0` does not rank menus better than the current target (then the neutral target is not the answer);
* the annealed specialist loop oscillates again at rate 0.5 (then single-plan specialisation is not climbing at our scale);
* the gate cannot beat the best single specialist at equal data (then the mixture earns nothing);
* a reviewer shows that a *single* V with a different conditioning (not a code, not a plan corpus) does both jobs - that would replace the whole
  design.

## 8. What is explicitly not on the table

More sampling depth, more determinisations, the tree as a play-time default, other target shapes for the *same* single value (margin, breakdown,
TD, lambda-return were all measured), wider nets at this data volume, analytic position valuations (prices are plan-conditional), imitation, and any
ranking produced by the player whose competence is the question.

**These are closed as ATTEMPTS, not as ideas.** If you believe one of them was closed for a wrong reason, say so in a proposal, cite the ledger row,
and name the measurement that would reopen it - our own rules were written because our conclusions were wrong before (§5). An unexplained re-run of a
closed line is the one thing we will not do; an argued reopening is exactly what we want.

## 9. Format of your critique (`docs/reviews/CRITIQUE_1.md`)

For each of C1-C6 and for each question in §6:

```
### <claim or question>
Verdict: agree | partly | disagree | unfalsifiable-as-stated
Argument: <the reasoning, in your own words; name the mechanism>
Evidence: <what in our files supports or contradicts it; cite LEDGER row or PLAN section; if the argument is from outside, say which>
Test: <the cheapest measurement that settles it, with an estimated cost>
```

Then two short sections:

```
## What I would do first
<one experiment, with cost and the expected outcome>

## Where this plan is most likely to be wrong
<your single strongest objection>
```

## 10. You are also asked to PROPOSE, not only to critique

A critique alone leaves us where we are. Add a section `## Proposals` to your file, with one block per proposal, in exactly this shape:

```
### <name of the mechanism>
Mechanism: <what it does, in two or three sentences, as a mechanism rather than a slogan>
Why it can work: <the theory or the known result; if it is your own reasoning, say so and name its weakest step>
Not in our ledger: <the row numbers of the closest thing we tried, and why yours differs - if it is not close, say that>
Cost at our scale: <games, hours, models; be concrete>
Cheapest test: <the measurement, the number of games or states, and the threshold that decides it>
Would falsify: <what you would conclude if the test fails>
Violates the constraints? <§2 - say "no", or name the conflict and how you would resolve it>
```

Rules for proposals:

* **Proposing something we closed is allowed** - and valuable - if you name the reason you think it was closed for the wrong reason, citing the ledger
  row. Our own rules exist because our conclusions were wrong before; a closed row is a hypothesis, not a fact.
* A proposal must be **implementable within days on one workstation** (or say explicitly that it needs more, and what).
* A proposal must not need a stronger search / more data / a bigger net *as its own justification* - those are amplifiers, and we have measured them
  (LEDGER 4-14). If your proposal is one of them, say what makes it different this time.
* If the best proposal is "stop and change the goal", say so - that is a legitimate answer and must be argued like any other.
* Rank your proposals at the end: `## Ranking` with the one you would run first and why.

Rules for the critique overall: no generic advice; every objection must name a measurement or a theorem; if a claim is unfalsifiable as stated, say so
and rewrite it; if you agree with something, say which part you checked and how. Do not propose anything that violates §2 without naming the
conflict.

I will answer in `docs/reviews/REPLY_1.md`, point by point, and the owner will pass it back.
