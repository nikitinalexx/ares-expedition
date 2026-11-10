package com.terraforming.ares.controllers;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Throughput benchmark of the simulation loop, meant to be run on the machine that does the overnight runs
 * (speeds measured on a different machine say nothing about it).
 * Example: {@code GET /simulations/bench?games=2000&threads=16&a=RANDOM&b=RANDOM}.
 * Reports games/s and engine steps/s, so "how fast is the game itself" can be separated from "how fast is the AI".
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class SimulationBenchController {
    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final AdvancedAiDataCollectionService dataCollectionService;

    public record BenchResult(String a, String b, int threads, int games, int warmupGames, double seconds,
                              double gamesPerSecond, double stepsPerSecond, double avgStepsPerGame,
                              double avgRounds, int failedGames, int availableProcessors,
                              boolean encode, double encodesPerSecond) {
    }

    @GetMapping("/simulations/bench")
    public BenchResult bench(@RequestParam(defaultValue = "1000") int games,
                             @RequestParam(defaultValue = "0") int threads,
                             @RequestParam(defaultValue = "RANDOM") PlayerDifficulty a,
                             @RequestParam(defaultValue = "RANDOM") PlayerDifficulty b,
                             @RequestParam(defaultValue = "50") int warmup,
                             @RequestParam(defaultValue = "false") boolean encode) throws InterruptedException {
        int poolSize = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();

        runGames(warmup, poolSize, a, b, encode, new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder());

        LongAdder steps = new LongAdder(), rounds = new LongAdder(), done = new LongAdder(), failed = new LongAdder(), encodes = new LongAdder();
        long start = System.nanoTime();
        runGames(games, poolSize, a, b, encode, steps, rounds, done, failed, encodes);
        double seconds = (System.nanoTime() - start) / 1e9;

        long played = done.sum();
        BenchResult result = new BenchResult(a.name(), b.name(), poolSize, (int) played, warmup, seconds,
                played / seconds, steps.sum() / seconds,
                played == 0 ? 0 : (double) steps.sum() / played,
                played == 0 ? 0 : (double) rounds.sum() / played,
                (int) failed.sum(), Runtime.getRuntime().availableProcessors(), encode, encodes.sum() / seconds);
        System.out.println("[BENCH] " + result);
        return result;
    }

    private void runGames(int games, int poolSize, PlayerDifficulty a, PlayerDifficulty b, boolean encode,
                          LongAdder steps, LongAdder rounds, LongAdder done, LongAdder failed, LongAdder encodes) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < games; i++) {
            pool.submit(() -> {
                try {
                    MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                            .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                            .computers(List.of(a, b))
                            .mulligan(true)
                            .expansion(Expansion.BASE).expansion(Expansion.BUFFED_CORPORATION).expansion(Expansion.DISCOVERY)
                            .dummyHand(true)
                            .build());
                    long[] stepCount = {0};
                    simulationProcessorService.processSimulation(game, g -> {
                        stepCount[0]++;
                        if (encode) {
                            // what a policy agent has to do at least once per decision: encode the state of each player
                            for (Player player : g.getPlayerUuidToPlayer().values()) {
                                float[] features = dataCollectionService.collectData(g, player.getUuid());
                                if (features.length == 0) {
                                    throw new IllegalStateException("empty features");
                                }
                                encodes.increment();
                            }
                        }
                    });
                    steps.add(stepCount[0]);
                    rounds.add(game.getTurns());
                    done.increment();
                } catch (Throwable t) {
                    failed.increment();
                    t.printStackTrace();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(24, TimeUnit.HOURS);
    }
}
