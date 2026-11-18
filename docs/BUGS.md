# Known engine issues

Statuses: **FIXED** (with test), **OPEN**, **BENIGN** (fragile but harmless now).
Reproduce: `mvn test`; more games: `-Dinvariants.games=200`.

## FIXED

### 1. Biased shuffle (`ShuffleService.shuffle`)
Naive swap-with-any-index (not Fisher–Yates). Measured bias on short lists (milestones/awards/decks). Fixed; `ShuffleServiceTest`.
Models trained before the fix saw the biased distribution — keep compares on one engine version.

### 2. Negative plants (`FoodFactory`, `BiomassCombustors`, `Potatoes`, `Moss`)
Plant payment (`RestructuredResources`) and the card’s own plant cost were validated separately, so total spend could exceed plants. Fixed in `PaymentValidationService` (sum both). `PlantSpendingValidationTest`.

## OPEN

### 3. Global parameters rise without matching TR
Invariant “TR progress ≥ temperature + oxygen + oceans + infrastructure (adjusted for card TR loss)” fails with and without dummy. May be legal effects or coarse TR-loss accounting. Need per-step “which action raised the param”.

## BENIGN / fragile

### 4. `assert` in main code
Ignored without `-ea`. Extra checks in `Player.setPlants/setHeat/setMc`. `setCardIncome` still only prints on negative.

### 5. Game copy (`new MarsGame(game)`)
Basis of all AI projections. `CopyFidelityTest` reflection-compares game vs copy. Some shared immutable-in-practice objects (`BuildDto`, param grades, …) — mutating them would corrupt the live game.

### 6. Effects that lower TR
Rules, not bugs — invariants must account for them.

### 7. `ObservationCompletenessTest.theDoublePaymentIsTheBestCardPerDimensionNotTheSum` is flaky
It picks two "can pay again" cards from a freshly shuffled projects deck, puts them on the table and compares the
observation's max-single-payment feature with the maximum it measures card by card. When the two cards synergise (a
payment that depends on the other card being on the table) the encoder's shared-copy measurement sees the combined
payment and the test fails (observed 2026-11-15: expected 0.1, got 0.3, then passed on the next run). The encoder is
right - the test needs a fixed pair of cards instead of whatever the shuffle deals.

## `DecisionLogs` leaked its ranking sidecar across recordings (fixed 18/11)

`DecisionLogs` is a singleton and `abort()` closed the binary writers but kept `rankingWriter` / `rankingTmp`. Since every
recorded game now writes one score-breakdown line into that sidecar, the sidecar is opened in games that used to have no
sidecar at all - and the next recording then tried to rename the *previous* directory's `RANKING.*.tmp`, which no longer
existed (`NoSuchFileException: PICK_PHASE.000001.ares.tmp -> ...ares`, seen through `closeCurrentChunk`). Symptoms were
order-dependent: the same test passed alone and failed in the class.

Fix: `abort()` and `startRecording()` both call `releaseRankings()` (close without renaming, forget the path). `DecisionLogsTest`
is green (9/9) and its whitelist of legal open files now includes `RANKING.000002.jsonl.tmp`.

## `mvn test` after the night: 11 red, none of them a regression

`Tests run: 125, Failures: 4, Errors: 7`. All eleven are pre-existing and belong to two groups:

- **AFTERSTATE is off by design** (`AfterstateRecordingTest` x2, `AgentAllDecisionsTest.everyDecisionTypeIsRecordedAtLeastOnce`):
  the recipe does not record afterstates, so a test that requires an AFTERSTATE row cannot pass. Either the tests or the
  recipe has to move; this is an owner decision, not a bug.
- **stale fixtures of the 1090/1256 encoder change**: `ObservationMaskTest.theMaskIsConsistent` expects 1044 blocks where
  the encoder now produces 1256, and all seven `MlpPhaseModelTest` cases load a model trained on a 780-value observation
  and are refused ("retrain it"). Both need the fixtures regenerated, not code fixed.
