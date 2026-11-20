# PLAN — the current plan only (20/11)

History and the reasoning that produced it: `docs/LEDGER.md` (what we ran and concluded) and `docs/METHOD.md` (the rules). Older plan documents are
in `docs/archive/` and are not current.

## Where we stand

* **Champion**: one plan at **73.17%** [72.3, 74.0] vs SMART (10 000 games), 98.6% RANDOM, **39.8%** vs NETWORK. Every later attempt to improve it
  in place - more data, deeper search, other targets, wider nets, the tree - landed at 71-73% (LEDGER rows 4-14).
* **Why**: the value is **plan-conditional**. It estimates the futures of the plan the policy already plays, so it rejects any other plan, and the
  corpus contains only its own states. Measured directly: the same policy scores +10 points under critics trained for its plan and -10 under the
  champion's (LEDGER row 19). Prices confirm it: +1 income costs 6.5 / 9.0 / 12.1 MC in three corpora, against ~15 by the rules (row 17).
* **What changed today**: a specialist trained on a corpus of one committed plan **climbs**: 44.75% -> **54.4%** [49.5, 59.3] vs SMART over two
  iterations, then falls back to 44.6% at the third (LEDGER 33). It is far below the champion, and it is the first mechanism in weeks that does
  not sit at 73%.
* **No mechanism above 73.17% has been demonstrated yet.** The champion and the pipeline (generate -> pack -> train -> measure) work; the
  *improvement loop* does not, yet. The plan below is a hypothesis with mechanism-level support and cheap gates, not a demonstrated path - and it
  must be read that way.

## What is learned

A set of **specialists** ("options"). Each is a policy **plus its own value**, trained on a corpus where that plan was actually played, and shipped
together - never as a policy dropped into another plan's search.

## The axis (exogenous, enumerated, not chosen by belief)

1. **Commitment** - the only axis with a measured climb, and its limit is now known: the champion already mixes the five phases (10/30/36/6/17), so
   forcing a phase biases the existing plan instead of creating a new one (LEDGER 40). The phase arm is therefore a *mechanism test* only, and it has
   served that purpose (the oscillation at rate 1.0, the hold at 0.5).
2. **The plan's CONTENT - the next axis.** Which economy is built (microbes, animals, energy, steel/titanium, plants, card draw) is determined by the
   cards, and the player does not cover it (Herbivores 0.21, Fish 0.05 per game). Two implementable routes, to be chosen by cost:
   * **filter by the deal** (no forcing): group game-seats by the opening hand's tags and train a specialist per group - needs a tag dump and a
     clustering rule, no engine change;
   * **a tag-preference nudge** (forcing, like the existing generator nudges): prefer cards of tag X at the draft/build menus - needs one engine change
     and a test, and it is the same kind of instrument as the phase nudge.
3. **Corporation - screened out at 2.4%** (LEDGER 39): endogenous, and it barely explains the outcome at the current skill.
4. **Outcome cells** (score composition) - an **inventory** of what our player does. Never a ranking (rows 20, 31).

## The player

A **mixture with a gate**: specialists are the parts; the gate chooses the option at plan points (the corporation offer, the phase pick). The gate's
value may be trained on a mix of corpora **including random play**, because random play visits states our policy never reaches - and the gate role is
the one place where a shared slope does not cancel (it was never tested there; only in the ranker role, where it correctly showed nothing).

**The seed anneals**: a generator nudge at rate 1.0 -> 0.5 -> 0.25 -> 0, and the final measurement is made with the nudge OFF. If a build-phase
specialist ends up playing the draft, that is its finding.

## Gates and stop rules

| gate | measurement | stop rule |
|---|---|---|
| G1 | a specialist's curve over iterations | must keep **climbing**; three iterations flat -> that arm is recorded unreachable and is not rescaled |
| G2 | profile at play time (builds, build acceptance, standard projects in the phase the player calls) | no degeneration ("learned not to build"). **The phase mix is not a criterion**: phase 4 is shared, so declining it can be the strong move when ahead on income, and picking phase 1 a lot is a plan, not a defect (`docs/GAME_RULES.md`) |
| G3 | the gate vs the best single specialist, then vs the champion (1 000 games) | must win, else the mixture earns nothing |
| G4 | the league of our own specialists (cross-play, own rows only) | answer to the NETWORK style gap; the **last** gate, not the first promise |

## Invariants

* No analytic valuation of a position (microbe chains cannot be priced; the same resource has different prices for different plans).
* No imitation of SMART, NETWORK or human games; they are benchmarks only.
* No ranking of an alternative by a player incompetent at it.
* No volume for volume's sake (10 000 games of one plan added nothing).
* Targets and weights live on the **round grid** (a plan's number of decisions is not a clock).
* Everything written is tested; every claim carries a measured number or a gate.

## Next steps, with cost

1. **Annealing** (running): generation at nudge rate 0.5 from the best specialist (p2), 5 000 games (~1 h), then training and a 400-game
   measurement. It tests whether the oscillation (44.75 -> 54.4 -> 44.6) was the compulsion or the mechanism.
2. **The menu oracle** (next, ~10 min of labels + ~5 min per target): extend the existing end-of-game audit to all decision types with a
   plan-neutral continuation (random), then score each candidate target by its agreement with the oracle on held-out menus. This is what turns "the
   right target" into a measurement (`## The target question` above).
3. **If annealing climbs**: continue to rate 0.25 and 0, and measure with the nudge off - that is the ship-ready shape.
4. **If annealing also oscillates**: the single-specialist mechanism is recorded as not climbing at our data volume, and the next candidate is the
   starting HAND axis (clustered by the cards' own tags), not more corporations (LEDGER 39) and not more volume.
5. **Gate** over the plan points (the phase pick, and later the corporation offer), then G3 against the best single specialist and the champion.
6. **Row weighting**: the per-round normalisation of the target/loss stays (LEDGER 38), and it must be applied when the annealed corpus is
   trained, because this is the last measurement that touches every corpus we make.

**External review:** the plan, the ledger and the method are under adversarial review by an outside model - see `docs/reviews/README.md` and
`docs/reviews/LETTER_1.md`; its critique and our reply are kept there. A reviewer's objection is handled like any other claim: with a measurement.

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
