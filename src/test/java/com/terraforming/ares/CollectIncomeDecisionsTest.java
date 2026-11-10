package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.CollectIncomeDecisions;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * COLLECT_INCOME: phase-4 double-produce menu; chosen card is passed through (regression for the null bug); stop works.
 */
class CollectIncomeDecisionsTest extends EngineTestBase {
    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    CollectIncomeDecisions collectIncomeDecisions;
    @Autowired
    CardService cardService;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void menuListsEligibleGreensAndStopAndPayAgainRuns() {
        MarsGame game = collectState();
        Player player = named(game, "A");
        armDoubleProduce(player);
        Card sponsors = cardService.getCard(190); // Sponsors: payAgain +2 MC
        assertTrue(sponsors.canPayAgain());
        player.getPlayed().addCard(sponsors.getId());

        assertTrue(collectIncomeDecisions.needsChoice(player));
        Decision decision = collectIncomeDecisions.build(game, player);
        assertEquals(CollectIncomeDecisions.STOP, decision.candidates().getLast().id());
        Set<Integer> offered = decision.candidates().stream()
                .map(c -> c.id())
                .filter(id -> id != CollectIncomeDecisions.STOP)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(Set.of(sponsors.getId()), offered);

        aiTurnService.collectIncomeTurn(player, sponsors.getId());
        assertEquals(sponsors.getId(),
                ((com.terraforming.ares.model.turn.CollectIncomeTurn) player.getNextTurn()).getDoubleCollectCardId());

        int mcBefore = player.getMc();
        sponsors.payAgain(game, cardService, player);
        assertEquals(mcBefore + 2, player.getMc());
    }

    @Test
    void noChoiceWithoutUpgradeOrEligibleCard() {
        MarsGame game = collectState();
        Player player = named(game, "A");
        player.setChosenPhase(4);
        assertFalse(collectIncomeDecisions.needsChoice(player));

        armDoubleProduce(player);
        assertFalse(collectIncomeDecisions.needsChoice(player));
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

        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("COLLECT_INCOME.")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.COLLECT_INCOME, content.schema().type());
                for (DecisionFile.Row row : content.rows()) {
                    assertTrue(row.chosen() < row.candidateCount());
                    boolean stop = false;
                    for (int i = 0; i < row.candidateCount(); i++) {
                        if (row.candidateIds()[i] == CollectIncomeDecisions.STOP) {
                            stop = true;
                        }
                    }
                    assertTrue(stop);
                }
            }
        }
    }

    private static void armDoubleProduce(Player player) {
        player.setChosenPhase(4);
        // PHASE_4_UPGRADE_DOUBLE_PRODUCE = 11 → phaseCards[3] == 2
        player.updatePhaseCard(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE / 3,
                Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE % 3);
        assertTrue(player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE));
    }

    private MarsGame collectState() {
        MarsGame game = gameService.createNewSimulation(parameters());
        game.setStateType(StateType.COLLECT_INCOME, marsContextProvider.provide(game));
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            while (player.getNextTurn() != null) {
                player.removeNextTurn();
            }
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
