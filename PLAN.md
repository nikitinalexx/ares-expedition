# PLAN — a player that can learn strategies, with a theoretical basis

Status: living document. Every claim below is either a measurement from this repository (with the command that produced it)
or an explicit assumption marked as such. Sections 1-3 are the evidence, 4-6 the design, 7-9 the roadmap with decision
rules, 10 the current state.

---

## 1. The goal and the hard constraints

**Goal.** A computer player that beats the heuristics (SMART) and then NETWORK, playing *fairly*: no hidden information and
no teacher other than the game itself. The target is not one strategy: it is a player that can *choose* among the styles a
position allows (economy build, blue actions, steel/titanium, plant/heat conversions, phase 1/2/5 loops) by planning,
because a benchmark that specialises in one of them is only beaten by a player that picks the right one for the position.

**Constraints (owner's rules).**
* SMART and NETWORK are benchmarks only: never a teacher, never an opponent in training, never a data source.
* Nothing hidden in the observation (opponent hand, deck order, dummy order); `ObservationCompletenessTest` guards it.
* No hand-written strategic heuristics in the player. A rule is acceptable only when the *rules of the game* make the move
  weakly better for every position (e.g. "an action that costs nothing and gains a card").
* Acceptance of a new player: first the behaviour profile (`ml/check_player.py`: cards per game, phases picked, phase-3
  spend; degenerate = <3 builds, >50% standard projects in phase 3, >80% of picks in one phase), then strength (10 000 games
  for effects below ~1.5 points; 3 000 games cannot resolve them).

## 2. What we measured (the evidence base)

| fact | number | command |
|---|---|---|
| our score against SMART | **37.5% ± 1** (best 37.8% on 2 000) | `--mode compare` |
| against RANDOM / NETWORK | ~93% / ~8-9% | same |
| score decomposition (agent vs SMART) | Tr 37.8/31.2, forests 13.9/8.9, **cards 3.5/12.8**, milestones+awards 11.8/17.6, total 71.5/76.4 | `--mode scorecard` |
| cards on the table | **13.2 vs 27.4**; printed points 3.2 vs 13.0 | same |
| inside a game, the bigger card table wins | **3 197 vs 1 569** decisive games | `ml/game_length.py` |
| what the final margin correlates with | money production +0.47, cards +0.25, game length -0.12 | same |
| critic's resolution | +3 printed points move V by **0.008** (ten milestone points: 0.19) | `ml/value_sensitivity.py` |
| critic on an engine | +4 activatable blue cards **lower** V by 0.50 logit; pressing them +0.30 | `--mode engineaudit` |
| the forced engine profile (build everything, press every blue action) | **57.0 points against 71.5** for normal play | `--mode play` + `ml/game_length.py` |
| paired plan test: "press every blue action, build non-green", 8 turns, n=500 | **-1.2 ± 1.2** points | `--mode macroaudit --macro free` |
| paired plan test: "build income cards, press free actions", 8 turns, n=500 | **+1.7 ± 1.4** points | `--mode macroaudit --macro income` |
| policy training (CE / PPO, four nights, 12-40k games per iteration) | plateau 37-40% | `ml/nightly.py`, `docs/EXPERIMENTS.md` |
| phase choice decided by a one-ply value search | collapse: 0.3% / 32.8% / 3.8% against SMART | `docs/EXPERIMENTS.md` |
| phase choice decided by a round playout | 65.9 points, 30% (below the 71.5 / 37% baseline) | `ml/check_player.py --phase-playout` |

## 3. Why the obvious approaches cannot work here (theory, confirmed by the table above)

1. **An improvement operator can only learn what it can evaluate.** A strategy is a conjunction of decisions with a
   delayed payoff (build economy cards now -> call phase 3 -> activate -> income -> cards -> points). The payoff of one
   decision in the chain is 1-3 points against a game noise of sd ~17, and it only appears ten rounds later. Outcome-driven
   policy gradient therefore sees a signal far below its noise floor, and the conjunction (build AND phase AND press) is
   never reinforced coherently. Four nights of PPO: plateau (measured).
2. **A value net cannot interpolate over a hole.** If no recorded game contains a running engine, V has never seen one, so
   V prices it at nothing - measured twice (`ml/value_sensitivity.py`, `--mode engineaudit`).
3. **Forcing the plan inside the player is worse than useless.** The forced engine profile scores 57.0 against 71.5: the
   plan is executed by a policy that does not understand it. Training on those games would teach "engine = bad".
4. **A one-ply comparison of "phase openings" is structurally biased.** Phase 1/2 hand over a build slot immediately, while
   income and the draft pay only when the phase is played, so the comparison prefers what pays first (measured: collapse).
5. **Resolution.** 3 points = 0.17 sigma of the score feature and ~0.005 of win probability: below V's error on a single
   state. Such differences can only be resolved by *paired* comparisons on the same state with common random numbers.

So the question is not "which loss function", it is "which operator can evaluate a plan the player does not execute".

## 4. The operator that does have a theoretical basis

**The game itself, used as an experiment.** The engine is an exact simulator with a legal hidden-information
determinization; we can execute a plan or a move on a copy, let the game finish, and read the actual points. That gives:

* an **unbiased** evaluation of any candidate action (no critic error, no resolution problem),
* **paired** comparisons (same deck order, same dummy, same opponent) which cancel the ~17-point game noise to the part
  that the candidate actually causes.

This is the same principle that makes strong chess engines work (search as the improvement operator, applied to a model of
the game), with two differences that force our design:

| chess (AlphaZero-style) | here |
|---|---|
| perfect information | hidden deck order and opponent hand -> **determinization** (our engine supports it: `RolloutBench` confirms a copy resumes at the same decision with the same candidate ids) |
| cheap terminal state, dense value | final points are 20-60 turns away -> rollouts are the honest evaluator, a value net is the cheap surrogate |
| 10^6-10^7 self-play games | one machine, ~10^4-10^5 games per night -> the search cannot be run on every move; it must be **sampled** and **distilled** |

The loop that follows from this, and that this plan commits to:

```
            +---------------------------------------------------------------+
            |                                                               |
   policy  -->  data: self-play games (policy + a validated plan)  -->  value V(s) trained on the outcomes
      ^                                                                   |
      |                                                                   v
      +------  distillation (CE to the search's choice)  <--  search: paired rollouts over candidates
```

Every arrow has a measurement (section 6), and every stage has a falsifier (section 7).

## 5. The components we build on (what already exists)

* **Simulator**: the real engine, exact copy constructor, determinization, `SimulationProcessorService`, ~0.5-1 s per full
  game from a mid-game position.
* **Encoder** (780 values): both tables, tags, resources, incomes, the engine's own score per player as ONE number, phase
  context, hand; audited (`ObservationCompletenessTest`, `ml/feature_audit.py`).
* **Policy**: 14 decision heads (candidate cross-entropy, warm start, exportable to pure Java).
* **Value**: `MlpValueModel` (margin or outcome target), running in the app without CUDA.
* **Search**: 1-ply value search (ranks builds and drafts), pairs (two builds deep), phase afterstate via the engine's own
  transition, and **rollout** evaluation (`macroaudit`, `rollouttest`).
* **Data**: self-play recorder with per-decision rows, outcomes, afterstates (optional), ranking sidecar.
* **Instruments**: `--mode scorecard` (points per term per side), `ml/check_player.py` (behaviour profile), `--mode sanity`
  (obvious-move bench, gate + red flags), `ml/value_sensitivity.py`, `ml/card_advantage.py`, `ml/game_length.py`,
  `--mode engineaudit`, `--mode macroaudit`, `--mode rollouttest`.
* **Pipeline**: `ml/nightly.py` (self-play -> train -> eval -> sanity gate -> plateau guard; benchmarks only for eval).

## 6. The search operator, precisely

**Decision-level rollout search** (the next implementation step):

```
evaluate(state, decision):
    for each candidate a in decision.candidates:        # 2..10, typically 4
        for sample s in 1..K:                           # K = 1..4 determinizations
            copy = determinize(state, seed + s)         # same seed for every candidate: common random numbers
            apply(a) on copy                            # by the engine's own transition
            finish(copy) with the policy                 # to the end of the game
            margin[a] += points(me) - points(opponent)
    choose argmax margin
```

* **Unbiased** (the game decides), **paired** (the candidate is the only difference), **determinized** (hidden information
  is guessed, not read).
* **Cost**: (candidates x samples) full games. At ~0.7 s per game and 4 candidates x 2 samples = ~6 s per decision. With
  ~12 build/phase decisions per game this is ~1-2 minutes per game: affordable for **labelling** a few hundred games per
  night, not for playing.
* **Where it is applied**: the decisions where strategy lives - `PICK_PHASE`, `BUILD_CARD` (which card, or stop), and
  act-vs-stop in `PERFORM_BLUE_ACTION` / `STANDARD_ACTION`.

**Distillation**: the labelled games give (state, search's chosen candidate) pairs; the policy heads are trained on them
(candidate cross-entropy), then the fast player plays, generating new games for the value. This is policy iteration with
the search as the improvement operator - the only operator we measured to have the right properties.

## 7. Roadmap with decision rules

Each stage is one or two nights. The decision rule is written before the run.

### Stage A - does a rollout beat the policy at the same decision?

**First measurement, and why it is not yet evidence.** 40 positions, `BUILD_CARD`, one rollout per candidate: the best by
rollout scored +7.9 against -4.5 for the policy's choice, a difference of **+12.3 ± 8.1**. That number is the *winner's
curse*: the candidate was selected on the same noisy rollouts that measured it, and the maximum of four estimates with a
noise of ~17 points is biased upward by roughly 15-20 points. It cannot be quoted as an improvement.

The honest version selects on one **determinization** of the hidden cards and evaluates on a different one
(`services/agent/Determinizer` is now shared by the search and the audits). That is the measurement Stage A goes by.

**Resolved, and negative.** 60 positions, `BUILD_CARD`, selection on determinization A and evaluation on determinization B,
against the deployed player (policy + 1-ply search + pairs):

| | margin |
|---|---|
| the policy's choice | +8.3 |
| the best by one rollout per candidate | +5.1 |
| **difference (out of sample)** | **-3.3 ± 6.5** (24 positions better with the rollout, 35 with the policy) |

So the in-sample +12.3 was the winner's curse in full, and a rollout per candidate **does not** beat the policy at our
compute: with a per-rollout noise of ~17 points and 4 candidates, a single sample per candidate is a choice made of noise.
Making it informative needs 10-20 samples per candidate (~40 s per decision, ~8 minutes per game), which does not scale to
the thousands of games training needs.

**Decision rule applied: branch 1 of the falsifiers.** Search is not the improvement operator at this compute. The plan
continues on the two things that measured positive: (a) *validated plans* distilled into the policy (Stage D: the income
preference is +1.7 ± 1.4 causally and moved the player from 36.5% to 37.8%), and (b) scale of the objective that already
works. Search at play time stays as it is (1-ply + pairs, which is measured positive), and its further growth is out of
scope until the compute allows many samples per candidate.

`--mode rollouttest --type BUILD_CARD --games 60` and the same for `PERFORM_BLUE_ACTION`, `PICK_PHASE`.
* measures: rollout's best candidate vs the policy's choice, paired, on the same positions.
* **go** if the paired difference is positive with a CI excluding zero. **Stop the whole plan** if it is zero or negative:
  then planning cannot improve this player and the answer is "make the policy stronger first" (data scale / architecture),
  which is a different plan.

### Stage B - does distillation carry the search's edge into the fast player?
Label 200-300 games with Stage A's search (phase and build decisions), train the heads, evaluate: agreement with the
search on held-out decisions, behaviour profile, 10 000 games vs SMART.
* **go** if the fast player keeps at least half of the search's measured edge. Otherwise the distillation is too lossy
  (or the search's edge is state-specific), and the loop cannot be closed at our scale.

### Stage C - the loop
Nightly: self-play with the distilled policy (a share of moves labelled by the search), value trained on the same games,
distillation each iteration, sanity gate, evaluation against RANDOM and SMART, benchmark NETWORK at the end.
* success: monotone improvement of the 10 000-game SMART win rate over ~10 iterations; the profile must move (cards,
  phases) before the win rate does.

### Stage D - the validated plan in the generator
The paired-plan audit is the *arbiter of plans*: any plan (an economy build preference, a phase preference, an activation
policy) is admitted only after a paired audit at n>=500 shows a positive effect. Admitted plans are used to generate data
(the plan is a teacher made of the game itself) and distilled. Measured so far: income preference +1.7 ± 1.4 (admitted),
press-every-blue-action -1.2 ± 1.2 (rejected).

**The limit of this stage, stated by the owner and accepted.** The audits can only judge plans somebody *names* - and the
names come from the owner's and my knowledge of the game, not from the game. If this game has twenty styles, this mechanism
finds the four we thought of. A player that is supposed to discover the space itself needs a mechanism that *generates*
diversity, not a list of it.

### Stage E - latent plans: the styles are found, not listed
Why this has a theoretical basis: a style is a basin in policy space, locally optimal against the population it meets. It
cannot be derived from the rules - only found by exploring policies and keeping the ones the game rewards. The standard
mechanism for that in a single-agent game with no teacher is a **latent variable**: one code per game that the policy
conditions on, sampled randomly during data generation, with the outcome deciding which codes were good
(DIAYN / "diversity is all you need", and the population-based idea behind AlphaStar's league in a cheap form).

What is implemented (`services/agent/PlanCodes.java`, the observation's last four values):
* `Player.planCode`, four bits, assigned per computer seat at game creation (`-Dares.agent.planRandom=true`);
* the four bits are **private** observation values (zeroed in `buildPublic`, like the hand), so the search and the opponent
  never see them;
* while **generating data** (`-Dares.agent.planNudges=true`) the bits switch the generator's exploration profile: 1 prefer
  income cards, 2 prefer non-green builds, 4 press every blue action, 8 prefer the build phases. These are *coverage*
  nudges, not claims: they exist so that the recorded games contain behaviours the current policy never produces (it picks
  phase 4 or 5 in 100% of the rounds, so without a nudge no game of it can say what calling phase 1 or 2 is worth);
* the trained policy is π(a|s,z): it can play a different style per code, and the outcomes decide which codes win;
* at play time the code is a knob: random (diverse player), fixed (`-Dares.agent.planCode=N`, the best measured style), or
  chosen by the value (only if the critic ever shows it can resolve the difference - it could not, section 3.5).

**The measurement that decides whether this works** is divergence, not the win rate: `ml/plan_report.py` splits a run's
decisions by the code of the player that made them and reports phases, builds, activations, spends, win rate and margin per
code, plus a chi-square "does the code predict the phase". It is run on a **play** run of the trained model with codes
assigned and nudges OFF - otherwise it would measure the generator instead of the policy.

First control, same model `r22_plan` (bits at zero weight), 400 games: builds 7.7-9.0 per code (spread 1.4, i.e. noise),
phases 49% income / 51% draft for every code, chi-square 0 - **no divergence when the nudges are off**, as it must be.
With the nudges on, the *generator* diverges strongly (builds 8.3-14.0, blue actions 0.7-8.5 per game), which is what makes
the divergence of the trained policy a real question rather than a tautology.

Falsifier for Stage E: after training on nudged data, a play run with nudges off shows no divergence (chi-square at chance
and a builds spread below ~2) -> the code carries nothing, the policy ignored it, and the honest conclusion is that a latent
channel at this data scale cannot hold a style. Next step then is scale (games per iteration), not more bits.

**First result (CE, 3 epochs, 6 000 nudged games, r23_plan_ce, 1 200 played games).** The channel holds *something*:
chi-square on the phase distribution across the 16 codes is **199 (dof 45, p = 2e-20)**, and codes carrying the build-phase
bit call a build phase 28% of the time against 21% for code 0. It is much weaker than the generator (where bit-8 codes
called them ~50% of the rounds and the others never did), and the build head shows **no** divergence at all (spread 1.4,
i.e. noise, exactly as in the control).

Two readings, and they matter for the next step:
1. the phone is real but slow - three epochs at lr 3e-4 from zero-initialised bits is not enough capacity-in-use, and the
   test of that is 10 epochs on the same data;
2. the policy took a *shortcut*: the nudged phase decision was conditional on holding a buildable card (that is a visible
   feature), so 21% of code-0 rounds call a build phase - the model learned part of the condition and only part of the code.
   A shortcut is not a style: it would hurt the baseline style while pretending to add a dimension.

So the go/no-go for Stage E is not the significance, it is the *magnitude*: the code must move the behaviour by roughly what
the nudge moved it (build acceptance 0.60 -> 0.93, build phases 0% -> 50%), otherwise there is nothing for an outcome to
select and nothing to deploy.

**Second result (same data, 10 epochs, r24_plan_ce10, 1 200 played games).** More training moves the channel from
"detectable" to "usable":

| | code 0 (no bits) | codes with the build-phase bit | generator (for scale) |
|---|---|---|---|
| phase 1 share | 15-17% | **29-34%** | 0% vs ~50% |
| phases 4/5 share | 43% / 41% | 33-36% / 32% | 50/50 vs 49/51 |
| cards built per game | 12.5-13.6 | 13.9-**14.9** | 8.3-9.0 vs 12.3-14.0 |

chi-square on the phase distribution across the 16 codes is now **2 367 (dof 45)**, and the build spread 2.5 cards. The
channel therefore carries a real, reproducible difference in behaviour: with the nudges off, the policy plays a different
style when the code says so.

One caveat the numbers state plainly: code 0 calls a build phase in 16% of the rounds where the pre-training policy called it
in 0%. Part of what ten epochs learned is the *context* half of the nudge ("holding a green card -> phase 1"), which fires
regardless of the code. So the baseline style moved too, and the first check is whether it moved for the better or worse
(`-Dares.agent.planCode=0` against SMART, 2 000 games, against the 37.8% of r21).

## 8. Falsifiers for the whole approach

1. Stage A says rollouts do not beat the policy -> planning is not an operator for this player. Next plan: scale/architecture
   of the policy itself (more data, wider nets, longer training), with the same acceptance discipline.
2. Stage B says distillation loses the edge -> the fast player cannot carry search results; the loop must be replaced by
   deploying the slow planner (acceptable for a human opponent, too slow for training).
3. Every admitted plan at n>=500 is neutral or negative -> with our execution quality no plan pays; the honest conclusion is
   that the gap is general play strength, not "missing a strategy", and the plan becomes purely "scale the policy and the
   value with the objective that already works".
4. The nightly loop plateaus at the same 37-40% with the search inside -> the search's edge is too small at our scale; then
   the budget goes to more games per iteration (nightly cost is ~2 minutes per 12 000 games, so a 10x-100x scale-up is a
   matter of hours, not architecture).

## 9. Compute budget (one machine, one RTX 3070 Ti, 16 cores)

| activity | cost |
|---|---|
| self-play game (policy + 1-ply search) | ~10-20 ms |
| full rollout from a mid-game position | ~0.5-0.7 s |
| one search decision (4 candidates, 2 samples) | ~5-6 s |
| labelling 250 games (~12 strategic decisions each) | ~4-5 h (one night) |
| value training (12 000 games, streamed) | ~5 min |
| policy training (12 chunks x 14 types, 3 epochs) | ~2-4 min |
| 10 000-game evaluation against SMART | ~20-30 min |

## 10. Current state and the immediate next actions

Implemented: everything in section 5, including the rollout and macro audits and the distillation trainer
(`--relabel`, `--card-weights`, CE to label, PPO), the latent plan (section, Stage E) and its instrument
(`ml/plan_report.py`). Measured today: the paired macro audit (section 2), the engine audit, the first positive plan
(income +1.7), its distillation (36.5% -> 37.8% vs SMART on 2 000 games), Stage A resolved negative (section 7), and the
no-divergence control of the latent channel.

Next actions, in order:

1. Train on nudged latent-plan data (`ml/plan_config.json`) and run the divergence test of Stage E. Decision rule: if the
   trained policy diverges per code, styles exist and can be selected (fixed best code -> a play-time choice by V -> maybe a
   league); if it does not, the channel needs more data, not more bits.
2. Measure every divergent code against SMART (`-Dares.agent.planCode=N`, 2 000 games each), so that the adopted style is a
   measurement, not a preference. Guard: each code must pass the sanity bench and the profile check before it is compared.
3. Keep the sanity bench as the gate: a model that refuses a free card cannot be promoted, whatever its win rate.
4. Keep generating data with the admitted income plan in the meantime (Stage D) - it is still the only plan that passed the
   audit, and it is the data the latent plan is trained on top of.

## 11. Decision log

**2026-11-15, owner's answer to "should the player be allowed a hand-written decision skeleton?"** No: the rule stands (no
hand-written strategic heuristics in the player), and the subject of the work becomes the **value function**. The reason
this matters: the strong benchmark in this repository (NETWORK) is a learned value *inside a hand-written decision skeleton*
(~570 lines of "when is phase 1/2/3/4/5 worth calling"); it is a ranking problem inside a strategy, which is why it learned
in one epoch. Our player is denied the skeleton, so it is solving the harder problem - searching for the strategy itself -
and that is where the plateau at 37-40% comes from. Keeping the rule means the honest question is whether a value function
alone, used greedily on every decision, is strong enough to be a player.

**The decisive experiment (running).** A critic `V(s)` trained on random-play games - broad state coverage, an allowed
training environment (RANDOM), no policy imitation - used greedily on every decision the search can apply
(`BUILD_CARD`, `STANDARD_ACTION`, `PERFORM_BLUE_ACTION`, `PICK_PHASE`, and the give-up-card menus), with `fullTypes` so V
also decides act-vs-stop:

| variant | what it tests |
|---|---|
| policy only (no search) | the reference: what imitation alone gives |
| V-greedy with `v_rand_outcome` / `v_rand_margin` | can a critic trained on random play be the player? |
| V-greedy with `v_r15_plan` (the policy-trained critic) | is the gap the *operator* or the *critic*? |

Decision rules, written before the run:
* any V-greedy variant above ~40% -> the operator and the critic work; the next step is policy iteration on top of it
  (play with V-greedy, train a critic on those games, repeat) and the policy heads become a fast prior, not the player;
* all variants below the policy -> the critic is the binding constraint even at full decision coverage; then the next work
  is the critic itself (coverage, target, resolution, features), not the policy, and the sanity bench (`--mode sanity`)
  says in which obvious positions it still fails.

### 11.1 What the experiment showed (measured, 2026-11-15)

Four configs, 1 000 games against SMART, same policy bundle `r22_plan` in every one:

| config | vs SMART |
|---|---|
| policy only (no search anywhere) | 35.6% |
| **rank inside BUILD_CARD/DRAFT_CARDS, critic trained on 20 000 games of mixed exploration, margin target** | **39.45%** [36.5, 42.5] |
| the same, critic from the old pipeline (`v_r15_plan`) | 37.8% (2 000 games, historical) |
| V decides act-vs-stop on BUILD_CARD as well (same new critic) | 36.0% |
| V decides everything on every menu (critic from uniform random play) | 0.1-0.6% |

Two conclusions, both useful:

1. **The critic's training distribution is the lever, not the operator.** The same ranking operator gained ~1.6 points from
   a critic trained on games with broad action coverage (build 50%, draft 30%, standard 20%, blue 20%, phase 20% exploration)
   instead of on policy play only. That is the first improvement measured in a while that came from the *critic* rather than
   from a new mechanism.
2. **A state value cannot decide act-vs-stop.** A build costs money now and pays ten rounds later, so V(after) < V(before)
   for any expensive action: measured 0.45 cards per game and a 1.2% build acceptance, and even with the better critic the
   act-vs-stop config is 3.4 points *below* the ranking config. This is not a critic-quality problem, it is a comparison
   problem, and it explains the earlier -13 points and -8 points results in one stroke. Fixing it needs the action's
   delayed effect inside the comparison: a projection of the phase/action (what NETWORK does) or a horizon playout, not a
   one-step value.

**Critic scaling (the first positive result of the day).** Same policy, same mechanism, only the critic: 37.8% (old critic, policy-play data) -> 38.5% +/- 0.6 (20 000 exploration games, margin target) -> **41.18% +/- 0.9** (100 000 games, 512x256, 10 000-game confirmation; 40.36% was the 6 000-game reading). Monotone in data, one extra V call per candidate, no change in deployment cost. The critic is the lever and it has a scaling curve; the mechanism does not need to change.

So the division of labour that the measurements support is: **the policy decides whether, the critic decides which**, and
the way to a stronger player is a better critic and a broader *ranked* menu - not letting the critic run the game.

### 11.2 The horizon fix for act-vs-stop: rejected (and by the same law)

`-Dares.agent.search.horizonStop=true -Dares.agent.search.horizon=2` scores the "stop" candidate by playing the same two
rounds of the policy that an acting candidate gets, so the delayed payoff is inside the window (the diagnosis of 11.1).

| config | vs SMART |
|---|---|
| rank + `v_mix2_margin`, policy decides stop | 38.5% +/- 0.6 (6 000 games) |
| the same, with the stop scored over a 2-round horizon | **19.5%** (400 games, +/- 4) |

Rejected, and for the reason Stage A already measured: a two-round playout is a *rollout*, its game noise is ~17 points,
and choosing among ten candidates by one rollout each is choosing noise (out of sample the rollout lost -3.3 +/- 6.5). The
horizon does not add information faster than it adds variance; making it informative needs 10-20 samples per candidate,
which is 8 minutes per game.

So the branch "value decides act-vs-stop" is closed by two independent routes (a one-step comparison is biased, a rollout
comparison is noise), and the division of labour measured in 11.1 stands: **policy decides whether, critic decides which**.
