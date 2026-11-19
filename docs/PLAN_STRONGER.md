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

## Log of the plan (keep it current)

**19/11, session 3**

- The generator's memory and data corruption were one bug: rollouts recorded phantom decisions (8 GB, 921k pending rows, OOM every
  ~250 games). Fixed with a single `record(...)` guard on `OnePlySearch.isBusy()`, and pinned by `RolloutRecordingTest` (2 tests).
  Heap after the fix: 370k `float[]` (0.6 GB) against 4.6M (8 GB).
- **Every corpus generated today with rollouts is polluted** (p1cur, p1b, h1cur) - specialist B's failure is confounded and is being
  re-run on a clean corpus.
- H1 (delayed payoff): the horizon moves the plan as predicted (16/20/24/18/22) but the player drops to 65.08% - closed at the
  first honest test, per the rule.
- H2a (wider critics, 768x384): **69.55%** against 73.17% - worse at the current corpus size. **Postponed, not closed** (owner,
  19/11: capacity may simply need more games). Re-open only together with a corpus of >= 20 000 games, and never as a separate
  variable before the obvious ones are checked.
- Correction to my own claim: the opponent block is NOT ignored - with it removed the player scores 57.4% against 73.17%. The loss
  metric was the wrong instrument; behaviour has to be measured by playing.

### Result of the ordered list

- **H1b (retrain on the clean horizon corpus)**: dead. With the horizon 21.0%, without 16.0%. The corpus was regenerated after
  the phantom-row fix, so this is not a data artefact: **a corpus inherits the strength of the player that played it** (the
  horizon player is 65%, its corpus trains 21%). This is the single rule that explains every failure today.
- **H3 (specialist on a clean corpus)**: predicted to fail by the same rule (the biased player is 44%); the corpus is
  generated but the result would confirm the rule rather than test a new idea - not run.
- **H5 (contest the opponent - the owner's idea in mechanism form)**: NEW and next in line. `-Dares.agent.search.opponentWeight`
  (default 1 = what `symmetric` always did): the candidate score is `(V(mine) - w * V(opponent)) / 2`. w=0 is blind ("play my
  own game"), w=2 doubles the weight on denying him. This is a decision-rule change, which is the class of change that has
  worked all day.
- **H4 (interaction) re-opened legally**: the hall of fame holds a strong, genuinely different player - the previous champion
  `1118-0058_13` (63.9%, phases 26/21/23/15/15). A corpus of **A vs the old champion** (`--models` A, `-Dares.agent.models2`
  champion, `--matchups AGENT:AGENT_V2:1`) is interaction data between two of our own strong players, with no benchmark in it.
  Generating now.

## Order after the first results (owner, 19/11: "check the obvious things first")

1. **H1b** - retrain on the clean horizon corpus and measure with and without the horizon (running).
2. **H2b** - 5 critics instead of 3 (cheap, no new corpus).
3. **H3 clean** - the phase-1 specialist retested on a *clean* corpus (the earlier failure is confounded by the phantom rows).
4. **H2c** - the seven-channel breakdown target, on a corpus that carries the RANKING sidecar.
5. **H4** - the interaction: A vs B corpus, then one policy that must beat both.

## Stop rules for this plan

- If H1 does not move either the mix or the win rate at two horizons, H1 is closed and H2 starts.
- If two consecutive changes move nothing, the *measurement* is re-examined before a third change (that is how the phase critic
  and the mulligan units were found).
- No experiment runs twice in the same shape (the night's 43.1% and today's 21.1% are the same mistake: heavy exploration).

# The global plan (owner, 19/11: "I don't see a plan that could produce a result")

## The diagnosis the plan follows from

Our loop is "the search picks, the policy imitates what the search picked". That only improves anything if the search is
**materially stronger than the policy it imitates** - that is the AlphaZero mechanism (hundreds of simulations behind every
target). Our search is one ply with a noisy value, so the target is barely better than the imitator and the loop sits at a fixed
point. Every gain of the last two days came from exactly this: making the search stronger (`phasePlayout` +5.9, the champion's
phase critic +3.3). Every failure came from trying to change the *data* instead (`docs/RESULTS.md`: the inheritance rule, the
one-policy rule).

A human beats the 73% player with a phase-1 economy engine "one-handed". That is not a different corpus, it is **sight over more
than one round**: a discount and a second build pay over many rounds, and our rollout sees one.

## The plan, in order of expected value per hour

### P1 - the value's target (the search's foundation)

The seven-channel breakdown target (`terraforming, forests, printed points, resources, milestones, awards, extra`) beats the single
margin by **+8.2 points** on the same 500-game corpus (19.8% vs 11.6%). It splits the credit, so a card that scores teaches
something even in a game lost on tempo.

Work: a clean 5 000-game corpus of A carrying the RANKING sidecar, then breakdown vs margin at that size, 1000 games each. If it
holds, the loop switches its target and every later experiment sits on a sharper V.

### P2 - depth where it is cheap (the human's advantage, mechanised)

`playout=BUILD_CARD`: score a build by **playing the round out** with the policy and reading V at the next pick - the same trick
that gave +5.9 on the phase pick. Never tried. The build menu is larger (10-40 candidates against 5 phases), so it is a play-time
flag first: at ~3 ms per played round and ~30 build decisions a game it costs a couple of seconds per game, which is acceptable
against a human and too slow for generation.

If it pays, the next step is the same trick for the *stop* side of the build decision (act vs stop is where 41% of offered cards
are declined on a one-ply hunch).

### P3 - interaction without destroying the value

The corpus of A vs the previous champion (two strong, different players) collapsed because the policy learned the average of the
two. The fix is now in place: `train_policy_ce --seat 0` trains on OUR seat's rows only - the opponent's different play stays in
the *states* (which is the point) while the action target stays our own policy. Measuring now.

If it works, the loop can generate each game with one policy per seat, from a pool of our own champions, and the value finally
sees what a different plan does to a position - which is the only route to "contest what he is building" that does not violate
the rule about SMART/NETWORK.

### P4 - the loop and the gates

Run P1-P3 in `night_loop.py` (it already has the collapse detector, the corpus gate, the one-generation exploration, the chunked
generator and the abort guard). Acceptance is the **worst** benchmark, not SMART: currently NETWORK 39.8% is the wall, and a
player that cannot beat NETWORK is not a stronger player, only a better rung on the same ladder.

## What would make 80% plausible

Measured today: the decision gap that matters (build vs stop, this card vs that) is 0.05-0.2 logits, and the value's error is of
the same order. 80% needs either (a) a value whose error on that scale is 3-4x smaller - P1 with 5-10x more data - or (b) a search
that looks far enough ahead to be right without a precise value - P2. Both are in the plan; neither is a promise.
