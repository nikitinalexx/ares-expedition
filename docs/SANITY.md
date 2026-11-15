# The obvious-move bench (sanity check of the whole decision path)

Win rate hides a player that never presses a free card. This bench asks the player positions where the *rules alone* say
what is better, and records whether it agrees. The expected move of every scenario is arithmetic of the game, never an
opinion, so the bench is a check of the entire chain (encoder, policy, critic, search) and it can be re-run on any future
model, including overnight:

```
java -cp "target/classes;$(cat target/cp.txt)" com.terraforming.ares.selfplay.SelfPlayMain \
     --mode sanity --out tmp/sanity.json [--models DIR] [search flags...]
```

Exit code 1 when a hard scenario fails. `--mode sanity` prints one line per scenario with the chosen action and a note;
the JSON has `name / why / expected / chosen / passed / note` per scenario.

## Scenarios (2026-11-15)

| # | name | claim (why) | kind |
|---|---|---|---|
| S1 | free blue action (`AiCentral`) | draws two cards for nothing: the action is free value | **hard** |
| S2 | one-credit card draw (`MatterManufactoring`) | costs 1 credit, gives a project card that sells for 3: +2 net | **hard** |
| S3 | pay with the discount (`RestructuredResources`) | one plant buys five credits back | report only - a plant can be worth more than five credits (a forest award) |
| S4 | build instead of passing | a build slot with a buildable card and 100 credits used is a card for the engine | **hard**, report only when the table pays for standard projects (`SmallAnimals`, `StandardTechnology`, `CommunityAfforestation`) |
| S5 | phase pick with nothing to do | no money and no activatable blue card: phases 1/2/3 would be empty, 4 or 5 pays | **hard** |
| S6 | `Deimos Down` vs standard oceans | three oceans for ~26 credits against 45 for three standard ones | report only (cross-type comparison is not a claim of the bench) |
| S7 | temperature against a standard forest (`Livestock`) | 14 credits buy a terraforming point and a Livestock animal; 23 buy a forest and an oxygen step | **hard**, report only when the table rewards standard projects |

Positions are real mid-game snapshots of the needed state, mutated minimally (a card added to the table or the hand, money
set where the scenario needs it). Every scenario runs through the same `AgentDecisionService` path the live player uses,
with `DecisionProbe` recording the chosen index.

## Measured on 2026-11-15 (policy `r15_parent`, critic `v_r15_outcome`)

| scenario | deployed search (BUILD_CARD, DRAFT_CARDS) | search also on blue / payment / standard actions |
|---|---|---|
| S1 free blue action | **FAIL - stop** | ok (`AiCentral`) |
| S2 one-credit draw | ok | ok |
| S3 pay with the discount | report: plain payment chosen | report: plain payment chosen |
| S4 build instead of passing | ok | ok |
| S5 phase pick | ok | ok |
| S6 Deimos vs standard oceans | report: bought a standard ocean | report: **stops**, keeps the money |

So the two "obvious" pathologies the owner predicted are real and are a **coverage** defect, not a value bug: the search
only re-ranks builds and drafts, so the blue-action menu and the phase-3 spend menu were decided by the policy alone, and
the policy had never seen a free draw in its own games (the card's bit never fires in 2 000 recorded games). Extending the
search to those menus fixes both scenarios, because `V(after two more cards) > V(now)`.

### What the bench made us change

Extending the **value search** to those menus fixed all six scenarios and cost 13 points of win rate (36.5% -> 23.5%
against SMART over 2 000 games): the critic handles the extremes but is worse than the policy in the average position of
those menus. So the fix is the one the owner already uses in his own agent - a **rule for guaranteed value**:

`services/agent/GuaranteedActions` - if a blue action spends no credit, heat or plant, gives up no card, and leaves at
least one card or resource more, press it. It fires at most once per phase (only before the phase action is used), so the
extra activation a phase upgrade grants stays a real decision.

| configuration | sanity | vs SMART (2 000 games) |
|---|---|---|
| deployed search only on builds and drafts | 5/6 (refused a free two-card draw) | 36.5% [34.4, 38.5] |
| search also on blue / payment / standard menus | **6/6** | **23.5%** [21.7, 25.3] |
| deployed + the guaranteed-value rule | **6/6** | 35.9% [33.8, 37.9] |

The rule costs nothing measurable and removes the pathology, and the label of every rule decision is still recorded, so
the policy can learn the behaviour for the cases where the rule does not apply.

### Open, found by the bench (report-only lines)

* the discounted payment (S3) is usually not taken - it may be right (a plant for a forest award), so it is watched;
* **S6 stays wrong**: the player buys a standard ocean while `Deimos Down` sits in its hand. Comparing a card in hand with
  a standard project is a cross-menu decision our architecture does not make, and the V search that could make it costs 13
  points. This needs either a rule or a decision type that spans menus.

## Adding a scenario

Every bug found becomes a scenario: build the position from a snapshot, state the arithmetic in `why`, return an
`Outcome(passed, chosen, note)`. `report only` scenarios carry a note that explains when the "wrong" answer may be right -
they are for looking, not for gating.
