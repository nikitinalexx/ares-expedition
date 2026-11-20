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
0.05-0.2 logit in the same units, and the margin target's error is tens of points. **A tree on that value aggregates its errors
more precisely; it cannot rank lines better.** So the search is not the lever until V resolves the difference.

## 2. Why the value does not resolve it (the target, not the capacity)

- 500 games -> 11.6%, 1250 -> 30%, 5 000 -> 73%, 10 000 -> 72.7%: data volume saturates at 5 000.
- Wider nets (768x384) at 2 000 games: 69.55% (worse). Capacity without data does not help.
- The target is the final margin (or the seven channels summed): a quantity dominated by tempo and by the last three rounds, i.e.
  a signal where "this line is 3 points better" is buried in tens of points of variance. The breakdown decomposition did not help
  at 1 250 games (27.9% vs 30.6%), because the noise of the *final score* is still inside every channel.

**The untested lever is the shape of the target:** predict the *local* consequence - the score change over the next H rounds -
instead of the final result. This is n-step TD, and it is the only change that targets exactly what the search reads ("what does
this action give me soon") without external data and without imitating anyone.

## 3. The plan

### S1. Record per-row score channels, then train an n-step target

- `DecisionLogs` writes my and the opponent's seven score channels for EVERY row (the game's own `ScoreBreakdown`, called at the
  state of that row). 14 floats per row; the pack carries them in `labels`.
- `train_value --target scoretd:H` sets the label to
  `[score_me(t+H) - score_me(t)] - [score_op(t+H) - score_op(t)]` from rows of the same game H rounds later (the pack already
  stores `round` and `game`).
- Why it should work: the target is a few points wide, its variance is far smaller than the final margin's, and it describes
  exactly the horizon the search can see (a round or two). If V cannot resolve 3 points over 4 rounds, nothing downstream can.

**Instruments, in order:** (a) the within-menu ranking accuracy of V against the rollout-best (a direct measure, not AUC); (b) the
flat search's win rate with the new V (1000 games); (c) the profile (builds, phases).

**Decision rule:** if (a) does not improve, or (b) is below 73.17% by more than noise, S1 is closed and the project stays at the
current champion - no rescaling of the same idea.

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

## 5. Why this is the most probable remaining path

Every failure so far has one shape: something downstream cannot see a difference that V cannot represent. The search, the
determinisation, the tree, the phase rollout and the mulligan all inherit V's resolution. The one thing never changed is the
quantity V predicts. Changing it is cheap (a recorder field, a pack field, a trainer target), it needs no new data source, and its
success is directly measurable (within-menu ranking, then the win rate).
