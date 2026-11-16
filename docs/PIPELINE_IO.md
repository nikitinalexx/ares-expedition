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
| bytes read by a 4-epoch value run | 4 x 119.7 GiB on the HDD | 26.3 GiB once (pack build) + 5.2 GiB once per run (pool to RAM) |
| bytes of a policy corpus | padded to the menu maximum | real candidates only |
| de-duplication set | rebuilt every run (12 M keys) | computed once, stored as a 12 M-bit mask |

## Rules that follow

1. A new corpus is packed once, immediately after it is generated, next to it (`<corpus>.pack`). Nothing trains from raw
   chunks twice.
2. A pack is bound to its schema hash, like the raw corpus. If the encoder changes, delete the pack with the corpus.
3. Never compress or truncate the observation to make a pack smaller: the remaining bytes are the model's input, and the
   point was to stop reading what the model does not use.
