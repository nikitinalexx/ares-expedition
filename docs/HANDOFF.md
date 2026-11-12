# HANDOFF — new AI for Expedition Ares

For the next assistant continuing this work. Also read `AGENTS.md`, `docs/AI_PLAN.md`, `docs/NIGHTLY.md`, `docs/EXPERIMENTS.md`.
Talk to the owner in **Russian**. Code/identifiers in English. Branch: `nn-pure-java-verify`.
**Owner commits; do not commit or push unless asked.**

## Goal and owner rules

Build a computer player stronger than **NETWORK** (prod) and NETWORK_V2. Constraints:

1. **Do not repeat the owner’s approach** (value net + hand-written projections + enumerating moves): slow (~1 game/s), biased assumptions, bugs. Chosen path: **policy over legal candidates** (`docs/AI_PLAN.md`), optional search later.
2. **No hand-written behaviour bias.** Reward = game outcome only. SMART heuristics are a measuring stick / temporary scaffold, never a training teacher.
3. **Ship the full agent (all decisions + observation) before serious training.** Isolated single-decision training is infrastructure only.
4. **Test new pieces.** Mutation-check tests when practical.
5. Engine bugs: find and cover them (`docs/BUGS.md`).
6. Nightly pipeline: one command, resumable, keeps finished chunks, never deletes models/evals.
7. Heroku/prod out of scope until the local AI is strong.

## Machine (owner runs nights here)

Windows, ~32 GB RAM, NVIDIA GPU (CUDA for PyTorch training only; self-play is CPU Java).
Disks: `C:` — OS, keep free (pipeline writes nothing important there); `D:` — runs/data/tmp (`D:\ares\…` in `ml/nightly_config.json`).
JDK 21+ (bytecode `release 21`). Maven: prefer `-s .mvn/settings.xml`. Python 3.x + torch in `ml/.venv` for training.

Typical night: start `nightly run …` and leave it; it runs until `nightly stop` / Ctrl+C (or disk). No need to set `hours`. Optional timed budget exists if someone wants one.

## What exists

**Engine / tests:** `mvn test` (no DB). Fixes: biased shuffle, negative plants on some payments. Open: TR-vs-params invariant (`KNOWN_ISSUES`).

**Agent:** all non-crisis `DecisionType`s; observation v3 (`ObservationBuilder`); `DecisionFile` / `DecisionLogs`; `SelfPlayMain`; `MlpPhaseModel` / `NeuralPolicy`; `ml/train_all.py` (shared trunk).

**Nightly:** `ml/nightly.py` — selfplay → train → eval, successive halving, disk rotation, OOM restart logging, window shrink, delete data of earlier runs on new start, round-robin time sharing, continue survivors while budget remains. Details: `docs/NIGHTLY.md`. Hypotheses log: `docs/EXPERIMENTS.md`.

**External players as yardsticks only:** RANDOM / SMART (fast eval), NETWORK / NETWORK_V2 (slow benchmark at end of night). Not training teachers.

## Working theory (short)

Nights 1–3: outcome self-play gives a signal; mixed pool + early stop + ≤32 rows/game/type removes night-1 memorisation; agent beats RANDOM (~35–44%) and scores a few % vs SMART; **0% vs NETWORK**, ~5% vs NETWORK_V2. Continuing self-play from a good checkpoint did not steadily climb (night3). Eval needs ≥2000 games/opponent to rank experiments. Disk (~12 KB/row) limits windows and self-play speed.

Owner notes (hypotheses, not axioms) about weak policies that pass/sell losing tempo to RANDOM’s constant small gains, and NETWORK always acting via VALUE — see `docs/EXPERIMENTS.md` (H-P1).

## Next

1. Fix scheduler leftovers / use full `--hours` (done after night3).
2. Diagnose passivity / tempo vs NETWORK (action distributions) before inventing architecture changes.
3. Nightly climb with fewer parallel experiments (disk) and hours covering the whole free window.
4. Compact decision format (long-term disk lever).
