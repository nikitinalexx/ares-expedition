# Search on top of the policy (family D) — Step 0: what it costs

The plan (`docs/AI_PLAN.md`, family D) is: keep the learned policy, add a small search at decision time.
Before building any of it, two questions had to be answered with numbers, because both decide whether search is
trainable, only deployable, or neither:

1. **Can a mid-game state be resumed?** A counterfactual rollout must start from exactly the state the decision was
   taken in. If the engine re-decides something on the way, the "counterfactual" is measured in a different game.
2. **What does a rollout cost?** Per decision, per candidate, per second of CPU.

Instrument: `SelfPlayMain --mode bench` → `com.terraforming.ares.selfplay.RolloutBench`
(machine: i5-12600KF, 10 cores / 16 threads, JDK 27, `-Xmx8g`).

```
java -cp "target/classes;$(cat target/cp.txt)" com.terraforming.ares.selfplay.SelfPlayMain \
  --mode bench --out bench.json --games 20 --captures-per-type 1 --max-captures 120 \
  --scales 1,4,8 --rollouts-per-capture 2 \
  --models D:/ares/runs/night7/handoff/night7_20261113_222800/best_model
```

**Which model.** The trained models of a night are in `D:/ares/runs/<night>/handoff/<stamp>/best_model`
(one folder per decision type, `model.json` + `weights.bin`, `MODEL_INFO.json` with the win rates) — **not** under
`D:/ares/data`, which holds only the datasets. `best_model` is loadable as is when its `schemaHash` equals the current
encoder's (the Java test `MlpPhaseModelTest.theFixtureBelongsToTheCurrentEncoder` prints the current hash).
`--hash-policy true` replaces the net with a deterministic stand-in (FNV hash of the observation + candidate ids) for
runs where no model is wanted; `--synthetic-model true` writes models with the current schema and **random** weights,
which measures the real inference path when only the shape matters.

## Result 1 — a decision point is replayable, exactly

The engine's decision points are not independent: one `AiService.processAiTurn` asks several decisions in a row
(sell → build card → inputs → payment; standard action → blue action → inputs). Re-entering the game at the *mid-turn*
state re-asks the whole chain and re-decides the prefix.

Fixed by adding one event: `DecisionProbe.turnStarted` at the start of `processAiTurn` + a replay that forces the
recorded prefix (type + candidate id) and then the captured candidate. Measured on 1159 captures / 40 games:

| metric | replay from `turnStarted` + prefix | re-enter at the mid-turn state (no prefix) |
|---|---|---|
| forced candidate applied | 100% | 96% |
| **forced decision state identical** (`observation` bit-for-bit) | **100%** | 61% |
| prefix replay broken | 0% | — |
| rollout reached `GAME_END` | 100% | 100% |

The same 100% holds with a **random** continuation policy (390 captures, `--hash-policy false`) and with the trained
policy (`--models`, 460 rollouts): the prefix is forced, not re-sampled, so the continuation policy's temperature does
not affect validity. Consequence: **counterfactuals are valid.** Keep the observation comparison as a runtime guard
anyway (`Arrays.equals`, ~1 µs) — it turns a silent engine regression into a missed sample.

One design consequence: the deck order is copied too, so K rollouts from one snapshot with a deterministic policy are
**identical**. Variance for a K-average must come from sampling the continuation policy (temperature > 0) or from an
explicit fair re-shuffle of the hidden information; a determinization mechanism does not exist yet.

## Result 2 — the net was the bottleneck, not the engine

One AGENT vs AGENT game is ~510–560 decisions. Measured with the trained night7 model, per game and per decision:

| cost | before the inference work | after | note |
|---|---|---|---|
| net forward pass, all decisions | **203 µs** / decision | **34 µs** / decision | 5.9× faster, see Result 3 |
| whole game (2 seats, one thread) | **176 ms** | **58 ms** | the net was 65% of it |
| one full rollout from a decision point | **125 ms** | **25.6 ms** | ~49 µs per remaining decision |
| full rollouts/s, 1 / 4 / 8 threads | 8.0 / 29.5 / — | **39 / 150 / 259** | sublinear (physical cores + heap) |
| `new MarsGame(game)` (copy) | 10 µs | 10 µs | p50 5.6, p95 27 |
| engine work between two decisions | 57 µs | 57 µs | apply + advance |

Also measured: **90% of an observation is exactly 0** (`obsNonZeroFraction = 0.1008` over 20 games) — that is the lever
Result 3 uses.

## Result 3 — making inference cheap (5.9×, exact)

`MlpPhaseModel` kept the trainer's row-major matrices and computed one dot product per neuron. Three changes, no
change of semantics (the 1e-4 agreement test with the independent numpy reference still passes):

1. **Transpose the weights at load** and project as `a[j] += W[i][j] * z[i]`. The row-major dot product is scalar
   (~3 cycles per multiply-add); the transposed form is a stride-1 SAXPY over `float[]` — the one shape HotSpot's
   SuperWord vectorises. 132 → 31 µs per decision with *dense* synthetic input.
2. **Zero-feature offset**: the projection is linear, so the contribution of every feature that is 0 is a constant
   (`aBase = b_obs + W_obs·clip(-mean/std)`, computed once per model); inference only adds the difference for the
   features that are actually present. On real data that is ~10% of 760 rows.
3. **Skip empty candidate features and relu zeros** the same way (a candidate vector has a handful of non-zeros out of
   6–116; half of the first hidden layer is 0 after relu).

Accumulation in `float` (instead of `double`) adds ~1e-6 over 760 terms — two orders of magnitude below the test
tolerance, and irrelevant for argmax. `MlpPhaseModelTest` now also refuses NaN/Inf weights at load: with the relu
branches a NaN would otherwise be swallowed and produce a silent garbage move.

Sanity, unchanged behaviour (600 games each, night7 model): **86.9% vs RANDOM** (night7 eval: 85.7% over 2000, CI
includes it) and **23.5% vs SMART** (night7: 25.9%, CI [20.3, 27.1] includes it). Throughput in `--mode compare`:
202 games/s vs RANDOM.

## What this means for the pipeline

Cost of one search move = `candidates × K × remaining decisions × 49 µs`.

| plan | per move | per game | 15 000 games | verdict |
|---|---|---|---|---|
| full rollouts, top-3, K=4 | 0.31 s (1 thread); **~46 ms on 8 threads** (259 rollouts/s) | — | ~900 CPU-h | deployment **affordable**, training **rejected** |
| 1-ply `Q(s,a) = V(apply(s,a))` (copy + apply + `V` ≈ 100 µs per candidate) | 0.5 ms for K=5 | ~45 ms over ~90 turn decisions | **~11 CPU-min** | trainable |

So the split is the opposite of "search replaces the policy":

- **Deployment** may search: a dozen full playouts per move is now a few tens of milliseconds on 8 threads.
- **Training** must stay cheap: the learning signal cannot come from rollouts at every decision. The workable shape is
  a **value function `V(s)`** trained on the observations already recorded (`obs → outcome`), then
  `Q(s,a) = V(apply(s,a))` — 1-ply, ~0.1 ms per candidate, no continuation, no determinization.

This also gives the `V(s)` baseline the current training lacks: today the only signal is one binary game outcome for
~500 decisions, applied to the single chosen candidate.

## Open questions (next, in order)

1. **Does 1-ply beat the policy?** Train `V(s)` on one night's data (the decision files already carry `observation` and
   `outcome`), then compare on held-out states: `Q_policy(s,a)` vs `V(apply(s,a))` — agreement, regret, and head-to-head
   games. Needs the apply step per decision type; `AgentDecisionService` already owns every apply call.
2. **Search potential with real rollouts** is now affordable for an offline study: 1000 states × 3 candidates × K=4
   full rollouts ≈ 105 s single-threaded (or ~16 s on 8 threads). Worth doing with the trained policy, not with random
   play.
3. **Data format v5** (top-k candidates, chosen-only value rows): the trainer needs ~1.7 KB/row instead of the current
   0.59 MB/game of padded candidates. Still open; it is I/O, not CPU.
4. **Variance for K>1 rollouts**: sampling with temperature > 0 is available today (`--temperature`); a fair hidden-info
   reshuffle is not.

Phase 1 conclusion (2026-11-13): search is feasible for deployment and, as 1-ply + `V`, for training. The immediate
lever is still data/behaviour (`H-P1`), and `V(s)` is needed for it anyway.

## Result 4 — 1-ply on a learned `V` beats the policy, but only as a re-ranker (2026-11-14)

Answer to open question 1. `V` is `ml/train_value.py` → `value_mlp` (AUC 0.856 vs-SMART data, 0.859 vs-RANDOM data),
inference is `MlpValueModel` (pure Java, self-verifying load). `OnePlySearch` applies each candidate card to a copy and
scores the afterstate; numbers, the full-vs-rank comparison, the shuffle control and the 10 000-game result are in
`docs/EXPERIMENTS.md` ("One-ply search on `V`").

Three findings that constrain the shape:

1. **Absolute is not comparable, ordering is.** `V(s)` vs `V(after build)` loses (84.5% of the decisions become "stop",
   60.6% vs RANDOM) because a post-build position sits off the training distribution of `V`; card against card works
   (87.08% RANDOM / 26.67% SMART at 3000 games) and a random permutation of the same candidates collapses to 15.28%
   vs SMART — the ordering, not the perturbation.
2. **~1.1 ms per decision** (4.3 candidates, copy + build + forward pass), ~46% of the choices change, and the change is
   worth **+1.6 points** on 10 000 games against both RANDOM and SMART (p≈0.001 / p≈0.011) — **+4.8 / +2.7 points** when
   the same afterstate is also read from the opponent's side (`V(me) − V(opp)`, 1.8 ms).
3. Extra decision types do not add signal: re-ranking `PERFORM_BLUE_ACTION` is neutral (26.35% vs 26.67% SMART).

## Result 5 — the draft is the second real gain (2026-11-14)

The search was extended to the decisions where the candidate is a card that **leaves the hand** (draft, discard, sale).
Applying such a candidate is one call (`hand.removeCard(id)`), and the draft phase is picked in half of all rounds, so it
was the cheapest place to look next. Measured at 10 000 games, frozen parent:

| decision types searched | vs SMART | vs RANDOM |
|---|---|---|
| `BUILD_CARD` | 29.27% | 87.42% |
| `BUILD_CARD,DRAFT_CARDS` | **31.39%** | **88.88%** |
| `BUILD_CARD,DISCARD_CARDS` | 28.48% | - |
| `BUILD_CARD,DRAFT_CARDS,DISCARD_CARDS` | 31.34% | 88.57% |
| `... + SELL_CARDS, SELL_CARDS_LAST_ROUND` | 24.90% | - |

**build + draft** is the deployment configuration: 31.39% / 88.88%, which is **+6.9 / +4.1 points over the policy alone**
(24.46% / 84.77%). Draft ranking works because "keep this card" is a pure hand change and the draft is what decides the
deck, so a better pick compounds for the rest of the game. Discarding is neutral. Selling is *harmful*: a card in hand is
worth what the policy plans to do with it, and V only knows the average plan.

Deployment flags (all off by default; nothing runs without them):

```
-Dares.agent.search=oneply
-Dares.agent.search.symmetric=true
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS
-Dares.agent.valueModel=D:/ares/runs/value/v_smart_it62
```

Cost at those settings: ~0.9 ms per searched decision, ~44 searched decisions per game (+40 ms of CPU per game), 64% of
the choices differ from the policy, 0 unusable candidates.

The same bug was found and closed here: an unimplemented decision type used to be scored **as a build** (a sell went
through the payment path and produced 4.7 million unusable candidates, 13.07% win rate). The dispatcher now returns "no
opinion" for any type it does not implement.
