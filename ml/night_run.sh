#!/usr/bin/env bash
# One night of NETWORK_V2 on a bigger machine: generate games of the current best model, merge, train, measure.
#
#   ./ml/night_run.sh                 # defaults: 50 000 games (half vs NETWORK, half self-play), one training pass
#   GAMES=80000 NET_SHARE=70 ./ml/night_run.sh
#
# Everything is written under $WORK (default ./night): data, models, logs, summary.txt. Re-running resumes: a chunk whose
# netdata.bin already exists is skipped, so a crash or a reboot costs only the chunk in flight.
set -euo pipefail

cd "$(dirname "$0")/.."

GAMES=${GAMES:-50000}
CHUNK=${CHUNK:-5000}
NET_SHARE=${NET_SHARE:-0}           # percent of chunks vs NETWORK. Measured 22/11: 50% NETWORK + 50% self-play collapsed the model to 12.5-16.8% (the value learns "my positions lose"); self-play only holds 30.6%. Set it only if you know why.
WORK=${WORK:-./night}
MODEL=${MODEL:-models/net_s3_e1.zip}
JAVA=${JAVA:-java}
MVN=${MVN:-mvn}
if [ -z "${PY:-}" ]; then
    if command -v py >/dev/null 2>&1 && py -3 -c "import sys" >/dev/null 2>&1; then
        PY_CMD=(py -3)
    elif command -v python3 >/dev/null 2>&1 && python3 -c "import sys" >/dev/null 2>&1; then
        PY_CMD=(python3)
    else
        PY_CMD=(python)
    fi
else
    read -r -a PY_CMD <<<"$PY"
fi
CONCURRENCY=${CONCURRENCY:-$( (nproc 2>/dev/null || echo 8) )}
THREADS=${THREADS:-$CONCURRENCY}   # all cores by default (the owner's ask: take every core the CPU has)
MIN_FREE_GB=${MIN_FREE_GB:-100}
EVAL_GAMES=${EVAL_GAMES:-1000}
EPOCHS=${EPOCHS:-2}                  # at least two passes: a dip at one step does not mean the later step is worse
VAL_STEPS=${VAL_STEPS:-2000}         # validate often enough to pick the best step, cheaply
PROFILE_GAMES=${PROFILE_GAMES:-300}  # phase-mix audit on NEW games (phasedist), not the agent's check_player
FRESH_GAMES=${FRESH_GAMES:-300}      # new games for the out-of-sample value check
BATCH=${BATCH:-64}                   # NETWORK_V2 value: 64 and 256 measured equal, 10 worse; the agent critic's 4096 is another model
ROWS_PER_GAME=${ROWS_PER_GAME:-0}    # 0 = all rows (the default; NOT used for NETWORK_V2; kept as an optional A/B only)
VAL_ROWS=${VAL_ROWS:-50000}          # validation rows; 50k is enough (it covers every held-out game; a bad curve shows even on 5k)
LR=${LR:-0.00005}                    # the best step moved 1500 -> 5500 when lr went 1e-3 -> 1e-4; a main suspect for the early val best
DROPOUT=${DROPOUT:-0.1}
WEIGHT_DECAY=${WEIGHT_DECAY:-0.0001} # the trainer had none; the owner's CONFIG_A used 5e-5
SAVE_CHECKPOINTS=${SAVE_CHECKPOINTS:-10} # N checkpoints + the last, picked by PLAY
CKPT_GAMES=${CKPT_GAMES:-1000}       # games per checkpoint in the play eval
RED_RISE=${RED_RISE:-0.03}           # RED FLAG: val >= best + rise
RED_RISES=${RED_RISES:-2}            # ... and this many consecutive rises
TRAIN_EVAL_ROWS=${TRAIN_EVAL_ROWS:-4096} # the last N train rows evaluated as "seen" at every validation (0 = off)
PROGRESS_MIN=${PROGRESS_MIN:-0}      # print the latest training line every N minutes to the console (0 = off)
PROGRESS_SEC=${PROGRESS_SEC:-$((PROGRESS_MIN * 60))}

log() { printf '%s %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { log "ERROR: $*"; exit 1; }
trap 'log "FAILED at line $LINENO; see $WORK/logs" >&2' ERR

mkdir -p "$WORK/data" "$WORK/models" "$WORK/logs"

log "=== preflight"
[ -f "$MODEL" ] || fail "model not found: $MODEL"
command -v "$JAVA" >/dev/null 2>&1 || fail "java not found"
command -v "${PY_CMD[0]}" >/dev/null 2>&1 || fail "python not found"
command -v "$MVN" >/dev/null 2>&1 || fail "maven not found"
"${PY_CMD[@]}" -c "import numpy" >/dev/null 2>&1 || fail "python numpy is missing (pip install numpy)"
"${PY_CMD[@]}" -c "import torch" >/dev/null 2>&1 || fail "python torch is missing (pip install torch)"
JAVA_MAJOR=$("$JAVA" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
[ "${JAVA_MAJOR:-0}" -ge 21 ] || fail "java 21+ is required (found ${JAVA_MAJOR:-unknown})"
FREE_GB=$(df -Pk "$WORK" | awk 'NR==2 {printf "%d", $4/1024/1024}')
[ "${FREE_GB:-0}" -ge "$MIN_FREE_GB" ] || fail "only ${FREE_GB} GB free on $WORK, need ${MIN_FREE_GB} GB"
log "java $JAVA_MAJOR, $CONCURRENCY cores ($THREADS threads), ${FREE_GB} GB free, model $MODEL"

log "=== build"
"$MVN" -q -s .mvn/settings.xml -DskipTests compile || fail "mvn compile failed (network needed on a fresh clone)"
"$MVN" -q -s .mvn/settings.xml dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime \
    || fail "mvn dependency:build-classpath failed"
CP=$(cat target/cp.txt)
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
    *) SEP=":" ;;
esac
[ -n "$CP" ] || fail "empty classpath"

generate() {   # dir, matchups, games, label
    local dir="$1" match="$2" games="$3" label="$4"
    if [ -s "$dir/netdata.bin" ]; then
        log "  $label: already has $(du -h "$dir/netdata.bin" | cut -f1), skipped"
        return 0
    fi
    mkdir -p "$dir" "$WORK/logs/decisions_$label"
    log "  $label: $games games ($match)"
    "$JAVA" -Xmx8g "-Dares.nn.model=$MODEL" "-Dares.netdata.dir=$dir" "-Dares.netdata.dedup=true" -cp "target/classes${SEP}${CP}" \
        com.terraforming.ares.selfplay.SelfPlayMain --mode play --out "$WORK/logs/decisions_$label" \
        --threads "$THREADS" --games-per-chunk 500 --matchups "$match" --max-rows-per-game-type 1 \
        --games "$games" >"$WORK/logs/gen_$label.log" 2>&1 \
        || fail "generation of $label failed; see $WORK/logs/gen_$label.log"
    log "  $label: $(du -h "$dir/netdata.bin" | cut -f1)"
}

if [ -f "$WORK/data/merged.ok" ]; then
    log "merged corpus already exists: $WORK/data/all.bin, skipping generation and merge"
    SKIP_GEN=1
else
    SKIP_GEN=0
fi

log "=== generate $GAMES games in chunks of $CHUNK (${NET_SHARE}% vs NETWORK)"
CHUNKS=$(( (GAMES + CHUNK - 1) / CHUNK ))
NET_CHUNKS=$(( CHUNKS * NET_SHARE / 100 ))
START=$(date +%s)
for i in $(seq 1 "$CHUNKS"); do
    if [ "$SKIP_GEN" = 1 ]; then break; fi
    if [ "$i" -le "$NET_CHUNKS" ]; then
        generate "$WORK/data/chunk_$(printf '%03d' "$i")" "NETWORK_V2:NETWORK:1.0" "$CHUNK" "vsnet_$(printf '%03d' "$i")"
    else
        generate "$WORK/data/chunk_$(printf '%03d' "$i")" "NETWORK_V2:NETWORK_V2:1.0" "$CHUNK" "self_$(printf '%03d' "$i")"
    fi
    DONE=$(( i * CHUNK ))
    RATE=$(( DONE * 60 / ( $(date +%s) - START + 1 ) ))
    log "  progress $DONE/$GAMES games, ${RATE}/min"
done

if [ "$SKIP_GEN" = 0 ]; then
    log "=== merge (game ids offset per chunk)"
    FILES=$(ls "$WORK"/data/chunk_*/netdata.bin | sort)
    # shellcheck disable=SC2086
    "${PY_CMD[@]}" ml/merge_bin.py --out "$WORK/data/all.bin" --in $FILES || fail "merge failed"
    touch "$WORK/data/merged.ok"
fi
du -h "$WORK/data/all.bin"

STAMP=$(date '+%Y%m%d_%H%M')
if [ -f "$WORK/models/trained.zip" ]; then
    cp "$WORK/models/trained.zip" "$WORK/models/trained_before_$STAMP.zip"
    log "kept the previous model as models/trained_before_$STAMP.zip"
fi
if [ -f "$WORK/summary.txt" ]; then
    cp "$WORK/summary.txt" "$WORK/summary_before_$STAMP.txt"
    log "kept the previous summary as summary_before_$STAMP.txt"
fi

log "=== train ($EPOCHS epochs, batch $BATCH, NO early stop, best validation kept, warm start from $MODEL)"
STATUS=0
if [ "$PROGRESS_SEC" -gt 0 ]; then
    START=$(date +%s)
    "${PY_CMD[@]}" ml/train_network_v2_torch.py --csv "$WORK/data/all.bin" --out "$WORK/models/trained.zip" \
        --warm-start "$MODEL" \
        --epochs "$EPOCHS" --batch "$BATCH" --lr "$LR" --seed 1 --val-steps "$VAL_STEPS" --val-rows "$VAL_ROWS" --patience 0 --dropout "$DROPOUT" --rows-per-game "$ROWS_PER_GAME" \
        --weight-decay "$WEIGHT_DECAY" --save-checkpoints "$SAVE_CHECKPOINTS" --red-rise "$RED_RISE" --red-rises "$RED_RISES" \
        --train-eval-rows "$TRAIN_EVAL_ROWS" \
        >"$WORK/logs/train.log" 2>&1 &
    TRAIN_PID=$!
    while kill -0 "$TRAIN_PID" 2>/dev/null; do
        sleep "$PROGRESS_SEC" || true
        if kill -0 "$TRAIN_PID" 2>/dev/null; then
            ELAPSED=$(( ( $(date +%s) - START ) / 60 ))
            LAST=$(grep -E "^(step|epoch)" "$WORK/logs/train.log" 2>/dev/null | tail -1 || true)
            BEST=$(grep -E "best so far" "$WORK/logs/train.log" 2>/dev/null | tail -1 || true)
            if [ -z "$LAST" ]; then
                LAST=$(tail -1 "$WORK/logs/train.log" 2>/dev/null | cut -c1-180 || true)
            fi
            if [ -z "$LAST" ]; then LAST="starting the trainer (loading the corpus)..."; fi
            printf '  [%s min] %s%s\n' "$ELAPSED" "$LAST" "${BEST:+ | $BEST}"
        fi
    done
    wait "$TRAIN_PID" || STATUS=$?
else
    "${PY_CMD[@]}" ml/train_network_v2_torch.py --csv "$WORK/data/all.bin" --out "$WORK/models/trained.zip" \
        --warm-start "$MODEL" \
        --epochs "$EPOCHS" --batch "$BATCH" --lr "$LR" --seed 1 --val-steps "$VAL_STEPS" --val-rows "$VAL_ROWS" --patience 0 --dropout "$DROPOUT" --rows-per-game "$ROWS_PER_GAME" \
        --weight-decay "$WEIGHT_DECAY" --save-checkpoints "$SAVE_CHECKPOINTS" --red-rise "$RED_RISE" --red-rises "$RED_RISES" \
        --train-eval-rows "$TRAIN_EVAL_ROWS" \
        >"$WORK/logs/train.log" 2>&1 || STATUS=$?
fi
if [ "$STATUS" -eq 42 ]; then
    log "RED FLAG: the training overfit; not measuring (see logs/train.log)"
    printf 'RED: overfitting, not measured\n' >"$WORK/summary.txt"
    exit 0
fi
[ "$STATUS" -eq 0 ] || fail "training failed; see $WORK/logs/train.log"
tail -3 "$WORK/logs/train.log"
grep -E "^(step|epoch|using the best|wrote)" "$WORK/logs/train.log" | tail -8 | sed 's/^/  /'

FINAL_MODEL="$WORK/models/trained.zip"
if [ "$SAVE_CHECKPOINTS" -gt 0 ]; then
    log "=== choose the checkpoint by PLAY ($CKPT_GAMES games vs NETWORK each)"
    BEST_PLAY="$WORK/models/trained.zip"
    BEST_SCORE=""
    for ckpt in "$WORK"/models/trained_ckpt_*.zip "$WORK/models/trained.zip"; do
        [ -f "$ckpt" ] || continue
        name=$(basename "$ckpt" .zip)
        "$JAVA" -Xmx10g "-Dares.nn.model=$ckpt" -cp "target/classes${SEP}${CP}" \
            com.terraforming.ares.selfplay.SelfPlayMain --mode compare --a NETWORK_V2 --b NETWORK \
            --games "$CKPT_GAMES" --concurrency "$THREADS" --out "$WORK/logs/${name}_vs_network.json" \
            >"$WORK/logs/${name}_vs_network.log" 2>&1 || { log "  $name: failed"; continue; }
        line=$(grep "CompareResult\[" "$WORK/logs/${name}_vs_network.log" | tail -1)
        score=$(printf '%s' "$line" | sed -n 's/.*aScore=\([0-9.]*\).*/\1/p')
        log "  $name: $score"
        if [ -n "$score" ] && { [ -z "$BEST_SCORE" ] || awk "BEGIN{exit !($score > $BEST_SCORE)}"; }; then
            BEST_SCORE="$score"
            BEST_PLAY="$ckpt"
        fi
    done
    FINAL_MODEL="$BEST_PLAY"
    log "best play: $FINAL_MODEL ($BEST_SCORE)"
fi

log "=== phase mix on NEW games ($PROFILE_GAMES vs SMART): the NETWORK_V2 profile (check_player is the AGENT's gate, not this value)"
"$JAVA" -Xmx10g "-Dares.nn.model=$FINAL_MODEL" -cp "target/classes${SEP}${CP}" \
    com.terraforming.ares.selfplay.SelfPlayMain --mode phasedist --a NETWORK_V2 --b SMART \
    --games "$PROFILE_GAMES" --concurrency "$THREADS" --out "$WORK/logs/phases.txt" \
    >"$WORK/logs/phasedist.log" 2>&1 || log "  phase audit failed; see the log"
if [ -f "$WORK/logs/phases.txt" ]; then
    sed 's/^/  /' "$WORK/logs/phases.txt"
fi

log "=== value on FRESH games ($FRESH_GAMES new games of the base, out of sample)"
if [ ! -s "$WORK/data/fresh/netdata.bin" ]; then
    mkdir -p "$WORK/data/fresh" "$WORK/logs/dec_fresh"
    "$JAVA" -Xmx8g "-Dares.nn.model=$MODEL" "-Dares.netdata.dir=$WORK/data/fresh" -cp "target/classes${SEP}${CP}" \
        com.terraforming.ares.selfplay.SelfPlayMain --mode play --out "$WORK/logs/dec_fresh" \
        --threads "$THREADS" --games-per-chunk 100 --matchups "NETWORK_V2:NETWORK_V2:1.0" \
        --max-rows-per-game-type 1 --games "$FRESH_GAMES" >"$WORK/logs/fresh_gen.log" 2>&1 \
        || log "  fresh generation failed; see $WORK/logs/fresh_gen.log"
fi
"${PY_CMD[@]}" ml/value_eval_bin.py --csv "$WORK/data/fresh/netdata.bin" --model "$FINAL_MODEL" \
    >"$WORK/logs/value_fresh.log" 2>&1 || log "  fresh value check failed; see $WORK/logs/value_fresh.log"
tail -2 "$WORK/logs/value_fresh.log" | sed 's/^/  /' || true

measure() {   # bench
    local bench="$1"
    log "=== measure vs $bench ($EVAL_GAMES games)"
    "$JAVA" -Xmx10g "-Dares.nn.model=$FINAL_MODEL" -cp "target/classes${SEP}${CP}" \
        com.terraforming.ares.selfplay.SelfPlayMain --mode compare --a NETWORK_V2 --b "$bench" \
        --games "$EVAL_GAMES" --concurrency "$THREADS" --out "$WORK/models/vs_$bench.json" \
        >"$WORK/logs/eval_$bench.log" 2>&1 || fail "measurement vs $bench failed"
    grep "CompareResult\[" "$WORK/logs/eval_$bench.log" | tail -1 | tee -a "$WORK/summary.txt"
}
: >"$WORK/summary.txt"
printf 'best play: %s\n' "$FINAL_MODEL" >>"$WORK/summary.txt"
measure NETWORK
measure RANDOM

log "=== done; summary"
cat "$WORK/summary.txt"
log "the final model is $FINAL_MODEL (the val-best is models/trained.zip; the checkpoints are models/trained_ckpt_*.zip)"
