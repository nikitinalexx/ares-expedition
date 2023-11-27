package com.terraforming.ares.controllers;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameDataset;
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
import com.terraforming.ares.services.ai.*;
import com.terraforming.ares.services.ai.turnProcessors.AiMulliganCardsTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terraforming.ares.model.Constants.WRITE_STATISTICS_TO_FILE;

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
    private final AiBalanceService aiBalanceService;
    private final CrisisRecordEntityRepository crisisRecordEntityRepository;
    private final SoloRecordEntityRepository soloRecordEntityRepository;
    private final DeepNetwork deepNetwork;
    private final DatasetCollectionService datasetCollectionService;
    private final GameFactory gameFactory;
    private final AiMulliganCardsTurn aiMulliganCardsTurn;
    private final MarsContextProvider marsContextProvider;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final TestAiService testAiService;

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

            if (gameParameters.getComputers().stream().anyMatch(computer -> computer != PlayerDifficulty.RANDOM && computer != PlayerDifficulty.SMART && computer != PlayerDifficulty.NONE && computer != PlayerDifficulty.NETWORK)) {
                throw new IllegalArgumentException("Only Random/Smart/Network computers are supported");
            }

            if (!gameParameters.getExpansions().contains(Expansion.DISCOVERY)) {
                throw new IllegalArgumentException("Discovery expansion is a default mode for this game");
            }

            int aiPlayerCount = (int) gameParameters.getComputers().stream().filter(item -> item != PlayerDifficulty.NONE).count();
            int playersCount = gameParameters.getPlayerNames().size();
            int[] extraPoints = gameParameters.getExtraPoints();

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

    @GetMapping("/simulations")
    public void runSimulations(@RequestBody SimulationsRequest request) throws IOException {
        TURNS_TO_GAMES_COUNT.clear();
        TURNS_TO_POINTS_COUNT.clear();
        if (request.isWithBatches() && request.getBatches() == 0) {
            throw new IllegalArgumentException("Batches can't be 0");
        }
        if (!request.isWithBatches()) {
            request.setBatches(1);
        }

        for (int batch = 0; batch < request.getBatches(); batch++) {
            request.setFileIndex(batch);

            Constants.FIRST_PLAYER_PHASES = new ConcurrentHashMap<>();
            Constants.SECOND_PLAYER_PHASES = new ConcurrentHashMap<>();

            int threads = Runtime.getRuntime().availableProcessors();

            if (request.getSimulationsCount() < threads) {
                return;
            }

            System.out.println("Starting simulations");

            GameStatistics gameStatistics = new GameStatistics();

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                Runnable worker = new WorkerThread(request.getSimulationsCount() / threads, gameStatistics, request.getFileIndex(), i);
                executor.execute(worker);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            System.out.println("Finished all threads");


            printStatistics(gameStatistics);

            if (Constants.COLLECT_DATASET) {
                List<String> fileNames = new ArrayList<>();

                for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
                    fileNames.add("dataset_" + Constants.SIMULATION_PLAYERS.stream().map(PlayerDifficulty::toString).collect(Collectors.joining("_")) + "_" + request.getFileIndex() + "_" + threadIndex + ".csv");
                }

                combineAndDelete(fileNames, "dataset_" + Constants.SIMULATION_PLAYERS.stream().map(PlayerDifficulty::toString).collect(Collectors.joining("_")) + "_" + request.getFileIndex() + ".csv");
            }


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
    }

    public static void combineAndDelete(List<String> filePaths, String combinedFilePath) throws IOException {
        // Create the combined CSV file
        FileWriter writer = new FileWriter(combinedFilePath);
        for (String filePath : filePaths) {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            while (line != null) {
                writer.write(line + "\n");
                line = reader.readLine();
            }
            reader.close();

            // Delete the old file
            File fileToDelete = new File(filePath);
            if (!fileToDelete.delete()) {
                System.err.println("Failed to delete file: " + filePath);
            }
        }
        writer.close();
    }

    @GetMapping("/mulligan/calibrate")
    public void calibrateMulligan() {
        Random random = new Random();
        GameParameters.GameParametersBuilder builder = GameParameters.builder();
        if (Constants.DISCOVERY_SIMULATIONS) {
            builder.expansion(Expansion.DISCOVERY);
        }

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
                    totalExtra += aiPickCardProjectionService.cardExtraChanceIfBuilt(gameCopy, gameCopy.getPlayerByUuid(player.getUuid()), cards.get(i), initialChance, 0);
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

    class WorkerThread implements Runnable {
        int simulationCount;
        GameStatistics gameStatistics;
        int fileIndex;
        int threadIndex;

        WorkerThread(int simulationCount, GameStatistics gameStatistics, int fileIndex, int threadIndex) {
            this.simulationCount = simulationCount;
            this.gameStatistics = gameStatistics;
            this.fileIndex = fileIndex;
            this.threadIndex = threadIndex;
        }

        @Override
        public void run() {
            GameParameters.GameParametersBuilder builder = GameParameters.builder();
            if (Constants.DISCOVERY_SIMULATIONS) {
                builder.expansion(Expansion.DISCOVERY);
            }

            List<String> playerNames = new ArrayList<>();
            int counter = 1;
            for (PlayerDifficulty simulationPlayer : Constants.SIMULATION_PLAYERS) {
                playerNames.add(simulationPlayer.name() + (counter++));
            }

            GameParameters gameParameters = builder
                    .playerNames(playerNames)
                    .computers(Constants.SIMULATION_PLAYERS)
                    .mulligan(true)
                    .expansion(Expansion.BASE)
                    .expansion(Expansion.BUFFED_CORPORATION)
                    .dummyHand(true)
                    .build();

            List<MarsGame> games = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            List<MarsGameDataset> marsGameDatasets = new ArrayList<>();

            for (int i = 1; i <= simulationCount; i++) {
                if (BREAK_SIMULATIONS_EARLY) {
                    break;
                }
                MarsGame marsGame = gameService.createNewSimulation(gameParameters);
                if (Constants.COLLECT_DATASET) {
                    MarsGameDataset dataSet = simulationProcessorService.runSimulationWithDataset(marsGame);
                    if (dataSet != null) {
                        marsGameDatasets.add(dataSet);
                    }
                } else {
                    simulationProcessorService.processSimulation(marsGame);
                }

                games.add(marsGame);

                if (i != 0 && i % 10 == 0) {
                    long spentTime = System.currentTimeMillis() - startTime;

                    long timePerGame = (spentTime / i);
                    System.out.println("Time left: " + (simulationCount - i) * timePerGame / 1000);
                }

                if (i % 100 == 0) {
                    gatherStatistics(games, gameStatistics);
                    if (Constants.SAVE_SIMULATION_GAMES_TO_DB) {
                        games.forEach(gameRepository::save);
                    }
                    saveExceptionalGames(games);
                    games.clear();
                }
            }

            gatherStatistics(games, gameStatistics);
            if (Constants.SAVE_SIMULATION_GAMES_TO_DB) {
                games.forEach(gameRepository::save);
            }
            saveExceptionalGames(games);

            if (Constants.COLLECT_DATASET) {
                try {
                    saveDatasets(marsGameDatasets, fileIndex, threadIndex);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveExceptionalGames(List<MarsGame> games) {
//        games.stream().filter(game -> game.getTurns() < 34 && game.getPlayerUuidToPlayer().values().stream().anyMatch(
//                player -> {
//
//                    int winPoints = winPointsService.countWinPoints(player, game);
//
//                    long activeCardsCount = player.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::isActiveCard).count();
//                    long greenCardsCount = player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN).count();
//
//                    if (activeCardsCount > 5 && winPoints > 80 && greenCardsCount < 13) {
//                        System.out.println("Active cards " + player.getUuid());
//                    }
//
//
//                    return (activeCardsCount > 5 && winPoints > 80 && greenCardsCount < 13);
//                }
//        )).forEach(gameRepository::save);

        games.forEach(game -> {
            TURNS_TO_GAMES_COUNT.compute(game.getTurns(), (turns, count) -> {
                if (count == null) {
                    count = 0;
                }
                count++;
                return count;
            });
            TURNS_TO_POINTS_COUNT.compute(game.getTurns(), (turns, pointsCount) -> {
                if (pointsCount == null) {
                    pointsCount = 0L;
                }
                Integer bestScore = game.getPlayerUuidToPlayer().values().stream().map(
                        p -> winPointsService.countWinPoints(p, game)
                ).max(Comparator.naturalOrder()).orElseThrow();
                pointsCount += bestScore;

                if (TURN_TO_AVERAGE_WINNER.containsKey(game.getTurns()) && bestScore > TURN_TO_AVERAGE_WINNER.get(game.getTurns()) * 1.1) {
                    gameRepository.save(game);
                }

                return pointsCount;
            });
        });
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

            List<Integer> winPoints = players.stream().map(player -> winPointsService.countWinPoints(player, game)).collect(Collectors.toList());

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

    private synchronized void saveDatasets(List<MarsGameDataset> marsGameDatasets, int index, int threadIndex) throws FileNotFoundException {
        File csvOutputFile = new File("dataset_" + Constants.SIMULATION_PLAYERS.stream().map(PlayerDifficulty::toString).collect(Collectors.joining("_")) + "_" + index + "_" + threadIndex + ".csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            for (MarsGameDataset dataset : marsGameDatasets) {
                writeMarsGameRows(dataset.getFirstPlayerRows(), pw);
                writeMarsGameRows(dataset.getSecondPlayerRows(), pw);
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
                    if (Constants.WRITE_STATISTICS_TO_CONSOLE) {
                        System.out.println("Id: " + cardId + ". " + cardService.getCard(cardId).getClass().getSimpleName());
                    }
                    writer.write("#" + cardId + " " + cardService.getCard(cardId).getClass().getSimpleName() + "\n");

                    for (int turn = 1; turn <= winCardOccurenceByTurn.size() && turn <= 50; turn++) {
                        Map<Integer, Integer> winCardOccurence = winCardOccurenceByTurn.get(turn);
                        Map<Integer, Integer> cardOccurence = occurenceByTurn.get(turn);

                        if (Constants.WRITE_STATISTICS_TO_CONSOLE) {
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

        long aiComputerCount = game.getPlayerUuidToPlayer().values().stream().filter(player -> player.getDifficulty() != PlayerDifficulty.NONE && player.getDifficulty() != PlayerDifficulty.RANDOM && player.getDifficulty() != PlayerDifficulty.SMART).count();
        Player aiComputer;

        if (aiComputerCount == game.getPlayerUuidToPlayer().size()) {
            aiComputer = game.getPlayerByUuid(playerUuid);
        } else {
            aiComputer =  game.getPlayerUuidToPlayer().values().stream().filter(player -> player.getDifficulty() != PlayerDifficulty.NONE && player.getDifficulty() != PlayerDifficulty.RANDOM && player.getDifficulty() != PlayerDifficulty.SMART).findFirst().orElse(null);
        }

        float winProbability;

        if (aiComputer == null) {
            aiComputerCount = 0;
            winProbability = 0;
        } else {
            winProbability = deepNetwork.testState(game, aiComputer);
        }

        Planet phasePlanet = game.getPlanetAtTheStartOfThePhase();

        final CrysisData crysisData = game.getCrysisData();
        return GameDto.builder()
                .phase(game.getCurrentPhase())
                .player(buildCurrentPlayer(game.getPlayerByUuid(playerUuid), game))
                .temperature(game.getPlanet().getTemperatureValue())
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperatureValue() : null)
                .phaseTemperatureColor(phasePlanet != null ? phasePlanet.getTemperatureColor() : null)
                .oxygen(game.getPlanet().getOxygenValue())
                .phaseOxygen(phasePlanet != null ? phasePlanet.getOxygenValue() : null)
                .phaseOxygenColor(phasePlanet != null ? phasePlanet.getOxygenColor() : null)
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
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperatureValue() : null)
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
