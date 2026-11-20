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
  iterations (row 33). It is far below the champion, and it is the first mechanism in weeks that does not sit at 73%.

## What is learned

A set of **specialists** ("options"). Each is a policy **plus its own value**, trained on a corpus where that plan was actually played, and shipped
together - never as a policy dropped into another plan's search.

## The axis (exogenous, enumerated, not chosen by belief)

1. **Commitment** - the only axis with a measured climb. Which phase you prefer when legal; forced in generation, and the specialist's curve is
   the gate. Its numbers must never be quoted as a strategy's strength, and the seed must anneal.
2. **Corporation - a screen said no.** Dealt and chosen before any strategy, 25 played with a median of 414 games each, so it looked like the
   natural axis - but it explains only **2.4%** of the margin's variance at the current skill (LEDGER 39). Endogenous (we chose it), so it is a
   screen rather than a refutation; still, 25 specialists (~19 h per iteration) are not justified.
3. **Starting hand** - the next candidate. It is what actually determines which strategies are possible; it cannot be enumerated, so it has to be
   clustered by the cards' own tags, not by my choice of strategy.
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
2. **If annealing climbs**: continue to rate 0.25 and 0, and measure with the nudge off - that is the ship-ready shape.
3. **If annealing also oscillates**: the single-specialist mechanism is recorded as not climbing at our data volume, and the next candidate is the
   starting HAND axis (clustered by the cards' own tags), not more corporations (LEDGER 39) and not more volume.
4. **Gate** over the plan points (the phase pick, and later the corporation offer), then G3 against the best single specialist and the champion.
5. **Row weighting**: the per-round normalisation of the target/loss stays (LEDGER 38), and it must be applied when the annealed corpus is
   trained, because this is the last measurement that touches every corpus we make.

## What is deliberately not in this plan

More sampling depth, more determinisations, the flat/line tree as a play-time default, other value targets, wider nets at this data volume,
analytic valuations, imitation, and any ranking produced by the player whose competence is the question.
