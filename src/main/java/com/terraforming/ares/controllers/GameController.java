package com.terraforming.ares.controllers;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dto.*;
import com.terraforming.ares.entity.CrisisRecordEntity;
import com.terraforming.ares.entity.SoloRecordEntity;
import com.terraforming.ares.factories.GameFactory;
import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.request.AllProjectsRequest;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.repositories.GameRepositoryImpl;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.SoloRecordEntityRepository;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.TestAiService;
import com.terraforming.ares.services.ai.advanced.CompleteHandEncoder;
import com.terraforming.ares.services.ai.advanced.CompleteTableEncoder;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dto.CardProjection;
import com.terraforming.ares.services.ai.turnProcessors.AiMulliganCardsTurn;
import com.terraforming.ares.services.simulations.CardPickStatistics;
import com.terraforming.ares.services.simulations.DatasetWriter;
import com.terraforming.ares.services.simulations.GameResultProducer;
import com.terraforming.ares.services.simulations.SampleBatch;
import lombok.RequiredArgsConstructor;
import org.nd4j.common.primitives.AtomicDouble;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.terraforming.ares.model.Constants.WRITE_STATISTICS_TO_FILE;
import static com.terraforming.ares.services.simulations.DatasetWriter.POISON;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class GameController {
    public static boolean BREAK_SIMULATIONS_EARLY = false;
    private final GameService gameService;
    private final CardFactory cardFactory;
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final CachingGameRepository cachingGameRepository;
    private final TurnService turnService;
    private final GameRepositoryImpl gameRepository;
    private final SimulationProcessorService simulationProcessorService;
    private final CrisisRecordEntityRepository crisisRecordEntityRepository;
    private final SoloRecordEntityRepository soloRecordEntityRepository;
    private final DeepNetwork deepNetwork;
    private final DatasetCollectionService datasetCollectionService;
    private final GameFactory gameFactory;
    private final AiMulliganCardsTurn aiMulliganCardsTurn;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final TestAiService testAiService;
    private final CardPickStatistics cardPickStatistics;
    private final CardCombinationCounter cardCombinationCounter;
    private final NNService nnService;
    private final IDataCollect dataCollect;

    @PostMapping("/state/test/{networkNumber}")
    public float testGameState(@RequestBody MarsGameRow row, @PathVariable int networkNumber) {
        return deepNetwork.testState(row, networkNumber);
    }

    @PostMapping("/game/new")
    public PlayerUuidsDto startNewGame(@RequestBody GameParameters gameParameters) {
        try {
            if (CollectionUtils.isEmpty(gameParameters.getComputers()) || CollectionUtils.isEmpty(gameParameters.getPlayerNames())
                    || gameParameters.getComputers().size() != gameParameters.getPlayerNames().size()
                    || gameParameters.getComputers().stream().anyMatch(Objects::isNull)
                    || gameParameters.getPlayerNames().stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Invalid number of computers and players, please reload the game");
            }

            if (gameParameters.getComputers().stream().anyMatch(computer -> !Constants.AVAILABLE_DIFFICULTIES.contains(computer))) {
                throw new IllegalArgumentException("Only Random/Smart/Network computers are supported");
            }

            if (!gameParameters.getExpansions().contains(Expansion.DISCOVERY)) {
                throw new IllegalArgumentException("Discovery expansion is a default mode for this game");
            }

            gameParameters.setComputers(List.of(PlayerDifficulty.NONE, PlayerDifficulty.NETWORK_V2));

            int aiPlayerCount = (int) gameParameters.getComputers().stream().filter(item -> item != PlayerDifficulty.NONE).count();
            int playersCount = gameParameters.getPlayerNames().size();
            int[] extraPoints = gameParameters.getExtraPoints();

//            gameParameters.getExpansions().add(Expansion.EXPERIMENTAL);//TODO remove

            if (gameParameters.getComputers().contains(PlayerDifficulty.NETWORK) && playersCount != 2) {
                throw new IllegalArgumentException("AI computer available only for 2 player game");
            }

            if (gameParameters.getComputers().contains(PlayerDifficulty.NETWORK) && gameParameters.getExpansions().contains(Expansion.INFRASTRUCTURE)) {
                throw new IllegalArgumentException("AI computer doesn't know how to play Infrastructure");
            }

            if (gameParameters.getPlayerNames().stream().anyMatch(name -> name.length() > 10)) {
                throw new IllegalArgumentException("Only names of length 10 or below are supported");
            }

            if (gameParameters.getPlayerNames().stream().distinct().count() != gameParameters.getPlayerNames().size()) {
                throw new IllegalArgumentException("Nicknames must be unique");
            }

            if (playersCount > Constants.MAX_PLAYERS) {
                throw new IllegalArgumentException("Only 1 to 4 players are supported so far");
            }
            if (extraPoints != null && extraPoints.length > 0) {
                if (extraPoints.length > 4) {
                    throw new IllegalArgumentException("Invalid extra points provided");

                }

                if (playersCount == 1 && extraPoints[0] != 0) {
                    throw new IllegalArgumentException("You can't play with a handicap in solo mode");
                }

                if (Arrays.stream(extraPoints).anyMatch(x -> x < 0)) {
                    throw new IllegalArgumentException("A handicap cannot be less than zero");
                }

                if (gameParameters.getExpansions().contains(Expansion.CRYSIS)) {
                    throw new IllegalArgumentException("No extra points allowed in Crisis mode");
                }
            }

            if (gameParameters.getExpansions().contains(Expansion.CRYSIS)) {
                if (!gameParameters.getExpansions().contains(Expansion.BUFFED_CORPORATION)) {
                    gameParameters.getExpansions().add(Expansion.BUFFED_CORPORATION);
                }
                if (playersCount != 1) {
                    throw new IllegalArgumentException("Only 1 player supported so far. Come back in a week!");
                }
                if (gameParameters.getComputers().stream().anyMatch(computer -> computer != PlayerDifficulty.NONE)) {
                    throw new IllegalArgumentException("Crysis is a cooperative mode, no computers supported");
                }
                if (!gameParameters.isMulligan()) {
                    throw new IllegalArgumentException("Crysis mode can be only played with mulligan option");
                }
                if (gameParameters.isDummyHand()) {
                    throw new IllegalArgumentException("Crysis mode has a dummy hand option by default");
                }
            }

            MarsGame marsGame = gameService.startNewGame(gameParameters);


            if (aiPlayerCount == playersCount) {
                turnService.pushGame(marsGame.getId());

                Thread.sleep(500);
            }

            Map<String, Player> playerNameToPlayer = marsGame.getPlayerUuidToPlayer().values().stream()
                    .collect(Collectors.toMap(
                            Player::getName, Function.identity()
                    ));


            return PlayerUuidsDto.builder()
                    .players(gameParameters.getPlayerNames().stream()
                            .map(
                                    playerName -> PlayerReference.builder()
                                            .name(playerName)
                                            .uuid(playerNameToPlayer.get(playerName).getUuid())
                                            .build()
                            )
                            .collect(Collectors.toList())
                    )
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Integer, Integer> TURNS_TO_GAMES_COUNT = new ConcurrentHashMap<>();
    private static Map<Integer, Long> TURNS_TO_POINTS_COUNT = new ConcurrentHashMap<>();

    static Map<Integer, Double> TURN_TO_AVERAGE_WINNER = new HashMap<>();

    static {
        TURN_TO_AVERAGE_WINNER.put(18, 98.0);
        TURN_TO_AVERAGE_WINNER.put(19, 84.55076951246996);
        TURN_TO_AVERAGE_WINNER.put(20, 88.85);
        TURN_TO_AVERAGE_WINNER.put(21, 90.58851502673461);
        TURN_TO_AVERAGE_WINNER.put(22, 89.7348072719857);
        TURN_TO_AVERAGE_WINNER.put(23, 91.56718321249518);
        TURN_TO_AVERAGE_WINNER.put(24, 93.70033667433975);
        TURN_TO_AVERAGE_WINNER.put(25, 94.93040094901617);
        TURN_TO_AVERAGE_WINNER.put(26, 96.85448928244499);
        TURN_TO_AVERAGE_WINNER.put(27, 99.11833576002077);
        TURN_TO_AVERAGE_WINNER.put(28, 101.44698210512115);
        TURN_TO_AVERAGE_WINNER.put(29, 104.1952261755979);
        TURN_TO_AVERAGE_WINNER.put(30, 106.48138525460016);
        TURN_TO_AVERAGE_WINNER.put(31, 107.68035410278775);
        TURN_TO_AVERAGE_WINNER.put(32, 112.43282733797317);
        TURN_TO_AVERAGE_WINNER.put(33, 116.53141963823941);
        TURN_TO_AVERAGE_WINNER.put(34, 121.18224693045502);
        TURN_TO_AVERAGE_WINNER.put(35, 125.9541191788592);
        TURN_TO_AVERAGE_WINNER.put(36, 123.07959197335057);
        TURN_TO_AVERAGE_WINNER.put(37, 130.26306841789037);
        TURN_TO_AVERAGE_WINNER.put(38, 136.79619494606467);
        TURN_TO_AVERAGE_WINNER.put(39, 144.4201232910156);
        TURN_TO_AVERAGE_WINNER.put(40, 152.2880622804627);
        TURN_TO_AVERAGE_WINNER.put(41, 146.00947329872534);
        TURN_TO_AVERAGE_WINNER.put(42, 144.11857169015067);
        TURN_TO_AVERAGE_WINNER.put(43, 177.08999938964843);
        TURN_TO_AVERAGE_WINNER.put(44, 133.16600036621094);
    }

    @GetMapping("/calculateRaw")
    public void calculateRaw(@RequestParam int cardId) throws InterruptedException {
        List<PlayerDifficulty> difficulties = List.of(PlayerDifficulty.NETWORK_V2, PlayerDifficulty.NETWORK_V2);

        GameParameters gameParameters = GameParameters.builder()
                .playerNames(List.of("a", "b"))
                .computers(difficulties)
                .mulligan(true)
                .expansion(Expansion.BASE)
                .expansion(Expansion.BUFFED_CORPORATION)
                .expansion(Expansion.DISCOVERY)
                .dummyHand(true)
                .build();

        Semaphore permits = new Semaphore(1000);

        AtomicDouble defaultAtomic = new AtomicDouble(0);
        AtomicDouble projectedAtomic = new AtomicDouble(0);

        int cardToTest = cardId;

        try (ExecutorService gameExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 50; i++) {
                permits.acquire(); // блокирует, но это platform thread (обычно контроллер)

                gameExecutor.submit(() -> {
                    try {
                        MarsGame game = gameService.createNewSimulation(gameParameters);
                        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
                        players.sort(Comparator.comparing(player -> player.getUuid().charAt(player.getUuid().length() - 1)));
                        Player player = players.get(0);
                        while (!player.getHand().containsCard(cardToTest)) {
                            game = gameService.createNewSimulation(gameParameters);
                            players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
                            players.sort(Comparator.comparing(p -> p.getUuid().charAt(p.getUuid().length() - 1)));
                            player = players.get(0);
                        }

                        Player anotherPlayer = players.get(1);
                        double defaultRawValue = 0;
                        for (int j = 0; j < 50; j++) {
                            MarsGame gameCopy = new MarsGame(game);
                            simulationProcessorService.processSimulation(gameCopy);

                            int firstPlayerWp = winPointsService.countWinPoints(gameCopy.getPlayerByUuid(player.getUuid()), gameCopy);
                            int secondPlayerWp = winPointsService.countWinPoints(gameCopy.getPlayerByUuid(anotherPlayer.getUuid()), gameCopy);

                            if (firstPlayerWp > secondPlayerWp) {
                                defaultRawValue += 1;
                            } else if (firstPlayerWp == secondPlayerWp) {
                                defaultRawValue += 0.5;
                            }
                        }
                        defaultRawValue /= 200.0;


                        player.getHand().removeCard(cardToTest);
                        player.getPlayed().addCard(cardToTest);


                        double projectedRawValue = 0;
                        for (int j = 0; j < 50; j++) {
                            MarsGame gameCopy = new MarsGame(game);
                            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());


                            simulationProcessorService.processSimulation(gameCopy);

                            int firstPlayerWp = winPointsService.countWinPoints(gameCopy.getPlayerByUuid(player.getUuid()), gameCopy);
                            int secondPlayerWp = winPointsService.countWinPoints(gameCopy.getPlayerByUuid(anotherPlayer.getUuid()), gameCopy);

                            if (firstPlayerWp > secondPlayerWp) {
                                projectedRawValue += 1;
                            } else if (firstPlayerWp == secondPlayerWp) {
                                projectedRawValue += 0.5;
                            }
                        }
                        projectedRawValue /= 200.0;

                        System.out.println("Default " + defaultRawValue + " proj " + projectedRawValue);

                        defaultAtomic.addAndGet(defaultRawValue);
                        projectedAtomic.addAndGet(projectedRawValue);

                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        permits.release();
                    }
                });
            }
        }

        System.out.println(defaultAtomic.get());
        System.out.println(projectedAtomic.get());
    }

    @GetMapping("/simulations/v2")
    public void runSimulationsV2(@RequestBody SimulationsRequest request) throws InterruptedException {

        int MAX_IN_FLIGHT_GAMES = 1000;
        Semaphore permits = new Semaphore(MAX_IN_FLIGHT_GAMES);


        List<PlayerDifficulty> difficulties = List.of(PlayerDifficulty.NETWORK_V2, PlayerDifficulty.NETWORK_V2);

        List<String> playerNames = new ArrayList<>();
        int counter = 1;
        for (PlayerDifficulty simulationPlayer : difficulties) {
            playerNames.add(simulationPlayer.name() + (counter++));
        }

        GameParameters gameParameters = GameParameters.builder()
                .playerNames(playerNames)
                .computers(difficulties)
                .mulligan(true)
                .expansion(Expansion.BASE)
                .expansion(Expansion.BUFFED_CORPORATION)
                .expansion(Expansion.DISCOVERY)
                .dummyHand(true)
                .build();

        AtomicInteger firstWins = new AtomicInteger();
        AtomicInteger secondWins = new AtomicInteger();
        AtomicInteger draw = new AtomicInteger();

        try (ExecutorService gameExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < request.getTotalSimulations(); i++) {
                permits.acquire(); // блокирует, но это platform thread (обычно контроллер)

                gameExecutor.submit(() -> {
                    try {
                        MarsGame game = gameService.createNewSimulation(gameParameters);
                        simulationProcessorService.processSimulation(game);

                        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
                        players.sort(Comparator.comparing(player -> player.getUuid().charAt(player.getUuid().length() - 1)));

                        int firstPlayerWp = winPointsService.countWinPoints(players.get(0), game);
                        int secondPlayerWp = winPointsService.countWinPoints(players.get(1), game);

                        if (firstPlayerWp > secondPlayerWp) {
                            firstWins.incrementAndGet();
                        } else if (secondPlayerWp > firstPlayerWp) {
                            secondWins.incrementAndGet();
                        } else {
                            draw.incrementAndGet();
                        }
                        int total = firstWins.get() + secondWins.get() + draw.get();
                        if (total % 10 == 0) {
                            System.out.printf("Total done %s/%s.%n", total, request.getTotalSimulations());
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        permits.release();
                    }
                });
                if (i % 10 == 0) {
                    System.out.printf("Sim loaded %s/%s. Wins 1=%s,2=%s,d=%s", i, request.getTotalSimulations(), firstWins, secondWins, draw);
                    System.out.println();

                }
            }
        }

        System.out.printf("Wins 1=%s,2=%s,d=%s", firstWins, secondWins, draw);
    }

    @GetMapping("/simulations/v3")
    public void runSimulationsV3(@RequestBody SimulationsRequest request) throws InterruptedException {
        List<PlayerDifficulty> difficulties = List.of(PlayerDifficulty.NETWORK, PlayerDifficulty.NETWORK_V2);

        List<String> playerNames = new ArrayList<>();
        int counter = 1;
        for (PlayerDifficulty simulationPlayer : difficulties) {
            playerNames.add(simulationPlayer.name() + (counter++));
        }

        GameParameters gameParameters = GameParameters.builder()
                .playerNames(playerNames)
                .computers(difficulties)
                .mulligan(true)
                .expansion(Expansion.BASE)
                .expansion(Expansion.BUFFED_CORPORATION)
                .expansion(Expansion.DISCOVERY)
                .dummyHand(true)
                .build();

        int totalSims = request.getTotalSimulations();
        LongAdder firstWins = new LongAdder(), secondWins = new LongAdder(), draws = new LongAdder();
        LongAdder completedCount = new LongAdder(); // Счетчик завершенных игр
        long startTime = System.currentTimeMillis();

        int MAX_CONCURRENT_GAMES = 2000;
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_GAMES);

        BlockingQueue<SampleBatch> queue = null;
        Thread writerThread = null;
        if (request.isCollectData()) {
            queue = new ArrayBlockingQueue<>(1000); // backpressure

            DatasetWriter writer = new DatasetWriter(queue);
            writerThread = new Thread(writer, "dataset-writer");
            writerThread.start();
        }

        Constants.FIRST_PLAYER_PHASES = new ConcurrentHashMap<>();
        Constants.SECOND_PLAYER_PHASES = new ConcurrentHashMap<>();

        ConcurrentHashMap<Integer, AtomicLong> pointsByTurn = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, AtomicLong> countByTurn = new ConcurrentHashMap<>();

        GameResultProducer sharedProducer = queue != null ? new GameResultProducer(queue, 512) : null;

        AtomicLong winPoints = new AtomicLong(0);
        AtomicLong turns = new AtomicLong(0);

        ConcurrentHashMap<Integer, AtomicLong> pickedCorporation = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, AtomicLong> wonCorporation = new ConcurrentHashMap<>();


        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalSims; i++) {
                executor.submit(() -> {
                    try {
                        semaphore.acquire();

                        MarsGame game = gameService.createNewSimulation(gameParameters);

                        if (!request.isCollectData()) {
                            simulationProcessorService.processSimulation(game);
                        }

                        GameResult gameResult = request.isCollectData() ? simulationProcessorService.runSimulationWithDataset(game) : null;

                        List<Player> players = game.getPlayerUuidToPlayer().values().stream()
                                .sorted(Comparator.comparing(p -> p.getUuid().substring(p.getUuid().length() - 1)))
                                .toList();
                        players.forEach(p -> pickedCorporation
                                .computeIfAbsent(p.getSelectedCorporationCard(), k -> new AtomicLong(0))
                                .incrementAndGet());


                        Player firstPlayer = players.get(0);
                        Player secondPlayer = players.get(1);
                        int p1 = winPointsService.countWinPoints(firstPlayer, game);
                        int p2 = winPointsService.countWinPoints(secondPlayer, game);

                        if (p1 != p2) {
                            int currentWonCorporation = p1 > p2 ? firstPlayer.getSelectedCorporationCard() : secondPlayer.getSelectedCorporationCard();

                            wonCorporation.computeIfAbsent(currentWonCorporation, k -> new AtomicLong(0)).incrementAndGet();
                        }


                        if (request.isCollectData()) {
                            sharedProducer.addResult(gameResult);
                        }

                        turns.addAndGet(game.getTurns());

                        if (p1 > p2) firstWins.increment();
                        else if (p2 > p1) secondWins.increment();
                        else draws.increment();

                        // Увеличиваем общий счетчик и проверяем, нужно ли печатать лог
                        completedCount.increment();
                        long currentTotal = completedCount.sum();

                        if (currentTotal % 50 == 0 || currentTotal == totalSims) {
                            double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                            double speed = currentTotal / Math.max(0.1, elapsed);

                            long f = firstWins.sum();
                            long s = secondWins.sum();

                            System.out.printf("[SIM] %d/%d (%.1f%%) | Speed: %.1f games/s | P1: %d%%, P2: %d%% %n",
                                    currentTotal, totalSims, (currentTotal * 100.0 / totalSims),
                                    speed, (f * 100 / currentTotal), (s * 100 / currentTotal));
                        }

                        // Собираем очки для обоих игроков
                        for (Player player : players) {
                            int p = winPointsService.countWinPoints(player, game);
                            winPoints.addAndGet(p);

                            // Статистика в разрезе конкретного хода
                            pointsByTurn.computeIfAbsent(game.getTurns(), k -> new AtomicLong(0)).addAndGet(p);
                            countByTurn.computeIfAbsent(game.getTurns(), k -> new AtomicLong(0)).incrementAndGet();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        semaphore.release();
                    }
                });
                if (i % 100 == 0) { // Реже логируем подачу задач
                    System.out.print(".");
                }
            }
            // Здесь поток ждет завершения всех задач
        }

        printCardStatistics();

        if (request.isCollectData()) {
            queue.put(POISON);
            writerThread.join();
        }

        long totalElapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf("%nFinal Result in %ds: 1=%d, 2=%d, D=%d%n",
                totalElapsed, firstWins.sum(), secondWins.sum(), draws.sum());

        System.out.println("Average win points " + winPoints.get() / 2 / request.getTotalSimulations() + ". Avg turns " + turns.get() / request.getTotalSimulations() + ".");

        System.out.println(Constants.FIRST_PLAYER_PHASES);
        System.out.println(Constants.SECOND_PLAYER_PHASES);

        // 2. После завершения цикла выводим таблицу "Нормативов"
        System.out.println("\n--- Performance Benchmarks (Average Points per Turn) ---");
        pointsByTurn.keySet().stream().sorted().forEach(t -> {
            long totalP = pointsByTurn.get(t).get();
            long games = countByTurn.get(t).get();
            double avg = (double) totalP / games;
            System.out.printf("Turn %d: Avg Points = %.2f (based on %d players)%n", t, avg, games);
        });

        System.out.println("\n" + "=".repeat(75));
        System.out.printf("%-30s | %-8s | %-6s | %-8s%n", "CORPORATION NAME", "PICKED", "WON", "WIN RATE");
        System.out.println("-".repeat(75));

        // Сортируем по количеству выборов (от популярных к редким)
        pickedCorporation.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .forEach(entry -> {
                    Card card = cardService.getCard(entry.getKey());
                    String name = card.getClass().getSimpleName();
                    long picked = entry.getValue().get();
                    long won = wonCorporation.getOrDefault(card.getId(), new AtomicLong(0)).get();
                    double winRate = (picked > 0) ? (won * 100.0 / picked) : 0;

                    // Печатаем строку: %-30s (30 символов под имя), %8d (8 под число) и т.д.
                    System.out.printf("%-30s | %8d | %6d | %7.1f%%%n",
                            name, picked, won, winRate);
                });

        System.out.println("=".repeat(75) + "\n");
    }

    public void printCardStatistics() {
        System.out.println("=== CARD PICK STATISTICS ===");

        cardPickStatistics.getStats().entrySet().stream()
                .filter(entry -> {
                    Integer cardId = entry.getKey();
                    return cardService.getCard(cardId).getColor() != CardColor.CORPORATION;
                })
                .map(entry -> {
                    int cardId = entry.getKey();
                    CardPickStatistics.CardStats s = entry.getValue();

                    double avgRank = s.averagePickedRank();
                    double winRate = s.winRate();
                    double score = avgRank * winRate * Math.log1p(s.timesPicked);

                    return new Object() {
                        final int id = cardId;
                        final CardPickStatistics.CardStats stats = s;
                        final double undervaluationScore = score;
                    };
                })
                .sorted(Comparator.comparingDouble(o -> -o.undervaluationScore))
//                .limit(30)
                .forEach(o -> {
                    CardPickStatistics.CardStats s = o.stats;

                    boolean suspicious =
                            s.averagePickedRank() > 6.0 &&
                                    s.winRate() > 0.55;

                    String marker = suspicious ? "  <<< ⚠ UNDERRATED" : "";

                    System.out.printf(
                            "score=%7.2f | picked=%4d | avgRank=%6.2f | winRate=%5.2f%% | games=%4d | %s%s%n",
                            o.undervaluationScore,
                            s.timesPicked,
                            s.averagePickedRank(),
                            s.winRate() * 100.0,
                            s.totalGames,
                            cardService.getCard(o.id).getCardMetadata().getName(),
                            marker
                    );
                });


        System.out.println("=== END ===");
    }

    @GetMapping("/simulations")
    public void runSimulations(@RequestBody SimulationsRequest request) throws IOException, InterruptedException {
        ArrayList<String> allFeatures = new ArrayList<>(CompleteTableEncoder.getAllFeatureNames());
        allFeatures.addAll(CompleteHandEncoder.getAllFeatureNames());

        System.out.println("All features " + allFeatures);

        assert allFeatures.size() == AiConstants.TOTAL_SIZE;

        TURNS_TO_GAMES_COUNT.clear();
        TURNS_TO_POINTS_COUNT.clear();

        List<PlayerDifficulty> playerDifficulties = Constants.SIMULATION_PLAYERS;

        Constants.FIRST_PLAYER_PHASES = new ConcurrentHashMap<>();
        Constants.SECOND_PLAYER_PHASES = new ConcurrentHashMap<>();

        int threads = Runtime.getRuntime().availableProcessors();

        if (request.getTotalSimulations() < threads) {
            throw new IllegalArgumentException("Simulation count less than total number of threads");
        }

        System.out.println("Starting simulations");

        GameStatistics gameStatistics = new GameStatistics();


        BlockingQueue<SampleBatch> queue = new ArrayBlockingQueue<>(1000); // backpressure

        DatasetWriter writer = new DatasetWriter(queue);
        Thread writerThread = new Thread(writer, "dataset-writer");
        writerThread.start();

        IntStream.range(0, threads).parallel().forEach(i -> {
            new WorkerThread(
                    playerDifficulties,
                    request.getTotalSimulations() / threads,
                    gameStatistics,
                    i,
                    queue // 👈 добавили
            ).run();
        });

        queue.put(POISON);
        writerThread.join();

        System.out.println("Finished all threads");
        System.out.println(Arrays.toString(globalMax));

        writeToFile(globalMax, allFeatures, Path.of("maxData.txt"));

        printStatistics(gameStatistics);

        System.out.println(Constants.FIRST_PLAYER_PHASES);
        System.out.println(Constants.SECOND_PLAYER_PHASES);
        System.out.println("Finished");

        System.out.println(maxMcIncome);
        System.out.println(maxHeatIncome);
        System.out.println(maxScience);
        System.out.println(maxCardsPlayed);
        System.out.println(maxSteelTitanium);
        System.out.println(maxResources);
    }

    public static void writeToFile(float[] values, List<String> names, Path filePath)
            throws IOException {

        if (values.length != names.size()) {
            throw new IllegalArgumentException("Размеры массива и списка не совпадают");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 0; i < values.length; i++) {
                writer.write(names.get(i) + " " + values[i]);
                writer.newLine();
            }
        }
    }

    @GetMapping("/mulligan/calibrate")
    public void calibrateMulligan() {
        Random random = new Random();
        GameParameters.GameParametersBuilder builder = GameParameters.builder();

        List<String> playerNames = new ArrayList<>();
        for (PlayerDifficulty simulationPlayer : Constants.SIMULATION_PLAYERS) {
            playerNames.add(simulationPlayer.name());
        }

        GameParameters gameParameters = builder
                .playerNames(playerNames)
                .computers(Constants.SIMULATION_PLAYERS)
                .mulligan(true)
                .expansion(Expansion.BASE)
                .expansion(Expansion.BUFFED_CORPORATION)
                .expansion(Expansion.DISCOVERY)
                .dummyHand(true)
                .build();

        double total = 0;

        for (int j = 0; j < 10000; j++) {
            MarsGame marsGame = gameFactory.createMarsGame(gameParameters);

            List<Player> players = new ArrayList<>(marsGame.getPlayerUuidToPlayer().values());

            Player player = players.get(random.nextInt(players.size()));
            aiMulliganCardsTurn.processTurn(marsGame, player);


            MarsGame gameAfterOpponentPlay = testAiService.projectOpponentCorporationBuildExperiment(marsGame, player);

            LinkedList<Integer> cards = player.getHand().getCards();

            float bestTotalExtra = 0;

            for (Integer corporation : player.getCorporations().getCards()) {
                MarsGame projectionAfterCorporation = testAiService.projectPlayerBuildCorporationExperiment(gameAfterOpponentPlay, player, corporation);

                float initialChance = deepNetwork.testState(projectionAfterCorporation, projectionAfterCorporation.getPlayerByUuid(player.getUuid()));

                float totalExtra = 0;

                for (int i = 0; i < cards.size(); i++) {
                    MarsGame gameCopy = new MarsGame(projectionAfterCorporation);
                    CardProjection cardProjection = aiPickCardProjectionService.cardExtraChanceIfBuilt(gameCopy, gameCopy.getPlayerByUuid(player.getUuid()), cards.get(i), initialChance, 0);
                    if (cardProjection.isUsable()) {
                        totalExtra += cardProjection.getChance();
                    }
                }

                if (totalExtra > bestTotalExtra) {
                    bestTotalExtra = totalExtra;
                }

//                System.out.println(cardService.getCard(corporation).getClass().getSimpleName() + " " + (initialChance + totalExtra / cards.size()));

            }

            total += bestTotalExtra;
//            System.out.println();
        }

        System.out.println(total);
    }

    float[] globalMax = new float[AiConstants.TOTAL_SIZE];
    final Object lock = new Object();

    class WorkerThread implements Runnable {
        int simulationCount;
        GameStatistics gameStatistics;
        int threadIndex;
        List<PlayerDifficulty> playerDifficulty;
        private final GameResultProducer producer;

        WorkerThread(List<PlayerDifficulty> playerDifficulty, int simulationCount, GameStatistics gameStatistics, int threadIndex, BlockingQueue<SampleBatch> queue) {
            this.simulationCount = simulationCount;
            this.gameStatistics = gameStatistics;
            this.threadIndex = threadIndex;
            this.playerDifficulty = playerDifficulty;
            this.producer = new GameResultProducer(queue, 512); // batch size
        }

        @Override
        public void run() {
            GameParameters.GameParametersBuilder builder = GameParameters.builder();

            List<String> playerNames = new ArrayList<>();
            int counter = 1;
            for (PlayerDifficulty simulationPlayer : playerDifficulty) {
                playerNames.add(simulationPlayer.name() + (counter++));
            }

            GameParameters.GameParametersBuilder gameParametersBuilder = builder
                    .playerNames(playerNames)
                    .computers(playerDifficulty)
                    .mulligan(true)
                    .expansion(Expansion.BASE)
                    .expansion(Expansion.BUFFED_CORPORATION)
                    .expansion(Expansion.DISCOVERY)
                    .dummyHand(true);
            if (Constants.INFRASTRUCTURE_SIMULATIONS) {
                gameParametersBuilder.expansion(Expansion.INFRASTRUCTURE);
            }
            GameParameters gameParameters = gameParametersBuilder.build();

            List<MarsGame> games = new ArrayList<>();


            for (int i = 1; i <= simulationCount; i++) {
                if (BREAK_SIMULATIONS_EARLY) {
                    break;
                }
                MarsGame marsGame = gameService.createNewSimulation(gameParameters);
                if (Constants.COLLECT_DATASET) {
                    GameResult dataSet = simulationProcessorService.runSimulationWithDataset(marsGame);
                    float[] localMax = dataSet.getLocalMax();
                    countGlobalMax(localMax);
//                    try {
//                        producer.addResult(dataSet);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                } else {
                    simulationProcessorService.processSimulation(marsGame);
                }

                games.add(marsGame);

                if (i != 0 && i % 10 == 0) {
                    System.out.printf("Done %s/%s%n", i, simulationCount);
                }

                if (i % 100 == 0) {
                    gatherStatistics(games, gameStatistics);
                    games.clear();
                }
            }

            gatherStatistics(games, gameStatistics);
        }

        private void countGlobalMax(float[] localMax) {
            synchronized (lock) {
                for (int i = 0; i < AiConstants.TOTAL_SIZE; i++) {
                    globalMax[i] = Math.max(globalMax[i], localMax[i]);
                }
            }
        }
    }

    private void gatherStatistics(List<MarsGame> games, GameStatistics gameStatistics) {
        List<MarsGame> finishedGames = games.stream()
                .filter(MarsGame::gameEndCondition)
                .filter(game -> game.getTurns() <= GameStatistics.MAX_TURNS_TO_CONSIDER)
                .collect(Collectors.toList());

        games.stream()
                .map(MarsGame::getTurns)
                .filter(turn -> turn > 60)
                .forEach(turn -> System.out.println("Long game " + turn));

        for (int i = 0; i < finishedGames.size(); i++) {
            MarsGame game = finishedGames.get(i);

            game.getPlayerUuidToPlayer().values().forEach(
                    player -> {
                        for (Integer playedCard : player.getPlayed().getCards()) {
                            if (playedCard < Constants.CORPORATION_ID_OFFSET) {
                                gameStatistics.cardOccured(
                                        player.getPlayed().getCardToTurn().get(playedCard),
                                        playedCard
                                );
                            }
                        }
                    }
            );


            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

            players.sort(Comparator.comparing(player -> player.getUuid().charAt(player.getUuid().length() - 1)));

            List<Integer> winPoints = players.stream().map(player -> winPointsService.countWinPoints(player, game)).toList();

            int maxWinPoints = Collections.max(winPoints);
            int winnerIndex = 0;

            int winners = 0;
            for (int j = 0; j < winPoints.size(); j++) {
                if (maxWinPoints == winPoints.get(j)) {
                    winners++;
                    winnerIndex = j;
                }
            }

            if (winners == 1) {
                gameStatistics.addTotalGames(1);
                gameStatistics.addTotalTurnsCount(game.getTurns());
                gameStatistics.addTotalPointsCount(winPoints.stream().mapToInt(Integer::intValue).sum());

                gameStatistics.addPlayerWins(winnerIndex);


                Player winner = players.get(winnerIndex);

                for (Integer playedCard : winner.getPlayed().getCards()) {
                    if (playedCard < Constants.CORPORATION_ID_OFFSET) {
                        gameStatistics.winCardOccured(
                                winner.getPlayed().getCardToTurn().get(playedCard),
                                playedCard
                        );
                    }
                }


                gameStatistics.corporationWon(winner.getSelectedCorporationCard());
                players.forEach(player -> {
                    gameStatistics.corporationOccured(player.getSelectedCorporationCard());
                });
            }
        }
    }

    static DecimalFormatSymbols symbols;
    static DecimalFormat floatDecimalFormat;

    static {
        symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        floatDecimalFormat = new DecimalFormat("#.####", symbols);
    }

    private String getFloatForWrite(float v) {
        if (Float.compare(v, 1f) == 0) {
            return "1";
        } else if (Float.compare(v, 0f) == 0) {
            return "0";
        } else {
            // Format the float as a string with 4 decimal places
            return floatDecimalFormat.format(v);
        }
    }

    float maxMcIncome = 0;
    float maxHeatIncome = 0;
    float maxScience = 0;
    float maxCardsPlayed = 0;
    float maxSteelTitanium = 0;
    float maxResources = 0;

    private void writeMarsGameRows(List<MarsGameRow> rows, PrintWriter pw) {
        String previousString = null;
        for (MarsGameRow row : rows) {
            if (row.getPlayer().getMcIncome() > maxMcIncome) {
                maxMcIncome = row.getPlayer().getMcIncome();
            }
            if (row.getPlayer().getHeatIncome() > maxHeatIncome) {
                maxHeatIncome = row.getPlayer().getHeatIncome();
            }
            if (row.getPlayer().getScienceTagsCount() > maxScience) {
                maxScience = row.getPlayer().getScienceTagsCount();
            }
            if (row.getPlayer().getCardsBuilt() > maxCardsPlayed) {
                maxCardsPlayed = row.getPlayer().getCardsBuilt();
            }
            if (row.getPlayer().getSteelIncome() + row.getPlayer().getTitaniumIncome() > maxSteelTitanium) {
                maxSteelTitanium = row.getPlayer().getSteelIncome() + row.getPlayer().getTitaniumIncome();
            }
            String newString = getMarsGameRowString(row);
            if (newString != null && (!newString.equals(previousString))) {
                pw.println(newString);
            }
            previousString = newString;
        }
    }

    private String getMarsGameRowString(MarsGameRow row) {
        if (row.getTurn() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        float[] output = datasetCollectionService.mapMarsGameToArrayForStudy(row);

        for (int i = 0; i < output.length; i++) {
            sb.append(getFloatForWrite(output[i]));
            if (i != output.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private void printStatistics(GameStatistics gameStatistics) {
        if (WRITE_STATISTICS_TO_FILE) {
            try (FileWriter fw = new FileWriter("cardStats.txt");
                 BufferedWriter writer = new BufferedWriter(fw)) {

                final Map<Integer, Map<Integer, Integer>> winCardOccurenceByTurn = gameStatistics.getTurnToWinCardsOccurence();
                final Map<Integer, Map<Integer, Integer>> occurenceByTurn = gameStatistics.getTurnToCardsOccurence();

                List<Integer> allCards = occurenceByTurn.values().stream()
                        .flatMap(cards -> cards.keySet().stream())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                for (Integer cardId : allCards) {
                    if (Constants.WRITE_CARD_STATISTICS_TO_CONSOLE) {
                        System.out.println("Id: " + cardId + ". " + cardService.getCard(cardId).getClass().getSimpleName());
                    }
                    writer.write("#" + cardId + " " + cardService.getCard(cardId).getClass().getSimpleName() + "\n");

                    for (int turn = 1; turn <= winCardOccurenceByTurn.size() && turn <= 50; turn++) {
                        Map<Integer, Integer> winCardOccurence = winCardOccurenceByTurn.get(turn);
                        Map<Integer, Integer> cardOccurence = occurenceByTurn.get(turn);

                        if (Constants.WRITE_CARD_STATISTICS_TO_CONSOLE) {
                            System.out.println("Turn " + turn + ". " + (double) winCardOccurence.getOrDefault(cardId, 0) * 100 / cardOccurence.get(cardId));
                        }
                        writer.write("." + turn + " " + (double) winCardOccurence.getOrDefault(cardId, 0) * 100 / cardOccurence.getOrDefault(cardId, 1) + "\n");
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        gameStatistics.getCorporationToWin().entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> (double) entry.getValue() / gameStatistics.getCorporationToOccurence().get(entry.getKey())))
                .forEachOrdered(entry -> {
                    System.out.println(cardService.getCard(entry.getKey()).getClass().getSimpleName() + " " + (double) entry.getValue() / gameStatistics.getCorporationToOccurence().get(entry.getKey()));
                });


        System.out.println("Total games: " + gameStatistics.getTotalGames());
        System.out.println((double) gameStatistics.getTotalTurnsCount() / gameStatistics.getTotalGames());

        System.out.println((double) gameStatistics.getTotalPointsCount() / (2 * gameStatistics.getTotalGames()));

        long totalWins = gameStatistics.getWinners().stream().mapToLong(Long::longValue).sum();

        for (int i = 0; i < gameStatistics.getWinners().size(); i++) {
            System.out.println("Player victories: " + gameStatistics.getWinners().get(i) + ". Ratio: " + ((double) gameStatistics.getWinners().get(i) / totalWins));
        }

//        System.out.println(TURNS_TO_GAMES_COUNT);
//        System.out.println(TURNS_TO_POINTS_COUNT);
//
//        for (Map.Entry<Integer, Integer> entry : TURNS_TO_GAMES_COUNT.entrySet()) {
//            System.out.println("Turns " + entry.getKey() + " average points: " + ((float) TURNS_TO_POINTS_COUNT.get(entry.getKey()) / entry.getValue()));
//        }
    }

    @GetMapping("/simulations/stop")
    public void getGameByPlayerUuid() {
        BREAK_SIMULATIONS_EARLY = true;
    }

    @GetMapping("/game/player/{playerUuid}")
    public GameDto getGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

        long aiComputerCount = game.getPlayerUuidToPlayer().values().stream().filter(player -> player.getDifficulty() != null && player.getDifficulty() != PlayerDifficulty.NONE && player.getDifficulty() != PlayerDifficulty.RANDOM && player.getDifficulty() != PlayerDifficulty.SMART).count();
        Player aiComputer = null;

        if (aiComputerCount == 2) {
            aiComputer = game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(playerUuid)).findFirst().orElse(null);
        } else if (aiComputerCount == 1) {
            aiComputer = game.getPlayerUuidToPlayer().values().stream().filter(player -> player.getDifficulty() != PlayerDifficulty.NONE && player.getDifficulty() != PlayerDifficulty.RANDOM && player.getDifficulty() != PlayerDifficulty.SMART).findFirst().orElse(null);
        }

        float winProbability;

        if (aiComputer == null) {
            aiComputerCount = 0;
            winProbability = 0;
        } else {

//            winProbability = deepNetwork.testState(game, aiComputer);
            winProbability = (float) nnService.predictBatch(List.of(dataCollect.collectData(game, aiComputer.getUuid())), NNService.ModelType.SECOND).getFirst().baseProb;
        }

        Planet phasePlanet = game.getPlanetAtTheStartOfThePhase();

        final CrysisData crysisData = game.getCrysisData();
        return GameDto.builder()
                .phase(game.getCurrentPhase())
                .player(buildCurrentPlayer(game.getPlayerByUuid(playerUuid), game))
                .temperature(game.getPlanet().getTemperatureValue())
                .oxygen(game.getPlanet().getOxygenValue())
                .infrastructure(game.getPlanet().getInfrastructureValue())
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperatureValue() : null)
                .phaseTemperatureColor(phasePlanet != null ? phasePlanet.getTemperatureColor() : null)
                .phaseInfrastructure(phasePlanet != null ? phasePlanet.getInfrastructureValue() : null)
                .phaseOxygen(phasePlanet != null ? phasePlanet.getOxygenValue() : null)
                .phaseOxygenColor(phasePlanet != null ? phasePlanet.getOxygenColor() : null)
                .phaseInfrastructureColor(phasePlanet != null ? phasePlanet.getInfrastructureColor() : null)
                .oceans(game.getPlanet().getOceans().stream().map(OceanDto::of).collect(Collectors.toList()))
                .phaseOceans(phasePlanet != null ? phasePlanet.getRevealedOceans().size() : null)
                .otherPlayers(buildOtherPlayers(game, playerUuid))
                .turns(game.isCrysis() ? crysisData.getCrysisCards().size() + crysisData.getEasyModeTurnsLeft() : game.getTurns())
                .dummyHandMode(game.isDummyHandMode())
                .usedDummyHand(game.getUsedDummyHand())
                .awards(game.getAwards().stream().map(AwardDto::from).collect(Collectors.toList()))
                .milestones(game.getMilestones().stream().map(MilestoneDto::from).collect(Collectors.toList()))
                .crysisDto(buildCrysisDto(crysisData))
                .stateReason(game.getStateReason())
                .aiComputer(aiComputerCount > 0)
                .winProbability(aiComputerCount > 0 ? (int) (winProbability * 100) : 0)
                .build();
    }

    private CrysisDto buildCrysisDto(CrysisData crysisData) {
        if (crysisData == null) {
            return null;
        }
        return CrysisDto.builder()
                .detrimentTokens(crysisData.getDetrimentTokens())
                .openedCards(crysisData.getOpenedCards().stream().map(cardService::getCrysisCard).map(CrysisCardDto::from).collect(Collectors.toList()))
                .cardToTokensCount(crysisData.getCardToTokensCount())
                .forbiddenPhases(crysisData.getForbiddenPhases())
                .currentDummyCards(crysisData.getCurrentDummyCards())
                .chosenDummyPhases(crysisData.getChosenDummyPhases())
                .wonGame(crysisData.isWonGame())
                .build();
    }

//    @GetMapping("/crisis/records/points")
//    public List<CrisisRecordEntity> findTopTwentyRecordsByPoints(@RequestParam int playerCount) {
//        return crisisRecordEntityRepository.findTopTwentyRecordsByPoints(playerCount);
//    }
//
//    @GetMapping("/crisis/records/turns")
//    public List<CrisisRecordEntity> findTopTwentyRecordsByTurns(@RequestParam int playerCount) {
//        return crisisRecordEntityRepository.findTopTwentyRecordsByTurns(playerCount);
//    }

    @GetMapping("/crisis/records")
    public CrisisRecordsDto findTopTwentyRecords(@RequestParam int playerCount, @RequestParam int difficultyLevel) {
        List<CrisisRecordEntity> crisisRecordEntityByTurns = crisisRecordEntityRepository.findTopTwentyRecordsByTurns(playerCount, difficultyLevel);
        List<CrisisRecordEntity> crisisRecordEntityByPoints = crisisRecordEntityRepository.findTopTwentyRecordsByPoints(playerCount, difficultyLevel);

        return new CrisisRecordsDto(crisisRecordEntityByPoints, crisisRecordEntityByTurns);
    }

    @GetMapping("/solo/records")
    public List<SoloRecordEntity> findTopTwentyRecordsByTurns() {
        return soloRecordEntityRepository.findTopTwentyRecordsByTurns();
    }

    @GetMapping("/recent")
    public List<RecentGameDto> findRecentTwentyGames() {
        return gameRepository.findRecentTwentyGames();
    }

    @GetMapping("/cache/reset")
    public int resetGameCache() {
        return cachingGameRepository.evictGameCache();
    }

    @GetMapping("/game/short/player/{playerUuid}")
    public GameDtoShort getShortGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

        Planet phasePlanet = game.getPlanetAtTheStartOfThePhase();

        return GameDtoShort.builder()
                .temperature(game.getPlanet().getTemperatureValue())
                .infrastructure(game.getPlanet().getInfrastructureValue())
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperatureValue() : null)
                .phaseInfrastructure(phasePlanet != null ? phasePlanet.getInfrastructureValue() : null)
                .oxygen(game.getPlanet().getOxygenValue())
                .phaseOxygen(phasePlanet != null ? phasePlanet.getOxygenValue() : null)
                .oceans(game.getPlanet().getOceans().stream().map(OceanDto::of).collect(Collectors.toList()))
                .phaseOceans(phasePlanet != null ? phasePlanet.getRevealedOceans().size() : null)
                .otherPlayers(buildOtherPlayers(game, playerUuid))
                .build();
    }

    @PostMapping("/projects")
    public List<CardDto> getAllProjectCards(@RequestBody AllProjectsRequest request) {
        List<Card> corporations = cardFactory.getAllCorporations(request.getExpansions());
        List<Card> projects = cardFactory.getAllProjects(request.getExpansions());

        return Stream.of(corporations, projects)
                .flatMap(List::stream)
                .map(CardDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/projects/experimental")
    public List<CardDto> getAllExperimentalProjectCards() {
        List<Card> corporations = List.of();
        List<Card> projects = cardFactory.getExperimentalProjects();

        return Stream.of(corporations, projects)
                .flatMap(List::stream)
                .map(CardDto::from)
                .collect(Collectors.toList());
    }

    private List<AnotherPlayerDto> buildOtherPlayers(MarsGame game, String currentPlayerUuid) {
        return game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(player -> !player.getUuid().equals(currentPlayerUuid))
                .map(player -> buildAnotherPlayer(player, game))
                .collect(Collectors.toList());
    }

    private AnotherPlayerDto buildAnotherPlayer(Player player, MarsGame game) {
        return AnotherPlayerDto.builder()
                .playerUuid(player.getUuid())
                .name(player.getName())
                .phase(player.getChosenPhase())
                .winPoints(winPointsService.countWinPoints(player, game))
                .mc(player.getMc())
                .mcIncome(player.getMcIncome())
                .cardIncome(player.getCardIncome())
                .heat(player.getHeat())
                .heatIncome(player.getHeatIncome())
                .plants(player.getPlants())
                .plantsIncome(player.getPlantsIncome())
                .steelIncome(player.getSteelIncome())
                .titaniumIncome(player.getTitaniumIncome())
                .terraformingRating(player.getTerraformingRating())
                .forests(player.getForests())
                .played(player.getPlayed().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .cardResources(getPlayerCardResources(player))
                .cardToTag(getPlayerCardToTag(player))
                .phaseCards(player.getPhaseCards())
                .austellarMilestone(player.getAustellarMilestone())
                .build();
    }

    private PlayerDto buildCurrentPlayer(Player player, MarsGame game) {
        Deck corporations = player.getCorporations();

        return PlayerDto.builder()
                .playerUuid(player.getUuid())
                .name(player.getName())
                .corporations(corporations.getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .hand(player.getHand().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .played(player.getPlayed().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .corporationId(player.getSelectedCorporationCard())
                .phase(player.getChosenPhase())
                .previousPhase(player.getPreviousChosenPhase())
                .mc(player.getMc())
                .mcIncome(player.getMcIncome())
                .cardIncome(player.getCardIncome())
                .heat(player.getHeat())
                .heatIncome(player.getHeatIncome())
                .plants(player.getPlants())
                .plantsIncome(player.getPlantsIncome())
                .steelIncome(player.getSteelIncome())
                .titaniumIncome(player.getTitaniumIncome())
                .nextTurn(buildTurnDto(player.getNextTurn()))
                .cardResources(getPlayerCardResources(player))
                .cardToTag(getPlayerCardToTag(player))
                .activatedBlueCards(player.getActivatedBlueCards().getCards())
                .activatedBlueCardsTwice(player.getActivatedBlueCardsTwice().getCards())
                .blueActionExtraActivationsLeft(player.getBlueActionExtraActivationsLeft())
                .terraformingRating(player.getTerraformingRating())
                .winPoints(winPointsService.countWinPoints(player, game))
                .forests(player.getForests())
                .builtSpecialDesignLastTurn(player.isBuiltSpecialDesignLastTurn())
                .phaseCards(player.getPhaseCards())
                .builds(player.getBuilds())
                .austellarMilestone(player.getAustellarMilestone())
                .build();
    }

    private Map<Integer, List<Tag>> getPlayerCardToTag(Player player) {
        return player.getPlayed().getCards().stream().map(cardService::getCard)
                .filter(card -> card.getTags().contains(Tag.DYNAMIC))
                .collect(Collectors.toMap(
                        Card::getId,
                        card -> player.getCardToTag().get(card.getClass())
                ));
    }

    private Map<Integer, Integer> getPlayerCardResources(Player player) {
        return player.getPlayed().getCards().stream().map(cardService::getCard)
                .filter(card -> card.getCollectableResource() != CardCollectableResource.NONE)
                .collect(Collectors.toMap(
                        Card::getId,
                        card -> player.getCardResourcesCount().get(card.getClass())
                ));
    }

    private TurnDto buildTurnDto(Turn turn) {
        if (turn != null) {
            if (turn.getType() == TurnType.DISCARD_CARDS) {
                DiscardCardsTurn discardCardsTurnDto = (DiscardCardsTurn) turn;
                return DiscardCardsTurnDto.builder()
                        .size(discardCardsTurnDto.getSize())
                        .onlyFromSelectedCards(discardCardsTurnDto.isOnlyFromSelectedCards())
                        .cards(
                                discardCardsTurnDto.getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList())
                        ).build();
            } else if (turn.getType() == TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE) {
                CrysisCardImmediateChoiceTurn turnWithChoice = (CrysisCardImmediateChoiceTurn) turn;
                return CardWithChoiceTurnDto.builder()
                        .playerUuid(turnWithChoice.getPlayerUuid())
                        .card(turnWithChoice.getCard())
                        .build();
            } else if (turn.getType() == TurnType.RESOLVE_PERSISTENT_WITH_CHOICE) {
                CrysisCardPersistentChoiceTurn turnWithChoice = (CrysisCardPersistentChoiceTurn) turn;
                return CardWithChoiceTurnDto.builder()
                        .playerUuid(turnWithChoice.getPlayerUuid())
                        .card(turnWithChoice.getCard())
                        .build();
            } else if (turn.getType() == TurnType.RESOLVE_IMMEDIATE_ALL) {
                CrysisImmediateAllTurn crysisImmediateAllTurn = (CrysisImmediateAllTurn) turn;
                return CardWithChoiceTurnDto.builder()
                        .playerUuid(crysisImmediateAllTurn.getPlayerUuid())
                        .build();
            }

        }
        return null;

    }

}
