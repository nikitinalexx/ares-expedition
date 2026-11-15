package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.PhaseCensus;
import com.terraforming.ares.services.SimulationProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Which phases each built-in difficulty actually calls, per game and as a share, plus the transitions it makes.
 * <p>
 * Why it exists: the recorded agent data contains agent decisions only, so "what does NETWORK call?" cannot be read from
 * it. Every phase turn of every difficulty passes through {@code PickPhaseProcessor}, so {@link PhaseCensus} counts all
 * of them. This is the reference against which the agent's own phase behaviour is judged - the agent picks income or
 * draft in ~100% of its rounds, and this shows whether that is a property of the game or a local optimum of ours.
 * <p>
 * {@code --mode phasedist --out FILE.json --games N --a NETWORK --b RANDOM [--concurrency C]}
 */
@Component
@RequiredArgsConstructor
public class PhaseDistribution {
    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;

    public record Report(int games, int failedGames, double avgTurns, List<Side> sides,
                         Map<String, Map<String, Long>> transitions) {
    }

    public record Side(String difficulty, long picks, double picksPerGame, Map<Integer, Double> share) {
    }

    public Report run(Path out, int games, PlayerDifficulty a, PlayerDifficulty b, int concurrency) throws Exception {
        PhaseCensus.enable();
        LongAdder failed = new LongAdder();
        DoubleAdder turns = new DoubleAdder();
        AtomicInteger done = new AtomicInteger();
        Semaphore permits = new Semaphore(Math.max(1, concurrency));
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < games; i++) {
                final boolean aFirst = i % 2 == 0;
                permits.acquire();
                executor.submit(() -> {
                    try {
                        List<PlayerDifficulty> difficulties = aFirst ? List.of(a, b) : List.of(b, a);
                        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                                .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                                .computers(difficulties)
                                .mulligan(true)
                                .expansion(Expansion.BASE)
                                .expansion(Expansion.BUFFED_CORPORATION)
                                .expansion(Expansion.DISCOVERY)
                                .dummyHand(true)
                                .build());
                        simulationProcessorService.processSimulation(game);
                        turns.add(game.getTurns());
                    } catch (Throwable t) {
                        FatalErrors.haltIfOutOfMemory(t);
                        failed.increment();
                    } finally {
                        permits.release();
                        int d = done.incrementAndGet();
                        if (d % 200 == 0 || d == games) {
                            System.out.println("[PHASEDIST] " + d + "/" + games);
                        }
                    }
                });
            }
        }
        int played = Math.max(1, (int) (games - failed.sum()));
        List<Side> sides = new ArrayList<>();
        PhaseCensus.calls().forEach((difficulty, byPhase) -> {
            long total = byPhase.values().stream().mapToLong(Long::longValue).sum();
            Map<Integer, Double> share = new LinkedHashMap<>();
            for (int phase = 1; phase <= 5; phase++) {
                share.put(phase, round(byPhase.getOrDefault(phase, 0L) / (double) Math.max(total, 1)));
            }
            sides.add(new Side(difficulty, total, round(total / (double) played), share));
        });
        Map<String, Map<String, Long>> transitions = PhaseCensus.transitions();
        PhaseCensus.disable();

        Report report = new Report(games, (int) failed.sum(), round(turns.sum() / played), sides, transitions);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        new ObjectMapper().writeValue(out.toFile(), report);
        for (Side side : sides) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, Double> entry : side.share().entrySet()) {
                sb.append(entry.getKey()).append('=').append(Math.round(entry.getValue() * 100)).append("% ");
            }
            System.out.println("[PHASEDIST] " + side.difficulty() + ": " + side.picksPerGame() + " picks/game, " + sb);
        }
        System.out.println("[PHASEDIST] " + games + " games, " + played + " played, avg turns " + report.avgTurns());
        return report;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
