package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
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
 * Where the points go: plays {@code a} against {@code b} and reports, for each side, the average of every term
 * {@link WinPointsService} scores - terraforming rating, forests, printed card points, resource points on cards,
 * milestones, awards - next to the win rate.
 * <p>
 * The win rate of two players that differ by 1-2 points is indistinguishable from noise; the decomposition shows which
 * term of the score is short, which is what makes the next training decision aimable. Diagnostic only: it records
 * nothing and changes nothing.
 * <p>
 * {@code --mode scorecard --out FILE.json --games N --a AGENT --b SMART [--concurrency C]}
 */
@Component
@RequiredArgsConstructor
public class Scorecard {
    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final WinPointsService winPointsService;
    private final com.terraforming.ares.services.CardService cardService;

    public record Report(String a, String b, int games, int failedGames, int aWins, int bWins, int draws,
                         double aWinRate, double aAvgPoints, double bAvgPoints, double avgTurns,
                         Map<String, Double> aAverages, Map<String, Double> bAverages,
                         Map<String, Double> aCards, Map<String, Double> bCards,
                         double aBuildsPerGame, double bBuildsPerGame,
                         double aPrintedPointsPerGame, double bPrintedPointsPerGame) {
    }

    /** One finished game, already reduced to the numbers the report needs. */
    private record Game(int aPoints, int bPoints, int turns,
                        WinPointsService.ScoreBreakdown aBreakdown, WinPointsService.ScoreBreakdown bBreakdown,
                        List<Integer> aPlayed, List<Integer> bPlayed) {
    }

    public Report run(Path out, int games, PlayerDifficulty a, PlayerDifficulty b, int concurrency) throws Exception {
        List<Game> results = java.util.Collections.synchronizedList(new ArrayList<>());
        LongAdder failed = new LongAdder();
        Semaphore permits = new Semaphore(Math.max(1, concurrency));
        AtomicInteger done = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < games; i++) {
                final boolean aFirst = i % 2 == 0;
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

                        WinPointsService.ScoreBreakdown aSide = null;
                        WinPointsService.ScoreBreakdown bSide = null;
                        List<Integer> aPlayed = null;
                        List<Integer> bPlayed = null;
                        for (Player player : game.getPlayerUuidToPlayer().values()) {
                            if (player.getDifficulty() == a) {
                                aSide = winPointsService.scoreBreakdown(player, game);
                                aPlayed = List.copyOf(player.getPlayed().getCards());
                            } else if (player.getDifficulty() == b) {
                                bSide = winPointsService.scoreBreakdown(player, game);
                                bPlayed = List.copyOf(player.getPlayed().getCards());
                            }
                        }
                        if (aSide == null || bSide == null) {
                            throw new IllegalStateException("Could not identify players by difficulty");
                        }
                        results.add(new Game(aSide.total(), bSide.total(), game.getTurns(), aSide, bSide, aPlayed, bPlayed));
                    } catch (Throwable t) {
                        FatalErrors.haltIfOutOfMemory(t);
                        failed.increment();
                        t.printStackTrace();
                    } finally {
                        permits.release();
                        int d = done.incrementAndGet();
                        if (d % 200 == 0 || d == games) {
                            System.out.println("[SCORECARD] " + d + "/" + games);
                        }
                    }
                });
            }
        }

        int aWins = 0, bWins = 0, draws = 0;
        LongAdder aPoints = new LongAdder(), bPoints = new LongAdder(), turns = new LongAdder();
        DoubleAdder[] aTerms = adders();
        DoubleAdder[] bTerms = adders();
        List<Game> snapshot;
        synchronized (results) {
            snapshot = new ArrayList<>(results);
        }
        for (Game game : snapshot) {
            if (game.aPoints() > game.bPoints()) {
                aWins++;
            } else if (game.bPoints() > game.aPoints()) {
                bWins++;
            } else {
                draws++;
            }
            aPoints.add(game.aPoints());
            bPoints.add(game.bPoints());
            turns.add(game.turns());
            add(aTerms, game.aBreakdown());
            add(bTerms, game.bBreakdown());
        }
        Map<Integer, Integer> aCards = new java.util.HashMap<>();
        Map<Integer, Integer> bCards = new java.util.HashMap<>();
        double aPrinted = 0, bPrinted = 0;
        int aBuilds = 0, bBuilds = 0;
        for (Game game : snapshot) {
            for (int id : game.aPlayed()) {
                aCards.merge(id, 1, Integer::sum);
                var card = cardService.getCard(id);
                if (card != null) {
                    aPrinted += card.getWinningPoints();
                }
                aBuilds++;
            }
            for (int id : game.bPlayed()) {
                bCards.merge(id, 1, Integer::sum);
                var card = cardService.getCard(id);
                if (card != null) {
                    bPrinted += card.getWinningPoints();
                }
                bBuilds++;
            }
        }
        int counted = Math.max(1, snapshot.size());
        Report report = new Report(
                a.name(), b.name(), games, (int) failed.sum(), aWins, bWins, draws,
                aWins / (double) counted,
                aPoints.sum() / (double) counted,
                bPoints.sum() / (double) counted,
                turns.sum() / (double) counted,
                averages(aTerms, counted),
                averages(bTerms, counted),
                cardUsage(aCards, counted),
                cardUsage(bCards, counted),
                aBuilds / (double) counted,
                bBuilds / (double) counted,
                aPrinted / counted,
                bPrinted / counted);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        new ObjectMapper().writeValue(out.toFile(), report);
        System.out.println("[SCORECARD] " + report.a() + " " + round(report.aAvgPoints()) + " : "
                + round(report.bAvgPoints()) + " " + report.b() + ", win rate " + round(report.aWinRate() * 100) + "%");
        return report;
    }

    private static final String[] TERMS = {"tr", "forests", "cards", "resourceCards", "milestones", "awards", "extra"};

    private static DoubleAdder[] adders() {
        DoubleAdder[] adders = new DoubleAdder[TERMS.length];
        for (int i = 0; i < adders.length; i++) {
            adders[i] = new DoubleAdder();
        }
        return adders;
    }

    private static void add(DoubleAdder[] adders, WinPointsService.ScoreBreakdown breakdown) {
        adders[0].add(breakdown.terraformingRating());
        adders[1].add(breakdown.forests());
        adders[2].add(breakdown.cards());
        adders[3].add(breakdown.resourceCards());
        adders[4].add(breakdown.milestones());
        adders[5].add(breakdown.awards());
        adders[6].add(breakdown.extra());
    }

    /** card id -> how many times per game it was on the table; the caller joins it with names and points. */
    private static Map<String, Double> cardUsage(Map<Integer, Integer> counts, int games) {
        Map<String, Double> usage = new java.util.TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            usage.put(String.valueOf(entry.getKey()), round(entry.getValue() / (double) games));
        }
        return usage;
    }

    private static Map<String, Double> averages(DoubleAdder[] adders, int games) {
        Map<String, Double> averages = new LinkedHashMap<>();
        for (int i = 0; i < TERMS.length; i++) {
            averages.put(TERMS[i], round(adders[i].sum() / games));
        }
        return averages;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
