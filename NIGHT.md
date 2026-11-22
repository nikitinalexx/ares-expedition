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

That is the only command. Defaults: **50 000 games** (all self-play), chunks of 5 000, threads = every core, then one training
pass **that continues from the same model** (`MODEL`, not from scratch), then two measurements (NETWORK and RANDOM, 1 000
games each). Everything goes under `./night/`:

```
night/data/chunk_*/netdata.bin   the raw corpora (one per chunk)
night/data/all.bin               merged, game ids offset per chunk
night/models/trained.zip         the trained value
night/logs/                      per-chunk generation, training, evaluation logs
night/summary.txt                the two CompareResult lines
```

**Re-running resumes**: a chunk whose `netdata.bin` already exists is skipped, so a crash or a reboot costs only the chunk
in flight. To start over, delete `./night` (or set `WORK`).

**If you started a run before 22/11 20:00 UTC:** that script trained from scratch, which is not the self-play ladder. The
games it generated are still valid - update the branch, re-run the same command, and it skips generation and the merge and
only redoes the training, this time warm-started from `MODEL`.

## Knobs (environment variables)

| variable | default | meaning |
|---|---|---|
| `GAMES` | 50000 | total games to generate |
| `CHUNK` | 5000 | games per generation run (resume unit) |
| `NET_SHARE` | **0** | percent of chunks played against NETWORK; the rest is self-play |
| `THREADS` | **all cores** | generation/eval threads |
| `WORK` | ./night | output directory |
| `EVAL_GAMES` | 1000 | games per benchmark at the end |
| `MODEL` | models/net_s3_e1.zip | the value the games are generated with **and that training continues from** |
| `MIN_FREE_GB` | 100 | refuse to start below this free space |

Examples:

```bash
GAMES=80000 ./ml/night_run.sh              # a longer night
GAMES=20000 ./ml/night_run.sh              # a short one (a test on real volume)
NET_SHARE=20 GAMES=50000 ./ml/night_run.sh # a fifth against NETWORK (see the warning)
```

## The warning that comes from measurement (22/11)

**Do not train the value on games against NETWORK yet.** Measured on the same base model: fine-tuning on 5 000 games of
itself vs SMART gave 31-33% against NETWORK, fine-tuning on 5 000 games *against NETWORK* gave **14.6%**, and the 50/50 mix
gave 12.5-16.8%. In those games our player wins ~7%, so the states are labelled "losing" and the value learns to score its
own positions low. `NET_SHARE` defaults to 0 for that reason; the owner's "half self-play, half NETWORK" idea is right in
principle but needs the player to win a real share of those games first.

## Time budget

At the measured 1.4-2.0 games/s (12 threads) 50 000 games are 7-10 hours; the training pass is 1-2 hours on a 3.9M-row
corpus (the trainer early-stops when the validation slice stops improving). The script prints the rate after every chunk,
so the real ETA is visible from `night/logs` and the console.

## Requirements

* JDK 21+ and Maven (the script builds `target/classes` and the classpath itself);
* Python 3 with `numpy` and `torch` (`pip install numpy torch`; a GPU is used automatically when present);
* ~100 GB free for 50 000 games (each 5 000-game chunk is ~6-7 GB of binary corpus and the merge writes one more copy; the script refuses to start below `MIN_FREE_GB=100`).

If something fails the script stops with `ERROR: ...` and the line number; every external command logs into `night/logs/`.
