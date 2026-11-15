# Revision of the representation, per decision (2026-11-15)

The owner's rule after the hand turned out to be invisible: revise the representation **by decision**, not by bit, and put a test
behind every claim. The test is `ObservationCompletenessTest` (`everyDecisionOffersTheOptionsTheRulesGive`,
`aCardCandidateCarriesItsPrintedPointsAndPrice`, and the fact lists `playerFacts()` / `gameFacts()`), and the to-do list of
known gaps is `KNOWN_BLIND_SPOTS` in the same class - a fact that is invisible fails the build unless it is listed there.

## What was found and fixed in this revision

| gap | evidence | fix |
|---|---|---|
| **the hand was not described at all** - only five specific hand cards had a bit | `ml/discount_report.py`: mean steel income 0.12, the discount changed the cheapest card in 1% of decisions; the critic could not see "I hold three cheap building-tag cards" | `HAND_SUMMARY_SIZE = 20` in the encoder: cards, count per colour, count per tag, sum of nominal and effective price, cheapest effective price, how many are affordable |
| **a card's printed victory points never reached the candidate vector** - the layout string documented `points/10` from the start, the code never wrote it | "which card to keep in the draft" could not see that one card scores 4 and another 1, except through its class signature | `CardFeatures.LENGTH` 82 -> 83, the points slot is written; the test recomputes it exactly from the card |

Both are the same failure mode: **a documented feature that was not there**. A dead bit is caught by `ml/feature_audit.py`
(a value that never changes); a *missing* one is invisible to that instrument. The new tests change a fact the rules say the
decision depends on and assert the observation moved - a missing feature cannot pass them.

## The decision-by-decision checklist

State facts are shared by every decision (one `ObservationBuilder`), so the fact tests cover all of them at once. The
per-decision part is the **menu**: what the options are, and what each option carries.

| decision | menu must be | what an option carries | status |
|---|---|---|---|
| `PICK_PHASE` | the legal phases (all but the one picked last round) | phase one-hot + "this phase is upgraded for me" | tested |
| `PICK_CORPORATION` | exactly the corporations the engine dealt | class signature, phase upgrades, tags, milestone slot | tested (menu); options are identity-only - the effect is learnable as an embedding, nothing else is representable |
| `BUILD_CARD` | exactly the cards in hand (+ stop) | signature, colour, tags, price, printed points, is-a-blue-action | tested |
| `DRAFT_CARDS` | exactly the cards on offer (+ stop) | same as above | tested (shared CardFeatures) |
| `DISCARD_CARDS`, `SELL_CARDS`, `SELL_CARDS_LAST_ROUND`, `MULLIGAN`, `COLLECT_INCOME` | the hand / the offer | same as above (a sale adds its price) | tested (shared CardFeatures) |
| `PERFORM_BLUE_ACTION` | only cards that are really activatable (+ stop) | card features | tested (menu) |
| `STANDARD_ACTION` | the legal spends (+ stop) | kind, costs, what it raises | by construction (`standardActionDecisions.options`) - the audit `AgentDecisionAuditTest` covers the free-card rule |
| `BUILD_PAYMENT` | every legal payment combination | restructured/anaerobic/heat/mc/need/discount | by construction - the double-payment block has its own exact test |
| `BUILD_INPUT`, `BLUE_INPUT` | the legal inputs of THIS card | kind, card features, phase, tag, amount | by construction |
| `UNMI_RT` | take / stop | kind, cost, can-afford | by construction |

## The tests that now guard this

| test | what it fails on |
|---|---|
| `whilePickingOnlyThePicksOfTheRoundAreHidden`, `insideARoundEverythingIsVisibleIncludingTheirPicks` | a fact the rules make visible that does not move the observation (the fact list includes the hand: empty, three more cards, a BUILDING-tag card, a SPACE-tag card, enough money to afford the hand) |
| `everyDecisionOffersTheOptionsTheRulesGive` | a menu built from a stale source: the build menu must offer the hand, the phase menu exactly the legal phases, the corporation menu exactly the dealt corporations, the blue-action menu only really activatable cards |
| `aCardCandidateCarriesItsPrintedPointsAndPrice` | a candidate slot that silently disappeared: it recomputes points/10 and price/50 from the card itself for twenty cards |
| `everyPrivateColumnIsZeroInThePublicView` | a new private block that forgets to declare itself in `buildPublic` (hidden information leaking into the symmetric search) |
| `theDoublePaymentIsTheBestCardPerDimensionNotTheSum` | the phase-4 double payment being a sum instead of one card's payment (its card pair is now chosen with *different* payments, which removed a flake that depended on deck order) |
| `buildingAnObservationNeverChangesTheGame` | an encoder that mutates the position it reads |

## Known blind spots that remain (deliberate, with reasons)

* the opponent's hand (only its size), the deck order, the dummy pool order - hidden by the rules of the game;
* a corporation's *starting* resources and its text: the candidate vector carries its identity (a 64-bit class signature),
  which the network can learn as an embedding, but not the numbers; adding them would mean re-deriving each corporation's
  effect in the encoder, i.e. a second implementation of the rules that can drift from the engine;
* card effects in general: the same argument - the class signature is the honest representation, and the engine is the only
  place the effect exists.

## The owner's seven card questions, checked one by one (2026-11-15)

Each item is answered from the code, not from memory.

| # | the card / rule | covered? | where |
|---|---|---|---|
| 1 | **Austellar** - on build the corporation chooses which milestone to mark | **yes** | the choice is an *input of the corporation decision*: `CorporationDecisions.legalInputs` emits one candidate per milestone slot and per tag (`AUSTELLAR_CORPORATION_MILESTONE`), the candidate vector carries `milestoneSlot(8)` + `milestoneType(15)`, and the table block has `milestone_*_isAustellarPick` for the rest of the game |
| 2 | cards that choose a **phase upgrade** on build | **yes** | `BuildInputDecisions` slot `KIND_PHASE` with `PHASE_UPGRADES = 10` candidates (5 phases x normal/upgraded), consumed by `InputFlag.PHASE_UPGRADE_CARD` |
| 3 | **Anaerobic Microorganisms + Viral Enhancers**: 1 plant or a microbe/animal on a card | **yes** | `BuildInputDecisions.viralEnhancersSlot`: counts the card's ANIMAL/MICROBE/PLANT tags and emits one candidate per remaining "take a plant" / "put a resource on that card" choice; the same slot mechanism covers the Anaerobic payment (`CommunicationStreamlining`-class effects) |
| 4 | **Self Replicating Bacteria**: +1 microbe, or spend 5 for a 25-MC build | **yes** | `BlueInputDecisions.addDiscardMicrobeChoices`: `KIND_MICROBE_ADD` always, `KIND_MICROBE_SPEND` when the card holds at least `max`; `SelfReplicatingBacteriaActionProcessor` then adds `new BuildDto(GREEN_OR_BLUE, 25)` to the player, which the next `BUILD_CARD` menu offers |
| 5 | **Software Streamlining**: draw 2, then discard 2 | **yes** | the engine asks `DISCARD_CARDS` with its own min/max; the agent decides it card by card (`HandCardDecisions`) and records every step |
| 6 | **Microprocessors**: draw 2, discard 1 | **yes** | same path as 5 (the count comes from the engine, the decision does not assume it) |
| 7 | **Martian Studies**: upgrade phase 2 and immediately take the bonus move (card or 6 MC) | **yes, and it is not a decision by design** | `PickExtraBonusTurnProcessor` takes the *card* if the granted slot is `BLUE_RED_OR_CARD`, else 6 MC if it is `BLUE_RED_OR_MC` - the choice was already made when the slot was granted, and taking a free card/MC is weakly better than skipping (the same class of rule as the free blue action). `DecisionType` documents it: "forced engine turns (free second-phase bonus) are outside this set" |

What this audit does *not* claim: that every card effect is visible in the observation. Card effects are represented by the
card's class signature (a 64-bit identity, learnable as an embedding) - the encoder never re-implements what a card does,
because a second implementation of the rules would drift from the engine. The inputs *of a decision* (the seven above) are
enumerated by the engine itself and offered as candidates, which is where the choice actually happens.
