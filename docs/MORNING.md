# MORNING REPORT — night of 20/21 November

Everything below is measured; the detailed records are in `docs/RESULTS.md` and the statuses in `docs/LEDGER.md` (rows 41-46).

## One line

Three card-content arms — events, economy, microbes — were run overnight on both benchmarks; **all three are negative**, the gentler the
perturbation the smaller the loss, so "make the player play more of X" is closed as a way to add strength. What did improve is the tooling: the
simulator is **35% faster** with no decision changed, and the instruments for a content experiment (colour/set nudges, a microbe definition by
resource, a health panel, a tag metric) are now tested and reusable.

## The arms (5 000 games each, the same base, critics, size, recipe)

Base: the clean fine-tune, **75.16% [73.94, 76.23]** vs SMART and **43.25% [40.2, 46.4]** vs NETWORK.

| arm | manipulation check | vs SMART | vs NETWORK | health panel |
|---|---|---|---|---|
| **red** (events), rate 0.3 | 35% red cards against 14% | **72.05% [70.79, 73.28]** | **39.15% [36.2, 42.2]** | collapse: builds -28%, acceptance -28%, card variety -29% |
| **green** (economy), rate 0.3 | green share up, red down | **70.23% [68.95, 71.48]** | **34.38% [31.5, 37.4]** | the same three collapses |
| **microbe** (resource, not tag), rate 0.15 | 1.5% of game-seats hold microbes against 0.6% | **72.46% [71.20, 73.68]** | **37.65% [34.7, 40.7]** | one collapse: acceptance 0.697 (-14%) |

Read together: the *content* of the forcing matters far less than the *forcing*; a compelled player builds less, accepts fewer builds and plays a
narrower card set, and the corpus inherits exactly that (the 19/11 rule, now measured from the other direction). Green — the economy — is the worst of
the three, so the owner's "more red is a consequence of a stronger economy" is **not** supported by this instrument: forcing the economy costs more
than forcing the events.

## What this closes, and what it leaves

* **Closed:** "teach it to play more events / more economy / more microbes and it will get stronger". It does not, on either benchmark, at any of the
  rates tried (0.15, 0.3, 0.5, 0.6 — the higher rates were measured in the smokes and destroy the build count first).
* **Not closed:** the *idea* that a plan is worth having — only the instrument, forcing, is. The microbe arm in particular shows the gentlest
  intervention loses least; whether a *learned* plan (a corpus produced by a player who chose it for its own reasons) would pay is a different question,
  and the only way to answer it is the measurement path from the review.
* **Unchanged:** the champion is the practice bundle: one fine-tune on 5 000 fresh self-play games, +2 points over the old champion, and a **second**
  fine-tune converged to the same player (panel identical). The +2 is a step, not a curve.

## Tooling built overnight (all tested)

* `ml/run_arm.ps1` — one command per arm: generate -> fine-tune on the whole corpus -> 5 000 vs SMART -> 1 000 vs NETWORK -> health panel.
* `ColourNudge` (6 tests): prefer a colour at build/draft, keep it at mulligan/discard/sell.
* `TagNudge` (5 tests) and `MicrobeCards` (5 tests): the microbe family is defined by the **resource** — 13 holders/producers from the owner's list,
  Worms and BacterialAggregates, the four red `ADD_MICROBE` one-shots, the buffed variants — not by the MICROBE tag, which the owner correctly called
  meaningless.
* `ml/tag_metric.py --microbes` (8 tests): the microbe resource actually held on cards, summed over the nine counters of `TableResourcesFeature`.
* `ml/health.py` (13 tests): the generalist-vs-specialist classifier; a strong degenerate is kept as a gatekeeper, never deleted.
* **Throughput:** 1.07 -> **1.44 games/s** (+35%), observation CPU per game 7.20 -> 3.74 s, critics unchanged; streams removed from the observation
  path and a flat `Card[] byId` replaced up to eight hash lookups per `getCard` (equivalence tested over ids 0..11 000).

## The 10 000-game confirmation

Running at the end of the night (the champion, clean fine-tune, 10 000 vs SMART and 2 000 vs NETWORK); the result is appended below when it lands.

## What I would do next (in order)

1. **The `Q^champ` oracle** (the review's #2, ~2 h at the new speed): on ~200 phase decisions, play every legal phase to the end with the champion as the
   continuation and compare the rollout's choice with the champion's own. It answers the one question no arm answered: **is the champion already
   one-step-optimal at the plan decision**, i.e. is there any headroom in the search at all?
2. **The size-matched control (P4, ~40 min)**: the champion's own corpus subsampled to 2 000 games. If that also trains a 25-35% player, the rule "a
   corpus inherits its generator's strength" is mostly a volume effect and two ledger rows need a CORRECTED status.
3. **A common reference opponent for the critics (P3)**: the structural way to make specialists comparable, i.e. the only path to a gate/league that
   does not compare scales.
4. Only then anything that costs a corpus: the practice loop already plateaued, and the three content arms say the corpus needs a *different player*,
   not a different bias.

## What I did not do, and why

No pruning, no public-observation removal (they buy speed with quality, the owner's call). No second microbe rate at the arm scale (the smokes showed
the direction: higher rates only add the build collapse). No reply written to the reviewer yet: the owner asked to verify first, and the verification is
`docs/reviews/VERIFICATION_1.md` — with the red/green/microbe arms now added to it as evidence.
