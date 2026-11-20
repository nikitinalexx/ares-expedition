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

1. **Iteration 2 of the practice loop** (~2.5 h): generate 5 000 games with the clean fine-tune (75.16%), fine-tune the champion's policy on them,
   measure at 5 000 vs SMART and 1 000 vs NETWORK. This decides whether the +2 is the first step of a curve or a one-off. **This is the only
   experiment that matters right now.**
1b. **The activatable-blue feature** (~1 h with tests, then a re-measurement): per side, the count of played blue cards that carry an action - the
   feature the `ObservationBuilder` docstring claims to have and does not (`RESULTS.md`, 20/11). The current phase-3 blind spot was measured on
   *all* blues (`blue_cards_count`), which is the wrong quantity; the fix is rule arithmetic on the card data, and it is the kind of thing practice
   cannot discover (an aggregate that does not exist).
2. **The whole corpus** (~30 min): the same run with `--max-chunks-per-type 20` (the trainer reads half of a 20-chunk corpus by default). Cheap, and
   it may make every later iteration stronger.
3. **P2, the forced-row flag and mask** (engine change + test + 1 h): required before any nudge is used again; it also tests the reviewer's explanation
   of the oscillation (off-policy labels), and it is the last thing standing between us and the red/event arm.
4. **The R / E arms** (2 x 2.5 h): red/event preference and economy preference, same size, same recipe, same critics, both benchmarks - the owner's
   objection (red cards may be a consequence of a stronger economy) is the reason E is not optional.
5. **The 10 000-game confirmation** (4 h) of whatever bundle is leading, by the project's rule for effects under 1.5 points.

Shelved until the loop stops paying: specialists, the gate, the two targets, the menu oracle as costed (the reviewer showed K=2 with a random
continuation cannot rank), the commitment axis, the corporation axis.

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
