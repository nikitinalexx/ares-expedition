# Results ledger (2026-11-15, all measured against SMART unless stated)

One place for every configuration measured during the day, with the number of games it was measured on. Read it with the
error bar in mind: **evaluation** noise is +/- 1 point at 10 000 games, but **critic training** variance is about +/- 3
points win rate (two critics with the same recipe on different 100k samples scored 42.98% and 39.38%, see
docs/EXPERIMENTS.md). The ensemble experiment exists because of that second band.

## The marginal rate (the honest accounting)

| period | change | what it was |
|---|---|---|
| until 15 Nov | 37.8% | the starting point of the session |
| 16 Nov, first half | 37.8 -> 42.98% | two one-off blindness repairs (the hand, the printed points) and the five-critic ensemble |
| 16 Nov, last ~24 h | 43.59 -> **44.00%** | tags and hand identities inside the ensemble: **+1 point, inside the +/- 1 band** |

At this rate the remaining distance to NETWORK (roughly 20 points of win rate against SMART) is not reachable by tuning:
`docs/FORECAST.md` states the kill criteria and the only two levers with a measured mechanism - data scale, and a class
change of the representation.

## What is deployed / best

| configuration | vs SMART | games |
|---|---|---|
| `r28_hand` (1090) + **six critics** (five remapped + `v_h_full`, trained with the tag and hand blocks) | **44.00% +/- 1.0** | 10 000 |
| `r28_hand` (1090) + five remapped critics (new blocks at zero weight) | 43.21% +/- 1.0 | 10 000 |
| `r25_rev2` (804) + **ensemble of five critics** (+ `v_ens_c`, `v_ens_d`) | 43.59% +/- 1.0 | 10 000 |
| `r25_rev2` + ensemble of three critics | 42.89% +/- 1.0 | 10 000 |
| `r23_price` (786) + `v_price2_margin_wide`, rank cards, pairs, symmetric | 42.98% +/- 1.0 | 10 000 |
| the same with `v_price1_margin_wide` | 42.05% +/- 1.0 | 10 000 |
| `r22_plan` + `v_mix3_margin_wide` | 41.18% +/- 0.9 | 10 000 |
| the same at 6 000 games | 40.36% +/- 1.4 | 6 000 |
| `r22_plan` + `v_mix2_margin` | 38.5% +/- 0.6 | 6 000 |
| `r22_plan` + `v_r15_plan` (the pipeline before today) | 37.8% +/- 2 | 2 000 |
| `r25_rev2` + `v_rev1_margin_wide` (one critic, 804) | 39.38% +/- 1.0 | 10 000 |
| policy alone, no search anywhere | 35.6% +/- 1.5 | 1 000 |

## Negative results, with the reason

| configuration | vs SMART | games | why it failed |
|---|---|---|---|
| V decides act-vs-stop on every menu, critic from random play | 0.4-0.6% | 1 000 | a state value is biased against spending now for a payoff ten rounds later: 759 of 768 build opportunities declined, 0.45 cards per game |
| V decides everything, policy critic | 0.1% | 1 000 | same bias |
| V re-ranks inside all menus (policy keeps act-vs-stop) | 5.8% / 0.85% | 1 000 | phases: the afterstate of a pick does not contain what the phase pays |
| V decides act-vs-stop with the coverage-trained critic | 36.0% | 1 000 | the bias again, smaller |
| `horizonStop` (stop scored over two rounds of policy playout) | 19.5% | 400 | one rollout per candidate = choosing by the ~17-point game noise |
| latent plans, code 0 (nudged data, no anchor) | 30.8% | 2 000 | imitating compulsion-generated games damages the base |
| latent plans, code 0, KL anchor to the parent | 34.95% | 2 000 | the anchor did not protect enough |
| the same budget without nudges (control) | 37.8% | 6 000 | the control: budget was not the problem |
| hand block zeroed inside the critic (same rows) | 27.5% | 3 000 | the block is worth ~12 points; removing it is a mistake |

## Paired plan audits (the arbiter of plans), 250-300 positions, 8-12 turns

| plan | difference | verdict |
|---|---|---|
| build income cards, press free actions | +1.7 +/- 1.4 | admitted (the only one) |
| press every blue action | -1.2 +/- 1.2 | rejected |
| call the build phase when the hand can pay | -0.5 +/- 1.7 | not paying |
| call the build phase **and** build the payable card | 0.0 +/- 1.6 | not paying |
| build the cards that raise steel/titanium income | -0.6 +/- 1.4 | not paying |

## Instruments built today (all in the repo, all reusable)

`ml/plan_report.py` (behaviour per latent code), `ml/slot_report.py` (how often a build phase offers two builds),
`ml/discount_report.py` (effective price and whether the player acts on it), `--mode phasedist` (which phases each
built-in bot calls), `--macro phases|phasesbuild|metals`, `--mode rollouttest` with an out-of-sample design,
`ml/train_value.py --names/--drop-prefix` (A/B one block of the representation on identical rows).
