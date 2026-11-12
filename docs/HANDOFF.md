# HANDOFF -- new AI for Expedition Ares

For the next assistant continuing this work. Also read AGENTS.md, docs/AI_PLAN.md, docs/NIGHTLY.md, docs/EXPERIMENTS.md.
Talk to the owner in Russian. Code/identifiers in English. Branch: nn-pure-java-verify.
Owner commits; do not commit or push unless asked.

## Goal and owner rules

Build a computer player stronger than NETWORK (prod) and NETWORK_V2. Constraints:

1. Do not repeat the owner's approach (value net + hand-written projections + enumerating moves): slow (~1 game/s), biased assumptions, bugs. Chosen path: policy over legal candidates (docs/AI_PLAN.md), optional search later.
2. No hand-written behaviour bias. Reward = game outcome only. SMART heuristics are a measuring stick / temporary scaffold, never a training teacher.
3. Ship the full agent (all decisions + observation) before serious training. Isolated single-decision training is infrastructure only.
4. Test new pieces. Mutation-check tests when practical.
5. Engine bugs: find and cover them (docs/BUGS.md).
6. Nightly pipeline: one command, resumable, keeps finished chunks, never deletes models/evals.
7. Heroku/prod out of scope until the local AI is strong.

## Machine (owner runs nights here)

Windows, ~32 GB RAM, NVIDIA GPU (CUDA for PyTorch training only; self-play is CPU Java).
Disks: C: -- OS, keep free (stage dir only, cleared); D: -- runs/data/tmp (D:/ares/... in ml/nightly_config.json). ~400 GB free on D:.
JDK 21+ (bytecode release 21). Maven: prefer -s .mvn/settings.xml. Python 3.x + torch in ml/.venv for training.

Throughput (measured night4): selfplay 120 games/s (CPU, 82% on neural forward); eval 128 games/s vs RANDOM / 86 vs SMART; train ~34s/iteration. Iteration budget: ~5-6 min at 30000 games.

## What exists

Engine / tests: mvn test (no DB). Fixes: biased shuffle, negative plants on some payments. Open: TR-vs-params invariant (KNOWN_ISSUES).

Agent: all non-crisis DecisionTypes; observation v3 (ObservationBuilder); DecisionFile / DecisionLogs; SelfPlayMain; MlpPhaseModel / NeuralPolicy; ml/train_all.py (shared trunk).

Nightly: ml/nightly.py -- selfplay -> train -> eval, successive halving, disk rotation, OOM restart logging, window shrink, delete data of earlier runs on new start, round-robin time sharing, continue survivors while budget remains. Details: docs/NIGHTLY.md. Hypotheses log: docs/EXPERIMENTS.md.

External players as yardsticks only: RANDOM / SMART (fast eval), NETWORK / NETWORK_V2 (slow benchmark at end of night). Not training teachers.

## Working theory (short)

Nights 1-3: outcome self-play (AGENT:AGENT) gives a signal; best result ~44% vs RANDOM, 0% vs NETWORK.
Night 4: rows8 (max 8 rows/game/type) it7 reached 45.7% vs RANDOM -- best so far; adopted as default.
Root diagnosis: AGENT:AGENT self-play is a neutral signal loop when both agents are ~50% vs each other (which is always -- zero-sum). The 'winner' is noise, not skill. Need an external reference to anchor the signal.

## Night 5 -- running

Hypothesis H-N1: AGENT:RANDOM gives cleaner outcome labels than AGENT:AGENT at current skill level.
Config: single experiment vs_random, AGENT:RANDOM:1, 30000 games/it, rows8, start-from night4 (rows8 it7).
Command: nightly run night5 --start-from night4
Success: win rate vs RANDOM >50% within 20 iterations. Else: H-N1 rejected, revisit strategy.

## Next if H-N1 fails

1. Filtered imitation: collect AGENT:RANDOM games, train only on AGENT's winning decisions (label=1 only).
2. Mixed pool: 50% AGENT:RANDOM + 50% AGENT:AGENT once win rate > 50%.
3. Measure H-P1 (action distribution: does agent pass/sell too much vs RANDOM/NETWORK?).
4. GPU inference for self-play (saves ~20% wall time; not the current bottleneck).
