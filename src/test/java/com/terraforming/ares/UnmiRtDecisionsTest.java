package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.UnmiRtDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UNMI_RT: act and stop are offered when the corporation action is live; act spends 6 MC for +1 TR; random agent finishes.
 */
class UnmiRtDecisionsTest extends EngineTestBase {
    private static final int UNMI_ID = 10016;

    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    UnmiRtDecisions unmiRtDecisions;
    @Autowired
    AiTurnService aiTurnService;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void actRaisesTrAndStopIsAlwaysOffered() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        armUnmi(player, 20);

        List<TurnType> possible = List.of(TurnType.UNMI_RT, TurnType.SKIP_TURN);
        assertTrue(unmiRtDecisions.available(player, possible));

        Decision decision = unmiRtDecisions.build(game, player);
        assertEquals(2, decision.candidates().size());
        assertEquals(UnmiRtDecisions.ACT, decision.candidates().get(0).id());
        assertEquals(UnmiRtDecisions.STOP, decision.candidates().get(1).id());

        int trBefore = player.getTerraformingRating();
        int mcBefore = player.getMc();
        MarsGame copy = new MarsGame(game);
        Player copyPlayer = copy.getPlayerByUuid(player.getUuid());
        aiTurnService.unmiRtCorporationTurn(copy, copyPlayer);
        assertEquals(trBefore + 1, copyPlayer.getTerraformingRating());
        assertEquals(mcBefore - UnmiRtDecisions.COST_MC, copyPlayer.getMc());
        assertFalse(copyPlayer.isHasUnmiAction());
        assertTrue(copyPlayer.isDidUnmiAction());
    }

    @Test
    void unavailableWithoutMcOrFlag() {
        MarsGame game = phaseThree();
        Player player = named(game, "A");
        armUnmi(player, 5);
        assertFalse(unmiRtDecisions.available(player, List.of(TurnType.UNMI_RT)));

        player.setMc(20);
        player.setHasUnmiAction(false);
        // engine would not list UNMI_RT without the flag
        assertFalse(unmiRtDecisions.available(player, List.of(TurnType.SKIP_TURN)));
    }

    @Test
    void aRandomAgentFinishesGamesAndMayRecordUnmiRows() throws Exception {
        decisionLogs.startRecording(dir);
        try {
            for (int g = 0; g < 8; g++) {
                MarsGame game = gameService.createNewSimulation(parameters());
                simulation.processSimulation(game);
                assertEquals(StateType.GAME_END, game.getStateType());
                decisionLogs.finish(game);
            }
        } finally {
            decisionLogs.stopRecording();
        }

        try (var files = java.nio.file.Files.list(dir)) {
            List<Path> unmiFiles = files.filter(f -> f.getFileName().toString().startsWith("UNMI_RT.")).toList();
            for (Path file : unmiFiles) {
                DecisionFile.Content content = DecisionFile.read(file);
                assertEquals(DecisionType.UNMI_RT, content.schema().type());
                assertEquals(UnmiRtDecisions.CANDIDATE_FEATURES, content.schema().candidateFeatures());
                for (DecisionFile.Row row : content.rows()) {
                    assertEquals(2, row.candidateCount());
                    assertTrue(row.chosen() < 2);
                }
            }
        }
    }

    private static void armUnmi(Player player, int mc) {
        player.setSelectedCorporationCard(UNMI_ID);
        player.getPlayed().addCard(UNMI_ID);
        player.setUnmiCorporation(true);
        player.setHasUnmiAction(true);
        player.setDidUnmiAction(false);
        player.setMc(mc);
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
