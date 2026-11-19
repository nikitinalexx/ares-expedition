# Plan: the next level (owner, 19/11: "make a stronger player, test variants, do not grind the same shape")

State at the start: **A = 73.17% [72.3, 74.0] vs SMART** (10 000 games), 98.6% vs RANDOM, **39.8% vs NETWORK**. Profile: 32.6
builds/game, phases 10/30/35/8/18, 16.7% standard projects in phase 3. Mulligan works per card (mean 4.0 per seat, 13% of games
discard nothing) but is score-neutral at 3000 games.

## What is already measured about the ceiling (do not re-test)

| finding | number |
|---|---|
| the phase pick is the dominant lever | `phasePlayout` +5.9; the phase critic swapped either way +-5 |
| the critic decides act-vs-stop badly | `stopMargins` -3.6; `stopMargins:300` -1.1 |
| a forced plan is weak, not just different | phase-1 bias 44.25% (A is 73.17) |
| a forced-plan corpus inherits the weakness | specialist B 21.1% / 32.9% / 25.3% |
| the model ignores the opponent | 446 `op_`+hand columns dropped: loss 1.11 -> 1.16; phase pick independent of the opponent's lean |
| the critic's calibration saturates | 1000-2000 games (loss 1.46/1.48), AUC from ~120 games |
| heavy exploration destroys a corpus | 30% sale -> 43.1%; 50% phases -> 21.1% |
| the generator dies on memory with the rollouts | every ~250 games; the pipeline now chunks + resumes |

## Hypotheses, in the order they will be tested

### H1 - the one-round window hides the economy (test now)

Theory: a discount, a second build and a steel/titanium engine pay over many rounds; a one-round rollout values only the action
that is usable immediately, which is why it prefers phase 3 (37%) over phase 1 (9%) and why forcing the phase costs 29 points:
the *play* inside the phase was never valued, only chosen.

Change: `-Dares.agent.search.horizon=3 -Dares.agent.search.horizonTypes=PICK_PHASE` (new flag): the phase candidate AND the
stop are played out three rounds with the policy, the rest of the menus keep the cheap one-round path.

Instruments: the phase mix (check_player, 60 games) and the win rate (1000 games). Variants: horizon 3 and 6; if the mix moves
toward phase 1/4 and the win rate does not fall, the window was the problem.

Decision: keep if the win rate >= 73% with a more balanced mix (entropy up, phase 1 >= 15%); otherwise the window is not the
lever and H2 starts.

### H2 - the value net is the next ceiling (after H1)

Theory: every decision is a difference of two V's; the calibration curve shows the net is trained on `margin` with a masked hand,
and the opponent's block is dropped as redundant - i.e. the value the search reads is "how good is my position", not "how good is
my position against THIS opponent".

Variants (one at a time, 1000 games each): `--target outcome`; the seven-channel breakdown critic (`train_breakdown.py`); a wider
net (768,384); 5 critics instead of 3. Acceptance as above, plus the profile gate.

### H3 - a genuinely different second plan (depends on H1/H2)

Theory: the AI cannot learn to contest a plan it never faces (self-play: the opponent is a copy). A second plan must be *good*
before the interaction can be learned; forcing a phase produces a bad plan, so the second plan has to be grown from a valuation
that can see its payoff (H1) or from a better net (H2).

Change: repeat the phase-1 curriculum with the improved valuation, and require the *fingerprint* (phase mix + upgrades) to differ
AND the win rate to stay within 5 points of A. Only then is it a specialist.

### H4 - the interaction (the owner's actual target)

Theory: the phase you pick also happens if the other seat picks it, so knowing their lean changes your best pick
("they will take 3 anyway, I take 4 and get both"). Nothing in the current data can teach this: the opponent's block is
redundant in mirror self-play.

Change: generate a corpus of A vs B (`--a AGENT --b AGENT_V2`, `-Dares.agent.models2=<B>`), train one policy on it, and measure
against **both** specialists plus SMART. This is the adaptive player.

## Gates (unchanged, from the owner's rules)

- Every candidate: profile first (builds, phases, standard projects), then the absolute benchmarks (SMART, RANDOM, NETWORK),
  then a 2000-game duel against the current champion, then the pool.
- An effect under 1.5 points is claimed only at 10 000 games.
- Nothing is deleted; every experiment goes into `docs/RESULTS.md` with its number, including the failures.
- The loop keeps the anti-narrowing guards (phase-concentration detector, corpus gate, one-generation exploration).

## Stop rules for this plan

- If H1 does not move either the mix or the win rate at two horizons, H1 is closed and H2 starts.
- If two consecutive changes move nothing, the *measurement* is re-examined before a third change (that is how the phase critic
  and the mulligan units were found).
- No experiment runs twice in the same shape (the night's 43.1% and today's 21.1% are the same mistake: heavy exploration).
