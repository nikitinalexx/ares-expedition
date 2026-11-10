package com.terraforming.ares.controllers;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Head-to-head strength comparison of two computer difficulties in the NETWORK_V2 training configuration
 * (2 players, mulligan, dummy hand, buffed corporations, discovery).
 * <p>
 * Differences from the old {@code /simulations/v3}:
 * <ul>
 *   <li>seats are swapped every game, so first-player advantage cancels out;</li>
 *   <li>players are identified by difficulty, not by uuid ordering;</li>
 *   <li>result includes the win rate of A with a 95% confidence interval and a z-score, so "a few percent" can be judged;</li>
 *   <li>no dataset collection or card statistics side effects; the result is returned as JSON as well as printed.</li>
 * </ul>
 * Example: {@code GET /simulations/compare?games=4000&a=NETWORK&b=NETWORK_V2}
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class SimulationCompareController {
    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final WinPointsService winPointsService;
    private final NNService nnService;

    public record CompareResult(
            String a, String b, int games, int failedGames,
            int aWins, int bWins, int draws,
            double aScore, double aScoreCi95Low, double aScoreCi95High, double zScore,
            double aAvgWinPoints, double bAvgWinPoints, double avgTurns,
            int aWinsWhenSeatedFirst, int gamesAFirst, int aWinsWhenSeatedSecond, int gamesASecond,
            double seconds
    ) {
    }

    @GetMapping("/simulations/compare")
    public CompareResult compare(@RequestParam(defaultValue = "2000") int games,
                                 @RequestParam(defaultValue = "NETWORK") PlayerDifficulty a,
                                 @RequestParam(defaultValue = "NETWORK_V2") PlayerDifficulty b,
                                 @RequestParam(defaultValue = "256") int concurrency) throws InterruptedException {
        CompareResult result = runCompare(games, a, b, concurrency);
        nnService.printVerifyStats();
        return result;
    }

    /**
     * Runs every {@code *.zip} model of a directory as the NETWORK_V2 brain against {@code opponent} and prints a table.
     * Example: {@code GET /simulations/compare/models?dir=candidates&games=200}
     */
    @GetMapping("/simulations/compare/models")
    public List<String> compareModels(@RequestParam(defaultValue = "candidates") String dir,
                                      @RequestParam(defaultValue = "200") int games,
                                      @RequestParam(defaultValue = "NETWORK") PlayerDifficulty opponent,
                                      @RequestParam(defaultValue = "64") int concurrency) throws Exception {
        java.io.File[] files = new java.io.File(dir).listFiles((d, n) -> n.endsWith(".zip"));
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No *.zip models in " + dir);
        }
        java.util.Arrays.sort(files);
        List<String> table = new ArrayList<>();
        for (java.io.File file : files) {
            String line;
            try {
                nnService.useModel(file);
                CompareResult r = runCompare(games, PlayerDifficulty.NETWORK_V2, opponent, concurrency);
                line = String.format("%-28s V2 score vs %s: %.1f%% [%.1f..%.1f] W%d L%d D%d | avgWP V2 %.1f vs %.1f | failed %d | %.0fs",
                        file.getName(), opponent, r.aScore() * 100, r.aScoreCi95Low() * 100, r.aScoreCi95High() * 100,
                        r.aWins(), r.bWins(), r.draws(), r.aAvgWinPoints(), r.bAvgWinPoints(), r.failedGames(), r.seconds());
            } catch (Exception e) {
                line = file.getName() + " FAILED: " + e;
            }
            System.out.println("[MODELS] " + line);
            table.add(line);
        }
        System.out.println("[MODELS] ===== TABLE =====");
        table.forEach(l -> System.out.println("[MODELS] " + l));
        return table;
    }

    private CompareResult runCompare(int games, PlayerDifficulty a, PlayerDifficulty b, int concurrency) throws InterruptedException {
        if (a == PlayerDifficulty.NONE || b == PlayerDifficulty.NONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a and b must be computers");
        }
        if (a == b) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a and b must differ (use /simulations/v2 for a mirror match)");
        }

        LongAdder aWins = new LongAdder(), bWins = new LongAdder(), draws = new LongAdder(), failed = new LongAdder();
        LongAdder aPoints = new LongAdder(), bPoints = new LongAdder(), turns = new LongAdder();
        LongAdder aWinsFirst = new LongAdder(), gamesAFirst = new LongAdder();
        LongAdder aWinsSecond = new LongAdder(), gamesASecond = new LongAdder();
        AtomicInteger done = new AtomicInteger();
        AtomicLong lastLog = new AtomicLong(System.currentTimeMillis());
        long start = System.currentTimeMillis();
        Semaphore permits = new Semaphore(Math.max(1, concurrency));

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < games; i++) {
                final boolean aFirst = i % 2 == 0; // alternate seats
                permits.acquire();
                executor.submit(() -> {
                    try {
                        List<PlayerDifficulty> difficulties = aFirst ? List.of(a, b) : List.of(b, a);
                        GameParameters parameters = GameParameters.builder()
                                .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                                .computers(difficulties)
                                .mulligan(true)
                                .expansion(Expansion.BASE)
                                .expansion(Expansion.BUFFED_CORPORATION)
                                .expansion(Expansion.DISCOVERY)
                                .dummyHand(true)
                                .build();

                        MarsGame game = gameService.createNewSimulation(parameters);
                        simulationProcessorService.processSimulation(game);

                        int aWp = -1, bWp = -1;
                        for (Player player : game.getPlayerUuidToPlayer().values()) {
                            int wp = winPointsService.countWinPoints(player, game);
                            if (player.getDifficulty() == a) aWp = wp;
                            else if (player.getDifficulty() == b) bWp = wp;
                        }
                        if (aWp < 0 || bWp < 0) {
                            throw new IllegalStateException("Could not identify players by difficulty");
                        }

                        aPoints.add(aWp);
                        bPoints.add(bWp);
                        turns.add(game.getTurns());
                        (aFirst ? gamesAFirst : gamesASecond).increment();
                        if (aWp > bWp) {
                            aWins.increment();
                            (aFirst ? aWinsFirst : aWinsSecond).increment();
                        } else if (bWp > aWp) {
                            bWins.increment();
                        } else {
                            draws.increment();
                        }
                    } catch (Throwable t) {
                        failed.increment();
                        t.printStackTrace();
                    } finally {
                        permits.release();
                        int d = done.incrementAndGet();
                        long now = System.currentTimeMillis();
                        long last = lastLog.get();
                        if ((d == games || now - last >= 15_000) && (d == games || lastLog.compareAndSet(last, now))) {
                            double elapsed = (now - start) / 1000.0;
                            double speed = d / Math.max(0.001, elapsed);
                            System.out.printf("[COMPARE] %d/%d | %.2f games/s | elapsed %.0fs, ETA %.0fs | %s %d : %d %s | draws %d | failed %d%n",
                                    d, games, speed, elapsed, (games - d) / Math.max(0.001, speed),
                                    a, aWins.sum(), bWins.sum(), b, draws.sum(), failed.sum());
                        }
                    }
                });
            }
        }

        int played = (int) (aWins.sum() + bWins.sum() + draws.sum());
        double score = played == 0 ? 0 : (aWins.sum() + 0.5 * draws.sum()) / played;
        double[] ci = wilson(score, played);
        double z = played == 0 ? 0 : (score - 0.5) / Math.sqrt(0.25 / played);

        CompareResult result = new CompareResult(
                a.name(), b.name(), played, (int) failed.sum(),
                (int) aWins.sum(), (int) bWins.sum(), (int) draws.sum(),
                score, ci[0], ci[1], z,
                played == 0 ? 0 : (double) aPoints.sum() / played,
                played == 0 ? 0 : (double) bPoints.sum() / played,
                played == 0 ? 0 : (double) turns.sum() / played,
                (int) aWinsFirst.sum(), (int) gamesAFirst.sum(), (int) aWinsSecond.sum(), (int) gamesASecond.sum(),
                (System.currentTimeMillis() - start) / 1000.0
        );
        System.out.println("[COMPARE] RESULT " + result);
        return result;
    }

    /**
     * Wilson 95% interval for the score (draws count as half a win - approximation, fine for a handful of draws).
     */
    private static double[] wilson(double p, int n) {
        if (n == 0) return new double[]{0, 0};
        double z = 1.96, z2 = z * z;
        double denom = 1 + z2 / n;
        double center = (p + z2 / (2.0 * n)) / denom;
        double half = z * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / denom;
        return new double[]{center - half, center + half};
    }
}
