# The state representation (encoder v3.2, 795 values)

Everything the learned agent (policy and value) sees is one float vector per decision, built by
`services/agent/ObservationBuilder.build(game, player)`, plus a small candidate vector per legal option (the decision
types are listed at the end). The vector is public information only: the opponent's hand, the deck order and the order
of the dummy pool never enter it (`ObservationCompletenessTest`).

Owner's rule for the score (2026-11-15): **the value function must not know where a winning point came from.** There is
one `winning_points` number (the engine's own total: printed card points, points on animal/microbe cards, terraforming,
forests, milestones, awards) and one `mc_income` number (money per round, which already contains the TR each round). The
per-source counters that follow are *state* (an animal on a card is also a resource that other cards consume), not a split
of the score. The statistics that *do* split the score exist outside the encoder: `--mode scorecard`.

## How it is assembled

| block | columns | source |
|---|---|---|
| A. table | 0–416 | V2 table encoder, 417 columns kept by `observation/v3_mask.json` (exact duplicates removed) |
| B. hand | 417–687 | hand encoder ("what the hand could add" + one bit per card class in hand) |
| C. round context | 688–725 | phase, picks, build slots, discounts, activations |
| D. cards V2 cannot tell apart | 726–742 | 17 bits/counters |
| E. history + dummy pool | 743–759 | round, previous phases, dummy pool |
| F. best single phase-4 repeat | 760–781 | 11 dimensions × me/opponent |
| G. phase-start rules | 782–787 | 4 + 2 |
| H. oceans | 788–794 | 7 |

The V2 block (0–687) is **normalised by `AiConstants.NORMALIZATION_VECTOR`** (a legacy table from NETWORK_V2): the value
in the vector is `real / divisor`, e.g. `winning_points / 100`, `mc_income / 121`, `blue_cards_count / 25`. The appended
blocks scale themselves (`/20`, `/10`, `/40`, `/9`, `/2`). `SelfPlayMain --mode scales --out FILE` writes the divisor of
every value, so tooling can talk in real units.

Each model then standardises with the mean/std it was trained with (stored in `model.json`), so a constant divisor does not
change the model's resolution — `(x/100 - m/100) / (sd/100)` is the same as `(x - m) / sd`. What limits resolution is the
*learned* weight per standard deviation (`ml/value_sensitivity.py`): about 0.02-0.03 of win probability per sigma for every
feature, so three points - 0.17 sigma of the score - move the critic by about 0.005.

## A. The table (0–416), my side 0–238, opponent 239–416 (mirror)

| index | feature | meaning |
|---|---|---|
| 0–2 | `oxygen_left`, `temperature_left`, `oceans_left` | global parameters still open |
| 3–42 | 10 milestones × {`isInGame`, `myProgress`, `opponentProgress`, `isAustellarPick`} | which milestones are in this game, how far each player is, whether the slot is the one an Austellar corporation chose |
| 43–60 | 6 awards × {`isInGame`, `myProgress_log`, `opponentProgress_log`} | same for awards (progress is log-scaled) |
| 61–70 | `table_phase_X_app_Y` | which phase-upgrade cards each player holds (level 1 / level 2 per phase) |
| 71–74 | `max_{mc,heat,plants,cards}_per_card` | the largest single income of that type among the played cards (what the phase-4 double-collect would take) |
| **75** | **`winning_points`** | **the whole score of that player: TR + printed card points + points on resource cards + forests + milestones + awards + extra** |
| 76 | `forests` | forests on the table (a counter, and 1 point each - already inside `winning_points`) |
| **77** | **`mc_income`** | **money per round: `getMcIncome() + TR` (the engine pays exactly this at income)** |
| 78–82 | `steel_income`, `titanium_income`, `plants_income`, `heat_income`, `card_income` | the other productions per round |
| 83–85 | `mc`, `plants`, `heat` | resources on hand |
| 86–89 | `played_cards_count`, `blue_cards_count`, `green_cards_count`, `hand_cards_count` | table size and hand size |
| 90–99 | `table_played_{space,earth,event,science,plant,energy,building,animal,jupiter,microbe}` | tags played |
| 100–113 | 14 tag-driven incomes (`MC_EARTH_INCOME`, `PLANT_PLANT_INCOME`, `MC_2_BUILDING_INCOME`, …) | the per-tag income sources that some cards give |
| 114–138 | 25 corporation bits | which corporation the player founded |
| 139–220 | 82 bits, one per card class with a table effect | which of those effects are on the table |
| 221 | `research_grant_dynamic_left` | cards still to draw from Research Grant |
| 222–229 | 8 animal-card counters (`birds`, `filterfeeders`, `fish`, `livestock`, `zoos`, `ecologicalzone`, `herbivores`, `smallanimals`) | animals per card, one number each |
| 230–238 | 9 microbe-card counters (`anaerobic`, `decomposers`, `decomposing_fungus`, `ghg`, `nitrite`, `regolith`, `self_replicating`, `tardigrades`, `bacterial_aggregates`) | microbes per card |
| 239–416 | the same as 3–238 with `op_` prefix | the opponent's table, from the same public information |

## B. The hand (417–687)

| index | feature | meaning |
|---|---|---|
| 417–419 | `hand:mcincome_log`, `hand:heat_log`, `hand:heat_income_log` | what the hand would add (log-scaled aggregate of what the held cards produce) |
| 420–687 | `hand:<CardClass>_hand` (268 classes) | one bit per project card class currently in my hand |

The hand is the player's own information; the opponent's hand is represented only by its size (`op_hand_cards_count`, 267).

## C. Round context (688–725)

| index | feature | meaning |
|---|---|---|
| 688 | `picks_are_known` | the phase has started, so the picks below are the current round's |
| 689–694 | `current_phase_1..5`, `current_phase_sell` | which phase is being played |
| 695–699 / 700–704 / 705–709 | `my_pick_*` / `opponent_pick_*` / `dummy_pick_*` | the phase each side announced this round |
| 710–713 | `my_green_builds`, `my_blue_red_builds`, `my_best_build_discount/20`, `my_tightest_build_price_limit/30` | build slots left and what they cost |
| 714–717 | the same for the opponent | |
| 718–719 | `extra_blue_activations/2` | phase-3 double-repeat, Project Inspection, … |
| 720–723 | `luna_project_office`, `project_inspection` | special one-shot activations |
| 724–725 | `activated_blue_cards/10` | how many blue actions were already used this phase |

## D. Cards the V2 encoder cannot tell apart (726–742)

| index | feature |
|---|---|
| 726–733 | `{my,opponent}_on_table_{MatterGenerator,Sawmill,FilterFeeders,Pets}` |
| 734–737 | `{my,opponent}_resources_on_{FilterFeeders,Pets}/10` |
| 738–742 | `in_my_hand_{BedrockWellbore,ArchitectureBlueprints,CityPlanning,GrainSilos,InvasiveIrrigation}` |

## E. History and the dummy pool (743–759)

`extra:round/40` (turns so far), `extra:{my,opponent}_previous_phase_1..5` (the phase each side took in the previous
round), `extra:dummy_pool_has_phase_1..5` (the *set* of phases the dummy can still draw - the order is hidden, so it is
not encoded) and `extra:dummy_pool_size/5`.

## F. The best single phase-4 repeat (760–781)

Eleven dimensions (`mc`, `plants`, `heat`, `cards`, `tr`, `mc_income`, `plants_income`, `heat_income`, `card_income`,
`steel_income`, `titanium_income`), each divided by 10, for me and for the opponent: the maximum that **one** played card
can pay again. It is computed by actually running every `canPayAgain` card's `payAgain` on a copy, so cards whose payment
depends on the state (steel, science, tags) are handled without a card table. Deliberately the maximum per dimension, not
the sum (a phase 4 collects one card only) and not "which card".

## G. Phase-start rules (782–787)

`planet:open_at_phase_start_{temperature,oxygen,infrastructure,oceans}` — whether that global could still be advanced
when the phase started (the engine's own rule; a maxed global can sometimes still be advanced through infrastructure), and
`player:{my,opponent}_special_design_last_turn` — a card with "special design" built last turn may ignore requirements.

## H. Oceans (788–794)

`oceans:last_opened_{cards,mc,plants}/10` — the reward of the last opened ocean (every further ocean pays exactly that
once all nine are open), `oceans:collected_{cards,mc,plants}/40` — the totals collected so far, `oceans:revealed/9`. The
positions are a shuffle and are not encoded.

## Candidate vectors (per legal option)

| decision | width | contents |
|---|---|---|
| `BUILD_CARD`, `DRAFT_CARDS`, `DISCARD_CARDS`, `SELL_CARDS`, `SELL_CARDS_LAST_ROUND`, `MULLIGAN`, `PERFORM_BLUE_ACTION`, `COLLECT_INCOME` | 83 (84 for a sale) | 64-bit class signature (±1 each), colour (4), tags (11), price/50, is-a-blue-action, stop flag, **printed victory points/10**; a sale adds sell price/10 |
| `PICK_PHASE` | 6 | phase one-hot (5) + "this phase is upgraded for me" |
| `PICK_CORPORATION` | 107 | class signature (64), phase-upgrade one-hot (10), tag (10), milestone slot (8), milestone type (15) |
| `STANDARD_ACTION` | 20 | kind one-hot (11: stop, heat exchange, forest, temperature, 2× infrastructure, 5 standard projects), MC/heat/plant cost, flags for what it raises, Cargo Ships heat variant, stop |
| `BUILD_PAYMENT` | 6 | Restructured, Anaerobic, heat/20, MC/20, (MC+heat)/20, discount/20 |
| `UNMI_RT` | 5 | kind (2) + cost/20 + can-afford + stop |
| `BUILD_INPUT` | 115 | kind (11 slots: phase, tag, resource target, plant, skip, oxygen, Decomposers, Cargo Ships, Mars University stop), card features (83), phase (10), tag (11) |
| `BLUE_INPUT` | 117 | kind (12), card features (83), phase (10), tag (11), amount/20 |

Stop ids: `0xFFFF` for the card menus, `0` for `STANDARD_ACTION`/`UNMI_RT`. `PICK_PHASE` has no stop (the engine always
asks for a phase); a candidate id is the phase number.

## What is deliberately NOT in the vector

* the opponent's hand (only its size), the deck order, and the order of the dummy pool (only its set);
* the identity of the unrevealed ocean slots (only the multiset of the rewards collected so far);
* anything about the cards in the draft before the engine reveals them;
* the engine's internal turn queue.

## Known gaps (offered for review, 2026-11-15)

1. **Science counters are missing.** Per-card resource counters exist for 8 animal and 9 microbe cards. `PhysicsComplex`
   (1 VP per 2 science), `FibrousCompositeMaterial` and `CommunicationCenter` hold `SCIENCE` and are not counted anywhere,
   although "one resource on one card matters" is exactly the owner's standard. The same question applies to every other
   `CardCollectableResource` (`FOREST`, `JUPITER`, …).
2. **The remaining deck composition is not encoded** (it is public: the game's card multiset minus everything played,
   discarded or in hand). It decides what a phase-5 draft can still bring.
3. `winning_points` includes forests while `forests` is also a separate counter — redundant, kept because a forest is a
   board object (it consumes plants/heat and can be raised).
4. TR has no field of its own by design: it enters as +1 `winning_points` and +1 `mc_income` (money per round).
