package com.terraforming.ares.services.agent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide counters for agent self-play throughput. Reset at the start of a self-play run; read for progress lines
 * and {@code summary.json}. Used to spot when neural inference starts dominating wall time (signal to consider a GPU path).
 * <p>
 * {@code *Nanos} fields are the sum of per-call durations across threads (not wall time). Compare them to
 * {@code wallSeconds * threads} to estimate how much of the CPU budget inference already eats.
 */
public final class AgentPerfStats {
    private static final AtomicLong OBSERVATION_CALLS = new AtomicLong();
    private static final AtomicLong OBSERVATION_NANOS = new AtomicLong();
    private static final AtomicLong NEURAL_CALLS = new AtomicLong();
    private static final AtomicLong NEURAL_NANOS = new AtomicLong();
    private static final AtomicLong NEURAL_CANDIDATES = new AtomicLong();
    private static final AtomicLong FALLBACK_CALLS = new AtomicLong();

    private AgentPerfStats() {
    }

    public static void reset() {
        OBSERVATION_CALLS.set(0);
        OBSERVATION_NANOS.set(0);
        NEURAL_CALLS.set(0);
        NEURAL_NANOS.set(0);
        NEURAL_CANDIDATES.set(0);
        FALLBACK_CALLS.set(0);
    }

    public static void recordObservation(long nanos) {
        OBSERVATION_CALLS.incrementAndGet();
        OBSERVATION_NANOS.addAndGet(Math.max(0, nanos));
    }

    public static void recordNeural(long nanos, int candidates) {
        NEURAL_CALLS.incrementAndGet();
        NEURAL_NANOS.addAndGet(Math.max(0, nanos));
        NEURAL_CANDIDATES.addAndGet(Math.max(0, candidates));
    }

    public static void recordFallback() {
        FALLBACK_CALLS.incrementAndGet();
    }

    public record Snapshot(long observationCalls, long observationNanos, long neuralCalls, long neuralNanos,
                           long neuralCandidates, long fallbackCalls) {
        public double observationUsMean() {
            return observationCalls == 0 ? 0 : observationNanos / 1e3 / observationCalls;
        }

        public double neuralUsMean() {
            return neuralCalls == 0 ? 0 : neuralNanos / 1e3 / neuralCalls;
        }

        public double neuralCandidatesMean() {
            return neuralCalls == 0 ? 0 : neuralCandidates / (double) neuralCalls;
        }

        /**
         * Fraction of the CPU budget (wall × threads) spent inside neural forward passes. Above ~0.5 with a large net
         * is the usual cue that a batched GPU path may pay off.
         */
        public double neuralCpuShare(double wallSeconds, int threads) {
            if (wallSeconds <= 0 || threads <= 0 || neuralNanos == 0) {
                return 0;
            }
            return Math.min(1.0, neuralNanos / 1e9 / (wallSeconds * threads));
        }

        public Map<String, Object> toMap(double wallSeconds, int threads, double gamesPerSecond) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("gamesPerSecond", round(gamesPerSecond, 2));
            map.put("threads", threads);
            map.put("observationCalls", observationCalls);
            map.put("observationUsMean", round(observationUsMean(), 1));
            map.put("neuralCalls", neuralCalls);
            map.put("neuralUsMean", round(neuralUsMean(), 1));
            map.put("neuralCandidatesMean", round(neuralCandidatesMean(), 2));
            map.put("neuralInferencesPerSecond", wallSeconds <= 0 ? 0 : round(neuralCalls / wallSeconds, 1));
            map.put("neuralCpuShare", round(neuralCpuShare(wallSeconds, threads), 3));
            map.put("fallbackCalls", fallbackCalls);
            map.put("hint", hint(wallSeconds, threads));
            return map;
        }

        public String progressLine(double wallSeconds, int threads, double gamesPerSecond) {
            if (neuralCalls == 0) {
                return String.format(Locale.ROOT, "%.1f games/s, obs %.0f us (no neural yet, fallback=%d)",
                        gamesPerSecond, observationUsMean(), fallbackCalls);
            }
            return String.format(Locale.ROOT,
                    "%.1f games/s, neural %.0f us/call (%.0f cand), %.0f inf/s, cpuShare=%.0f%%, obs %.0f us",
                    gamesPerSecond, neuralUsMean(), neuralCandidatesMean(),
                    wallSeconds <= 0 ? 0 : neuralCalls / wallSeconds,
                    100 * neuralCpuShare(wallSeconds, threads), observationUsMean());
        }

        private String hint(double wallSeconds, int threads) {
            double share = neuralCpuShare(wallSeconds, threads);
            if (neuralCalls == 0) {
                return "no neural inference in this run (random/SMART or models not loaded)";
            }
            if (share >= 0.5) {
                return "neural forward already uses >=50% of the CPU budget — consider batched GPU inference";
            }
            if (share >= 0.25) {
                return "neural forward is a noticeable share of CPU — watch the trend as the net grows";
            }
            return "inference is cheap vs game logic on CPU; GPU unlikely to help yet";
        }

        private static double round(double value, int digits) {
            double scale = Math.pow(10, digits);
            return Math.round(value * scale) / scale;
        }
    }

    public static Snapshot snapshot() {
        return new Snapshot(OBSERVATION_CALLS.get(), OBSERVATION_NANOS.get(), NEURAL_CALLS.get(), NEURAL_NANOS.get(),
                NEURAL_CANDIDATES.get(), FALLBACK_CALLS.get());
    }
}
