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

1. **Corporation** - primary. Dealt and chosen before any strategy; it sets the economy's currency. In our own 5 000-game corpus: **25 played,
   median 414 games each** (row 32). No forcing needed: the natural experiment is in the data.
2. **Commitment** (which phase you prefer when legal) - a cheap ACTION axis, used to probe the *mechanism* (does a specialist loop climb at all).
   Its numbers must never be quoted as a strategy's strength.
3. **Outcome cells** (score composition) - an **inventory** of what our player does. Never a ranking (rows 20, 31).

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
| G2 | profile at play time (builds, phase mix) | no degeneration ("learned not to build") |
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

1. **Screen** (minutes): how much does the final margin vary by corporation in the existing corpus? A screen only - the choice of corporation is
   endogenous.
2. **Corporation specialists** (cheapest path): one base model, then a fine-tuned copy per corporation on its ~414 games, then measure each curve.
   Alternative at equal total games: one model conditioned on the corporation - the decisive A/B, because the measured 7-point "tax" (row 25) may
   belong to the generator's nudges rather than to conditioning.
3. **Gate** over the corporation offer and the phase pick; measure G3.
4. **Annealing** schedule and a final measurement with the nudge off.

## What is deliberately not in this plan

More sampling depth, more determinisations, the flat/line tree as a play-time default, other value targets, wider nets at this data volume,
analytic valuations, imitation, and any ranking produced by the player whose competence is the question.
