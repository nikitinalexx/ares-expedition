# Forecast: what the current approach costs, and where a real step could come from

Written 2026-11-16 at the owner's request ("we must soberly estimate what it will cost to reach the result with this
approach"). Numbers are measured unless marked as an estimate.

## The rate, honestly

| period | result | what produced it |
|---|---|---|
| start of 2026-11-15 | 37.8% +/- 2 vs SMART | value ranking cards, critic trained on policy play |
| end of 2026-11-15 | 43.59% +/- 1 (10 000 games) | two **one-off** fixes: the hand in the observation (worth ~12 points *inside* the critic) and the printed points in the candidate vector; plus the ensemble (+3.5 over one critic) |

So +5.8 points in a day, but almost all of it is blindness removal, not a rate. The marginal rate now is 0-1 point per day
of hand-tuning, and the remaining gap is large: NETWORK beats this player 90/10, i.e. roughly 20 points of win rate against
SMART. At +1 point a day that is a month of perfect days with no diminishing returns - not a plan.

Why the marginal rate is so low: a candidate costs ~1 hour (generate data, train, 10 000 games), and the critic's own
training variance is +/- 3 points, so most single changes cannot even be measured in a day.

## The three things that could be a step rather than a crawl

| # | lever | measured evidence | expected effect | cost |
|---|---|---|---|---|
| 1 | **the price of an income stream** (6.5 MC vs the rules' ~15) | `--mode audit`, 600 positions: every slope right, income wrong; 12.9 cards per game against 27 | if it moves, the player builds ~20 cards instead of 13 and the card term (+9 points of score) is the largest single gap | 1-2 days: builder-data critic (running), TD with full per-decision rows, then the decider + 10 000 games |
| 2 | **the value representation** (417 legacy table bits among 804 values) | NETWORK's net had ~80 engine aggregates and beat everything here; my block is a legacy layout with 210 duplicated columns (documented in ObservationBuilder) | unknown, but this is a *class* change, not a parameter: a compact engine-focused input can be measured with the same 10 000-game protocol | 2-3 days: `--drop-prefix` A/B per block (1 h each), then rebuild the block and retrain |
| 3 | **projections in the comparison** | implemented today (`-Dares.agent.search.projectBuilds`); first measurement 37.3% against 36.0% without, with a critic whose income price is wrong | with a correct critic this is the mechanism NETWORK used; +1-3 points expected, maybe more on the decider configuration | 1 day (done, waiting for a better critic) |

## What will NOT be done

* no more ensemble members for half a point (five already, and the sixth and seventh made it worse);
* no more named plans tested by hand: every plan goes through the paired audit at n>=500 or it is not measured;
* no more "one candidate per night" - hypotheses run in parallel as independent critics, and only the winner of a batch
  goes to the 10 000-game confirmation.

## Kill criteria, written before the runs

1. **The price test.** If neither the builder-data critic nor the full-row TD critic moves `+1 income` from 6.5 MC toward
   12-16 MC (audit, 600 positions), the "the value is mispriced" explanation is dead. Then the representation is the only
   remaining lever (item 2), and it gets one attempt.
2. **The representation test.** If dropping the legacy table block (417 values) and replacing it with engine aggregates
   does not beat 43.59% at 10 000 games, then with this budget there is no measured path from here to NETWORK's level, and
   the honest conclusion is written into HANDOFF: one machine + self-play only + this representation does not reach it.
3. **The decider test.** If the projection configuration with the *best* critic stays below the ranking configuration, the
   value is not good enough to decide act-vs-stop, and the player stays a ranker - which caps it near 44-46%.

## A note on what the goal actually is

Beating SMART by 50%+ is the owner's bar; NETWORK is the bar for "worth keeping". The measurements say the gap is not in the
loop (search, ensembles, TD, latent plans all measured), it is in **what the value knows about an engine**: money converts
to cards converts to points, and our critic prices that conversion at half of what the rules do. Everything above is aimed
at that one quantity, because it is the only one that has been measured to be wrong.
