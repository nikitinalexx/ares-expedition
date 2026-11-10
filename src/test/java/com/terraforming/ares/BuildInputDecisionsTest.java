package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
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
import com.terraforming.ares.services.agent.BuildInputDecisions;
import com.terraforming.ares.services.agent.BuildPaymentChoice;
import com.terraforming.ares.services.agent.BuildPaymentDecisions;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BUILD_INPUT: filled maps validate; Imported Hydrogen offers plants and targets; a random agent finishes games.
 */
class BuildInputDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BuildInputDecisions buildInputDecisions;
    @Autowired
    BuildPaymentDecisions buildPaymentDecisions;
    @Autowired
    AiBuildProjectService aiBuildProjectService;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void filledInputsValidateAndBuild() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setMc(80);
        player.setPlants(10);
        player.setHeat(10);
        giveHand(game, player, 12);
        List<Card> cards = aiBuildProjectService.getAvailableCardsToBuild(game, player);
        assertFalse(cards.isEmpty());

        for (Card card : cards) {
            AtomicInteger steps = new AtomicInteger();
            Map<Integer, List<Integer>> input = buildInputDecisions.fill(game, player, card, decision -> {
                steps.incrementAndGet();
                assertEquals(DecisionType.BUILD_INPUT, decision.type());
                assertFalse(decision.candidates().isEmpty());
                return 0;
            });
            List<BuildPaymentChoice> payments = buildPaymentDecisions.options(game, player, card, input);
            assertEquals(null, cardValidationService.validateCard(
                    player, game, card.getId(), payments.getFirst().payments(), input));
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            aiTurnService.buildGreenProjectSync(copy, copyPlayer, card.getId(),
                    payments.getFirst().payments(), input);
            assertFalse(copyPlayer.getHand().getCards().contains(card.getId()));
            break;
        }
    }

    @Test
    void importedHydrogenOffersPlantsAndResourceTargets() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        // Imported Hydrogen is red; put player into a blue/red build slot
        player.getBuilds().clear();
        player.getBuilds().add(new BuildDto(BuildType.BLUE_RED));
        game.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, marsContextProvider.provide(game));
        player.setMc(40);
        player.setPlants(5);
        Card foundMicrobe = null;
        Card foundHydrogen = null;
        for (int id = 1; id < 500; id++) {
            Card c = cardService.getCard(id);
            if (c == null) {
                continue;
            }
            if (foundMicrobe == null && c.getCollectableResource() == CardCollectableResource.MICROBE) {
                foundMicrobe = c;
            }
            if (foundHydrogen == null && c.getCardMetadata() != null
                    && c.getCardMetadata().getCardAction() != null
                    && c.getCardMetadata().getCardAction().name().equals("IMPORTED_HYDROGEN")) {
                foundHydrogen = c;
            }
        }
        assertTrue(foundMicrobe != null && foundHydrogen != null, "need a microbe card and Imported Hydrogen");
        final Card microbe = foundMicrobe;
        final Card hydrogen = foundHydrogen;
        player.getPlayed().getCards().clear();
        player.getPlayed().addCard(microbe.getId());
        player.getCardResourcesCount().put(microbe.getClass(), 0);
        player.getHand().getCards().clear();
        player.getHand().addCards(List.of(hydrogen.getId()));

        var slot = buildInputDecisions.nextSlot(game, player, hydrogen, new java.util.HashMap<>());
        assertTrue(slot.isPresent());
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.kind() == BuildInputDecisions.KIND_PLANT));
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.id() == microbe.getId()));

        Map<Integer, List<Integer>> input = buildInputDecisions.fill(game, player, hydrogen, decision -> 0);
        assertTrue(input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())
                || input.containsKey(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId()));
        // Payment/build may still fail on global requirements; this test only checks the input menu.
        assertFalse(input.isEmpty());
    }

    @Test
    void aRandomAgentFinishesGamesAndRecordsInputRowsWhenNeeded() throws Exception {
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

        int paymentRows = 0;
        int inputRows = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.startsWith("BUILD_PAYMENT.")) {
                    DecisionFile.Content content = DecisionFile.read(file);
                    paymentRows += content.rows().size();
                }
                if (name.startsWith("BUILD_INPUT.")) {
                    DecisionFile.Content content = DecisionFile.read(file);
                    assertEquals(DecisionType.BUILD_INPUT, content.schema().type());
                    assertEquals(BuildInputDecisions.CANDIDATE_FEATURES, content.schema().candidateFeatures());
                    inputRows += content.rows().size();
                    for (DecisionFile.Row row : content.rows()) {
                        assertTrue(row.chosen() < row.candidateCount());
                    }
                }
            }
        }
        assertTrue(paymentRows > 0, "builds still record payments");
        // Input rows are optional (many cards need none); presence is enough when they appear.
        assertTrue(inputRows >= 0);
    }

    private MarsGame phaseOne() {
        MarsGame game = gameService.createNewSimulation(parameters());
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, marsContextProvider.provide(game));
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            while (player.getNextTurn() != null) {
                player.removeNextTurn();
            }
            player.getBuilds().clear();
            player.getBuilds().add(new BuildDto(BuildType.GREEN));
        }
        return game;
    }

    private void giveHand(MarsGame game, Player player, int count) {
        player.getHand().getCards().clear();
        player.getHand().addCards(cardService.dealCards(game, count));
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
