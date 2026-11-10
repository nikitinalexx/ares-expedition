package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.BlueActionDecisions;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.agent.CardFeatures;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PERFORM_BLUE_ACTION: offered cards activate with SMART inputs; stop is always present; random agent finishes games.
 */
class BlueActionDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BlueActionDecisions blueActionDecisions;
    @Autowired
    BlueInputDecisions blueInputDecisions;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void everyOfferedActionActivatesWithSmartInputAndStopIsLast() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setMc(40);
        player.setHeat(10);
        player.setPlants(10);
        // Seed a few free blue actions; init resource maps the engine expects after a real build.
        for (int id = 1; id < 500 && player.getPlayed().size() < 8; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.isActiveCard() && card.getColor() != null
                    && card.getColor().name().equals("BLUE")) {
                player.getPlayed().addCard(id);
                if (card.getCollectableResource() != null) {
                    player.getCardResourcesCount().putIfAbsent(card.getClass(), 0);
                }
            }
        }

        Decision decision = blueActionDecisions.build(game, player);
        assertEquals(BlueActionDecisions.STOP, decision.candidates().getLast().id());
        Set<Integer> offered = decision.candidates().stream()
                .map(c -> c.id())
                .filter(id -> id != BlueActionDecisions.STOP)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<Integer> expected = blueActionDecisions.available(game, player).stream()
                .map(Card::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expected, offered);

        for (int id : offered) {
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            Card card = cardService.getCard(id);
            Map<Integer, List<Integer>> input = blueInputDecisions.fill(copy, copyPlayer, card, d -> 0);
            assertEquals(null, cardValidationService.validateBlueAction(copyPlayer, copy, id, input));
            aiTurnService.performBlueAction(copy, copyPlayer, id, input);
            assertTrue(copyPlayer.getActivatedBlueCards().containsCard(id)
                    || copyPlayer.getActivatedBlueCardsTwice().containsCard(id));
        }
    }

    @Test
    void stopCandidateHasStopFeatures() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        Decision decision = blueActionDecisions.build(game, player);
        float[] stop = decision.candidates().getLast().features();
        assertEquals(1f, stop[CardFeatures.LENGTH - 1], 1e-6);
    }

    @Test
    void aRandomAgentFinishesGamesAndRecordsBlueActionRows() throws Exception {
        decisionLogs.startRecording(dir);
        try {
            for (int g = 0; g < 6; g++) {
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
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("PERFORM_BLUE_ACTION.")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.PERFORM_BLUE_ACTION, content.schema().type());
                assertEquals(BlueActionDecisions.CANDIDATE_FEATURES, content.schema().candidateFeatures());
                for (DecisionFile.Row row : content.rows()) {
                    rows++;
                    assertTrue(row.chosen() < row.candidateCount());
                    boolean stop = false;
                    for (int i = 0; i < row.candidateCount(); i++) {
                        int id = row.candidateIds()[i];
                        if (id == BlueActionDecisions.STOP) {
                            stop = true;
                        }
                    }
                    assertTrue(stop);
                }
            }
        }
        assertTrue(rows > 0, "phase 3 records blue-action decisions (including stop)");
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
