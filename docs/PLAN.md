# PLAN — the current plan only (20/11)

History and the reasoning that produced it: `docs/LEDGER.md` (what we ran and concluded) and `docs/METHOD.md` (the rules). Older plan documents are
in `docs/archive/` and are not current.

## Where we stand

* **Champion**: 73.17% [72.3, 74.0] vs SMART (10 000), **39.8%** vs NETWORK.
* **The practice lever is real but bounded**: one fine-tune on 5 000 fresh self-play games gives **75.16%** [73.94, 76.23], and the second iteration
  **74.53%** [73.30, 75.75] - flat, and the health panel shows a *behaviourally identical* player (LEDGER 43). So +2 once, then a converged fixed
  point. It is the first measured gain over the champion, and it is not a loop that keeps paying.
* **The NETWORK gap is tempo, not conversion**: NETWORK plays twice the red cards (8.6 vs 4.2 per game) and wins TR, printed points, milestones and
  awards; the agent wins the resource-card channel by 10 points. Against NETWORK the agent is 43.25% [40.2, 46.4] after two practice steps.
* **Why the old readings were wrong**: the 30-point gap between a specialist's own critics and the champion's is critic quality and off-policy
  labels, not plan-conditionality (VERIFICATION_1). The value is plan-conditional by definition; it was never the binding constraint.

## The plan

**One loop, and it is the practice loop.**

1. Generate 5 000 games with the current best player (~1 h).
2. Fine-tune the champion's policy on them (~1 min), **with `--max-chunks-per-type` set to use the whole corpus**, and later with forced rows masked
   (P2) once the flag exists.
3. Measure at 5 000 against SMART and 1 000 against NETWORK (~1.5 h).
4. Keep the new bundle if it is better; repeat. Stop when two consecutive iterations are flat within the noise model (±4 points at 5 000 games).

Three things to fix inside the loop, in order, each cheap:

* **the half-corpus default** (`--max-chunks-per-type 10` on a 20-chunk corpus): one run tells whether the full corpus trains better;
* **the flag and mask for forced rows** (P2): required before any nudge is used again, including the red/event one;
* **the red/event practice arm** (R), and the economy arm (E) as its control: same size, same recipe, same critics, both benchmarks - because the
  owner's objection is right that more red cards may simply be a consequence of a stronger economy.

The specialist/gate/two-target machinery is **shelved** until the practice loop stops paying. None of it is needed for the +2, and the control
says the plan was the wrong unit.

## The health panel: strength must not be bought with degeneration, but a strong degenerate is KEPT

Owner's rule (20/11): keep the hand on the pulse, and do not let strength come from collapse - but if a degenerate player is stronger, keep it anyway,
because it becomes a **gatekeeper** for testing generalists later.

Instrument: `ml/health.py` (13 unit tests). Five families of numbers, computed from the corpus each iteration generates: profile (builds, acceptance,
standard projects), phase diversity (per-game entropy), card diversity (colour entropy, distinct cards, HHI), **adaptation to the corporation**
(how far each corporation's phase mix moves from the global mix - 0 means the deal is ignored), and the score composition spread.

**It is a classifier, not a veto:**

| outcome | classification | what happens |
|---|---|---|
| wins and no metric collapsed | **generalist improvement** | becomes the new best and the reference for the next panel |
| wins but a metric collapsed beyond tolerance | **specialist / gatekeeper** | kept and labelled; it becomes a fixed strong opponent for testing generalists; it does NOT become the reference, and it does not close the search for a general player |
| does not win, panel fine | an ordinary failed iteration | archived |
| does not win, panel collapsed | a degenerate dead end | archived with the numbers |

The thresholds are relative to the reference (builds ±20%, acceptance ±10%, standard projects ±30%, phase entropy ±20%, colour entropy ±15%, distinct
cards ±20%, adaptation ±30%). They exist to detect *collapse*, not to define a good player: there is no healthy profile in this project, and a
measured count never becomes a norm.

**First panel (p1's corpus against the champion's):** acceptance 0.711 vs 0.814 (**collapse, -13%**), phase entropy 0.468 vs 0.531 (-12%, inside
tolerance), colour entropy 0.864 vs 0.893, distinct cards 2.84 vs 3.25 (-13%, inside), standard projects 1.03 vs 0.91, and **adaptation to the
corporation 0.041 in both** - the player barely changes its plan with the deal, which is the owner's "dynamic specialist" question and the next
thing the panel makes measurable.

## Invariants
* No analytic valuation of a position (microbe chains cannot be priced; the same resource has different prices for different plans).
* No imitation of SMART, NETWORK or human games; they are benchmarks only.
* No ranking of an alternative by a player incompetent at it.
* No volume for volume's sake (10 000 games of one plan added nothing).
* Targets and weights live on the **round grid** (a plan's number of decisions is not a clock).
* Everything written is tested; every claim carries a measured number or a gate.

## Next steps, with cost

**Closed by measurement, do not re-open without a new number:**

* *more search* and *a better one-step critic*: the corrected audit (benchmark as the rollout opponent, split sample) shows **+0.56 +- 0.75** at the phase
  against SMART, **-0.24 +- 0.76** against NETWORK, and **+0.01 +- 0.62** at the build (LEDGER 50).
* *forcing card content*: red, green and microbe arms all lose on both benchmarks (LEDGER 44-46).
* *practice beyond one step*: iteration 2 on the whole corpus is flat (74.53 vs 75.16), and the 10 000-game volume conclusion is confounded by the
  trainer's `--max-chunks-per-type` default; iteration 2 used the whole corpus and still converged (LEDGER 43, 47).
* *the late game as a defect*: the second-half drift is symmetric (wins +16.8, losses -21.1). What is asymmetric is the **early lead**, which predicts
  LOSING to NETWORK: -14.9 points at 1 000 games (LEDGER 51).
* *a better NETWORK_V2 in `advanced_ai`*: the committed model is the best of the five and scores 37.75% against NETWORK (behind our 41.7%); the documented
  25-round player is the older single-tower generation (LEDGER 56).

**Open, in order of cost:**

1. **The corporation audit** (~30 min): `judgeTypes=PICK_CORPORATION`, opponent NETWORK, split sample, on the 1 000-game corpus. The corporation is a real
   decision in 913/1 000 games and the outcome spans 63% to 27% by chosen corporation (p = 0.0006, LEDGER 52). If the audit shows no headroom, the
   choice is fine under our own line and the spread is the deal/plan, not a decision error.
2. **The early-investment arm** (~2.5 h): nudge toward production/cards in the FIRST rounds only, generate 5 000 games, fine-tune, measure **without** the
   nudge on both benchmarks. This is the direct test of the measured mechanism (LEDGER 51); the three colour arms are the reason the prior is low, and
   the timing is the one dimension they did not vary.
3. **A rush arm** (~2.5 h): the rules allow a rush - the game ends when the planet is maxed and only the raiser gets TR - and the measured lengths say why it
   matters: the agent's own games end at **26.7** rounds, NETWORK's at **30.3**, and the matchup at **29.2** (LEDGER 53). A rush needs ~1.2 parameter
   steps/round from one player (all 31 steps) against the agent's current 0.58; **no game in any corpus ends before round 23**, so the strategy is absent
   and must be generated before it can be judged. One-step search will reject it by construction (its continuation is our own non-rushing policy).
4. **The specialist / two-line corpus with a plan-neutral gate V0** (the expensive path, owner-postponed): the only version where the line is not forced.

**Measurement rules that came out of this week:** the result comes from the engine's `ScoreBreakdown`, never from the trajectory; a per-seat quantity is
oriented by the recorded seat (the agent is seat 0 in 46/100 games); the same-policy continuation cannot judge a plan, so any plan claim must say which
continuation judged it.


**External review:** the plan, the ledger and the method are under adversarial review by an outside model - see `docs/reviews/README.md`,
`LETTER_1.md`, `CRITIQUE_1.md` and `VERIFICATION_1.md`. A reviewer's objection is handled like any other claim: with a measurement. The first
round has already changed the plan (the critic main effect, the off-policy nudges, the corpus-size confound, V0's failure), and its strongest
objection - "you diagnosed API as a plan pathology" - is now confirmed by the clean-fine-tune control.

## What is deliberately not in this plan

More sampling depth, more determinisations, the flat/line tree as a play-time default, other value targets, wider nets at this data volume,
analytic valuations, imitation, and any ranking produced by the player whose competence is the question.

## The target question: two targets, not one (20/11)

Everything measured today says a single value cannot serve both jobs it is given:

* as a **tactical critic** it must know the plan it is finishing (measured: the same policy is +10 points under its own critics and -10 under
  another plan's);
* as a **judge between plans** it must be neutral about which plan is being executed, or it will always prefer its own.

Our single `V` (final margin, trained on our own games) is plan-conditional, so it does the first job only. The fix is not a third target for the
same network; it is **two targets for two roles**:

1. **The specialist's value: Q(s, plan).** Target = the final margin of games where that plan was committed. This is what the specialist loop
   already trains, and it is the right target *for executing the plan*.
2. **The gate's value: V0(s).** Target = the mean final margin when, from `s`, **both seats play a fixed, plan-neutral policy** (uniform random) to
   the end. Unbiased by construction: the reference policy is not the learning policy, so the label carries no plan. It ignores competence on
   purpose - the gate compares what the position holds, and the specialists supply the skill.

**Why this is the plan's answer to "find the right target", and not another guess.** It is the only formulation in which the two measured failures
disappear at once: the label no longer depends on our plan (so it is valid across plans), and it is unbiased (the continuation is fixed). It is also
exactly what the owner's NETWORK_V2 did: a value trained on random play, then a phase played out to the end and evaluated with it - which produced
dynamic switching between styles.

**The instrument that decides it (`menu oracle`).** For a sample of real states, for every candidate of the menu, play the game to the end with the
random continuation in K determinised worlds and record the mean margin. This is plan-neutral ground truth for ranking candidates. Then the question
"which target is right" becomes measurable: train `V` on each candidate target from the SAME corpus, and score each by its agreement with the
oracle on held-out menus (top-1 and rank correlation). The current target (final margin) is the baseline; the neutral target and the round-grid
lambda-return are the challengers.

**Cost.** The oracle labels one state per round per seat: 2 000 games x 11 rounds x K = 22 000K full games; with K = 2 and 14 threads that is
~10 minutes. Training one critic per target is ~5 minutes; the agreement script is seconds.

**Falsifiable prediction:** a gate built on V0 beats a gate built on any specialist's value or on the champion's value at equal data. If it does
not, the "two targets" decomposition is wrong and the gate has to be learned directly on outcomes (a contextual bandit over plans).

## Throughput: only changes that cannot change a decision (owner, 20/11)

Measured baseline (practice2 run, 14 threads): **1.07 games/s**, 26 485 observation builds per game, and the perf counters split the CPU as
**observation 36 007 s against critics 7 476 s** - 83% versus 17%. The network is not the bottleneck; the observation is.

Done, and both are semantics-preserving:

* **streams removed from the hot path** (`ObservationBuilder`): the opponent lookup, the hand identity bits (83 x hand with a `getCard` per pair) and
  `onTable` now resolve the card instances once and then loop;
* **flat `Card[] byId` table in `CardService`**: `getCard` used to walk every expansion map plus four more maps per call (up to eight hash lookups,
  millions of calls per game); it is now one array index, with the old method kept as `slowGetCard` and an equivalence test over ids 0..11 000.

**Rejected by the owner, and not to be re-proposed:** dropping the public (opponent) observation, and pruning candidate menus by the policy prior.
Both change what the search sees, i.e. they buy speed with quality.

Still allowed if needed, because neither can change a decision: JVM/GC flags (young generation, G1 tuning), thread count, and hoisting repeated work.
Anything that alters the candidate set, the observation or the value is out of scope for throughput.

## Night of 20/21: the R/E arm protocol (autonomous run, owner asleep)

Run with `ml/run_arm.ps1 -Name red -Colour red -Rate R` and the same for `green`; everything is identical except the colour, including the base
bundle (the clean fine-tune at 75.16%), the critics, the corpus size (5 000), the training recipe and the two benchmarks.

* **Manipulation strength is a measured parameter, not a setting of taste.** At rate 1.0 the colour nudge is a compulsion: the smoke corpus came out
  97.6% red with 0.8 builds a game, i.e. the player stops building anything else and starves. The arms therefore run at a rated preference (0.3 or
  0.5, chosen by a 100-game smoke that keeps the red share in a plausible band ~25-35% and the build count off the floor).
* **Success is not "R wins"**: the owner's objection is that more red cards may simply follow from a stronger economy. So E (green) is not optional -
  the interesting outcomes are R > E (red is causal), E > R (the economy is the cause, as the owner suspected), or R ~ E ~ champion (the NETWORK gap
  is not in card colour).
* **Every arm goes through the health panel** (generalist vs specialist/gatekeeper) and is measured against both benchmarks.

### Microbes are a RESOURCE plan, not a tag (owner's correction, 20/11)

The first attempt used the MICROBE tag, and the owner rejected the definition: the tag says almost nothing, because the cards that matter are defined by
the microbe RESOURCE they hold, produce, consume or hand over. The set is now explicit (`MicrobeCards`), built from the owner's list plus the code:

* holders and producers: AnaerobicMicroorganisms, ConservedBiome, Decomposers, DecomposingFungus, ExtremeColdFungus, FilterFeeders,
  GhgProductionBacteria, NitriteReductingBacteria, RegolithEaters, SelfReplicatingBacteria, SymbioticFungus, Tardigrades, ViralEnhancers;
* consumers: Worms, BacterialAggregates;
* red one-shots with `CardAction.ADD_MICROBE`: ControlledBloom, Cyanobacteria, ImportedNitrogen, ImportedNutrients;
* the buffed variants of the same cards.

Instrument: `-Dares.agent.preferSet=microbe` (with `-Dares.agent.tagNudgeRate`), same rated-preference protocol as the colour arms.
Metric: `ml/tag_metric.py --microbes`, which sums the nine microbe counters of `TableResourcesFeature` at each seat's last round - the microbe resource
actually held on cards, not the number of tags played.

Why this matters beyond microbes: it is the first axis defined by a card's *mechanism* rather than by its label, which is what the plan's content axis
was supposed to be.

## Current step (21/11): the decision audit, and the 10k cycles postponed by the owner

The owner's decision: 10 000-game specialist cycles are postponed - they are long, and their outcome is vague. So the next step is the cheapest direct
measurement we can still make: **the decision audit**, which answers "is our value ranking the plan decision the way the game's own outcome does" on real
decisions of the champion.

* `judgeTypes` (default PICK_PHASE) lets the audit cover any decision type, and `ml/judge_report.py` (9 tests) turns the log into two numbers per type: the
  agreement between the value's ranking and the end-of-game judge's, and the points the judge's preferred candidate gains over the value's choice.
* First run: the champion, PICK_PHASE, 8 worlds, 400 decisions, 200 games vs SMART.
* What each outcome means: high agreement and a small advantage -> the champion is already one-step-optimal at the phase pick, and the search has no
  headroom there (the review's C1 holds, and the plan must look elsewhere); low agreement and a positive advantage -> there are decisions where the value
  is wrong in a way the game's own outcome sees, and that is the first measured target for a better value or a better label.
