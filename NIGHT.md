# night-sim: one night of NETWORK_V2 on a bigger machine

This branch exists to run one long experiment unattended. It carries:

* `models/net_s3_e1.zip` - the current best NETWORK_V2 value (the recovered `iter2` model fine-tuned on 10k games of itself
  vs SMART; 32.85% [30.0, 35.8] against NETWORK, 99% against RANDOM; plays through the owner's `Network2*` processors);
* `ml/night_run.sh` - the whole night as one script.

## Run it

```bash
git checkout night-sim
./ml/night_run.sh
```

That is the only command. Defaults: **50 000 games** (all self-play), chunks of 5 000, threads = every core, then **two
training passes** that continue from the same model (`MODEL`, not from scratch) with **no early stop** and the best
validation checkpoint kept; then the phase mix on 300 new games (phasedist), the value check on 300 fresh games, and two
measurements (NETWORK and RANDOM, 1 000 games each). Everything goes under `./night/`:

```
night/data/chunk_*/netdata.bin    the raw corpora (one per chunk)
night/data/all.bin                merged, game ids offset per chunk
night/data/fresh/netdata.bin      300 NEW games of the base, for the out-of-sample value check
night/models/trained.zip          the trained value (best validation)
night/models/trained_before_*.zip the previous model, kept before the retrain
night/logs/                       per-chunk generation, training, profile, value and eval logs
night/summary.txt                 the two CompareResult lines
```

**Re-running resumes**: a chunk whose `netdata.bin` already exists is skipped, and if `night/data/merged.ok` exists the
whole generation and merge are skipped - the existing corpus is reused as-is and only the training (plus the checks and the
measurements) runs again. The previous `trained.zip` is copied to `trained_before_<stamp>.zip` before it is overwritten.
The 300 fresh games live in `night/data/fresh` and never touch the main corpus.

**If you started a run before 22/11 20:00 UTC:** that script trained from scratch, which is not the self-play ladder. The
games it generated are still valid - update the branch, re-run the same command, and it skips generation and the merge and
only redoes the training, this time warm-started from `MODEL`. The 23/11 update additionally removes the early stop and runs
two epochs; the same `night/data` is reused.

## Knobs (environment variables)

| variable | default | meaning |
|---|---|---|
| `GAMES` | 50000 | total games to generate |
| `CHUNK` | 5000 | games per generation run (resume unit) |
| `NET_SHARE` | **0** | percent of chunks played against NETWORK; the rest is self-play |
| `THREADS` | **all cores** | generation/eval threads |
| `WORK` | ./night | output directory |
| `EVAL_GAMES` | 1000 | games per benchmark at the end |
| `EPOCHS` | 2 | training passes; no early stop, the best validation checkpoint is kept |
| `VAL_STEPS` | 2000 | validate every N steps; the best checkpoint is chosen from these |
| `BATCH` | 64 | 64 and 256 measured equal for this value, 10 worse (the agent critic's 4096 is another model) |
| `ROWS_PER_GAME` | 0 | cap rows per game before the split (0 = all); the 50k corpus has ~375 rows/game and ONE outcome, so all rows memorise the games |`n| `VAL_ROWS` | 50000 | rows in the validation slice; 50k at 375/game is ~133 games, 200k is ~533 |`n| `LR` | 0.00005 | learning rate; the best step moved 1500 -> 5500 when 1e-3 -> 1e-4 |`n| `DROPOUT` | 0.1 | dropout |
| `WEIGHT_DECAY` | 0.0001 | L2 weight decay (the trainer had none; CONFIG_A used 5e-5) |
| `SAVE_CHECKPOINTS` | 10 | checkpoints plus the last state, each measured by PLAY |
| `CKPT_GAMES` | 1000 | games per checkpoint in the play eval |
| `RED_RISE` | 0.03 | RED FLAG when val >= best + rise ... |
| `RED_RISES` | 2 | ... and this many consecutive rises |`n| `PROGRESS_MIN` | 0 | print the latest training line (step/val/AUC + best) to the console every N minutes; 0 = off |`n| `PROFILE_GAMES` | 300 | new games for the phase-mix audit (`--mode phasedist`); `check_player.py` is the AGENT's gate, not this value's |
| `FRESH_GAMES` | 300 | new games for `value_eval_bin.py` (loss/AUC out of sample) |
| `MODEL` | models/net_s3_e1.zip | the value the games are generated with **and that training continues from** |
| `MIN_FREE_GB` | 100 | refuse to start below this free space |

Examples:

```bash
GAMES=80000 ./ml/night_run.sh              # a longer night
GAMES=20000 ./ml/night_run.sh              # a short one (a test on real volume)
NET_SHARE=20 GAMES=50000 ./ml/night_run.sh # a fifth against NETWORK (see the warning)
PROGRESS_MIN=10 ./ml/night_run.sh         # watch the training stats every 10 minutes
ROWS_PER_GAME=16 VAL_ROWS=200000 ./ml/night_run.sh  # cap rows/game and use a larger validation (the 50k diagnosis)
LR=0.00002 ./ml/night_run.sh              # a slower schedule (the best step should move later)
```

## The warning that comes from measurement (22/11)

**Do not train the value on games against NETWORK yet.** Measured on the same base model: fine-tuning on 5 000 games of
itself vs SMART gave 31-33% against NETWORK, fine-tuning on 5 000 games *against NETWORK* gave **14.6%**, and the 50/50 mix
gave 12.5-16.8%. In those games our player wins ~7%, so the states are labelled "losing" and the value learns to score its
own positions low. `NET_SHARE` defaults to 0 for that reason; the owner's "half self-play, half NETWORK" idea is right in
principle but needs the player to win a real share of those games first.

## The red flag and the checkpoint play eval

A small wobble in val is ignored. When the val is clearly running away, the trainer stops with `RED FLAG: overfitting` and exit
code 42 as soon as `val >= best + RED_RISE` and there are `RED_RISES` consecutive rises (default 0.03 and 2). The night script
then writes `RED: overfitting, not measured` into `night/summary.txt` and does not measure the run.

When the curve is healthy, the training writes `SAVE_CHECKPOINTS` evenly spaced checkpoints plus the last state
(`night/models/trained_ckpt_*.zip`). The script measures each of them **and** the val-best `trained.zip` on `CKPT_GAMES` games
against NETWORK, picks the best by PLAY (not by val), and runs the phase mix, the fresh-value check and the final 1000+1000 on
that best-play model. Example:

```bash
WEIGHT_DECAY=0.0001 SAVE_CHECKPOINTS=10 PROGRESS_MIN=10 ./ml/night_run.sh
RED_RISE=0.03 RED_RISES=2 ./ml/night_run.sh      # the red-flag thresholds
```

## Time budget

At the measured 1.4-2.0 games/s (all cores) 50 000 games are 7-10 hours; **two full passes** over an 18M-row corpus are
~1.5-2 hours on the same machine (the first 50k run did 0.7M rows plus validation in ~4 minutes), the checkpoint play eval is ~1.5 hours (11 candidates x 1000 games), and the profile/fresh/final measure block is
~40 minutes. There is no early stop: a dip at one validation does not end the run, and the
best checkpoint over both epochs is what gets saved. The script prints the rate after every chunk, so the real ETA is visible
from `night/logs` and the console.

## Requirements

* JDK 21+ and Maven (the script builds `target/classes` and the classpath itself);
* Python 3 with `numpy` and `torch` (`pip install numpy torch`; a GPU is used automatically when present);
* ~100 GB free for 50 000 games (each 5 000-game chunk is ~6-7 GB of binary corpus and the merge writes one more copy; the script refuses to start below `MIN_FREE_GB=100`).

If something fails the script stops with `ERROR: ...` and the line number; every external command logs into `night/logs/`.
