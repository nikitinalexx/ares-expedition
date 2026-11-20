# PLAN V3: raise the VALUE's resolution, then let the search spend it

Written 20/11 at the owner's request: the most precise plan that can close what has failed, before writing more code. Every claim
below is tied to a number measured in `RESULTS.md`.

## 1. Why the search cannot help today (the mechanism, not a hunch)

AlphaZero improves because its search is STRONGER than the network: MCTS turns `p(a|s)` and `v(s)` into a better `π` and a better
`z`, the network imitates them, and the loop closes upwards. The precondition is that more search produces a better answer.

Measured here, on this value:

| more search | result |
|---|---|
| phase, 2 rounds instead of 1 | 65.08% (-8) |
| phase, 8 determinisations | 66.9% (-6.3) |
| a played-out round after a build | 73.15% (0) |
| a played-out round after a card input | 71.65% (-1.5) |
| the flat tree (PUCT + prior, 32 samples) | 71.65% (-1.5) |
| the line tree over 4 of our decisions | running |

Every one of them is at or below the flat 73.17%. The reason is the resolution mismatch we measured: the one-step V difference
carries noise of ~0.4 logit (the mulligan gate's gap distribution at zero true effect) while a card or a plan choice is worth
0.05-0.2 logit in the same units, and the margin target's error is tens of points. **More search reads the same V at states that
are further apart; if V does not resolve the difference, the extra lookahead has nothing to read.** So the search is not the lever
until V resolves the difference.

## 2. Why the value does not resolve it (the target, not the capacity)

- 500 games -> 11.6%, 1250 -> 30%, 5 000 -> 73%, 10 000 -> 72.7%: data volume saturates at 5 000.
- Wider nets (768x384) at 2 000 games: 69.55% (worse). Capacity without data does not help.
- The target is the final margin (or the seven channels summed): a quantity dominated by tempo and by the last three rounds, i.e.
  a signal where "this line is 3 points better" is buried in tens of points of variance. The breakdown decomposition did not help
  at 1 250 games (27.9% vs 30.6%), because the noise of the *final score* is still inside every channel.

**The untested lever is the shape of the target:** predict the *local* consequence - the score change over the next H rounds -
instead of the final result. This is n-step TD, and it is the only change that targets exactly what the search reads ("what does
this action give me soon") without external data and without imitating anyone. Note what it does NOT claim: it adds no information
the final margin lacks (the margin already contains the local change plus the rest). It claims **variance**: one sample per state
means the margin critic learns a near-constant value over a menu, and a tighter target lets the same corpus support a sharper V.

## 3. The plan

### S1. Record per-row score channels, then train a MIXED target (n-step with bootstrap + a terminal anchor)

**The naive version is wrong and must not be implemented** (owner's question, 20/11: a build lowers the near-term score and
raises the income, so "score in H rounds minus now" would teach V not to build). Two corrections:

1. **The target is not a truncated return, it is a bootstrapped one:**
   `target(t) = [reward(t..t+H)] + V(s(t+H))`, where `reward` is the score-difference change over the window and `V(s(t+H))` is the
   network's own prediction H rounds later. The delayed payoff enters through the bootstrap: a build hurts `reward` and helps
   `V(s(t+H))`, and the target is roughly neutral for a good build. The window H only reduces the variance of the sampled part; it
   does not cut the horizon.
2. **Tempo is not lost**: the bootstrap is read on a state of a LATER round, and the round number is in the observation, so V can
   represent "the same card pays 18 times in round 5 and 3 times in round 20". It learns that recursively from the terminal.

**The risk this creates, and the mandatory remedy:** bootstrapping an inaccurate V amplifies its own bias (the deadly triad). If V
believes building is bad, the n-step target confirms it. So the objective is MIXED: a share of the rows (a `--anchor` fraction,
e.g. 0.5) keeps the true terminal outcome as the label (the Monte-Carlo anchor we already train on today), the rest use the n-step
bootstrap. That is TD(lambda) with lambda < 1 in practice, and it is the only version worth testing.

**The failure mode is measured, not hoped away:** if the target learns "do not build", the profile gate catches it - builds per
game and build acceptance collapse (today 32.5 and 0.77). So S1 is judged by the profile FIRST, then by the within-menu ranking,
then by the win rate.

### S1 is not new code: TD already exists, and the change is the ROLE and the CHECK (found by re-reading 16/11)

`train_value --td-bootstrap <model> --td-steps N` already implements the bootstrapped target: the reward per step is the EXACT
score change (the score is visible at every state, `pool["score"]`), the bootstrap is the value of the state N steps later, and the
last rows of a game keep the exact final margin. On 16/11 it was measured in the DECIDER role: it priced +1 income at 7.7 MC
(9.3 / 7.5 / 6.4 by round) against the policy critic's flat 6.5 and the random critic's 12.1, and it was **not adopted** because
that same night showed a well-calibrated critic moved the RANKER's win rate by nothing (43.59% -> 42.98%): in a ranker the shared
slope cancels.

What changes now is the role, and this is the whole argument:

* a ranker compares two afterstates of ONE position, so the errors are almost common-mode and cancel;
* the LINE tree does **not** sum values along the path (`TreeDriver.backup` pushes ONE leaf value to every node on the line, by
  design - summing V would need a reward model in the engine). What changes is the DISTANCE between the compared states: the
  leaves of two lines are a round or more apart, so the critic's error is no longer common-mode and stops cancelling. That is
  weaker than summing, and it is said here because the experiment must be judged on what the code actually does;
* and the reason a different TARGET can matter at all is variance, not information: the final margin is one sample per state
  (huge variance), so the critic can only learn a near-constant function of the menu; the n-step target's variance is much
  smaller, so the same corpus can support a genuinely state-dependent V. Higher resolution from the same data, no new data.

That gives a sharp, falsifiable prediction, which is what makes the experiment worth an hour:

| measurement | prediction if TD is the missing resolution |
|---|---|
| within-menu ranking of V vs the rollout-best | improves |
| the LINE tree's win rate (same critics) | improves above 73.17% |
| the FLAT search's win rate (same critics) | unchanged (72-73%) |

If the tree improves AND the flat does not, the mechanism is confirmed and the search gets its resolution. If both stay flat, or
both move together, the hypothesis is dead - and S1 closes without a second attempt at another corpus size.

### S1b. (Cancelled) Recording per-row score channels

Not needed: the states already carry the score and the pack already links rows to the next row of the same game (`pool["next"]`),
which is what the existing `--td-steps` walks.

- `DecisionLogs` writes my and the opponent's seven score channels for EVERY row (the game's own `ScoreBreakdown`, called at the
  state of that row). 14 floats per row; the pack carries them in `labels`.
- `train_value --target scoretd:H --anchor 0.5` sets the label to
  `([score_me(t+H) - score_me(t)] - [score_op(t+H) - score_op(t)]) + gap(t+H)` for the bootstrap share, where `gap(t+H)` is the
  critic's own current prediction at the later row (computed once per corpus pass with the frozen critic, not during training),
  and to the true final gap for the anchor share. The pack already stores `round` and `game`, so the windows are joinable.
- Why it should work: the target is a few points wide, its variance is far smaller than the final margin's, and it describes
  exactly the horizon the search can see (a round or two). If V cannot resolve 3 points over 4 rounds, nothing downstream can.

**Instruments, in order:** (a) the within-menu ranking accuracy of V against the rollout-best (a direct measure, not AUC); (b) the
flat search's win rate with the new V (1000 games); (c) the profile (builds, phases).

**Decision rule:** if (a) does not improve, or (b) is below 73.17% by more than noise, S1 is closed and the project stays at the
current champion - no rescaling of the same idea.

### S0. The owner's objection (20/11) is right, and it demotes S1: TD cannot fix a BIASED value

Three separate things were being conflated. Naming them changes the plan:

1. **Resolution**: can V tell two candidates apart at the scale that matters (3-5 points)? S1 addresses this, and only this.
2. **Variance of the judge**: does one game's margin carry enough information to rank a decision? Answered by pairing, not by
   smoothing.
3. **Bias of the judge**: does the quantity being optimised point at the right thing at all? **TD cannot fix this.** `target =
   reward + V(next)` hands the bias to the label through the bootstrap, and fitted value iteration then converges to the same
   preference with less variance - a more confident version of the same mistake (the deadly triad, already written in S1 as a risk;
   the owner's examples show it is the whole problem, not a footnote).

**The diagnosis that makes the owner's examples one problem.** `V(x)` is not the value of the position, it is
`E[outcome | x, our policy keeps playing]` - a *policy-conditional* value. Measured evidence: V does see stockpiles (my 200 MC
+2.92 logits, the opponent's -2.20), so the blind spot is not in the input; and against NETWORK it says +0.94 margin while we lose
two of three. So "5 steel income + 5 titanium income, money spent" is scored as a loss because *our player* will buy one more blue
card next round and convert nothing. V is right about our play and wrong about the position, and **every deeper search asks it the
same question.** That, not noise, is why depth never paid.

**The arithmetic that closes the whole short-horizon family** (all of 19/20 November): a production card pays over 10-15 rounds.
The horizons we measured were 1 round (phase playout, build playout, input playout), 2 rounds (-8) and 3-4 of our decisions (the
line tree). **A horizon shorter than the payoff's time scale cannot see the payoff at all, whatever V is.** So the only horizon
that contains the delayed value is the end of the game - which is exactly what the owner said, and exactly what the last-round
90 -> 180 objection is about.

### S1'. The only judge that cannot be biased: a PAIRED end-of-game rollout, at plan-level decisions only

- **Judge**: play each candidate to `GAME_END` and read the final score difference. No V, so no policy-conditional bias.
- **Variance**: the owner's objection is real for independent rollouts (sd of the level ~25-30 points, a human jumps 90 -> 180
  in the last round), and the owner's second objection (20/11) is also real: **pairing does not survive the first divergence** -
  `Determinizer` fixes the world at the root, but the tier decks, the dummy hand and the draft rows are shuffled with an unseeded
  `ThreadLocalRandom` (`ShuffleService` 18, `CardService` 134-160, `MarsGame` 338), and a draw is a pop, so a shifted count changes
  every later card. So the judge is NOT a per-move decider:
  * as a **ruler** it averages the paired difference over ~100 states, and the per-state noise averages out (sd ~20 / sqrt(100) ~ 2
    points, enough for a 3-5 point effect) without needing pairing;
  * as a **label source** it produces unbiased but noisy targets, which is exactly what a critic's regression tolerates (noise
    averages out over thousands of rows; bias does not);
  * real pairing, if it is ever needed, requires LOGICAL seeding (seed each random event by round + event kind + slot, not by its
    position in a stream), which is a refactor and is not paid for before the ruler shows a gain.
  This is why the phase pick's existing shared-seed comparison is a *convenience*, not a guarantee.
- **Where it is allowed to judge**: plan-level decisions only (PICK_PHASE, the plan-defining builds), and per the divergence above,
  **never as the decider of a single move** until logical seeding exists. Judging every decision is unaffordable anyway.
- **Competent exploration is a precondition, not a detail**: if the continuation is the policy, the judge says "production loses"
  and is right, because the continuation does not convert (measured: a forced plan 44.25%, its corpus trains 25-33%). So inside a
  chosen plan the decisions must be searched too, or the exploring games teach the wrong lesson.
- **What it buys if it works**: the corpus of judged games carries production plans the judge endorsed, with the game's own label.
  V finally sees those states labelled "won" - the only mechanism that removes the bias instead of entrenching it. V then returns
  to its correct role: prior and cheap play, not judge.

**Instrument, in this order (all affordable, all falsifiable):**

| step | measurement | stop rule |
|---|---|---|
| A | `-Dares.agent.search.judgeAudit=true`: per candidate, per world, the final margin on ~100 real phase states - the MEAN paired difference (se ~ sd/sqrt(100)), and sd(level) vs sd(paired) reported so the pairing's real strength is on the record | if the averaged judge-vs-V difference is inside noise, V is not the problem and S1' ends here |
| B | only if A shows V's preference disagreeing with the game: judged games become the corpus (unbiased, noisy labels), V retrained on their outcomes | the 5-steel/5-titanium probe must flip, and the profile gate (32.5 builds, 0.77 acceptance) must hold |
| C | real pairing, only if a per-decision judge is ever wanted | logical seeding of every random event; measured sd(paired) must drop well below sd(level) or it is not paid for |

A is a pure diagnostic: the flag prints the matrix and changes no decision (`auditEndJudge` in `OnePlySearch`).

### S1'c. Where the judge is used, exactly, and what it buys (owner's question, 20/11)

**What it adds is not "the result"** - we already have the result of every game we play. It adds the **counterfactual**: the result of
the plan we did NOT choose. That is the one quantity a policy-conditional V cannot produce, and it is produced for four candidates
at once, i.e. several plan comparisons per game instead of one outcome. The worlds are only *partially* common (the divergence
above), which is why the product is an average over states, never a verdict on one move.

**It is never a per-decision judge.** Cost, measured ingredients: inside a rollout the search is bypassed (`choose()` returns -1 while
`busy`), so a full game costs ~60-100 ms; a judged decision is `candidates x K` full games; PICK_PHASE has ~4 candidates and occurs
once per round per seat (~12-15 times a game).

| use | cost | where |
|---|---|---|
| 100 phase states, 4 candidates, K = 8 | ~3 200 games ~ 5 minutes | offline, once (instrument A) |
| judge at one plan decision per game | 4 x 8 x ~80 ms = ~2.5 s per decision | NOT justified until logical seeding: per-move noise ~ the effect |
| judge at all 12-15 phase picks | ~30-40 s per game | rejected: 30-40x the game's own cost for decisions whose effect is not a plan |

**The decision this plan makes:** the judge is first a **ruler**, not a player. It answers "is V's plan preference right?" on 100
states, which is the one question the project has argued about without a number. Only if the ruler shows V is wrong does the judge
become the **label source** for V (the judged games carry the endorsed plan with the game's own outcome), and only then does the
continuation have to be searched inside the chosen plan - because otherwise the exploration teaches "do not build" (44.25%).

So the order is: (1) ruler on 100 states, (2) if V disagrees with the game, judge-guided generation + retrain, (3) if V agrees,
the bias hypothesis is dead and the plan returns to the data/search question - with a number instead of an opinion.

### S2. Spend the resolution on the search

Only if S1 resolves the differences: re-run the measured set (flat one ply, one round on the phase, the line tree) with the new V.
The rule stays: a variant that does not beat 73.17% at 1000 games is not scaled.

### S3. The loop

`night_loop` with the new target (a `--value-target` flag), generating from the current best bundle, keeping the collapse detector,
the corpus gate and the abort guard. Every candidate goes through the HoF gates (profile -> worst benchmark -> duel 2 000).

### S4. Throughput - only after S2 pays

A tree corpus is 6-20 hours at the current speed, so the tree must first prove itself at play time. If it does: pruning to top-k
on the sell/discard/input menus (1.5-2x, measured menu sizes), a micro-batcher for the ensemble (1.3-1.5x, bounded by Amdahl), and
incremental observation (1.8-2.5x). Not before.

## 4. What this plan deliberately does not do

- No more corpus-volume scaling past 10 000 (measured: no gain at 10 000 vs 5 000).
- No more flat deepening, no more determinisation counts, no bigger nets at this corpus size.
- No imitation of SMART, NETWORK or the owner's games (owner's rule).
- No plan-forcing bias: measured to produce weak play in the generated corpus (25-33%).

## 5. Why this ordering, after the owner's objection (20/11)

Every failure so far has one shape: something downstream asks `V` a question `V` answers for a player who cannot execute the line.
The search, the determinisation, the tree, the phase rollout and the mulligan all inherit that, and so does the TD target through
its bootstrap. So the ordering is by what can possibly remove the CAUSE, not by what is cheapest:

1. **S1' (paired end-of-game judge)** - the only judge that carries no policy-conditional bias, and pairing is the only known
   answer to its variance. First the variance audit (A), because if a paired se cannot resolve 3-5 points at an affordable K,
   nothing downstream matters and this is closed with a number.
2. **S1 (the target's shape)** stays as a resolution measurement only. Its flat half already read 72.50% against 73.17% - a
   measured "no", which is why it is not allowed to grow into a second attempt at another corpus size.
3. **Nothing here is scaled on hope**: each step has a number that closes it, and the only thing that may be rescaled is a
   mechanism that has already beaten 73.17% at 1 000 games.

The claim S1' makes, stated so it can fail: with the judge playing the whole game in paired worlds, the phase/plan choice stops
being a vote for "the plan our value likes" and becomes a measurement of the plan's actual final score - and the production line
our value rejects today becomes visible as soon as the continuation can execute it.
