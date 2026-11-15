# Playing against the learned agent in the UI

The learned player is `PlayerDifficulty.AGENT_V2` ("Ares" in the new-game dialog). It plays with the **second** model
set, so the ordinary `AGENT` keeps the previous weights and you can compare both in one session.

## What to start

The models are chosen by JVM properties, so pass them to the Spring Boot process (IDE run configuration → VM options, or
`spring-boot.run.jvmArguments`):

```
-Dares.agent.models=D:/ares/runs/night7/exp/vs_random/it_0046/model_r2
-Dares.agent.models2=D:/ares/runs/policy/r11_cards
-Dares.agent.search=oneply
-Dares.agent.search.symmetric=true
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS
-Dares.agent.search.pairs=true
-Dares.agent.valueModel=D:/ares/runs/value/v_r12_outcome
```

- `ares.agent.models` — the model set of `AGENT` (optional: without it `AGENT` falls back to random, `AGENT_V2` still works).
- `ares.agent.models2` — the model set of `AGENT_V2`. The strongest player we have, in the current (805-value) encoder:
  `D:/ares/runs/policy/r11_cards` (a copy of PPO iteration 12 zero-padded to the 805-value encoder, then trained on fresh
  self-play and on card heads with a points-based advantage). About **37% vs SMART / 93% vs RANDOM** with search, 13 cards
  per game. The old-encoder bundles (`ppo1/exp/ppo_mixed/it_0012/model`, 795 values) no longer load: the encoder gained the
  five score terms per player and the printed points of a card, so convert them with `ml/pad_model.py --expected 805`
  (that is exactly how `r9_805_parent` was made).
- The three `search` properties switch on the 1-ply symmetric re-ranking at play time (about 1.4 ms per decision). They
  apply to both agents; drop them to see the same weights without search.
- The frontend: `cd frontend && npx ng serve` (the dialog now has the "Ares" option next to Easy / Normal / Hard / Expert).

The 805-value model bundle must match the current encoder; a model trained on another schema is refused at startup with an
explicit message (that is intentional — no silent reshaping). `ml/pad_model.py` is the offline conversion.

### The scorecard: what the player is worth, in points

```
java -cp "target/classes;$(cat target/cp.txt)" com.terraforming.ares.selfplay.SelfPlayMain \
     --mode scorecard --out tmp/scorecard.json --games 300 --a AGENT --b SMART --models <bundle>
```

prints the average of every term the engine scores (terraforming rating, forests, printed card points, resource points,
milestones, awards) for both seats, plus how many cards each side put on the table. It is the fastest way to see what a
change actually bought: the current player is ahead on Tr and forests and behind on cards (13.3 cards / 3.3 printed
points against SMART's 27.4 / 13.2).

## What the model set is

A bundle is a folder with one sub-folder per decision type (`BUILD_CARD/`, `DRAFT_CARDS/`, …), each holding `model.json`,
`weights.bin`, `reference.json`. Every sub-folder has its own schema hash; the engine checks it against the live encoder.

## Making a new one

```
py -3 ml/train_policy_ce.py --data D:/ares/data/r4_ce1 --out D:/ares/runs/policy/r4_ce2 \
    --prev D:/ares/runs/policy/r3_ce --epochs 2 --rows-per-game 8 --max-chunks-per-type 15
```

or, as a whole night with self-play and evaluation: `py -3 ml/nightly.py run --config ml/ce_config.json --name ce9
--start-from D:/ares/runs/policy/r4_ce2` (the pipeline writes `DONE` and the eval numbers itself).
