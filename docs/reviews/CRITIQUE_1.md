# CRITIQUE 1 — reply to LETTER_1 (20/11)

*Reviewer: Claude Opus 5.5, working in the repository at commit `11f707cc` and reading only files in it. **A note on independence that
the owner should weigh**: the author of LETTER_1 is an assistant from the same model family, so this review is "outside" in its context
but not in its training. Where my argument rests on my own reasoning rather than on a file or a published result, I say so. A second
review from a model of a different family would be a real control.*

*What I read in full: `docs/PLAN.md`, `docs/LEDGER.md`, `docs/METHOD.md`, `docs/GAME_RULES.md`, `docs/reviews/README.md`, and these
sections of `docs/RESULTS.md`: header, bootstrap and night loops (lines 1-320), phasePlayout (1194-1252), specialist B / the forced plan /
"a corpus inherits..." (1396-1540), V blind / plan probe (1638-1760), and everything from 1864 to the end. In the code I checked two facts:
`services/agent/AgentDecisionService.pickPhase` (how a nudged pick is recorded) and `PlanCodes` / `ObservationBuilder` (whether the plan
code enters the observation).*

---

## The three findings that matter most (summary)

1. **Row 19, read straight, says the opposite of how the plan uses it.** The specialist policies play **better** with the champion's
   critics than with their own: p1 goes from 44.75% to **76.5%** (+32), p2 from 54.4% to 66.25% (+12). "+10 under its own critics, -10 under
   the champion's" is an *interaction* (the p1→p2 change has opposite signs under the two critic sets). It is not a main effect. The largest
   effect in the table is "the champion's critics are better critics for these policies". That contradicts the PLAN rule "never as a policy
   dropped into another plan's search". p1 + the champion's critics is also the **highest point estimate in the whole project (76.5%, 200
   games)**, and nobody has re-measured it.
2. **The specialist corpora are off-policy, and the code shows it.** `AgentDecisionService.pickPhase` (lines ~165-178) records a nudged phase
   pick through the same `record(game, player, decision, index)` call as a policy choice. Nothing flags it. So at every nudged pick the
   policy's cross-entropy imitates the nudge. Both seats are nudged, so the critics learn margins against a nudged mirror opponent. At
   evaluation the nudge is off and the opponent is SMART. This mechanism alone predicts all three observed facts: the specialists' own
   critics are weak (row 19), quality falls as the nudge compounds (row 33, rate 1.0), and halving the nudge helps (52.5%). No
   "plan-conditionality" is needed to explain them.
3. **`V0` is not plan-neutral. It is the value under one particular plan: "both seats play uniformly at random".** Row 17 already measures
   that plan's price (+1 income = 12.1 MC). A gate on `V0` will prefer the plans that need the least skill to convert, which is the opposite
   of the plans the content axis is meant to find (microbe and animal chains). And the oracle as costed (K = 2) is almost certainly too noisy
   to rank anything (see C5).

There is also a fourth, cheaper finding that reopens part of the ledger. **"A corpus inherits the strength of the player that played it"
(RESULTS ~1516) is confounded with corpus size.** Every weak-generator corpus in that table has 1 000-2 000 games, and row 10 says the
champion's *own* play at 1 250 games trains a 30% player.

---

## Part I — the claims

### C1. The fixed point
Verdict: **partly / unfalsifiable-as-stated**

Argument: The loop is approximate policy iteration (API). A critic is fit to the current policy (`V^π`), a one-ply greedy search over it
gives the improved player, and the policy imitates the search (expert iteration). In exact PI, "V is conditioned on π" is not a trap. It is
how PI works, and its fixed point is the optimum. API stalls for two specific reasons:
- the one-step improvement available is smaller than the critic's error at the candidate level;
- greedy optimisation against an approximate value exploits that value's errors. This is the maximisation bias ("winner's curse") of
  argmax over noisy estimates.

The second mechanism explains rows 4, 5 and 8 better than "the same plan" does. More determinisations, more depth and PUCT all give the
argmax more room to find the critic's over-estimates, and that is why they make things *worse* (−6 to −8) rather than flat. "Amplifiers of the
same value" would predict 0, not −8.

The history also shows the ceiling moving whenever the improvement operator gained resolution where `V^π` was poor: the critic given two
more menus (27.9% → 47.6%), and phasePlayout (+9.2). So the 73% is not shown to be a fixed point of the mechanism. It is a fixed point of
*this* operator at *this* critic error.

Rewritten so it can be falsified: *"On real decisions of the champion, the candidate the champion picks is, within noise, the argmax of
`Q^π(s,a)` (full-game outcome with the champion continuing). So no one-step improvement exists that a better `V^π` could find."*

Evidence: LEDGER 2, 4, 5, 8, 10, 14. RESULTS 610-695 (giving the critic the other menus: 27.9 → 47.6 → 57.45). The fixed-point argument in
RESULTS 1864-1890.

Test: the `Q^π` rollout oracle of **P1** on ~200 phase decisions. About 1.5-5 h on 14 threads (cost derivation in P1). If the measured gain
is ≈ 0, C1 holds in its strong form, and that *supports* the specialist plan (improvement then needs multi-step change). If the gain is
clearly positive, the stall is critic error and the specialists are not needed to get past 73%.

### C2. The value is plan-conditional
Verdict: **partly** (true by definition; the row-19 evidence is confounded)

Argument: `V^π` depends on π by definition, and row 17 (three prices for one income) is a clean illustration. That part I accept.

The specific claim — that the conditioning is on the *plan*, strongly enough that each plan needs its own critics — rests on row 19, and row
19 has three uncontrolled differences between "own critics" and "champion's critics":
1. **Training quality.** The corpus-1 critics have loss 2.03 against 1.33-1.40 for corpus 2. Loss is not fully comparable across corpora,
   but the corpus-1 critics are visibly the worst fit in the series.
2. **Behaviour policy.** The own critics are `V` of a nudged, mirror-nudged policy pair (point 2 of the summary). At evaluation neither seat is
   nudged and the opponent is SMART. That is off-policy evaluation without correction.
3. **A possible input shift** (verify, I could not see the eval command line). The 4 plan-code bits are part of the observation
   (`ObservationBuilder` ~618, `PlanCodes.code`). The specialist corpora were generated with `planCode=8`, so bit 3 = 1 in every training
   row. Bit 3 also requires `planNudges=true` to switch the *nudge*, but the bit enters the observation whatever the nudge flag. If the
   400-game evaluations did not pass `-Dares.agent.planCode=8`, the specialist stacks saw a bit value they never trained on.

With these three uncontrolled, the interaction (+9.65 under own critics, −10.25 under the champion's) could come from "corpus-2 critics are
better critics than corpus-1 critics" plus noise. Neither interval is at 10 000 games. The cross-stacks are 200 games each, about ±6.

Evidence: LEDGER 17, 19. RESULTS 2125-2150 (the table and the loss numbers). Code: `AgentDecisionService.pickPhase` and `record(...)`,
`ObservationBuilder` line ~618.

Test: the replicated cross-stack in "What I would do first" (about 2-3 h). The decisive quantity is the interaction term with a confidence
interval, *with critic-seed replicates*.

### C3. The plan is the unit; the oscillation
Verdict: **partly**. The climb and the oscillation are both within ~2σ once training variance is counted, and the mechanism the letter
favours — "the compulsion" — has a sharper, testable form: off-policy labels.

Argument (noise model, my arithmetic):
- **Evaluation noise.** At 400 games and p ≈ 0.5 the evaluation sd is 2.5 points.
- **Training noise.** The only same-recipe pair in the record is 42.98% / 39.38% (RESULTS header and EXPERIMENTS ~1405), so one critic's seed
  sd is ≈ 2.5 points, roughly ≈ 1.5 for a three-critic ensemble.
- **Combined.** A point has sd ≈ 2.9-3.5, and a difference of two points has sd ≈ 4.1-5.0.
- **The z-scores.** 44.75 → 54.4 is z ≈ 1.9-2.4. 54.4 → 44.6 is z ≈ 2.0-2.4. 52.5 vs 44.6 is z ≈ 1.6-1.9.

So "it climbed", "it fell" and "the anneal held the level" are each one ~2σ event, and three of them are being read together. That is
suggestive, not established. G1 ("three iterations flat") has no noise model at all, so it cannot tell "flat" from "noisy".

Mechanism: see Q3.

Evidence: LEDGER 33; RESULTS 2346-2430; RESULTS header (critic-seed variance).

Test: seed replicates plus the masked-label retrain (Q3), about 3 h.

### C4. Two targets, not one
Verdict: **disagree**

Argument: Two separate errors.

**(a) "Unbiased by construction" is true only of the estimator.** `V0` is an unbiased estimator of `Q^{rand}`: the margin when both seats
play uniformly at random. Random play is a plan: it has a measured price (+1 income = 12.1 MC, row 17), and it systematically fails any line
that needs a coherent sequence of actions. That covers microbe → animal chains, saving to convert, and timing a discount. So the gate's
yardstick is "what this position is worth to an incompetent player". It favours positions whose value is realised automatically: points
already on the board, TR, immediate production. It disfavours exactly the content plans the next axis is meant to find (PLAN, "The axis",
item 2).

**(b) The gate does not need neutrality. It needs comparability.** In the options framework (Sutton, Precup and Singh 1999, "Between MDPs and
semi-MDPs") the value of choosing option *i* in state *s* is the return when option *i*'s own policy is then followed:
`Q(s, i) = V^{π_i}(s)`. That *is* plan-conditional, and correctly so: the gate asks "if I commit to plan *i*, which I will then play with
specialist *i*'s skill, how do I do?". The comparison across *i* is valid if all `V^{π_i}` are estimated:
- on-policy (no nudges at play time, and none uncorrected in the data);
- against the **same opponent**;
- on the same scale (final margin).

Under those conditions the specialists' own critics *are* the gate's values. A second, neutral target is unnecessary, and it is harmful for
the reason in (a).

The owner's NETWORK_V2 precedent is not evidence of strength. I found no measured win rate for it against SMART in the files I read. It is no
longer offered (RESULTS ~834), and `docs/HANDOFF.md:13` names it as a player to beat. "Produced dynamic switching" is qualitative.

Evidence: LEDGER 17 (the random corpus's price); PLAN "The target question"; RESULTS ~834.

Test: two parts.
- **Fatal-flaw check for (a), about 1 h.** On ~300 phase decisions, compare the argmax of `Q^{rand}` (oracle) with the argmax of `Q^{champ}`
  (P1, a small K is enough for agreement statistics). If `V0`'s preferred phases agree with `Q^{champ}` no better than the current `V` does,
  `V0` brings nothing to the gate.
- **(b) is P3.**

### C5. The menu oracle
Verdict: **disagree with the instrument as costed; agree with the idea of an outcome-based instrument**

Argument:
- **Noise at K = 2.** With K = 2 worlds, the per-candidate oracle has sd ≈ σ/√2. σ is the sd of the final margin under random continuation
  from a mid-game state. It is not measured, but the per-state difference sd under the *policy* continuation is quoted as ~20 (RESULTS
  ~1918), and random continuation can only be noisier.
- **A small signal.** One decision followed by ~250 random decisions per seat leaves a small effect under random continuation. My estimate
  is ≤ 1-2 points, own reasoning, unmeasured.
- **The consequence.** The label's reliability (signal variance over total variance) is then of order 1-2/(1-2 + 200) ≈ 0.01. Top-1
  agreement will sit at chance for *every* target, and the experiment will return "no difference". That is a non-result dressed as a result.
- **What survives the noise.** Comparing targets by their correlation with the *same* noisy oracle is valid in principle, because the
  attenuation is common to both. But it needs n ≈ (z/ρ)² menus with ρ the attenuated correlation. At ρ ≈ 0.05 that is several thousand menus
  at K = 2, not one sample per round over 2 000 games.
- **A second objection.** The oracle uses the same random continuation as `V0`, so it grades `V0` on `V0`'s own ground truth. That
  agreement is circular: it measures how well a network fits `Q^{rand}`, not whether `Q^{rand}` ranks moves well.

Evidence: PLAN "The instrument that decides it" (the cost line); RESULTS ~1910-1925 (pairing and per-state variance).

Test: **split-half reliability first, ~5 minutes.** Run the oracle twice, independently, at K = 2 on the same 200 menus and correlate the two
vectors of candidate differences. If r < 0.2, the oracle cannot rank targets at this K (Spearman-Brown gives the K needed). Do this before
training any target against it. And replace the random continuation by the champion's (P1) for any use that is meant to judge moves.

### C6. The axis
Verdict: **partly**

Argument: Exogeneity is needed to *compare* plans without bias. It is not needed to *create* a specialist, and the plan conflates the two.

**"Filter by the deal" cannot create a missing plan.** It stratifies the champion's games by the opening hand. The champion does not play
microbes even when dealt them (Fish 0.05 per game, Herbivores 0.21), so a "microbe-hand specialist" trained this way learns the champion's
non-microbe play in microbe hands, from 1/k of the data. Row 10's volume curve (1 250 games → 30%) then predicts a weaker player.

**The tag nudge changes the data**, which is right, but it will have the off-policy problem of the summary's point 2 unless forced decisions
are flagged (P2).

**On the axis itself**: the opening hand is exogenous, which is good. Clustering it by tags is a choice of metric, and the 25 corporations
already showed that a deal variable can explain little (2.4%). A cheaper first screen exists: the eta² of the margin on the **count of
engine-starter tags in the opening hand plus the first 3 drafts**, computed on an existing pack. That takes minutes, it is like row 39, and
it is subject to the same "screen, not a refutation" caveat.

Evidence: LEDGER 39, 40; RESULTS "What the agent actually builds" (~1691); row 10.

Test: the tag eta² screen (minutes). Then a tag-nudged corpus *with flagged nudges* (P2), one iteration, 1 000 games.

---

## Part II — the questions of §6

### Q1. Is there a single-target formulation that is valid across plans AND captures competence?
Verdict: **yes, in my view; it is the options value with a common opponent**

Argument: The target is `Q(s, i) = E[final margin | s, my seat follows π_i from here, the opponent follows π_ref]`, for a fixed reference
opponent (the champion). It captures competence because π_i is the competent specialist. It is valid across plans because every *i* is
measured against the same opponent, on the same scale, on-policy. Physically it can be K separate critics, which avoids the shared-trunk tax
of row 25. They are one target with K conditionings, not "two targets".

The reason the current specialist critics do not already have this property is that their corpora are mirror self-play with both seats
nudged.

Evidence: LEDGER 24 and 25 (why not one trunk); RESULTS 2192 ("both seats, self-play").

Test: P3. The calibration of each critic against realised margins of π_i vs the champion, on held-out states.

Cost: one 5 000-game corpus per specialist with an asymmetric seat assignment, the same cost as today's iterations.

### Q2. V0's competence gap: feature or fatal flaw?
Verdict: **fatal for the stated purpose** (choosing *among content plans*), harmless for the job it is not being asked to do

Argument: Neutrality of the label does not make the comparison neutral. It replaces "biased towards our plan" with "biased towards plans an
incompetent player converts". Those are the plans whose value is already on the board, which is the opposite of "find the microbe engine".

The known constructions that keep comparability *and* price competence condition on the competent policy and fix the opponent: Q1, or P3.
Pure neutrality, i.e. a value of the position itself, does not exist in a game where the price of a resource depends on who will spend it.
The project's own METHOD §5 says so.

Evidence: LEDGER 17; METHOD §5 ("There is no strategy-independent position value").

Test: the C4 fatal-flaw check (`Q^{rand}` vs `Q^{champ}` argmax agreement on ~300 phase decisions, about 1 h).

### Q3. The oscillation: (a) the nudge degrading the corpus, (b) too little data per plan, or (c) the wrong granularity?
Verdict: **(a), in a specific form, is the leading hypothesis; noise (d) is not excluded; (b) and (c) are not yet testable separately**

Argument: (a) has a concrete mechanism visible in the code.
- **The policy side.** A nudged pick is recorded exactly like a chosen one (`AgentDecisionService.pickPhase` →
  `record(game, player, decision, index)`). At rate 1.0 the nudge fires at *every* legal pick with a buildable card, so a large share of the
  phase rows teach the compulsion. Each iteration starts from the previous policy (`--prev`), so the compulsion compounds. That fits the
  drift (income 5 → 17 → 0%, draft 5 → 5 → 22%) and the fall at iteration 3.
- **The value side.** The critic estimates the margin of a nudged pair. At evaluation it is used for an un-nudged player against SMART.
- **The prediction.** This explains why halving the rate helps. It also predicts that masking the forced rows out of the policy's
  cross-entropy restores most of the loss *without* changing the rate.

Measurements that separate (a), (b), (c) and (d):
- **(d) noise.** Retrain iteration 2 and iteration 3 from their existing corpora with 3 seeds each and evaluate each at 1 000 games. That is
  6 × 1 000 games, ~2-3 h. If the 3-seed means differ by < 5 points, there was no oscillation to explain.
- **(a) off-policy labels.** Regenerate the iteration-3 corpus with a `forced` flag on every nudged row (one engine change in
  `DecisionLogs`, plus a test). Train the policy with flagged rows dropped and evaluate at 1 000 games. If it recovers ≥ 5 points over the
  unmasked retrain, (a) is confirmed in this form.
- **(b) data per plan.** Train the iteration-2 recipe on half of its corpus (2 500 games). If it still scores ≈ 52-54, data volume is not
  binding at 5 000 for this plan. The pooled iteration 2 + iteration 3 corpus is the other half of that test.
- **(c) granularity** can only be tested after (a) is fixed, by G3. Today it is confounded by (a).

Evidence: code in `AgentDecisionService` lines ~150-178; RESULTS 2333-2346 (drift) and 2397-2430 (anneal).

Test: as above; (d) is the cheapest and comes first.

### Q4. Does the random-continuation oracle order plans the way competent play would?
Verdict: **no, not in general**

Argument: Ordering by `Q^{rand}` agrees with ordering by `Q^{competent}` only where the difference between candidates is realised without
further skill. Plans differ precisely in how much later skill they need. The cheaper and better instrument is the oracle with the
**champion as the continuation** (`Q^{champ}`, P1). Policy-only continuation is the cheapest version (~80 ms per game, RESULTS ~1937), but its
guarantee is relative to the policy-only player, which is much weaker than the champion (policy alone gave 22.5% for the specialist).

Its bias is towards the champion's plan, but that is the right bias for a *policy improvement* question. Bertsekas's rollout theorem says
greedy over `Q^π` is at least as good as π (with exact Q). There is no such guarantee for greedy over `Q^{rand}`.

For the *plan comparison* question, use each specialist as its own continuation (Q1, P3).

Evidence: LEDGER 17, 34; RESULTS ~1937.

Test: as in C4.

### Q5. Sample complexity: is a mixture with a learned gate realistic?
Verdict: **a gate learned from outcomes, no; a gate read from comparable critics, possibly**

Argument (arithmetic):
- **The outcome noise.** The margin sd within a corporation is 41-48 (row 39).
- **The bandit.** A contextual bandit that learns from outcomes which option is better by Δ = 3 points, at 5% significance and 80% power,
  needs n ≈ 2·(1.96 + 0.84)²·45²/3² ≈ 3 500 games **per option per context cell**. With 5 options and even 10 context cells that is
  ~175 000 games. That is not realistic, and the plan's fallback ("a contextual bandit over plans", PLAN last paragraph) is therefore not a
  fallback at this scale.
- **The value-based gate.** It reads `Q(s, i)` from the specialists' critics. That is feasible only if the critics are comparable (Q1), and
  then its failure mode is the critics' cross-plan calibration. Measure that before G3.

Per-corporation specialists with ~414 games each cannot be trained from scratch (row 10: 500 games → 11.6%). Only fine-tuning from the
champion is plausible.

Evidence: LEDGER 10, 32, 39.

Test: the calibration of P3; then G3.

### Q6. Is the starting hand a better axis than the phase commitment?
Verdict: **partly**. It is a better *comparison* axis and no better a *generation* axis.

Argument: See C6. In a hidden-information card game, the exogenous, enumerable variables are what the deal gives: corporation, opening
hand, first draft rows. What *creates* a plan is a commitment in the data: which cards are kept and built. My suggestion:
- generate with a content commitment (tag nudge, flagged — P2);
- grade with the comparable critics (P3);
- use the opening-hand tags as the *context* the gate conditions on, not as the specialist's identity.

Evidence: LEDGER 39, 40.

Test: the C6 screen, then one flagged tag-nudge iteration.

### Q7. Is METHOD too strong?
Verdict: **partly**. Rule 1 is right for negative conclusions and too strong for positive ones. Rule 3 needs a noise model.

Argument:
- **Rule 1 (no conclusion about a plan from a player not trained for it).** A result obtained by a player incompetent at plan X is a **lower
  bound** on X's value against that opponent. So positive evidence is admissible ("even our untrained player gets 44% with phase-1 play"),
  and only negative conclusions ("X is bad") are blocked. The line: *a negative conclusion about X needs a player to which the improvement
  operator has been applied on X's data until its curve is flat within the noise model.* That is rule 3, made quantitative.
- **Rule 2 (the value may not arbitrate exploration)** is stated too broadly. `Q^π` from rollouts *can* arbitrate one-step exploration with a
  guarantee (P1). What may not arbitrate *plan-level* exploration is a learned `V^π` at a one-ply leaf.
- **Rule 3 (derivative gate).** With the noise model of C3, a derivative measured between two 400-game points is ±4-5 points. "Three
  iterations flat" needs a stated test, for example a slope over the last 3 points with a 1 000-game evaluation each and seed replicates.
- **One rule is missing**: *"a cross-comparison of corpora must hold corpus size fixed"*. The "a corpus inherits the strength of the
  generator" table (RESULTS ~1516) compares 1 000-2 000-game corpora against a 5 000-game one, and row 10 alone (1 250 games → 30%) predicts
  most of the gap. Rows 22 and 23 and the specialist-B attempts inherit this confound. I could not find the guided-exploration corpus size,
  so that row is uncertain.

Evidence: METHOD §3; RESULTS 1516-1530; LEDGER 10, 22, 23.

Test: the size-matched control (P4), 30-40 min.

### Q8. What would you do first with 1-2 days?
See the next section. Day 1, about 4-5 h of compute:
1. the row-19 replication;
2. the size-matched control (P4);
3. the oracle split-half reliability;
4. check that `planCode` was set in the specialist evaluations.

Day 2:
1. the `Q^{champ}` phase oracle (P1);
2. if row 19 holds up, the masked-label retrain of iteration 3 (P2).

### Q9. What have we not considered at all?
Verdict: the mechanisms below do not appear in LEDGER. Details are in the Proposals.
- Rollout-based policy iteration with full-game `Q^π` labels (Bertsekas; Tesauro and Galperin 1996 for backgammon). RESULTS ~1937 proposed
  the "paired end-of-game judge", but it was never run.
- Off-policy correction of nudged data (flag and mask, or clipped importance weights in the style of Retrace / V-trace).
- A common reference opponent for every specialist's critic (the options value, Q1).
- A best-response loop against a frozen champion. A stationary environment makes PI's convergence argument apply; self-play does not.
- The simultaneous phase pick as a matrix game: equilibrium or robust mixing instead of a deterministic best response to a self-model.
  The owner's phase-4 note describes a game of chicken.

Of these, the first three need no new search machinery.

---

## What I would do first

**Replicate row 19 as a factorial, with seeds, at a sample size that can separate the terms.**

Stacks, 1 000 games each vs SMART, same flags as the champion, nudges off. If the specialists were trained with `planCode=8`, it is set for
them at evaluation; verify which was used in the original runs.

| policy \ critics | champion's | own (seed 1) | own (seed 2, retrained on the same corpus) |
|---|---|---|---|
| champion `1118-2332_17` | (= 73.17, already at 10k) | — | — |
| p1 | ● | ● | ● |
| p2 | ● | ● | ● |

That is 6 runs × 1 000 games, about 2-3 h at the generation speed in RESULTS 2061, plus ~10 min of critic training.

**Expected outcome (my prediction, so it can be checked):**
- p1 + the champion's critics regresses from 76.5% towards 73-75%, because it was the maximum of four 200-game cells.
- The critic main effect stays large: champion's critics > own critics by ≥ 10 points for both policies.
- The p1/p2 interaction shrinks below 2σ once own critics are seed-replicated.

**If that happens:**
- the PLAN's "ship as a stack with its own critics" rule is reversed;
- the "plan-conditional" claim falls back to row 17 alone;
- the next step is P2 + P3: fix the off-policy data and the opponent, then re-ask.

**If instead p1 + the champion's critics stays ≥ 75%:** go to 10 000 games. It would be the first demonstrated mechanism above 73.17%: a
policy fine-tuned on a perturbed corpus under the champion's critics, which is a form of data augmentation for the policy.

Sidecar, 30-40 min, can run in parallel: **P4**, the size-matched control.

## Where this plan is most likely to be wrong

**It diagnoses an ordinary approximate-policy-iteration problem as a plan-level pathology, and then builds the cure on the components the
data says are weakest.**

The observed facts are:
- the specialists' own critics lose 12-32 points to the champion's (row 19);
- the specialist corpora record forced actions as choices against a nudged mirror opponent (code, `pickPhase`);
- the quality of the loop falls with the nudge rate (row 33).

All three are explained by off-policy data and critic error, without "the value is plan-conditional" doing any work. The planned mixture would
then gate between critics trained against *different* nudged mirror opponents. A gate comparing them compares scales, not plans, so G3 would
fail for a reason unrelated to whether plans matter. The plan would record "the mixture earns nothing", and that would be the next wrongly
closed row.

The neutral target `V0` does not repair this. It replaces our policy's bias with random play's bias, which points away from skill-intensive
plans.

---

## Proposals

### P1. Rollout policy improvement with full-game `Q^{champ}` labels at the plan decision
Mechanism:
- For sampled real phase decisions, play every legal phase to the **end of the game** with the champion (its policy and its one-ply search,
  phasePlayout on) as the continuation for both seats, in K determinised worlds. The mean final margin estimates `Q^{champ}(s, phase)`.
- Use it, first, as the instrument C1 and C5 need. Second, if it shows a gain, as the training label for the phase critic (or as a CE target
  for the phase policy).
- No `V` at any leaf.

Why it can work:
- Bertsekas's rollout theorem: the policy that is greedy over `Q^π` is no worse than π, and strictly better wherever π is not greedy over
  its own Q.
- Tesauro and Galperin (1996) obtained large improvements in backgammon from exactly this, with a weak base policy.
- The project's own +9.2 from phasePlayout is a one-round, V-leaf version of the same idea.
- Weakest step: variance. With σ ≈ 20 per game and K = 8, the per-candidate SE is ≈ 7. Gains are therefore only visible **averaged over
  states**, and the argmax must be evaluated on fresh rollouts (split: select on K, evaluate on another K) to remove the winner's curse.

Not in our ledger:
- The closest rows are 2 and 4 (one or two rounds then `V`), `horizonStop` (one rollout, RESULTS "Negative results") and row 9 (tree).
- All of them read `V` at the leaf or used one sample. This one reads the final score and uses K worlds, as a measurement or a label, not a
  play-time default.
- RESULTS ~1937 proposed exactly this measurement ("does V's plan preference agree with the paired end-of-game judge") and it was not run.
- The §8 exclusion ("more sampling depth") was argued on the basis that a deeper leaf is the same biased V. With no leaf, that argument does
  not apply.

Cost at our scale:
- One champion game is ≈ 10 s per thread (1.4 games/s on 14 threads, RESULTS 2061).
- 200 decisions × ~4 legal phases × K = 8 = 6 400 games ≈ 1.3 h. Doubling K for the fresh evaluation set is ≈ 2.6 h.
- A policy-only continuation is ~100× cheaper (~80 ms). Use it for a smoke test only, because its guarantee is relative to a much weaker
  base.

Cheapest test:
- 200 held-out phase decisions from champion games.
- Statistic: the mean over states of `Q_eval(rollout-argmax) − Q_eval(champion's choice)`, on the independent evaluation rollouts.
- Decides: a 95% CI excluding 0 with mean ≥ 1 point.
- Secondary: the agreement rate of the champion's choice with the rollout argmax.

Would falsify: a gain ≈ 0 with a tight CI. The champion is then one-step-greedy-optimal at the phase pick, C1 holds strongly, and multi-step
(plan-level) change is the only route. That supports the specialist plan with a measurement instead of an argument.

Violates the constraints? No. No imitation, no heuristic. It is the game's own outcome. It is not "more search as justification": the
justification is the guarantee and the absence of a V leaf.

### P2. Off-policy-correct nudged corpora (flag, mask, and optionally weight)
Mechanism: Every decision overridden by a generator nudge carries a `forced` bit in the decision log.
- The policy's cross-entropy skips forced rows, so the policy learns only its own (search) choices in the states the nudge led to.
- The critic either keeps them (it is then `V` of the behaviour policy, honestly labelled) or reweights trajectories with clipped
  per-decision importance ratios.
- The ratio is computable, because the nudge probability is known: behaviour `b(a) = r·1[a = forced] + (1 − r)·π(a)`.
- Only ≤ ~11 phase picks per seat per game are nudged, so the product of ratios is short.

Why it can work:
- Standard off-policy learning: Retrace (Munos et al. 2016) and V-trace (Espeholt et al. 2018) correct exactly this behaviour/target
  mismatch.
- Masking is the zero-variance special case for the policy.
- My reasoning, weakest step: the critic's off-policy error may be dominated by the nudged *opponent*, not by our own nudged picks. That is
  why P3 removes the opponent's nudge.

Not in our ledger:
- Rows 20, 22, 25 and 33 all trained on forced choices as if chosen. The code confirms it (`AgentDecisionService.pickPhase`).
- Row 25's "anchor" (KL to the parent) is a regulariser, not a correction.

Cost at our scale: one engine change (a flag in `DecisionLogs` plus a recording test), a pack-schema field, a trainer option, one 5 000-game
regeneration (~1 h), training (minutes) and 1 000 games (~15 min).

Cheapest test: regenerate iteration 3 at rate 1.0 with flags, train the policy with forced rows masked, and evaluate at 1 000 games against
the unmasked retrain of the same corpus. Decides: ≥ +5 points.

Would falsify: no recovery means the drift is in the states the nudge visits, not in the labels. Then annealing is the right tool after all
and the letter's reading of row 33 stands.

Violates the constraints? No.

### P3. A common reference opponent for every specialist critic: one target, K conditionings
Mechanism:
- Generate each specialist's corpus with **seat A = the specialist** (nudged with flags, or annealed) and **seat B = the frozen champion,
  un-nudged**.
- Train the specialist's critic on seat-A rows only. It then estimates `Q(s, i) = E[margin | π_i vs π_champ]`.
- All specialists are now on one scale against one opponent, and the gate is `argmax_i Q(s, i)` at the plan points.
- As a by-product, each specialist is trained as a best response to the champion. That is a stationary environment, so the classic PI
  argument applies; mirror self-play is non-stationary.

Why it can work:
- The options framework (Sutton, Precup and Singh 1999) defines the gate's value as each option's own return.
- Comparability needs a shared environment, and the opponent is part of the environment.
- Best-response training against a fixed population is the inner step of PSRO (Lanctot et al. 2017). The league in G4 is its outer loop.
- Weakest step: a best response to the champion may exploit champion-specific weaknesses that do not transfer to SMART or NETWORK.
  Measure against both (point 2 of the test).

Not in our ledger:
- Row 24 and RESULTS ~1530 (A vs the old champion) trained *one policy on both seats' rows*, which averages the two styles.
- Here the opponent's rows are not used for the specialist at all.
- The current specialist corpora are mirror self-play (RESULTS 2192).

Cost at our scale: the same as today's specialist iterations (5 000 games ≈ 1 h per iteration). Half the rows per game are kept (only seat
A), which is compensated by the asymmetric seat, so possibly 7-8 000 games per iteration.

Cheapest test:
1. Calibration. On 300 held-out states, regress the realised margin of π_i-vs-champion games from *s* on critic *i*'s prediction for two
   specialists (the phase-1 specialist and the champion itself as option 0). A slope within [0.8, 1.2] and an intercept within ±3 points for
   both means comparable.
2. Then G3: the gate vs the best single option at 1 000 games, against SMART **and** NETWORK.

Would falsify: if the critics are not calibrated even against a common opponent, value-based gating is infeasible. Combined with Q5's
arithmetic for outcome bandits, the mixture is then not viable at this scale, and the plan's "drop" criterion 3 is met with a reason.

Violates the constraints? No. The opponent is our own champion, not a benchmark.

### P4. The size-matched control: reopen "a corpus inherits its generator's strength"
Mechanism: Subsample the champion's own 10 000-game corpus to 1 000 and 2 000 games, train the full stack with the unchanged recipe, and
evaluate at 1 000 games each. Compare against the 1 000-2 000-game weak-generator results: H1b 21.0% at 1 000; B 25-33% and exploration
21.1% at 2 000.

Why it can work: Row 10 shows a steep volume curve in exactly that range (1 250 → 30%, 5 000 → 73%). If a size-matched *champion* corpus
also lands at 20-35%, the "inherits" rule is a volume effect. Rows 22 and 23 and specialist B were then never tests of their mechanisms.

Not in our ledger: Row 10 measured volume, and RESULTS ~1516 compared generators. Nobody crossed them.

Cost at our scale: no generation. 2 packs, 2 trainings (~10-20 min), 2 × 1 000 games (~30 min).

Cheapest test: as described. Decides: if the 2 000-game champion stack is ≤ 40%, the rule is mostly volume, and RESULTS ~1516, LEDGER 22 and
LEDGER 23 get a CORRECTED status for their conclusions (not their numbers).

Would falsify: if the 2 000-game champion stack scores ≥ 60%, the rule stands as written.

Violates the constraints? No. The "do not scale a failed mechanism" rule is not touched: this is a smaller corpus, as a control.

### P5. The phase pick as a simultaneous-move matrix game
Mechanism:
- At PICK_PHASE, estimate the matrix `M[i][j]` = value of my phase *i* when the opponent picks *j*, for the legal phases (≤ 4 × 4). Use the
  existing round playout, with the opponent's pick fixed instead of sampled from our policy.
- Play the **maximin mixture** (a 4 × 4 LP, microseconds) or the regret-matching mixture, instead of a deterministic best response to our own
  policy's opponent model.

Why it can work:
- The phase pick is simultaneous (GAME_RULES, RESULTS ~1359).
- The owner's phase-4 note describes a chicken-like structure: whoever picks income pays the opportunity cost and everyone collects.
- In such games a pure best response to a mis-specified opponent model can be badly wrong, while the maximin mixture guarantees its value
  against *any* opponent.
- Weakest step: against a fixed, non-adaptive benchmark (SMART), the best response to the *true* opponent beats the mixture. The gain, if
  any, should appear against NETWORK and humans (a different style), not necessarily against SMART.

Not in our ledger: Row 18 (plan probe) and the phase critic rows model the opponent's pick by our own policy (RESULTS ~1359: "an opponent
model is the missing piece"). Nothing treats the pick as a matrix game.

Cost at our scale: ×(number of opponent phases) playouts at PICK_PHASE only, roughly 3-4× the phase-pick cost. That is one decision per round,
so the game cost rises by an amount to be measured. Implementation is about one day (the matrix collection plus the LP).

Cheapest test: 2 000 games each vs SMART and NETWORK, against the champion at the same seeds. Decides: ≥ +3 points against NETWORK without
losing > 1.5 against SMART.

Would falsify: no gain against NETWORK means the NETWORK gap is not in phase-pick robustness.

Violates the constraints? No. The matrix uses only our own playouts, and no model of SMART or NETWORK is built.

### P6. Measure every gate against NETWORK as well as SMART
Mechanism: a protocol change, not a model. The project's goal includes NETWORK (39.8%), but G1-G3 and every specialist number are measured
against SMART only. A specialist that adds a plan NETWORK cannot handle would be invisible on a SMART-only curve.

Why it can work:
- Non-transitivity: the champion is 73% vs SMART and 40% vs NETWORK, so the two benchmarks rank players differently (RESULTS 297-320: a
  player at 11% vs SMART was at 53% vs RANDOM).
- A plan-level change is the kind most likely to move the two benchmarks differently.

Not in our ledger: rows 33-40 are SMART-only.

Cost at our scale: +400-1 000 games per measurement point (~10-15 min each).

Cheapest test: re-measure p2 and the annealed specialist against NETWORK, 1 000 games each. Decides: if either is ≥ 40% vs NETWORK while 20
points below the champion vs SMART, the specialist is a different player in the sense G4 needs, and that is direct evidence for the league.

Would falsify: if both specialists sit far below 39.8% vs NETWORK as well, they are just weaker, not different.

Violates the constraints? No. The benchmark is used as a benchmark.

---

## Ranking

1. **The row-19 factorial** (What I would do first) **together with P4**. 3-4 h in total, no engine change. Together they decide whether the
   plan's two load-bearing beliefs are measured or artefacts: "specialists need their own critics" and "weak generators make weak corpora".
   Everything else depends on the answer.
2. **P1** (`Q^{champ}` phase oracle, ~3 h). It turns C1 into a measurement and is a valid replacement for the C5 oracle. Its negative outcome
   is also informative: it would vindicate the specialist direction.
3. **P2** (off-policy flag and mask). About a day including the engine change. It is the most likely explanation of the oscillation, and it
   is needed before *any* nudged corpus (phase or tag) can be trusted.
4. **P3** (common reference opponent). The structural answer to C4 and Q1. Run it after P2, because both change how specialist corpora are
   made.
5. **P6** (NETWORK alongside SMART). Cheap, run continuously.
6. **P5** (matrix-game phase pick). Real but speculative, about a day of implementation; after the above.

One thing I would **not** run as planned: the oracle at K = 2 with random continuation, and `V0` as the gate's target. Run the 5-minute
split-half reliability first. If it is under 0.2, the oracle as costed cannot answer the question it was built for.

---

## Appendix — facts checked in the code, so the reply can verify them

* `AgentDecisionService.pickPhase` (lines ~151-178): when `PlanCodes.rollNudge() && PlanCodes.preferBuildPhases(player)` fires,
  `index = buildPhaseWithWork(...)`, then `record(game, player, decision, index)`. That is the same call that records the
  policy and search's choices. `record(...)` (line 146) takes no flag. So nudged picks are indistinguishable from chosen ones in the corpus.
* `ObservationBuilder` (line ~618) writes the 4 bits of `PlanCodes.code(player)` into the observation. `PlanCodes.code` returns the seat's
  code, or 0 when none is set. `GameService` (lines ~41-56) sets the code only when `planRandom` or `planCode` is passed. The nudge itself
  also requires `planNudges=true`, but the observation bit does not.
* The noise numbers used above: the evaluation binomial sd is √(p(1−p)/n). The critic-seed sd ≈ 2.5 points comes from the one same-recipe
  pair (42.98 / 39.38 at 10 000 games each). That is a single pair, so treat it as an order of magnitude.
