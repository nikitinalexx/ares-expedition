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

Nightly: ml/nightly.py -- selfplay -> train -> eval, successive halving PLUS a per-experiment plateau guard (stops an experiment that is no longer beating its own best score, independent of rung timing -- this is what makes scheduling more experiments per night reasonable), disk rotation (shrink messages now name the actual violated constraint, not just the data budget), OOM restart logging, delete data of earlier runs on new start, round-robin time sharing, continue survivors while budget remains. Trainers (train_all.py / train_phase.py) read every chunk file ONCE with a buffered read, build train+val together in that pass, then train from RAM only -- an earlier version re-read from disk (train_phase.py: every epoch) via np.memmap, which on a hard disk is the gap between seconds and most of a quarter hour with the disk pegged and the GPU idle. Details: docs/NIGHTLY.md. Hypotheses log: docs/EXPERIMENTS.md ("I/O" section has the night5 incident this came from).

External players as yardsticks only: RANDOM / SMART (fast eval), NETWORK / NETWORK_V2 (slow benchmark at end of night). Not training teachers.

## Working theory (short)

Nights 1-3: outcome self-play (AGENT:AGENT) gives a signal; best result ~44% vs RANDOM, 0% vs NETWORK.
Night 4: rows8 (max 8 rows/game/type) it7 reached 45.7% vs RANDOM -- best so far; adopted as default.
Root diagnosis: AGENT:AGENT self-play is a neutral signal loop when both agents are ~50% vs each other (which is always -- zero-sum). The 'winner' is noise, not skill. Need an external reference to anchor the signal.

## Night 5 -- stopped early by the owner at it7, H-N1 still open

Hypothesis H-N1: AGENT:RANDOM gives cleaner outcome labels than AGENT:AGENT at current skill level.
Config: single experiment vs_random, AGENT:RANDOM:1, 30000 games/it, rows8, start-from night4 (rows8 it7).
Command: nightly run night5 --start-from night4 (same command resumes)
Success: win rate vs RANDOM >50% within 20 iterations. Else: H-N1 rejected, revisit strategy.

Stopped not because of H-N1's numbers but because of real pipeline bugs the owner caught by watching it run (confusing
logs, a disk-bound train stage, a confusing disk-shrink message, no guard against running a dead experiment out its
full time) -- all fixed; see docs/EXPERIMENTS.md, "Night 5 -- 2026-11-12", for exactly what was wrong and what changed.
Resuming the same command is the natural next step before judging H-N1.

## Next if H-N1 fails

1. Filtered imitation: collect AGENT:RANDOM games, train only on AGENT's winning decisions (label=1 only).
2. Mixed pool: 50% AGENT:RANDOM + 50% AGENT:AGENT once win rate > 50%.
3. Measure H-P1 (action distribution: does agent pass/sell too much vs RANDOM/NETWORK?).
4. GPU inference for self-play (saves ~20% wall time; not the current bottleneck).
