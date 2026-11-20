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

**Self-training decays silently (owner, 2026-11-17).** A loop that writes its next corpus with its own greedy choice can drop
an action and never notice: the phase mix went 20% (uniform) -> 7% -> 2.2%, the mulligan is declined in 100% of rows, and
nothing looked. So every self-play iteration must measure **action coverage per decision type** (`ml/action_coverage.py`:
slot entropy / menu, thin options in small menus, options that shrank against the previous corpus) and turn a flag into
exploration for that type in the next generation. A gap with a recorded reason lives in `ml/gap_allowlist.json` - the
detector must be teachable, or the exploration it injects is wasted. The ledger records the flags, so a new gap appears in
the record the moment it starts; finding gaps is the loop's job, not the owner's.

Nobody’s claims (owner or AI) are axioms: they are hypotheses checked by nightly runs, one variable at a time.
See `docs/EXPERIMENTS.md`.

**Read `docs/METHOD.md` before designing an experiment.** Its first rule came from a mistake that cost weeks: *a treatment that changes
BEHAVIOUR without changing the TRAINING DATA is a statement about the current player, not about the thing being tried.* Forcing a plan
onto a player trained for another plan (44.25%) was recorded as "that plan is bad" - it measured only that our player cannot play it, and
that misreading closed three directions that were never actually tested. Two corollaries: the value may not arbitrate exploration (it
rejects an untrained plan by construction), and a new plan is gated on the **derivative** of its curve, not on its level.

**Accepting a new network (owner's rule, 2026-11-14).** Win rate alone hides a degenerate player: a model beat RANDOM 95%
while a human playing it saw "picks phase 3, buys standard projects, 0-2 cards per game". So every new model is first
measured on **cards built per game** and **which phases it picks** — `py -3 ml/check_player.py --model DIR` prints both,
the phase-3 spend mix and a verdict (exit 1 when the profile is degenerate: <3 builds, >50% standard projects in phase 3,
>80% of picks in one phase). Only then look at the win rate.

**There is no "healthy" profile (owner, 2026-11-17).** The gate above catches degeneracy - never building, always the same
phase, standard projects instead of cards - and nothing more. There is **no upper band on cards per game** (more cards is
plausibly stronger, not a defect) and NETWORK's profile is a *clue*, not a standard: the numbers of a genuinely strong player
are unknown until a player stronger than NETWORK exists and we measure it. Do not turn a measured count into a norm.

## Agent (`services/agent`)
- `AGENT` / `AGENT_R`: all non-crisis decisions go through `DecisionAgent` (uniform random without a model).
- Observation must not contain hidden info — `ObservationCompletenessTest`.
- Training: `ml/train_all.py`; nightly: `ml/nightly.py` (`docs/NIGHTLY.md`). Self-play: `SelfPlayMain` (profile `selfplay`).
- Pipeline rules: nothing heavy on `C:`; volume free-space reserves; budget is **active** time; never delete models/evals.
  Exception agreed with the owner (2026-11-16): a *packed* dataset may live on the SSD (`C:`), because the pack is what
  makes training I/O cheap.
- **Efficiency rule (owner, 2026-11-16): every process must be efficient, not just "a bit less data".** One pass over a
  corpus, then only the bytes a model reads: raw chunks are packed once (`ml/pack_dataset.py`) and every trainer reads
  the pack (`--packed`). Never re-read the same data per epoch from the disk, never store candidate padding a model does
  not use, never rebuild a 12-million-key duplicate set on every run. See `docs/PIPELINE_IO.md`.
- Default night: runs **until stop** (no hours guessing). Optional `hours` / `--hours` only if you want a timed cap.

## Tests / docs
- `mvn test` — engine tests without DB. Known issues: `GameInvariantsTest.KNOWN_ISSUES`, `docs/BUGS.md`.
- `docs/GAME_RULES.md`, `docs/AI_PLAN.md`, `docs/NIGHTLY.md`, `docs/EXPERIMENTS.md`.
- Compare: `/simulations/compare`, `/simulations/compare/models`.
