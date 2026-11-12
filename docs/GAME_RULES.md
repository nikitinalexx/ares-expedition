# Game rules (as the code implements them)

Reconstructed from code (`Constants`, `states/`, `DraftCardsService`, …). Items marked ❓ need owner confirmation — wrong rules poison training.

## Round

1. **Phase pick.** Each player picks one of 5 phases (not the same as last round). Picks are **simultaneous**.
2. **Chosen phases resolve for both players**, in order **1 → 5**. The player who picked a phase gets its **boosted** effect (and that phase’s upgrades).
3. Then **sell extras** (hand limit 10) and next round. Mid-turn sell for **3 MC** is also allowed when it is not the only action (does not spend the build; money arrives immediately). Exocorp effects can +1 MC to sell price.

| # | Phase | Effect |
|---|---|---|
| 1 | Build green | play a green project |
| 2 | Build blue/red | blue (permanents/actions) or red (events) |
| 3 | Blue actions | each blue once/round (upgrade: twice / peek); standard projects |
| 4 | Income | MC = income + TR; heat/plants/cards; upgrade can replay greens |
| 5 | Draft | pick 2 of 5 if you chose it, else 1 of 2; upgrades change counts |
| 6 | Sell extras | automatic |

Phase upgrades: two options per phase, granted by cards.

## Resources and scoring

- Player: `mc`, `heat`, `plants`; incomes including steel/titanium/cards.
- Globals: temperature, oxygen, oceans (+ infrastructure in expansion). Raising a step usually grants TR to the actor.
- VP: TR + forests + card VP + milestones + awards + resources on cards (`WinPointsService`).
- Start: pick corporation (2 options), optional mulligan.

## End of game

Game ends in the **phase where terraforming completes** (all globals maxed); that phase finishes. Leader wants to end sooner; trailer wants to stall.

## Dummy hand (always on for AI play)

A third “player” draws a phase each round from a pool of 1–5 without replacement; empty pool reshuffles. Remaining pool is **public** and must be in the observation. Speeds the game and forces income/build diversity.

Other modes: buffed corporations, infrastructure, experimental, crisis (coop) — out of agent scope for now.

## Decision types (for the agent)

Corporation/mulligan → phase → build (card / payment / input) → blue actions / standard projects → draft → sell/discard → endgame. On the order of hundreds of decisions per game.
