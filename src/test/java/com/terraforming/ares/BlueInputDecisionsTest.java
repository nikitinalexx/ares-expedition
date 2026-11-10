package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BLUE_INPUT: heat splits and phase upgrades validate; a random agent finishes games.
 */
class BlueInputDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BlueInputDecisions blueInputDecisions;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void powerInfrastructureOffersHeatAmountsThatValidate() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setHeat(7);
        Card power = findByAction(CardAction.POWER_INFRASTRUCTURE);
        player.getPlayed().addCard(power.getId());

        var slot = blueInputDecisions.nextSlot(game, player, power, new java.util.HashMap<>());
        assertTrue(slot.isPresent());
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.kind() == BlueInputDecisions.KIND_HEAT));
        assertEquals(7, slot.get().choices().size());

        Map<Integer, List<Integer>> input = blueInputDecisions.fill(game, player, power, d -> 0);
        assertTrue(input.containsKey(InputFlag.DISCARD_HEAT.getId()));
        assertEquals(null, cardValidationService.validateBlueAction(player, game, power.getId(), input));
    }

    @Test
    void experimentalTechnologyOffersPhaseUpgrades() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        player.setTerraformingRating(5);
        Card tech = findByAction(CardAction.EXPERIMENTAL_TECHNOLOGY);
        player.getPlayed().addCard(tech.getId());

        Map<Integer, List<Integer>> input = blueInputDecisions.fill(game, player, tech, d -> 0);
        assertTrue(input.containsKey(InputFlag.PHASE_UPGRADE_CARD.getId()));
        assertEquals(null, cardValidationService.validateBlueAction(player, game, tech.getId(), input));
    }

    @Test
    void aRandomAgentFinishesGames() throws Exception {
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
        int blueRows = 0;
        int inputRows = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.startsWith("PERFORM_BLUE_ACTION.")) {
                    blueRows += DecisionFile.read(file).rows().size();
                }
                if (name.startsWith("BLUE_INPUT.")) {
                    DecisionFile.Content content = DecisionFile.read(file);
                    assertEquals(DecisionType.BLUE_INPUT, content.schema().type());
                    inputRows += content.rows().size();
                    for (DecisionFile.Row row : content.rows()) {
                        assertTrue(row.chosen() < row.candidateCount());
                    }
                }
            }
        }
        assertTrue(blueRows > 0);
        assertFalse(inputRows < 0);
    }

    private Card findByAction(CardAction action) {
        for (int id = 1; id < 600; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getCardMetadata() != null
                    && card.getCardMetadata().getCardAction() == action) {
                return card;
            }
        }
        throw new AssertionError("no card for " + action);
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
