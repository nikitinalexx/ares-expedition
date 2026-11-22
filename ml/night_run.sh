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
    "$JAVA" -Xmx8g "-Dares.nn.model=$MODEL" "-Dares.netdata.dir=$dir" -cp "target/classes${SEP}${CP}" \
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

log "=== train (one pass, early stop on a 50k validation slice)"
"${PY_CMD[@]}" ml/train_network_v2_torch.py --csv "$WORK/data/all.bin" --out "$WORK/models/trained.zip" \
    --epochs 1 --batch 64 --lr 0.00005 --seed 1 --val-steps 500 --val-rows 50000 --patience 8 --dropout 0.1 \
    >"$WORK/logs/train.log" 2>&1 || fail "training failed; see $WORK/logs/train.log"
tail -3 "$WORK/logs/train.log"
grep -E "^(step|epoch|using the best|wrote)" "$WORK/logs/train.log" | tail -5 | sed 's/^/  /'

measure() {   # bench
    local bench="$1"
    log "=== measure vs $bench ($EVAL_GAMES games)"
    "$JAVA" -Xmx10g "-Dares.nn.model=$WORK/models/trained.zip" -cp "target/classes${SEP}${CP}" \
        com.terraforming.ares.selfplay.SelfPlayMain --mode compare --a NETWORK_V2 --b "$bench" \
        --games "$EVAL_GAMES" --concurrency "$THREADS" --out "$WORK/models/vs_$bench.json" \
        >"$WORK/logs/eval_$bench.log" 2>&1 || fail "measurement vs $bench failed"
    grep "CompareResult\[" "$WORK/logs/eval_$bench.log" | tail -1 | tee -a "$WORK/summary.txt"
}
: >"$WORK/summary.txt"
measure NETWORK
measure RANDOM

log "=== done; summary"
cat "$WORK/summary.txt"
log "the trained model is $WORK/models/trained.zip (use -Dares.nn.model=<path>)"
