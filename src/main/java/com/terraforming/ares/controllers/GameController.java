package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.*;
import com.terraforming.ares.entity.CrisisRecordEntity;
import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.mars.MarsGameDataset;
import com.terraforming.ares.mars.MarsGameRow;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.request.AllProjectsRequest;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.repositories.GameRepositoryImpl;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.AiBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    @PostMapping("/game/new")
    public PlayerUuidsDto startNewGame(@RequestBody GameParameters gameParameters) {
        try {
            int aiPlayerCount = (int) gameParameters.getComputers().stream().filter(item -> item).count();
            int playersCount = gameParameters.getPlayerNames().size();

            if (gameParameters.getPlayerNames().stream().anyMatch(name -> name.length() > 10)) {
                throw new IllegalArgumentException("Only names of length 10 or below are supported");
            }

            if (playersCount == 0 || playersCount > Constants.MAX_PLAYERS) {
                throw new IllegalArgumentException("Only 1 to 4 players are supported so far");
            }

            if (gameParameters.getExpansions().contains(Expansion.CRYSIS)) {
                if (playersCount != 1) {
                    throw new IllegalArgumentException("Only 1 player supported so far. Come back in a week!");
                }
                if (gameParameters.getComputers().stream().anyMatch(Boolean.TRUE::equals)) {
                    throw new IllegalArgumentException("Crysis is a cooperative mode, no computers supported");
                }
                if (!gameParameters.isMulligan()) {
                    throw new IllegalArgumentException("Crysis mode can be only played with mulligan option");
                }
                if (!gameParameters.getExpansions().contains(Expansion.DISCOVERY)) {
                    throw new IllegalArgumentException("Crysis mode can be only played with Discovery expansion");
                }
                if (gameParameters.isDummyHand()) {
                    throw new IllegalArgumentException("Crysis mode has a dummy hand option by default");
                }
            }

            final List<Expansion> expansions = gameParameters.getExpansions();
            if (expansions.contains(Expansion.DISCOVERY) && aiPlayerCount > 0) {
                throw new IllegalArgumentException("Computer is not smart enough to play the Discovery expansion lol!");
            }

            MarsGame marsGame = gameService.startNewGame(gameParameters);

            if (aiPlayerCount == playersCount) {
                turnService.pushGame(marsGame.getId());
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
        }
    }

    @GetMapping("/simulations")
    public void runSimulations(@RequestBody SimulationsRequest request) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        if (request.getSimulationsCount() < availableProcessors) {
            return;
        }


        int threads = availableProcessors;

        List<Double> calibrationValues = aiBalanceService.calibrationValues();

        if (request.isWithParamCalibration()) {
            for (Double calibrationValue : calibrationValues) {
                System.out.println();
                System.out.println("Calibration value " + calibrationValue);
                aiBalanceService.setCalibrationValue(calibrationValue);

                GameStatistics gameStatistics = new GameStatistics();

                ExecutorService executor = Executors.newFixedThreadPool(threads);
                for (int i = 0; i < threads; i++) {
                    Runnable worker = new WorkerThread(request.getSimulationsCount() / threads, gameStatistics, request.isWithParamCalibration());
                    executor.execute(worker);
                }
                executor.shutdown();
                while (!executor.isTerminated()) {
                }
                System.out.println("Finished all threads");


                printStatistics(gameStatistics);
            }
        } else {
            System.out.println("Starting simulations");

            GameStatistics gameStatistics = new GameStatistics();

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                Runnable worker = new WorkerThread(request.getSimulationsCount() / threads, gameStatistics, request.isWithParamCalibration());
                executor.execute(worker);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            System.out.println("Finished all threads");


            printStatistics(gameStatistics);
        }

    }

    class WorkerThread implements Runnable {
        int simulationCount;
        GameStatistics gameStatistics;
        boolean withCalibration;

        WorkerThread(int simulationCount, GameStatistics gameStatistics, boolean withCalibration) {
            this.simulationCount = simulationCount;
            this.gameStatistics = gameStatistics;
            this.withCalibration = withCalibration;


        }

        @Override
        public void run() {
            GameParameters gameParameters = GameParameters.builder()
                    .playerNames(List.of("ai1", "ai2"))
                    .computers(List.of(true, true))
                    .mulligan(true)
                    .expansions(List.of(Expansion.BASE, Expansion.BUFFED_CORPORATION))
                    .build();

            List<MarsGame> games = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            List<MarsGameDataset> marsGameDatasets = new ArrayList<>();

            for (int i = 1; i <= simulationCount; i++) {
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

                if (i != 0 && i % 50 == 0) {
                    long spentTime = System.currentTimeMillis() - startTime;

                    long timePerGame = (spentTime / i);
                    if (!withCalibration) {
                        System.out.println("Time left: " + (simulationCount - i) * timePerGame / 1000);
                    }
                }

                if (i % 100 == 0) {
                    gatherStatistics(games, gameStatistics);
                    if (Constants.SAVE_SIMULATION_GAMES_TO_DB) {
                        games.forEach(gameRepository::save);
                    }
                    games.clear();
                }
            }

            gatherStatistics(games, gameStatistics);
            if (Constants.SAVE_SIMULATION_GAMES_TO_DB) {
                games.forEach(gameRepository::save);
            }

            if (Constants.COLLECT_DATASET) {
                try {
                    saveDatasets(marsGameDatasets);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void gatherStatistics(List<MarsGame> games, GameStatistics gameStatistics) {
        gameStatistics.addTotalGames(games.size());
        for (MarsGame game : games) {
            gameStatistics.addTotalTurnsCount(game.getTurns());

            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
            Player firstPlayer = players.get(0);
            Player secondPlayer = players.get(1);

            int firstPlayerPoints = winPointsService.countWinPoints(firstPlayer, game);
            int secondPlayerPoints = winPointsService.countWinPoints(secondPlayer, game);

            gameStatistics.addTotalPointsCount(firstPlayerPoints + secondPlayerPoints);

        }

        List<MarsGame> finishedGames = games.stream()
                .filter(MarsGame::gameEndCondition)
                .filter(game -> game.getPlayerUuidToPlayer().size() == 2)
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
                            if (playedCard < 250) {
                                gameStatistics.cardOccured(
                                        player.getPlayed().getCardToTurn().get(playedCard),
                                        playedCard
                                );
                            }
                        }
                    }
            );


            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
            Player firstPlayer = players.get(0);
            Player secondPlayer = players.get(1);

            int firstPlayerPoints = winPointsService.countWinPoints(firstPlayer, game);
            int secondPlayerPoints = winPointsService.countWinPoints(secondPlayer, game);

            if (firstPlayerPoints != secondPlayerPoints) {
                Player winCardsPlayer = (firstPlayerPoints > secondPlayerPoints ? firstPlayer : secondPlayer);
                Player anotherPlayer = (secondPlayerPoints > firstPlayerPoints ? firstPlayer : secondPlayer);


                if (winCardsPlayer.getUuid().endsWith("0")) {
                    gameStatistics.addFirstWins();
                } else {
                    gameStatistics.addSecondWins();
                }

                for (Integer playedCard : winCardsPlayer.getPlayed().getCards()) {
                    if (playedCard < 250) {
                        gameStatistics.winCardOccured(
                                winCardsPlayer.getPlayed().getCardToTurn().get(playedCard),
                                playedCard
                        );
                    }
                }

                gameStatistics.corporationWon(winCardsPlayer.getSelectedCorporationCard());
                gameStatistics.corporationOccured(winCardsPlayer.getSelectedCorporationCard());
                gameStatistics.corporationOccured(anotherPlayer.getSelectedCorporationCard());

            }
        }
    }

    private synchronized void saveDatasets(List<MarsGameDataset> marsGameDatasets) throws FileNotFoundException {
        File csvOutputFile = new File("dataset.csv");
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            for (MarsGameDataset dataset : marsGameDatasets) {
                List<MarsGameRow> rows = dataset.getFirstPlayerRows();
                for (MarsGameRow row : rows) {
                    write(row, pw);
                }
                rows = dataset.getSecondPlayerRows();
                for (MarsGameRow row : rows) {
                    write(row, pw);
                }
            }
        }
    }

    private void write(MarsGameRow row, PrintWriter pw) {
        pw.print(row.getTurn());
        pw.print(',');
        pw.print(row.getWinPoints());
        pw.print(',');
        pw.print(row.getMcIncome());
        pw.print(',');
        pw.print(row.getMc());
        pw.print(',');
        pw.print(row.getSteelIncome());
        pw.print(',');
        pw.print(row.getTitaniumIncome());
        pw.print(',');
        pw.print(row.getPlantsIncome());
        pw.print(',');
        pw.print(row.getPlants());
        pw.print(',');
        pw.print(row.getHeatIncome());
        pw.print(',');
        pw.print(row.getHeat());
        pw.print(',');
        pw.print(row.getCardsIncome());
        pw.print(',');
        pw.print(row.getCardsInHand());
        pw.print(',');
        pw.print(row.getCardsBuilt());
        pw.print(',');
        pw.print(row.getOxygenLevel());
        pw.print(',');
        pw.print(row.getTemperatureLevel());
        pw.print(',');
        pw.print(row.getOceansLevel());
        pw.print(',');
        pw.print(row.getOpponentWinPoints());
        pw.print(',');
        pw.print(row.getOpponentMcIncome());
        pw.print(',');
        pw.print(row.getOpponentMc());
        pw.print(',');
        pw.print(row.getOpponentSteelIncome());
        pw.print(',');
        pw.print(row.getOpponentTitaniumIncome());
        pw.print(',');
        pw.print(row.getOpponentPlantsIncome());
        pw.print(',');
        pw.print(row.getOpponentPlants());
        pw.print(',');
        pw.print(row.getOpponentHeatIncome());
        pw.print(',');
        pw.print(row.getOpponentHeat());
        pw.print(',');
        pw.print(row.getOpponentCardsIncome());
        pw.print(',');
        pw.print(row.getOpponentCardsBuilt());
        pw.print(',');
        pw.print(row.getWinner());
        pw.println();
    }


    private void printStatistics(GameStatistics gameStatistics) {
        /*
        if (Constants.STATISTICS_BY_TURN) {
            for (int i = 1; i <= winCardOccurenceByTurn.size(); i++) {
                System.out.println("Turn " + i);

                final List<Integer> winCardOccurence = winCardOccurenceByTurn.getOrDefault(i, new ArrayList<>());
                final List<Integer> cardOccurence = occurenceByTurn.getOrDefault(i, new ArrayList<>());

                if (winCardOccurence.isEmpty()) {
                    for (int j = 0; j < 220; j++) {
                        winCardOccurence.add(0);
                    }
                }

                if (cardOccurence.isEmpty()) {
                    for (int j = 0; j < 220; j++) {
                        cardOccurence.add(0);
                    }
                }

                for (int j = 1; j < cardOccurence.size(); j++) {
                    System.out.println("j: " + j + " " + " % " + (double) winCardOccurence.get(j) * 100 / cardOccurence.get(j) + " " + cardService.getCard(j).getClass().getSimpleName());
                }

                System.out.println();
            }
        }*/

        if (WRITE_STATISTICS_TO_FILE) {
            try (FileWriter fw = new FileWriter("cardStats.txt");
                 BufferedWriter writer = new BufferedWriter(fw)) {

                final Map<Integer, List<Integer>> winCardOccurenceByTurn = gameStatistics.getTurnToWinCardsOccurence();
                final Map<Integer, List<Integer>> occurenceByTurn = gameStatistics.getTurnToCardsOccurence();

                for (int cardId = 1; cardId <= 219; cardId++) {
                    if (Constants.WRITE_STATISTICS_TO_CONSOLE) {
                        System.out.println("Id: " + cardId + ". " + cardService.getCard(cardId).getClass().getSimpleName());
                    }
                    writer.write("#" + cardId + " " + cardService.getCard(cardId).getClass().getSimpleName() + "\n");

                    for (int turn = 1; turn <= winCardOccurenceByTurn.size() && turn <= 50; turn++) {

                        final List<Integer> winCardOccurence = winCardOccurenceByTurn.getOrDefault(turn, new ArrayList<>());
                        final List<Integer> cardOccurence = occurenceByTurn.getOrDefault(turn, new ArrayList<>());

                        if (winCardOccurence.isEmpty()) {
                            for (int k = 0; k < 220; k++) {
                                winCardOccurence.add(0);
                            }
                        }

                        if (cardOccurence.isEmpty()) {
                            for (int k = 0; k < 220; k++) {
                                cardOccurence.add(0);
                            }
                        }

                        if (Constants.WRITE_STATISTICS_TO_CONSOLE) {
                            System.out.println("Turn " + turn + ". " + (double) winCardOccurence.get(cardId) * 100 / cardOccurence.get(cardId));
                        }
                        writer.write("." + turn + " " + (double) winCardOccurence.get(cardId) * 100 / cardOccurence.get(cardId) + "\n");
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

        System.out.println("Old: " + gameStatistics.getFirstWins() + "; New: " + gameStatistics.getSecondWins() +
                "; Ratio: " + ((double) gameStatistics.getFirstWins() / (gameStatistics.getFirstWins() + gameStatistics.getSecondWins())));
    }

    @GetMapping("/game/player/{playerUuid}")
    public GameDto getGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

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

    @GetMapping("/crisis/records/points")
    public List<CrisisRecordEntity> findTopTwentyRecordsByPoints(@RequestParam int playerCount) {
        return crisisRecordEntityRepository.findTopTwentyRecordsByPoints(playerCount);
    }

    @GetMapping("/crisis/records/turns")
    public List<CrisisRecordEntity> findTopTwentyRecordsByTurns(@RequestParam int playerCount) {
        return crisisRecordEntityRepository.findTopTwentyRecordsByTurns(playerCount);
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
