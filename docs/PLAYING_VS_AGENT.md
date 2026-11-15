# Playing against the learned agent in the UI

The learned player is `PlayerDifficulty.AGENT_V2` ("Ares" in the new-game dialog). It plays with the **second** model
set, so the ordinary `AGENT` keeps the previous weights and you can compare both in one session.

## What to start

The models are chosen by JVM properties, so pass them to the Spring Boot process (IDE run configuration → VM options, or
`spring-boot.run.jvmArguments`):

```
-Dares.agent.models=D:/ares/runs/night7/exp/vs_random/it_0046/model_r2
-Dares.agent.models2=D:/ares/runs/ppo1/exp/ppo_mixed/it_0012/model
-Dares.agent.search=oneply
-Dares.agent.search.symmetric=true
-Dares.agent.search.types=BUILD_CARD,DRAFT_CARDS
-Dares.agent.search.pairs=true
-Dares.agent.valueModel=D:/ares/runs/value/v_r3_outcome
```

- `ares.agent.models` — the model set of `AGENT` (optional: without it `AGENT` falls back to random, `AGENT_V2` still works).
- `ares.agent.models2` — the model set of `AGENT_V2`. This is the strongest player we have: `ppo1/exp/ppo_mixed/it_0012`
  (PPO iteration 12: **39.9% vs SMART / 93.2% vs RANDOM** with search, 12.0 cards per game; the parent of the whole lineage
  was 24.05% / 84.8%).
- The three `search` properties switch on the 1-ply symmetric re-ranking at play time (about 1.4 ms per decision). They
  apply to both agents; drop them to see the same weights without search.
- The frontend: `cd frontend && npx ng serve` (the dialog now has the "Ares" option next to Easy / Normal / Hard / Expert).

The model bundle must match the current encoder; a model trained on another schema is refused at startup with an explicit
message (that is intentional — no silent reshaping).

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
