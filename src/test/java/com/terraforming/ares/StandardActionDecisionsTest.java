package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.StandardActionChoice;
import com.terraforming.ares.services.agent.StandardActionDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase-3 spends: every generated candidate is accepted by the engine, Helion exchange goes through the real turn,
 * and a random agent finishes games.
 */
class StandardActionDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    StandardActionDecisions standardActionDecisions;
    @Autowired
    StandardProjectService standardProjectService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void everyGeneratedSpendIsAcceptedAndMatchesTheValidator() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setMc(40);
        player.setHeat(20);
        player.setPlants(20);

        List<TurnType> possible = gameService.getPossibleTurns(game, player.getUuid());
        assertTrue(possible.contains(TurnType.STANDARD_PROJECT));
        assertTrue(possible.contains(TurnType.PLANT_FOREST));
        assertTrue(possible.contains(TurnType.INCREASE_TEMPERATURE));

        List<StandardActionChoice> choices = standardActionDecisions.options(game, player, possible);
        Set<Integer> kinds = choices.stream().map(StandardActionChoice::kind).collect(Collectors.toCollection(TreeSet::new));
        assertTrue(kinds.contains(StandardActionDecisions.STOP));
        assertTrue(kinds.contains(StandardActionDecisions.PLANT_FOREST));
        assertTrue(kinds.contains(StandardActionDecisions.INCREASE_TEMPERATURE));
        assertTrue(kinds.contains(StandardActionDecisions.SP_OCEAN)
                || kinds.contains(StandardActionDecisions.SP_FOREST)
                || kinds.contains(StandardActionDecisions.SP_TEMPERATURE));
        assertFalse(kinds.contains(StandardActionDecisions.EXCHANGE_ONE_HEAT), "player is not Helion");

        for (StandardActionChoice choice : choices) {
            if (choice.kind() == StandardActionDecisions.STOP) {
                continue;
            }
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            play(copy, copyPlayer, choice);
        }

        player.setMc(0);
        List<StandardActionChoice> broke = standardActionDecisions.options(game, player,
                gameService.getPossibleTurns(game, player.getUuid()));
        assertTrue(broke.stream().noneMatch(c -> c.projectType() != null));
        assertTrue(broke.stream().anyMatch(c -> c.kind() == StandardActionDecisions.STOP));
    }

    @Test
    void helionMayExchangeOneHeatThroughTheEngineTurn() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setSelectedCorporationCard(10000);
        player.getPlayed().addCard(10000);
        player.setHeat(5);
        player.setMc(0);

        List<TurnType> possible = gameService.getPossibleTurns(game, player.getUuid());
        List<StandardActionChoice> choices = standardActionDecisions.options(game, player, possible);
        assertTrue(choices.stream().anyMatch(c -> c.kind() == StandardActionDecisions.EXCHANGE_ONE_HEAT));

        StandardActionChoice exchange = choices.stream()
                .filter(c -> c.kind() == StandardActionDecisions.EXCHANGE_ONE_HEAT).findFirst().orElseThrow();
        aiTurnService.exchangeHeat(game, player, 1);
        assertEquals(4, player.getHeat());
        assertEquals(1, player.getMc());

        Player opponent = named(game, "B");
        opponent.setHeat(10);
        assertThrows(IllegalStateException.class, () -> aiTurnService.exchangeHeat(game, opponent, 1));
        assertEquals(exchange.kind(), StandardActionDecisions.EXCHANGE_ONE_HEAT);
    }

    @Test
    void featuresMarkCostsAndStop() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setMc(40);
        player.setHeat(10);
        player.setPlants(10);
        List<StandardActionChoice> choices = standardActionDecisions.options(game, player,
                gameService.getPossibleTurns(game, player.getUuid()));
        var decision = standardActionDecisions.decision(game, player, choices);
        assertEquals(StandardActionDecisions.CANDIDATE_FEATURES, decision.candidates().getFirst().features().length);
        float[] stop = decision.candidates().getLast().features();
        assertEquals(1f, stop[StandardActionDecisions.STOP]);
        assertEquals(1f, stop[StandardActionDecisions.CANDIDATE_FEATURES - 1]);
        StandardActionChoice forest = choices.stream()
                .filter(c -> c.kind() == StandardActionDecisions.PLANT_FOREST).findFirst().orElseThrow();
        float[] plants = standardActionDecisions.features(player, forest);
        assertEquals(Constants.FOREST_PLANT_COST / 10f, plants[StandardActionDecisions.KINDS + 2]);
    }

    @Test
    void hiddenInformationDoesNotChangeTheObservation() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        Player opponent = named(game, "B");
        player.setMc(20);
        float[] seen = standardActionDecisions.decision(game, player,
                standardActionDecisions.options(game, player, gameService.getPossibleTurns(game, player.getUuid())))
                .observation();
        java.util.Collections.shuffle(opponent.getHand().getCards());
        java.util.Collections.shuffle(game.getProjectsDeck().getCards());
        assertTrue(Arrays.equals(seen, standardActionDecisions.decision(game, player,
                standardActionDecisions.options(game, player, gameService.getPossibleTurns(game, player.getUuid())))
                .observation()));
    }

    @Test
    void aRandomAgentFinishesGamesAndRecordsStandardActions() throws Exception {
        decisionLogs.startRecording(dir);
        try {
            for (int g = 0; g < 4; g++) {
                MarsGame game = gameService.createNewSimulation(parameters());
                simulation.processSimulation(game);
                assertEquals(StateType.GAME_END, game.getStateType());
                decisionLogs.finish(game);
            }
        } finally {
            decisionLogs.stopRecording();
        }

        int rows = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("STANDARD_ACTION.")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.STANDARD_ACTION, content.schema().type());
                assertEquals(StandardActionDecisions.CANDIDATE_FEATURES, content.schema().candidateFeatures());
                for (DecisionFile.Row row : content.rows()) {
                    rows++;
                    assertTrue(row.chosen() < row.candidateCount());
                    boolean stop = false;
                    boolean onlyProjects = true;
                    for (int i = 0; i < row.candidateCount(); i++) {
                        int id = row.candidateIds()[i];
                        assertTrue(id >= 0 && id < StandardActionDecisions.KINDS, "kind " + id);
                        if (id == StandardActionDecisions.STOP) {
                            stop = true;
                            onlyProjects = false;
                        } else if (id != StandardActionDecisions.SP_OCEAN
                                && id != StandardActionDecisions.SP_FOREST
                                && id != StandardActionDecisions.SP_TEMPERATURE
                                && id != StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS
                                && id != StandardActionDecisions.SP_INFRASTRUCTURE_HEAT) {
                            onlyProjects = false;
                        }
                    }
                    // Normal phase-3 menu always has stop; endgame records projects only (confirm forbids leaving them).
                    assertTrue(stop || onlyProjects);
                }
            }
        }
        assertTrue(rows > 0, "phase 3 always offers the spend menu");
    }

    private void play(MarsGame game, Player player, StandardActionChoice choice) {
        switch (choice.kind()) {
            case StandardActionDecisions.EXCHANGE_ONE_HEAT -> aiTurnService.exchangeHeat(game, player, 1);
            case StandardActionDecisions.PLANT_FOREST -> aiTurnService.plantForest(game, player);
            case StandardActionDecisions.INCREASE_TEMPERATURE -> aiTurnService.increaseTemperature(game, player);
            case StandardActionDecisions.INCREASE_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.INCREASE_INFRASTRUCTURE_HEAT ->
                    aiTurnService.increaseInfrastructure(player, game, choice.input());
            case StandardActionDecisions.SP_OCEAN, StandardActionDecisions.SP_FOREST,
                 StandardActionDecisions.SP_TEMPERATURE, StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.SP_INFRASTRUCTURE_HEAT -> {
                assertEquals(null, standardProjectService.validateStandardProject(game, player, choice.projectType()));
                aiTurnService.standardProjectTurn(game, player, choice.projectType(), choice.input());
            }
            default -> throw new IllegalStateException("unexpected " + choice.kind());
        }
    }

    private MarsGame phaseThree() {
        MarsGame game = gameService.createNewSimulation(parameters());
        game.setStateType(StateType.PERFORM_BLUE_ACTION, marsContextProvider.provide(game));
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            while (player.getNextTurn() != null) {
                player.removeNextTurn();
            }
            player.getBuilds().clear();
        }
        return game;
    }

    private static GameParameters parameters() {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
    }

    private static Player named(MarsGame game, String name) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }
}
