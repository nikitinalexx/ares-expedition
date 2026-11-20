# PLAN V4 — the theory of change: break the fixed point by separating conversion from adversarial play

Written 20/11 at the owner's instruction: stop testing theories one by one and state a plan that is theoretically capable of
working, with the reason it can work and the reason each earlier attempt could not. Everything below is either a measurement
already in `RESULTS.md` or an explicit hypothesis with a gate that can kill it cheaply.

## 1. The object

We maximise one number: the final score difference against a strong opponent, at a fixed compute budget. Nothing else is a goal -
not AUC, not cards per game, not the phase mix. Those are instruments that tell us whether the number is being chased for the right
reason.

## 2. Why the loop cannot leave 73.17% (the fixed point, stated exactly)

Let `π_k` be the policy, `V_k(s) = E[u | s, a ~ π_k from s]` the value, `u` the final score difference, and `D_k` the corpus of games
`π_k` played against itself. Our loop is:

```
V_k    = regress u on the states of D_k
search = argmax_a  V_k(afterstate(s, a))      (+ noise, determinisations, deeper lines)
π_k+1  = imitate the search's choices
D_k+1  = games of π_k+1 vs π_k+1
```

Three facts, all measured, and together they prove the loop is closed at the current point:

1. `V_k` is an estimate of `π_k`'s futures. It is not an estimate of the position; it is an estimate of the position *as our player
   plays it onwards* (the owner's two examples: 5 steel + 5 titanium income, and AssetLiquidation with TR 0 and many green cards).
   The features are not blind - V prices a 200 MC stockpile at +2.92 logits - the conditioning is the problem.
2. The search's ranking of an action is `V_k` of its afterstate. So the search cannot prefer an action whose value is only realised by
   a continuation `π_k` does not play: it scores it as "our player, in this position, later" - which is bad. Hence search improves the
   choice *within* the current class of plans and cannot leave it. Measured five ways: 2-round horizon -8, 8 determinisations -6.3,
   build playout 0, input playout -1.5, the line tree -1.5, all at or below 73.17%.
3. `D_k` contains only states `π_k` reaches, and the label of a state is the outcome of `π_k`'s continuation. So the corpus cannot
   teach the value of a plan our player does not execute: the exploring games, when forced, are weak (44.25%, and their corpus trains
   25-33%), which the critic honestly learns as "this plan loses".

**Corollary (this is what closes the last month of work).** Better resolution (TD, more data, wider nets, thrift instrument), more
depth (rounds, determinisations, trees) and more compute inside the current loop are *amplifiers*, not escapes: they make the same
conditional value more precise. 10 000 games of the same policy is 10 000 games of the same plan; a tree sums the same bias.

## 3. The precondition for any escape

To learn that a plan is good we need data in which that plan was played **competently**; competent play cannot come from `π_k` (by
§2.2) and may not come from another player (owner's rule: no imitation of SMART, NETWORK or human games, no distillation). So the
competence must be *learned*, and the only place it can be learned is a task whose optimum is reachable by our own improvement
operator. Enumerating the possibilities:

| source of competence | status |
|---|---|
| deeper search (see the payoff, 10-15 rounds ahead) | infeasible: ~500 decisions a game, 40-wide menus |
| another agent as teacher | forbidden by the owner's rule, and it would import a style, not our strength |
| hand-written rules ("buy production first") | forbidden: measured to produce 44.25% and a corpus that trains 25-33% |
| **a reduced game where the missing skill is learnable without an adversary** | **untested, and the only one left** |

## 4. The mechanism: a sandbox curriculum (conversion without denial)

**Definition, precisely.** The same engine, the same rules, the same clock (rounds and terraforming end conditions), the same card
pool, the same scoring - with a **non-adaptive opponent** (RANDOM is already supported: `SelfPlayMain --a AGENT --b RANDOM`). The
objective is the score **margin**, not the win bit. No human, SMART or NETWORK game is used anywhere.

**Why the fixed point does not exist there.** In the full game, deviating towards an economy is *punished* by an adaptive opponent:
you lose the terraforming race while your income is still small. That punishment is real, and it is why the exploring games were bad
and the critic was right to call them bad. In the sandbox there is no adaptive punishment: a deviation shows its own consequence,
and the credit for *converting* resources into points is clean, because nobody denies the conversion. That makes the task a
single-agent MDP with a stationary environment - the setting in which policy improvement is known to work, and the exact skill the
owner's examples are about: **what a position is worth if it is converted**.

**What the sandbox preserves, and what it does not.** It preserves the clock (a slow player still runs out of rounds), the card pool
(scarcity of cards, tiers, drafts), the rules of conversion (money -> cards and standard projects, steel/titanium -> builds, plants
-> greeneries, heat -> temperature, microbes/animals -> points) and the game's own scoring. It does not preserve adversarial denial:
nobody takes the card or the tile you need on purpose. That is the known limitation, and it is exactly why the sandbox is a
*curriculum*, not the final player.

## 4b. The label-maker inside the curriculum: the lambda-return (owner's suggestion, 20/11)

The owner's description - the terminal result propagated backwards, `1`, then `1 - delta`, then `1 - delta - delta` - is TD(lambda):
the lambda-return, i.e. the eligibility-trace view of the same idea. Its standard form is **geometric**, not linear, and in our game
(with the score visible at every row and rewards almost always zero) it collapses to something very clean:

```
G^lambda(t) = (1 - lambda) * SUM_{n < D} lambda^(n-1) * V(s(t+n))  +  lambda^(D-1) * (final margin)
D = number of decision rows from t to the end of the game
```

So the terminal outcome enters row `t` with weight `lambda^D`, and the bootstrap fills the near term. `lambda = 1` is the pure final
margin (today's target), `lambda = 0` is one step plus a bootstrap (`--td-bootstrap`), and everything in between is the blend the
owner is describing. It is computed in ONE backward pass per game (`G_t = r + (1 - lambda) V(s_{t+1}) + lambda G_{t+1}`, gamma = 1 in an
undiscounted game), and the pack already links rows to the next row of the same game.

**The quantitative point that decides its value here.** A game is ~150 decision rows, so "the terminal signal reaches the start"
requires `lambda^150` to be non-trivial: lambda = 0.99 gives 22%, lambda = 0.995 gives 47%, lambda = 0.9 gives 0.0000001. The values
used in RL benchmarks (0.9-0.95) correspond to episodes of tens of steps, not 150. In other words: *propagating the result to the
beginning means lambda close to 1, which means nearly Monte Carlo, which means nearly the variance the owner objected to.* The tradeoff
curve is not free, and its two endpoints are already measured here: pure margin 73.17% and 3-step TD 72.50%. A blend of two
estimators that are both ~73% is still ~73% - **it is an estimator change, not a capability.**

**What it does fix, and it is worth having:** the two objections the owner raised are about opposite ends of this curve (a biased V is
smooth but wrong; the final score is right but a 90 -> 180 jump noisy). TD(lambda) is precisely the dial between them, and inside the
sandbox curriculum (section 5) the dial is genuinely useful, because there the bootstrap is trained on the same distribution and the
label has to reach back ~150 rows without paying full Monte-Carlo variance. So it goes into the plan as the **label-maker of G1**, not
as the escape from the fixed point: it cannot create competence, and only competence changes what V can see.

## 5. The causal chain to the real game (why this can move 73.17%)

1. **Sandbox iteration** produces a policy `π_s` that executes conversions well (it is the only thing it is rewarded for, and it is
   not punished for investing).
2. `π_s` is used as the **continuation** in the plan-level end-of-game judge, and as the **initialisation** of the real-game loop. The
   plan-level judge no longer says "production loses" for the trivial reason that its continuation cannot convert; that was the
   precondition named in PLAN_V3 §S1'.
3. The real-game corpus now contains production plans **played competently**, labelled by the real game's own outcome. `V` finally
   sees those states as wins, so the delayed value becomes visible to the search, and the plan preference can shift.
4. Only then do the ordinary levers (search depth, data) become amplifiers of a *better* value instead of a more precise biased one.

This is the only ordering in which each step's precondition is met by the previous step. It also answers the owner's objection to
the round playout: the horizon is the whole game, and the variance of the judge is handled by averaging over states and by the
unbiased label argument (PLAN_V3 §S1', RESULTS "Pairing does not survive the first divergence").

## 6. Gates — each one cheap and able to kill the plan

| gate | measurement | rule |
|---|---|---|
| **G1. Does the game reward conversion at all?** | sandbox: own score per game, builds per game, production at round 5/10, and the share of points from card VP vs TR, over ~5 000 games vs RANDOM (one iteration) | if the score does not rise clearly above the current player's own score in the same setting, the sandbox teaches nothing and the plan dies here (and it dies cheap: ~1.5 h of generation) |
| **G2. Did the bias move?** | the owner's probe: V on a production position (5 steel + 5 titanium income, money spent) against a blue-points position, before and after `π_s` | if the gap does not shrink, the sandbox competence does not transfer to V, and step 3 of the chain is false |
| **G3. Does the real loop improve its plan?** | real-game iteration initialised from `π_s`: phase mix, builds, acceptance, and the plan-level judge's preference | the profile gate first (it catches "learned not to build"); then the mix must move towards the plans the judge endorses |
| **G4. Does it beat the champion?** | 1 000 games vs SMART, then NETWORK, then the duel | not above 73.17% -> closed; above and < 1.5 points -> 10 000 games, the usual rule |

**Cost of G1** (the decisive one): ~5 000 sandbox games at ~1 game/s on 14 threads is ~1.5 h, plus one critic/policy pass on the
packed corpus (~10 min each). It is the same pipeline we already run; no new infrastructure.

## 7. What this plan does not promise, and how it would fail

- **Denial may outweigh conversion.** A player that converts well but loses the terraforming race to SMART can still be below 73.17%.
  Caught at G4, and it is informative: it would say the cap is adversarial play, not the delayed value, which is a different project.
- **The sandbox optimum may be rushing, not building.** Then G1 shows points rising without production rising, and the plan is dead
  with a number.
- **The transfer may not happen.** Then G2 stays flat while G1 rose: the competence exists but does not reach V. That would point at
  the observer/label interface, not at the theory.
- **Night 7 already trained against RANDOM and stalled at ~68 points.** That is a real precedent against this plan, and the
  difference must be stated: night 7 used a win/loss objective with a saturated reward (the "68-point ceiling" diagnosis in
  `HANDOFF.md`), no critic, no margin and no search. The sandbox proposed here optimises the margin with the current value/policy
  infrastructure. If G1 does not separate the two, the precedent wins and the plan is closed.

## 8. What is deliberately not in this plan

- No further knob-tuning of the current bundle (each has a measured negative or neutral result).
- No new corpus volume beyond what a gate needs (10 000 already showed saturation).
- No imitation, no distillation, no distillation-from-search without a stronger search.
- No per-move end-of-game judging (the pairing breaks at the first divergence; see RESULTS "Pairing does not survive the first
  divergence").
- No step started before the previous gate's number exists.

## 9. Why this is a plan and not another experiment

It has one claim — **conversion skill is learnable without an adversary, and that skill is what the value is missing** — and one
cheap test of that claim (G1), whose possible outcomes each close or open the rest in a defined way. If G1 fails, we stop with a
reason instead of trying the next idea; if G1 passes, the chain has a mechanism at every step, and the only remaining uncertainty is
the ordinary one (whether the improved player beats the current champion).
