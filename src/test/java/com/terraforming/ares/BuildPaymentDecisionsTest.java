package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.BuildPaymentChoice;
import com.terraforming.ares.services.agent.BuildPaymentDecisions;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BUILD_PAYMENT: every generated payment is accepted; Helion may split heat; a random agent finishes games.
 */
class BuildPaymentDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BuildPaymentDecisions buildPaymentDecisions;
    @Autowired
    AiBuildProjectService aiBuildProjectService;
    @Autowired
    AiCardBuildParamsService aiCardBuildParamsService;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void everyGeneratedPaymentIsAcceptedByTheEngine() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setMc(80);
        player.setPlants(10);
        giveHand(game, player, 15);
        List<Card> cards = aiBuildProjectService.getAvailableCardsToBuild(game, player);
        assertFalse(cards.isEmpty());

        for (Card card : cards) {
            Map<Integer, List<Integer>> input = aiCardBuildParamsService.getInputParamsForBuild(game, player, card);
            List<BuildPaymentChoice> choices = buildPaymentDecisions.options(game, player, card, input);
            assertFalse(choices.isEmpty());
            Set<Integer> ids = new HashSet<>();
            for (BuildPaymentChoice choice : choices) {
                assertTrue(ids.add(choice.id()), "duplicate payment id " + choice.id());
                assertEquals(null, cardValidationService.validateCard(player, game, card.getId(), choice.payments(), input));
            }
            BuildPaymentChoice first = choices.getFirst();
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            aiTurnService.buildGreenProjectSync(copy, copyPlayer, card.getId(), first.payments(), input);
            assertFalse(copyPlayer.getHand().getCards().contains(card.getId()));
            break; // one successful sync build is enough; the rest are validated above
        }
    }

    @Test
    void helionMayPayWithHeatSplits() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setSelectedCorporationCard(10000);
        player.getPlayed().addCard(10000);
        player.setMc(14);
        player.setHeat(30);
        // Space Station costs 14 and needs no on-built input; put it in hand directly.
        Card card = cardService.getCard(189);
        player.getHand().getCards().clear();
        player.getHand().addCards(List.of(card.getId()));
        Map<Integer, List<Integer>> input = aiCardBuildParamsService.getInputParamsForBuild(game, player, card);
        List<BuildPaymentChoice> choices = buildPaymentDecisions.options(game, player, card, input);
        assertTrue(choices.size() > 1, "Helion should have more than one heat/MC split: " + choices.size());
        assertTrue(choices.stream().anyMatch(c -> (c.id() >>> 2) > 0), "at least one heat split");
        assertTrue(choices.stream().anyMatch(c -> (c.id() >>> 2) == 0), "pure MC when heat is optional");
        for (BuildPaymentChoice choice : choices) {
            assertEquals(null, cardValidationService.validateCard(player, game, card.getId(), choice.payments(), input));
            int heat = choice.payments().stream()
                    .filter(p -> p.getType() == PaymentType.HEAT)
                    .mapToInt(Payment::getValue)
                    .sum();
            assertEquals(choice.id() >>> 2, heat);
        }
    }

    @Test
    void aRandomAgentFinishesGamesAndRecordsPaymentRows() throws Exception {
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
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("BUILD_PAYMENT.")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.BUILD_PAYMENT, content.schema().type());
                assertEquals(BuildPaymentDecisions.CANDIDATE_FEATURES, content.schema().candidateFeatures());
                for (DecisionFile.Row row : content.rows()) {
                    rows++;
                    assertTrue(row.chosen() < row.candidateCount());
                    assertTrue(row.candidateCount() >= 1);
                }
            }
        }
        assertTrue(rows > 0, "every built card records a payment decision");
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
