# HANDOFF -- new AI for Expedition Ares

For the next assistant. Also read AGENTS.md, docs/AI_PLAN.md, docs/NIGHTLY.md, docs/EXPERIMENTS.md.
Talk to the owner in Russian. Code/identifiers in English. Branch: nn-pure-java-verify.
Owner commits; do not commit or push unless asked.

## Goal

Build a computer player stronger than NETWORK (prod) and NETWORK_V2.
Approach: policy over legal candidates. No hand-written bias. Reward = game outcome only.
Heroku/prod out of scope until local AI is strong.

## Machine

Windows, ~32 GB RAM, NVIDIA GPU (PyTorch training). D: ~590 GB free.
JDK 21+. Maven: -s .mvn/settings.xml. Python in ml/.venv.
Throughput: selfplay 52 games/s vs SMART, 170+ vs RANDOM. Train ~30s. Eval 41s.
Iteration budget: ~3.5-4 min at 10K-12K games/it.

## State after Night 7 (13 hours, 120 iterations)

Best model: Night 7 `vs_random` it46.
- **85.7% vs RANDOM** (up from 42% in Night 5, 61.8% in Night 6)
- **25.9% vs SMART** (up from 4% in Nights 1-4, 7% in Night 5-6)
- **0.5% vs NETWORK** (avg points: 67.6 vs 104.72)

## Key Diagnosis: The 68-Point Ceiling
- The agent currently averages **~68 points** per game.
- Against RANDOM (scores ~50 pts), 68 points wins 86% of games. RANDOM is solved.
- After iteration 46, win share vs RANDOM reached 84%. The agent stopped growing because beating RANDOM by 1 point or 50 points gives the exact same reward (`outcome = 1.0`). There is zero gradient signal pushing for higher engine output.
- Against SMART (scores ~82 pts), 68 points loses ~74% of games.
- Against NETWORK (scores ~105 pts), 68 points loses 99% of games.
- **Solution**: Curriculum stage 2 -- train against SMART. In games vs SMART, only plays that produce >82 points receive `outcome = 1.0`, directly forcing the neural policy to build stronger engines and optimize terraforming tempo.

## Night 8 -- Ready to Run

Config (`ml/nightly_config.json`):
- Experiment 1: `vs_smart` (`AGENT:SMART:1`, 10000 games/it)
- Experiment 2: `mixed_smart_random` (`AGENT:SMART:0.7,AGENT:RANDOM:0.3`, 12000 games/it)
- `selfplay.temperature`: 0
- `keep_data_iterations`: 1
- `rungs.keep`: 2 (both run round-robin all night until stopped)
- `plateau_patience`: null (disabled)

Command: `nightly run night8 --start-from night7`
