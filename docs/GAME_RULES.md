# Game rules (as the code implements them)

Verified against the code, not against memory. Sources: `model/Constants.java`, `states/`, `services/StateTransitionService.java`,
`services/DraftCardsService.java`, `mars/MarsGame.java`. Anything not verified here is marked as such.

## The round is the unit of time

`game.getTurns()` increments **once per round**, when the game enters `PICK_PHASE` (`MarsGame.setStateType`, line ~258).
Typical game: ~23 rounds (compare mode reports `avgTurns` ≈ 22.8). The agent's decisions are per phase, so a game is
~500 decisions: ~6 decision chains per round (one per phase state, one per player, plus inner build decisions).

## What happens in a round

1. **`PICK_PHASE`** — both players pick one of the 5 phases **simultaneously**; a player may not pick the phase they picked
   last round (`AiTurnService.choosePhaseTurn` rejects it). With the dummy hand (always on for AI play) a third, neutral
   "player" draws a phase too: `game.updateDummyHand(...)`, `game.getCurrentDummyHand()`, from a pool of 1..5 **without
   replacement**, reshuffled when empty. The remaining pool is public information.
2. The picked phases are collected into a set (`StateTransitionService.performStateTransferFromPhase`) and resolve in
   ascending order **1 → 5**, one state per phase. **Every player takes part in every phase that was picked**; the player
   who picked it gets a boosted version.
3. After phase 5 (or the last picked phase): if any hand is over the limit (10, `MAX_HAND_SIZE_LAST_ROUND`) →
   `SELL_EXTRA_CARDS`; otherwise next round.
4. `game.gameEndCondition()` is checked at every transition: oceans **and** temperature **and** oxygen maxed (plus
   infrastructure when that expansion is on) → `GAME_END` immediately, so the terraforming phase does not finish.

## The five phases (what a player actually can do)

| # | State | Everyone gets | The picker gets (upgrade in brackets) |
|---|---|---|---|
| 1 | `BUILD_GREEN_PROJECTS` | one green project at full price | discount 6 **or** discount 3 **plus** a second green build at −12 |
| 2 | `BUILD_BLUE_RED_PROJECTS` | one blue/red project at full price | **one more**: another project, or project-or-MC, or project-or-card |
| 3 | `PERFORM_BLUE_ACTION` | each played blue action once per round | double activation **or** draw 3 blue/red cards |
| 4 | `COLLECT_INCOME` | MC = income + TR; plants/heat/cards per income | extra MC **or** replay of greens (double production) |
| 5 | `DRAFT_CARDS` | see 2, keep 1 | see 5, keep 2 (upgrade: 4/3 "keep extra" or 8/2 "see extra") |

Consequences that matter for the agent:

- A player has **~1 build opportunity per build phase per round** (+1 if they picked it). Building is therefore a
  frequent, small decision, and *declining* to build is a real choice (the search countermeasures measure exactly that:
  ~40% of build opportunities are declined by the policy).
- **Production pays from the round after the card is built** (income resolves in phase 4). A card built in round *r*
  pays `23 − r` times, so the same card is worth much more early — the timing argument that motivated the situational
  value function.
- **The phase pick is a first-class decision** (an extra build, a −6 discount, an extra income, an extra draft card) and
  the search does not touch it at all. It is one of the largest levers left.
- Builds are limited by `player.getBuilds()` — a list of `BuildDto(type, discount)`, rebuilt at the start of every build
  phase. That list *is* the candidate set for a BUILD_CARD decision.

## Resources and scoring

- Player counters: `mc`, `heat`, `plants`, `terraformingRating`, `forests`, `extraPoints`; incomes: MC, cards, steel,
  titanium, plants, heat. There is **no steel/titanium stock** — those "incomes" are discounts:
  `DiscountService`: steel → 2 MC per point on **building** cards, titanium → 3 MC per point on **space** cards (more
  with Advanced Alloys / Phobolog).
- Globals: temperature, oxygen, oceans. Raising a step normally grants +1 TR to the actor.
- Final score (`WinPointsService.countWinPoints`): TR + forests + card VP + milestones + awards + resources stored on
  cards.
- Start: corporation out of 2, optional mulligan; hand limit 10, selling a card gives 3 MC (more with Exocorp effects).

## Not verified here

- Exact draft counts with every combination of upgrades and special effects (`DraftCardsService` is the source of truth).
- Crisis / co-op modes, infrastructure, experimental cards: out of scope for the agent.

### Phase 4 is a SHARED phase, and declining it can be correct (owner, 20/11)

The table above says what everyone gets; the strategic consequence is the owner's point and it is not obvious from the table:

* **Phase 4 resolves for everyone.** If phase 4 is picked - by either player or by the dummy - everybody collects income. If nobody picks it,
  nobody collects.
* **The pick costs an opportunity.** There are five phases and one pick; picking income buys a *small extra* income for the picker while a build
  phase buys an extra build or a -6 discount. So the marginal value of picking 4 falls as income grows.
* **When you dominate income, declining 4 can be the strong move.** The opponent then either wastes their pick on a small income (they "bury
  themselves": their extra is small while your pick goes into a build), or the round collects no income for anyone, which costs you little
  relatively because your lead comes from buildings, not from one more income collection.
* **Corollary for the agent:** "does not pick phase 4" is NOT a degeneracy signal, and neither is "picks phase 1 a lot". The phase pick is a
  first-class decision whose value depends on the opponent's income and on the alternative use of the pick.

This is why the profile gate judges outcomes and build/standard-project behaviour, not the phase mix alone.

### The phase-3 "reveal cards" upgrade, as the code actually implements it (owner, 20/11)

`StateTransitionService` (lines 220-237) does not give three cards. It does this:

```java
List<Integer> cards = cardService.dealCards(game, 3);        // three cards are REMOVED from the project deck
for (Integer id : cards) {
    Card card = cardService.getCard(id);
    if (card.getColor() == BLUE || card.getColor() == RED) player.getHand().addCard(id);
}                                                             // the others are discarded, not returned
```

so the picker **sees three and keeps only the blue/red ones**; the rest leave the game. With blue (101) + red (55) against green (192) in the card
pool, the expected keep is about **1.3-1.4 cards per activation**, plus ~1.6 cards removed from the shared deck (a denial effect, not just a draw).

Not broken, and the pieces are consistent: the upgrade id is 8 (`PHASE_3_UPGRADE_REVEAL_CARDS`), `hasPhaseUpgrade(8)` is `phaseCards[2] == 2`, the
observation column `table_phase_3_app_2` is exactly that flag, and only the player who chose phase 3 receives it. What was wrong was my sentence "the
picker gets three cards": that overstates the payoff by more than a factor of two, and it weakens the argument that picking phase 3 while behind on
activations is justified.
