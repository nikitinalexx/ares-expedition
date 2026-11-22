package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.cards.blue.ConservedBiome;
import com.terraforming.ares.cards.blue.EcologicalZone;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.blue.Fish;
import com.terraforming.ares.cards.blue.Herbivores;
import com.terraforming.ares.cards.blue.SmallAnimals;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Does the player put its animals where the POINTS are? The cards that hold animals pay differently: Fish pays 1 point per
 * animal, the others pay 0.5 per animal (so they need 2 for a point). The owner's test (22/11): in games where several of
 * these are on the table, how often does the winner have its animals on Fish?
 * <p>
 * The audit plays N games between two difficulties and, at GAME_END, reads `player.getCardResourcesCount()` per card class,
 * plus the planet's final parameters (an ended game is one where they were pushed to the maximum). It counts, per side,
 * the games where each card held at least 1 and at least 2 animals, and the win rate in those games - so "Fish wins more"
 * is a measurement, not an impression.
 * <p>
 * {@code --mode cardaudit --a NETWORK_V2 --b NETWORK --games 200 --out FILE.json}
 */
@Component
@RequiredArgsConstructor
public class CardResourceAudit {

    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final WinPointsService winPointsService;
    private final com.terraforming.ares.services.CardService cardService;

    private static final Map<String, Class<? extends Card>> CARDS = new LinkedHashMap<>();

    static {
        CARDS.put("Fish", Fish.class);
        CARDS.put("ConservedBiome", ConservedBiome.class);
        CARDS.put("EcologicalZone", EcologicalZone.class);
        CARDS.put("FilterFeeders", FilterFeeders.class);
        CARDS.put("Herbivores", Herbivores.class);
        CARDS.put("SmallAnimals", SmallAnimals.class);
        CARDS.put("ArclightCorporation", ArclightCorporation.class);
    }

    public record CardStat(long gamesWith1, long winsWith1, long gamesWith2, long winsWith2) {
    }

    /** The owner's test: games where Fish is on the table TOGETHER with another animal card, and where the animals went. */
    public record FishChoice(long gamesWithFishAndOther, long gamesUsedFish, long winsUsedFish,
                             long gamesSkippedFish, long winsSkippedFish) {
    }

    public record Side(String difficulty, long games, long wins, Map<String, CardStat> cards, FishChoice fishChoice) {
    }

    public record Report(int games, int failed, double avgTurns, double avgTemperature, double avgOxygen,
                         double avgOceans, List<Side> sides) {
    }

    public Report run(Path out, int games, PlayerDifficulty a, PlayerDifficulty b, int concurrency) throws Exception {
        LongAdder failed = new LongAdder();
        DoubleAdder turns = new DoubleAdder(), temperature = new DoubleAdder(), oxygen = new DoubleAdder(), oceans = new DoubleAdder();
        Map<String, Map<String, long[]>> stats = new ConcurrentHashMap<>(); // difficulty -> card -> [g1, w1, g2, w2]
        Map<String, long[]> total = new ConcurrentHashMap<>();              // difficulty -> [games, wins]
        Map<String, long[]> fish = new ConcurrentHashMap<>();               // difficulty -> [withBoth, used, usedWins, skipped, skippedWins]
        AtomicInteger done = new AtomicInteger();
        Semaphore permits = new Semaphore(Math.max(1, concurrency));
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < games; i++) {
                final boolean aFirst = i % 2 == 0;
                permits.acquire();
                executor.submit(() -> {
                    try {
                        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                                .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                                .computers(aFirst ? List.of(a, b) : List.of(b, a))
                                .mulligan(true)
                                .expansion(Expansion.BASE)
                                .expansion(Expansion.BUFFED_CORPORATION)
                                .expansion(Expansion.DISCOVERY)
                                .dummyHand(true)
                                .build());
                        simulationProcessorService.processSimulation(game);
                        turns.add(game.getTurns());
                        temperature.add(game.getPlanet().getTemperatureValue());
                        oxygen.add(game.getPlanet().getOxygenValue());
                        oceans.add(game.getPlanet().getOceans().size());
                        for (Player player : game.getPlayerUuidToPlayer().values()) {
                            String difficulty = player.getDifficulty() == null ? "UNKNOWN" : player.getDifficulty().name();
                            int mine = winPointsService.countWinPoints(player, game);
                            int other = game.getPlayerUuidToPlayer().values().stream()
                                    .filter(p -> !p.getUuid().equals(player.getUuid()))
                                    .mapToInt(p -> winPointsService.countWinPoints(p, game)).max().orElse(0);
                            boolean won = mine > other;
                            long[] totals = total.computeIfAbsent(difficulty, key -> new long[2]);
                            synchronized (totals) {
                                totals[0]++;
                                if (won) {
                                    totals[1]++;
                                }
                            }
                            for (Map.Entry<String, Class<? extends Card>> card : CARDS.entrySet()) {
                                int count = player.getCardResourcesCount().getOrDefault(card.getValue(), 0);
                                if (count <= 0) {
                                    continue;
                                }
                                long[] row = stats.computeIfAbsent(difficulty, key -> new ConcurrentHashMap<>())
                                        .computeIfAbsent(card.getKey(), key -> new long[4]);
                                synchronized (row) {
                                    row[0]++;
                                    if (won) {
                                        row[1]++;
                                    }
                                    if (count >= 2) {
                                        row[2]++;
                                        if (won) {
                                            row[3]++;
                                        }
                                    }
                                }
                            }
                            // The owner's test: Fish on the table AND another animal card on the table. Did the animals go
                            // to Fish (1 point each) or to a 0.5-point card, and which of those games were won?
                            boolean fishOnTable = player.getPlayed().getCards().stream()
                                    .map(cardService::getCard).filter(java.util.Objects::nonNull)
                                    .anyMatch(c -> c.getClass() == Fish.class);
                            boolean otherAnimalOnTable = player.getPlayed().getCards().stream()
                                    .map(cardService::getCard).filter(java.util.Objects::nonNull)
                                    .anyMatch(c -> c.getClass() != Fish.class && CARDS.containsValue(c.getClass())
                                            && !c.getClass().equals(ArclightCorporation.class));
                            if (fishOnTable && otherAnimalOnTable) {
                                long[] row = fish.computeIfAbsent(difficulty, key -> new long[5]);
                                boolean usedFish = player.getCardResourcesCount().getOrDefault(Fish.class, 0) > 0;
                                synchronized (row) {
                                    row[0]++;
                                    if (usedFish) {
                                        row[1]++;
                                        if (won) {
                                            row[2]++;
                                        }
                                    } else {
                                        row[3]++;
                                        if (won) {
                                            row[4]++;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        FatalErrors.haltIfOutOfMemory(t);
                        failed.increment();
                    } finally {
                        permits.release();
                        int d = done.incrementAndGet();
                        if (d % 50 == 0 || d == games) {
                            System.out.println("[CARDAUDIT] " + d + "/" + games);
                        }
                    }
                });
            }
        }
        int played = Math.max(1, (int) (games - failed.sum()));
        List<Side> sides = new ArrayList<>();
        stats.forEach((difficulty, byCard) -> {
            long[] totals = total.getOrDefault(difficulty, new long[2]);
            Map<String, CardStat> cards = new LinkedHashMap<>();
            byCard.forEach((card, row) -> cards.put(card, new CardStat(row[0], row[1], row[2], row[3])));
            long[] f = fish.getOrDefault(difficulty, new long[5]);
            sides.add(new Side(difficulty, totals[0], totals[1], cards,
                    new FishChoice(f[0], f[1], f[2], f[3], f[4])));
        });
        sides.sort((l, r) -> l.difficulty().compareTo(r.difficulty()));
        Report report = new Report(games, (int) failed.sum(), round(turns.sum() / played),
                round(temperature.sum() / played), round(oxygen.sum() / played), round(oceans.sum() / played), sides);
        Files.createDirectories(out.getParent());
        new ObjectMapper().writeValue(out.toFile(), report);
        System.out.printf("[CARDAUDIT] games %d, turns %.1f, temperature %.1f, oxygen %.1f, oceans %.1f%n",
                played, report.avgTurns(), report.avgTemperature(), report.avgOxygen(), report.avgOceans());
        for (Side side : sides) {
            System.out.printf("[CARDAUDIT] %s: %d/%d won (%.0f%%)%n", side.difficulty(), side.wins(), side.games(),
                    100.0 * side.wins() / Math.max(side.games(), 1));
            side.cards().forEach((card, value) -> System.out.printf(
                    "[CARDAUDIT]   %-20s >=1 animal: %4d games, won %3.0f%%   >=2: %4d games, won %3.0f%%%n",
                    card, value.gamesWith1(), 100.0 * value.winsWith1() / Math.max(value.gamesWith1(), 1),
                    value.gamesWith2(), 100.0 * value.winsWith2() / Math.max(value.gamesWith2(), 1)));
            FishChoice choice = side.fishChoice();
            System.out.printf("[CARDAUDIT]   Fish + another animal on the table: %d games; animals on Fish %d (won %.0f%%), "
                            + "only on the others %d (won %.0f%%)%n",
                    choice.gamesWithFishAndOther(), choice.gamesUsedFish(),
                    100.0 * choice.winsUsedFish() / Math.max(choice.gamesUsedFish(), 1),
                    choice.gamesSkippedFish(),
                    100.0 * choice.winsSkippedFish() / Math.max(choice.gamesSkippedFish(), 1));
        }
        return report;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

