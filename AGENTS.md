# AGENTS.md — ares-expedition

> Continuing the new AI? Read `docs/HANDOFF.md` first (state, owner rules, file map, commands, task queue).

Online port of *Terraforming Mars: Expedition Ares* (Spring Boot 3 + PostgreSQL, Angular in `frontend/`),
with computer players of different strength. Production is Heroku (auto-deploy from `main`).

## Build and run
- **JDK 21+** (`pom.xml`: `release 21`). On JDK 25+ Lombok needs `<proc>full</proc>` or use JDK 21.
- If `~/.m2/settings.xml` points at a private repo, use the project file: `mvn -s .mvn/settings.xml compile`.
- DB: PostgreSQL (see `README.md`), Spring profile `local`. Heroku uses `application-prod.properties`.
- Heroku Java version is in `system.properties` (21). Prod is out of scope until the local AI is strong.
- Frontend: `cd frontend && ng serve`.

## Code map (`src/main/java/com/terraforming/ares`)
`cards`, `model` / `mars`, `processors` / `states` / `services`, `validation`,
`controllers/GameController`, `entity` / `repositories`.

## Computer players (`model/PlayerDifficulty`, `services/ai`)
| Difficulty | What |
|---|---|
| `RANDOM`, `SMART` | heuristics (`AiService`, …) |
| `NETWORK` | old feed-forward net (`DeepNetwork`). Weights: `1701242492845_epoch1_result_N_reg4_smart_1.dnet_epoch_1.txt`. **On prod `main`.** |
| `NETWORK_EXPERIMENT` | NETWORK variant |
| `NETWORK_V2` | value net (`services/ai/network2`). Pure-Java inference via `NNService` → `PureJavaValueNetwork` (no CUDA/ND4J in the app). |

Do not put ND4J/DL4J/CUDA back into the app `pom.xml`. Do not push to `main` or change `system.properties`/pom without an explicit ask.

## Training foundations
1. **Data quality** — what we record, who plays, how much, shuffle, leakage, label balance.
2. **Training quality** — architecture, regularisation, early stopping — only on top of good data.

Nobody’s claims (owner or AI) are axioms: they are hypotheses checked by nightly runs, one variable at a time.
See `docs/EXPERIMENTS.md`.

## Agent (`services/agent`)
- `AGENT` / `AGENT_R`: all non-crisis decisions go through `DecisionAgent` (uniform random without a model).
- Observation must not contain hidden info — `ObservationCompletenessTest`.
- Training: `ml/train_all.py`; nightly: `ml/nightly.py` (`docs/NIGHTLY.md`). Self-play: `SelfPlayMain` (profile `selfplay`).
- Pipeline rules: nothing heavy on `C:`; volume free-space reserves; budget is **active** time; never delete models/evals.
- Default night: runs **until stop** (no hours guessing). Optional `hours` / `--hours` only if you want a timed cap.

## Tests / docs
- `mvn test` — engine tests without DB. Known issues: `GameInvariantsTest.KNOWN_ISSUES`, `docs/BUGS.md`.
- `docs/GAME_RULES.md`, `docs/AI_PLAN.md`, `docs/NIGHTLY.md`, `docs/EXPERIMENTS.md`.
- Compare: `/simulations/compare`, `/simulations/compare/models`.
