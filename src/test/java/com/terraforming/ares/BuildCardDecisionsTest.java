package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
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
import com.terraforming.ares.services.agent.BuildCardDecisions;
import com.terraforming.ares.services.agent.CardFeatures;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BUILD_CARD: every offered card builds with the SMART payment/params helpers, stop is always present, and a random
 * agent finishes games.
 */
class BuildCardDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BuildCardDecisions buildCardDecisions;
    @Autowired
    AiBuildProjectService aiBuildProjectService;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;
    @Autowired
    AiCardBuildParamsService aiCardBuildParamsService;
    @Autowired
    AiPaymentService aiPaymentService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void everyOfferedCardBuildsWithSmartPaymentAndParams() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setMc(60);
        player.setHeat(20);
        player.setPlants(20);
        giveHand(game, player, 12);

        Decision decision = buildCardDecisions.build(game, player);
        assertEquals(BuildCardDecisions.STOP, decision.candidates().getLast().id());
        Set<Integer> offered = cardIds(decision);
        Set<Integer> expected = aiBuildProjectService.getAvailableCardsToBuild(game, player).stream()
                .map(Card::getId).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expected, offered);

        for (int id : offered) {
            MarsGame copy = new MarsGame(game);
            Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
            Card card = cardService.getCard(id);
            Map<Integer, List<Integer>> input = aiCardBuildParamsService.getInputParamsForBuild(copy, copyPlayer, card);
            var payments = aiPaymentService.getCardPayments(copy, copyPlayer, card, input);
            assertEquals(null, cardValidationService.validateCard(copyPlayer, copy, id, payments, input));
            // phases 1–2 queue the turn asynchronously; sync is what the engine applies when the turn runs
            aiTurnService.buildGreenProjectSync(copy, copyPlayer, id, payments, input);
            assertFalse(copyPlayer.getHand().getCards().contains(id));
        }
    }

    @Test
    void onlyGreenCardsAppearInPhaseOneAndStopIsAlwaysThere() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setMc(80);
        giveHand(game, player, 10);
        Decision decision = buildCardDecisions.build(game, player);
        for (var candidate : decision.candidates()) {
            if (candidate.id() == BuildCardDecisions.STOP) {
                assertEquals(1f, candidate.features()[CardFeatures.LENGTH - 1]);
                continue;
            }
            assertEquals(CardColor.GREEN, cardService.getCard(candidate.id()).getColor());
        }
        assertTrue(decision.candidates().stream().anyMatch(c -> c.id() == BuildCardDecisions.STOP));
    }

    @Test
    void handOrderDoesNotChangeCandidatesAndHiddenFactsDoNotChangeObservation() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Player opponent = named(game, "B");
        player.setMc(60);
        giveHand(game, player, 8);
        giveHand(game, opponent, 8);
        Decision first = buildCardDecisions.build(game, player);
        float[] seen = first.observation();
        Set<Integer> ids = cardIds(first);

        java.util.Collections.shuffle(player.getHand().getCards());
        assertEquals(ids, cardIds(buildCardDecisions.build(game, player)));

        java.util.Collections.shuffle(opponent.getHand().getCards());
        java.util.Collections.shuffle(game.getProjectsDeck().getCards());
        assertTrue(Arrays.equals(seen, buildCardDecisions.build(game, player).observation()));
    }

    @Test
    void aRandomAgentFinishesGamesAndRecordsBuildCardRows() throws Exception {
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
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("BUILD_CARD.")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.BUILD_CARD, content.schema().type());
                for (DecisionFile.Row row : content.rows()) {
                    rows++;
                    assertTrue(row.chosen() < row.candidateCount());
                    boolean stop = false;
                    for (int i = 0; i < row.candidateCount(); i++) {
                        int id = row.candidateIds()[i];
                        assertTrue(id == BuildCardDecisions.STOP || (id > 0 && id < 10000), "id " + id);
                        if (id == BuildCardDecisions.STOP) {
                            stop = true;
                        }
                    }
                    assertTrue(stop);
                }
            }
        }
        assertTrue(rows > 0, "agents with a build slot are asked which card to play");
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

    private List<Integer> giveHand(MarsGame game, Player player, int count) {
        player.getHand().getCards().clear();
        List<Integer> cards = cardService.dealCards(game, count);
        player.getHand().addCards(cards);
        return new ArrayList<>(cards);
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

    private static Set<Integer> cardIds(Decision decision) {
        Set<Integer> ids = new TreeSet<>();
        for (var candidate : decision.candidates()) {
            if (candidate.id() != BuildCardDecisions.STOP) {
                ids.add(candidate.id());
            }
        }
        return ids;
    }
}
