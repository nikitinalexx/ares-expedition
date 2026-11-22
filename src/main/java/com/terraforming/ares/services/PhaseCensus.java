package com.terraforming.ares.services;

import com.terraforming.ares.model.PlayerDifficulty;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts which phases every player actually calls, by difficulty. One funnel ({@code PickPhaseProcessor}) sees every
 * phase turn of every difficulty, so this is the only honest way to answer "what does NETWORK call?" - the recorded agent
 * data contains agent decisions only.
 * <p>
 * Audit-only: disabled by default, one volatile read per phase turn when off. {@code --mode phasedist} turns it on.
 */
public final class PhaseCensus {

    private static final Map<String, LongAdder> CALLS = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> TRANSITIONS = new ConcurrentHashMap<>();
    private static final Map<String, long[]> PICKS = new ConcurrentHashMap<>();
    private static volatile boolean enabled;

    private PhaseCensus() {
    }

    public static void enable() {
        enabled = true;
        CALLS.clear();
        TRANSITIONS.clear();
        PICKS.clear();
    }

    public static void disable() {
        enabled = false;
    }

    /** How many times this player picked each phase (index 0 = phase 1) in the census window. */
    public static long[] picks(String player) {
        return PICKS.getOrDefault(player, new long[5]);
    }

    public static void record(com.terraforming.ares.model.Player player, Integer previousPhase, int phase) {
        if (!enabled) {
            return;
        }
        String name = player.getDifficulty() == null ? "UNKNOWN" : player.getDifficulty().name();
        CALLS.computeIfAbsent(name + "|" + phase, key -> new LongAdder()).increment();
        if (previousPhase != null) {
            TRANSITIONS.computeIfAbsent(name + "|" + previousPhase + ">" + phase, key -> new LongAdder()).increment();
        }
        if (phase >= 1 && phase <= 5) {
            PICKS.computeIfAbsent(player.getUuid(), key -> new long[5])[phase - 1]++;
        }
    }

    /** difficulty -> phase -> calls. */
    public static Map<String, Map<Integer, Long>> calls() {
        Map<String, Map<Integer, Long>> result = new TreeMap<>();
        for (Map.Entry<String, LongAdder> entry : CALLS.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            result.computeIfAbsent(parts[0], key -> new TreeMap<>())
                    .merge(Integer.parseInt(parts[1]), entry.getValue().sum(), Long::sum);
        }
        return result;
    }

    /** difficulty -> "previous>next" -> calls. */
    public static Map<String, Map<String, Long>> transitions() {
        Map<String, Map<String, Long>> result = new TreeMap<>();
        for (Map.Entry<String, LongAdder> entry : TRANSITIONS.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            result.computeIfAbsent(parts[0], key -> new TreeMap<>()).merge(parts[1], entry.getValue().sum(), Long::sum);
        }
        return result;
    }
}

