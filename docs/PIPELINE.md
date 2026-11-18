# The pipeline: stages, sensors, actions

One run of `ml/night_loop.py` = one loop of the self-training pipeline. Every stage below either verifies its input or refuses
it; every sensor has an action attached, so a problem changes the next iteration instead of waiting for a human to notice.
The order matters: data quality, then coverage, then training, then measurement, and only then the next lever.

```
generate ─► pack ─► verify ─► census ─► critics ─► policy ─► measure ─► decide
   ▲                                                                      │
   └──────────────────────── next generation (exploration, lever) ◄───────┘
```

## 1. Generate (`SelfPlayMain --mode play`)

* plays with the **deployment configuration**: the search ranks the menus in `--types`, the phase is ranked by its own model
  (`valueModel.phase`), the policy fills the rest;
* writes the raw corpus **to the SSD**, and the opponents are a parameter (`--matchups`): 85% mirror matches once produced a
  player that beat a copy of itself (10.9% vs SMART, 53% vs RANDOM) - variety is the difference;
* the play log is a sensor: `CAP OVERFLOW` (the engine's "menu has N candidates" guard) means the menu caps were measured on
  an older player and must be re-measured with `ml/menu_sizes.py`.

## 2. Pack, verify (`pack_dataset.py`, `pack_check.py`)

* one sequential pass, real candidates only, the de-duplication mask computed once;
* `pack_check` reproduces the raw chunks bit for bit (observation, labels, candidates, ids) or the iteration is refused;
* the raw corpus is deleted afterwards: the verified pack is the dataset.

## 3. Census (`action_coverage.py`) - before any training

Per decision type: slot coverage (entropy of the chosen position / mean log menu), thin options in small stable menus, and
options that shrank against the previous corpus. A flag becomes `-Dares.agent.explore=TYPE:0.60` in the next generation,
automatically. A gap with a recorded reason lives in `ml/gap_allowlist.json` (e.g. `SELL_CARDS`: the game sells automatically
after phase 5).

This stage exists because an action can die silently: the phase mix went 20% (uniform) -> 7% -> 2.6% -> 2.2% while nothing
looked, and the mulligan is declined in 100% of rows.

## 4. Critics (`train_value.py`)

* N critics per iteration (the search averages them: 10.28% vs 8.63% for one, 10 000 games, disjoint intervals);
* the hand identity block is masked, and the export gate **proves** the deployed model ignores it (a dropped column whose
  weights stay random would inject noise in play);
* the standardisation sample is a uniform sample of the training rows (`--stats random`), not the first rows of the pool;
* the exported model carries `modelHash` (type + widths + observation layout), so changing a menu cap - which changes the file
  hash - does not invalidate the model.

## 5. Policy (`train_policy_ce.py`)

* cross-entropy on the search's own choices (expert iteration), with:
* `--entropy` - the term without which the trainer sharpens the policy onto one option every iteration;
* `--adv-weight-clip` - a bounded, positive weight, because a signed unbounded advantage is unbounded below (measured:
  PICK_PHASE val loss 860).

## 6. Measure (`check_player.py`, `--mode compare`, `--mode scorecard`)

* both numbers, every iteration: **SMART** (the benchmark) and **RANDOM** (the floor a real player must clear);
* the profile: cards per game, phase mix, phase-3 spend - the degeneracy gate, not a style target (there is no healthy band,
  see `docs/AUDIT.md`);
* `ml/audit_player.py` runs the same census on the **deployed** bundle - the deployed phase mix (28/8/27/14/23) differs
  completely from the training corpus's (14/23/18/42/2), so the corpus alone can lie about the player.

## 7. Decide (the escalation ladder)

On a plateau (no improvement in `--patience` iterations) the loop takes the next lever from the list and records which one,
instead of only enlarging the corpus or stopping:

1. double the corpus (more data for the critic);
2. raise the entropy bonus;
3. raise the exploration shares;
4. let the critic rank one more menu (`STANDARD_ACTION`);
5. stop and say so (the lever list is exhausted).

The full candidate list, with what each lever should move and its status, is in `docs/AUDIT.md`.

## Artifacts and names

* `C:/ares/pack/<run>-<iteration>` - the datasets (keep 2);
* `D:/ares/runs/policy/<run>-<iteration>`, `D:/ares/runs/value/v_<run>-<iteration>_s{k}` - models, never deleted;
* every name carries the run tag (`%m%d-%H%M`), because a second run once overwrote the first run's best bundle;
* `D:/ares/tmp/night_ledger.json` - one row per iteration: score vs SMART and RANDOM with intervals, cards, phase mix,
  exploration, entropy, collapses, cap overflow, regret;
* `ml/night_report.py` prints the ledger.

## Tests

`ml/tests/test_pipeline.py` (10 tests) pins the census, the phase mix, the pack roundtrip, the regret signs and the loop's
parsers; the whole ml suite is 99 tests. A wrong instrument is worse than none: both instruments that changed a conclusion
were wrong, and both are now covered (`phase_mix` off-by-one, coverage normalisation).

## The cost of a corpus, by how many menus the critic ranks

Measured 18/11: `--mode play`, 14 threads, `--max-rows-per-game-type 8`, `AGENT:AGENT:0.85,AGENT:RANDOM:0.15`.

| ranked menus | games/s | 20 000 games |
|---|---|---|
| 3 (BUILD_CARD, DRAFT_CARDS, PICK_PHASE) | ~5 | ~1 h 07 m |
| 8 (+ STANDARD_ACTION, PERFORM_BLUE_ACTION, DISCARD_CARDS, SELL_CARDS, SELL_CARDS_LAST_ROUND) | 2.6 | ~2 h 10 m |

Every ranked menu costs a `new MarsGame` copy, an observation build and a forward pass **per candidate**, so the generation
time is roughly linear in the number of ranked menus times the average menu size. `play()` used to cap the corpus at a fixed
7200 s; the night of 18/11 hit it at 18 772 of 20 000 games and lost two hours. It now scales with the game count
(`games * 5 + 600`). A 240-minute budget therefore allows two iterations of 6 000-8 000 games with eight menus, or one big
corpus - not two of 20 000.

When a corpus dies mid-generation, do not regenerate it: pack what exists (`pack_dataset.py` ignores the unfinished chunk),
check it, and train on that. The raw corpus is the expensive artifact.

## Storage: what the replay costs and what may never be deleted (owner, 18/11)

Measured on 18/11 before cleanup:

| disk | total | free before | after cleanup |
|---|---|---|---|
| C: (SSD) | 464 GB | 64 GB | **153 GB** |
| D: (HDD) | 1863 GB | 332 GB | 332 GB |

What occupies C: and what it is for:

- **the replay set**: the newest `replay_packs + 1` packs, ~7-9 GiB each (three today: 19.6 GiB). This is the anti-narrowing
  buffer - losing it silently re-opens the failure the owner described for his NETWORK, so it is budgeted, not "spare space";
- **one raw corpus being generated**: ~27 GiB for 20 000 games. Deleted automatically once its pack is verified;
- **the code, the JDK, the maven cache** (not ours to prune).

The 18/11 cleanup freed 82.8 GiB by deleting *only* redundant data: `bd40` (an experiment corpus whose pack had already been
pruned), the raw `1118-0058_13` (its pack exists, the pack is the dataset), the smoke corpora `bd1` and `break_smoke`, and
the partial `1118-1233_14` left by a killed run. No pack and no model was touched.

Rules now enforced by `night_loop` (they are not advice):

1. **A raw corpus with a pack is redundant and goes first** (`reclaim`), newest first, so a disk guard never has to touch a
   pack while an equivalent amount of raw data is sitting there.
2. **A raw corpus WITHOUT a pack is never deleted automatically.** It is the only copy of that corpus, and the 17/11
   incident (an 80 000-game corpus thrown away by a crash path) is the reason.
3. **Packs are cut only beyond `max(keep_packs, replay_packs + 1)`.** The replay buffer can therefore never be destroyed by
   the low-disk branch, which used to keep a single pack (`keep_packs // 2`).
4. Every reclaim logs the gigabytes it freed and each item with its size, so the arithmetic is auditable the next morning.

Budget rule of thumb: a 20 000-game night needs ~27 GiB (raw) + ~9 GiB (new pack) and must keep `K x 10` GiB of replay
packs, so it starts only with **60 GiB free on C:**; below that the guard reclaims before generating instead of after.
