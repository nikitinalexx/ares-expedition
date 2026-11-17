# Audit before tuning: the behavioural census

Why this file exists (owner, 2026-11-17). One remark about the phase distribution moved the player further in one afternoon
than a whole autonomous night: 10.89% -> 17.05% against SMART (3 000 games) and 53% -> 74% against RANDOM. Meanwhile the night
spent its hours tuning what I had chosen to measure (critic data volume, blocks, ensemble) and never audited the *decision
space*: which of the game's actions does the agent actually use, and how often? The phase 5 share had fallen 20% -> 2.2% and
nothing in the process looked, because I had built a sensor for one action (the stop of BUILD_CARD) and treated that as
enough.

The rule that follows: **tuning answers questions about a player whose behaviour is already census-clean or explained.** No
new lever is picked before the census has been run on the current bundle and every flag is either fixed or allowlisted with a
recorded reason.

## The census, one command

```
py -3 ml/audit_player.py --policy D:/ares/runs/policy/<bundle> \
    --critics D:/ares/runs/value/v_a,D:/ares/runs/value/v_b,D:/ares/runs/value/v_c \
    [--phase-critic D:/ares/runs/value/v_a] [--games 60]
```

It plays `--games` games against SMART **with the exact deployment configuration**, packs the recorded rows, and prints:

1. **score, points and cards per game for both sides** (`--mode scorecard`) - who develops whom;
2. **the phase mix** of our player;
3. **the action census for every decision type** (`ml/action_coverage.py`): slot coverage, thin options, options that
   shrank against the previous census, and the flags that follow from them;
4. **the allowlist**, so a known-benign narrowness is visible and not re-argued.

## What each sensor catches, and what it cannot

| sensor | catches | cannot catch |
|---|---|---|
| `action_coverage` flags | an action type (or one of its options) stops being used | whether the action that *is* used is the right one |
| phase mix | a phase that is never called | whether the mix is good (no healthy band exists) |
| scorecard cards/points | whether we take anything from the opponent (NETWORK denies SMART, we did not) | why |
| `critic_ablation.py` | a block of the observation the critic ignores | a block that helps only in rare positions |
| `--mode sanity` | a model that refuses free value | a model that is weak but not blind |
| win rate at 10 000 games | whether any of it matters | nothing about the mechanism |

## The second lesson: one variable, actually one

The jump above landed together with a second change (opponents 45% mirror / 55% RANDOM), so the +6 points cannot be
attributed between the two. That is exactly the accounting the owner asked for, and it is now enforced by run tags:
`ml/night_loop.py` prefixes every artifact with the run's timestamp, so a later run can never overwrite an earlier bundle
(it happened once: the 10.89% bundle was replaced by the next run's iteration 2).

## The standing list of levers

Keep the candidates here instead of choosing whatever was looked at last. One line per lever: what it changes, what it
should move, and its status.

| lever | what it changes | expected to move | status |
|---|---|---|---|
| phase critic + phase exploration | the phase decision becomes value-driven, all five phases in the corpus | phase mix, win rate | measured +6 points (confounded with opponents) |
| opponent variety | 45% mirror / 55% RANDOM instead of 85/15 | win rate vs RANDOM | measured with the phase change; needs its own run |
| entropy bonus on the policy | the policy cannot sharpen onto one option | coverage, stability | implemented, not yet measured |
| coverage guard | exploration is injected where an action collapses | coverage | implemented, in the loop |
| build/decline balance | cards per game (we build 24, SMART 32-34, NETWORK 45) | cards, points | open |
| critic data scale on 1256 | corpus 40k -> 150k games | value quality | open |
| blue-table block re-test | critic input | value quality | open (1 of 3 seeds on the random corpus) |
| compact simulator output | I/O: 27.6 GB -> ~4 GB per 20k games | wall time | open |
