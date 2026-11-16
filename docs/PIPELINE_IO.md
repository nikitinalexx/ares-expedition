# Pipeline I/O: one pass over the corpus, then only what the model reads

The owner's rule (2026-11-16): *"all processes must be efficient, not just slightly reducing the data volume"*. This page
records what was measured, why the old pipeline was slow, and how the pack works. Nothing here changes the game or the
model - it changes how often the same bytes are read.

## What was measured (16.11.2026)

| fact | number |
|---|---|
| dataset `D:/ares/data/scale1090` | 750 chunks, **119.7 GiB**, 12.08 M rows |
| disk `D:` sequential read, whole-file `read()` | **106-113 MB/s** (ST2000DM008, HDD) |
| disk `D:` sequential read through `np.memmap` | **48 MB/s** - a memmap faults one page at a time |
| disk `C:` | Samsung 980 PRO SSD, 465 GB, 164 GB free |
| share of a row that is the candidate block (padded to the menu maximum) | **76-96%** for most decision types |
| observation bytes of the whole corpus | 12.08 M x 2180 B = **26.3 GiB** |
| `v_scale_full` (4 epochs on that corpus, memmap reader) | **40 min** wall, AUC 0.8676 |

Two separate wastes, both measured: the volume (candidate padding) and the reader (memmap instead of one `read()`).

The candidate padding was the volume: a `BUILD_INPUT` row is 7 real candidates padded to 200 (95% of the row), a
`BUILD_CARD` row 3.7 padded to 81 (86%). A value model never reads a byte of it, and every training run re-read the whole
corpus for every epoch.

The reader was the second one: `np.memmap` reads 48 MB/s where a single `read()` of the same chunk reads 106-113 MB/s,
because a memmap faults pages one at a time and the access pattern (observation, skip the candidate block, observation)
is not what the readahead expects. `ml/pack_dataset.py` therefore reads whole files.

## The pack (`ml/pack_dataset.py`)

One sequential pass, writing only what a model reads:

```
<out>/<TYPE>.obs.f16    float16 [n, obsSize], contiguous
<out>/<TYPE>.lab.npz    outcome, margin, seat, context, round, game, step, keep
<out>/manifest.json     schema hash, layout, rows per type, source
```

`keep` is the cross-type de-duplication mask over `(game, step)` - the same state recorded by several decision types must
count once for the value pool. It is computed **once**, instead of rebuilding a 12-million-key Python set on every start.

With `--candidates` it also writes the candidate block **without padding**:

```
<out>/<TYPE>.cand.f16   float16 [sum(count) + 1, candidateFeatures]   (the last row is zeros, used for padding)
<out>/<TYPE>.cids.u2    uint16  [sum(count) + 1]
<out>/<TYPE>.cand.npz   count, chosen, offset [n+1]
```

A batch pads to the largest row **in that batch** and reads the zero row for the padding slots, so a batch of 512 rows of
`BUILD_INPUT` costs 512 x 7 real candidates instead of 512 x 200.

`ml/pack_check.py` proves the pack is bit-exact against the raw chunks: observation values, every label, every candidate
feature, every id, and that the padding is zero. It is the gate before any training run uses a pack.

## How the trainers use it

* `ml/train_value.py --packed DIR`: builds the same pool as before (de-duplication, per-game cap, `(game, seat, step)`
  order, TD links) from the pack. If the capped pool is at most `--pool-ram-gb` (default 8), it is read into RAM in one
  ascending pass and the epochs then never touch the disk. Otherwise `PackedObs` gathers each batch from the memmap.
* `ml/train_policy_ce.py --packed DIR`: per-type candidate tensors are gathered per batch (`PackedCandidates`), so a full
  corpus no longer needs a chunk subset to fit in RAM. The cross-type de-duplication mask is *not* applied to policy
  pools: the same state under two decision types is a legitimate row for both heads.
* `--rows-per-game`, `--val-fraction`, `--drop-prefix`, `--include-afterstates`, TD targets and `--score` columns behave
  as before; `--max-chunks-per-type` is applied when packing, because a pack only contains what was packed.

## Numbers after the change

| | before | after |
|---|---|---|
| pack build on `D:/ares/data/scale1090` | - | **25 min**, 24.5 GiB written, 1 047 208 duplicate rows dropped once |
| bytes read by a 4-epoch value run | 4 x 119.7 GiB on the HDD | 24.5 GiB once per run from the SSD, then 5.2 GiB into RAM once |
| **wall time of the same 4-epoch run** | **2406 s** | **79.8 s** |
| best AUC of that run (margin target, 16 rows/game) | 0.8676 | 0.8736 (same seed; the two runs draw slightly different per-game rows) |
| bytes of a policy corpus | padded to the menu maximum | real candidates only |
| de-duplication set | rebuilt every run (12 M keys) | computed once, stored as a 12 M-bit mask |

## Rules that follow

1. A new corpus is packed once, immediately after it is generated, next to it (`<corpus>.pack`). Nothing trains from raw
   chunks twice. `ml/nightly.py` does this per window automatically (`ensure_pack`) and falls back to the raw chunks if
   the pack fails.
2. A pack is bound to its schema hash, like the raw corpus. If the encoder changes, delete the pack with the corpus.
3. Never compress or truncate the observation to make a pack smaller: the remaining bytes are the model's input, and the
   point was to stop reading what the model does not use.
4. The same rule applies to every new tool: read the corpus once, keep only what is used, and state the measured cost.

## The producer wrote 87% padding: where the HDD time actually goes (2026-11-16, night)

Owner's question: why is RAM at 9 GB of 32 GB while the HDD sits at 100%? Measured during a bootstrap iteration:

| | value |
|---|---|
| self-play `boot3` | 19 860 games, 1.69 M rows, **27.6 GB written to the HDD** in ~18 min (18.2 games/s) |
| the same data, observation only | 1.69 M x 2512 B = **4.2 GB** |
| the verified pack of it | **3.1 GB** |
| disk during packing | **66 MB/s, queue length 79, disk time 242%**; the SSD sat at 0% |
| RAM | python 1.7 GB (one chunk at a time, by design) |

So the memory number is not the problem: the packer streams a chunk, writes the observation, drops the chunk. The disk is
the problem, and the cause is one level up from the packer: the simulator writes every row with its candidate block padded to
the menu cap (`BUILD_CARD` 40 x 269 x 2 = 21.5 KB per row, `BLUE_INPUT` 48 x 269 x 2 = 25.8 KB), and the packer then reads
all of it back to throw the padding away. One iteration costs **55 GB of HDD traffic** (27.6 write + 27.6 read) about 9
minutes of wall time with the CPU and the SSD idle.

Immediate fix (in `ml/bootstrap_loop.ps1`): the raw corpus is written to the **SSD**, and after the bit-exact pack check the
raw files are deleted - the pack is the dataset (it reproduces the chunks exactly), and the raw files exist only to be packed.

The structural fix, not done yet: emit the compact form directly from the simulator (real candidates, variable length), i.e.
make the pack the native output format. The corpus would then be ~4-5 GB instead of 27.6 GB, generation writes 5-7x less and
the separate packing pass disappears. Until that lands, every iteration pays ~55 GB of HDD traffic it does not need.
