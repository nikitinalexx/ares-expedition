# The `policy_ai` branch: what it did, and why it kept diversity but not strength

Read on 2026-11-15 at the owner's suggestion, as a diagnostic. Facts are from the branch itself
(`git show policy_ai:<path>`); the interpretation is checked against the measurements of the current branch (see
`docs/EXPERIMENTS.md`, 2026-11-15).

## What is in it

* **A factored action space.** `services/policyai/action`: `ActionHead` (HEAD_SELECTION, BUILD, SELL, SYSTEM, BLUE, …),
  one head per family of action, a legal-action mask per head, and 268 possible card actions. The policy is sampled twice
  per decision: first the head, then the action inside it.
* **Explicit exploration noise.** `PolicyActionSelector.chooseBestAction`: temperature on the head (0.9) and on the action
  (0.85), Gumbel noise (0.15) on both, Dirichlet mix (alpha 0.5, 10% of decisions) on the card distribution.
* **A per-action delta as the learning signal.** Every decision stores `policyDelta`; `StateTransitionService` and
  `GameController` compute it as `PolicyLogitDeltaHelper.calculateLogitAdvantage(predictionBefore, predictionAfter)` between
  *consecutive* predictions and propagate it backwards through the game (`isBackwardPropagationStop`, `finalizedDelta`).
* **Snapshots for expensive decisions.** `PolicyRecord.marsReference` keeps a `MarsGame` copy for builds, standard
  projects, blue actions and sell/helion mode, i.e. the positions where a later evaluation is worth the memory.
* **A dataset layer.** `PolicyDatasetWriter(2)`, `PolicyBinaryCodec(2)`, `PolicyReplayBuffer`, legal-action/legal-head
  masks, `PolicyRecordAnalyzer` + `ValidationReport`.
* **MCTS.** `PolicyMctsService` exists with a phase-selection search (batched, virtual threads, `TAU`, visit counts), but the
  `runMctsPhaseSelection` body is commented out - the deployed path does not search.

## Why it behaved the way the owner describes

1. **The diversity was the sampling, not a learned preference.** Temperature + Gumbel noise + a 10% Dirichlet mix force the
   player to try different heads and cards on every decision. Nothing in the loop had to *learn* that a mix is good, so
   nothing could collapse it either - which is exactly the observed "it did not fall into phases 4-5".
2. **The learning signal was a difference between adjacent predictions.** A delta of that kind is dominated by (a) the
   model's own error on two neighbouring states and (b) the *immediate* resource change: money out looks worse at once,
   money in looks better at once. The same bias, measured on the current branch's critic, is: **0.45 cards per game and a
   1.2% build acceptance** when a state value decides act-vs-stop (the position after a build is poorer until the card pays,
   ten rounds later). The mirror image of "never builds" is "sells a lot": selling is the action whose delta is immediately
   positive. Both symptoms the owner reports follow from the same sign of bias.
3. **Hundreds of thousands of games cannot fix a biased target.** The amount of data reduces variance, not bias: if the
   target says "build is bad", more games say it more precisely. That is also why the current branch's PPO plateaued at
   37-40% with game outcomes as the target - the effect of one decision is 1-3 points against a game noise of ~17.

## What is worth keeping from it

* **Legal-action masks per head.** Already equivalent to our candidate sets; nothing to port.
* **Snapshots of expensive decisions (`marsReference`).** Same idea as our afterstate stream; useful only if the evaluation
  of those positions is unbiased, which is the open problem, not the storage.
* **Effect projections.** The branch has `rollout/*Effect` classes (phase upgrades, `ViralEnhancers*`, …): engine-level
  projections of what an action *does*. This is the one part that matches the diagnosis of the current branch: a phase pick
  cannot be judged by its afterstate (the income of phase 4, the cards of phase 5 and the discount of phase 1 are simply not
  in it), so the value has to read a *projected* state. Porting those projections onto our critic is a live option.
* **MCTS**: not at this compute. The measured cost of an honest rollout decision is ~5-6 s (4 candidates x 2 samples) and
  one rollout per candidate measured as noise (-3.3 +/- 6.5 points out of sample).
