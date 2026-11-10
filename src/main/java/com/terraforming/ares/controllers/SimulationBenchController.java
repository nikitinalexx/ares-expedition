package com.terraforming.ares.controllers;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
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

    public record BenchResult(String a, String b, int threads, int games, int warmupGames, double seconds,
                              double gamesPerSecond, double stepsPerSecond, double avgStepsPerGame,
                              double avgRounds, int failedGames, int availableProcessors) {
    }

    @GetMapping("/simulations/bench")
    public BenchResult bench(@RequestParam(defaultValue = "1000") int games,
                             @RequestParam(defaultValue = "0") int threads,
                             @RequestParam(defaultValue = "RANDOM") PlayerDifficulty a,
                             @RequestParam(defaultValue = "RANDOM") PlayerDifficulty b,
                             @RequestParam(defaultValue = "50") int warmup) throws InterruptedException {
        int poolSize = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();

        runGames(warmup, poolSize, a, b, new LongAdder(), new LongAdder(), new LongAdder(), new LongAdder());

        LongAdder steps = new LongAdder(), rounds = new LongAdder(), done = new LongAdder(), failed = new LongAdder();
        long start = System.nanoTime();
        runGames(games, poolSize, a, b, steps, rounds, done, failed);
        double seconds = (System.nanoTime() - start) / 1e9;

        long played = done.sum();
        BenchResult result = new BenchResult(a.name(), b.name(), poolSize, (int) played, warmup, seconds,
                played / seconds, steps.sum() / seconds,
                played == 0 ? 0 : (double) steps.sum() / played,
                played == 0 ? 0 : (double) rounds.sum() / played,
                (int) failed.sum(), Runtime.getRuntime().availableProcessors());
        System.out.println("[BENCH] " + result);
        return result;
    }

    private void runGames(int games, int poolSize, PlayerDifficulty a, PlayerDifficulty b,
                          LongAdder steps, LongAdder rounds, LongAdder done, LongAdder failed) throws InterruptedException {
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
                    simulationProcessorService.processSimulation(game, g -> stepCount[0]++);
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
