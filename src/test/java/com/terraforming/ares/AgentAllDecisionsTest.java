package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.DecisionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Criterion from the plan: a uniform-random agent that decides every non-crisis {@link DecisionType} must finish
 * games without errors, and enough self-play must exercise every decision type at least once.
 */
class AgentAllDecisionsTest extends EngineTestBase {
    private static final int FINISH_GAMES = 40;
    private static final int COVERAGE_MAX_GAMES = 200;

    @TempDir
    Path dir;

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    DecisionLogs decisionLogs;

    @Test
    void randomAgentFinishesGamesOnAgentAndAgentR() {
        for (int g = 0; g < FINISH_GAMES; g++) {
            PlayerDifficulty a = g % 3 == 0 ? PlayerDifficulty.AGENT_R : PlayerDifficulty.AGENT;
            PlayerDifficulty b = g % 3 == 1 ? PlayerDifficulty.AGENT_R : PlayerDifficulty.AGENT;
            MarsGame game = gameService.createNewSimulation(parameters(a, b));
            simulation.processSimulation(game);
            assertEquals(StateType.GAME_END, game.getStateType(), "game " + g + " (" + a + " vs " + b + ")");
        }
    }

    @Test
    void everyDecisionTypeIsRecordedAtLeastOnce() throws Exception {
        decisionLogs.startRecording(dir);
        Set<DecisionType> seen = EnumSet.noneOf(DecisionType.class);
        Map<DecisionType, Integer> counts = new EnumMap<>(DecisionType.class);
        int played = 0;
        try {
            for (int g = 0; g < COVERAGE_MAX_GAMES && seen.size() < DecisionType.values().length; g++) {
                PlayerDifficulty a = g % 2 == 0 ? PlayerDifficulty.AGENT : PlayerDifficulty.AGENT_R;
                PlayerDifficulty b = g % 3 == 0 ? PlayerDifficulty.AGENT_R : PlayerDifficulty.AGENT;
                MarsGame game = gameService.createNewSimulation(parameters(a, b));
                simulation.processSimulation(game);
                assertEquals(StateType.GAME_END, game.getStateType());
                decisionLogs.finish(game);
                played++;
                // Chunks stay .tmp until rotate; readers ignore .tmp, so flush before scanning coverage.
                if (played % 10 == 0) {
                    decisionLogs.rotate();
                    refreshSeen(seen, counts);
                }
            }
        } finally {
            decisionLogs.stopRecording();
        }
        refreshSeen(seen, counts);

        Set<DecisionType> missing = EnumSet.allOf(DecisionType.class);
        missing.removeAll(seen);
        assertTrue(missing.isEmpty(),
                "after " + played + " games still missing " + missing + "; counts=" + counts);
    }

    private void refreshSeen(Set<DecisionType> seen, Map<DecisionType, Integer> counts) throws Exception {
        counts.clear();
        seen.clear();
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".ares")).toList()) {
                DecisionFile.Content content = DecisionFile.read(file);
                DecisionType type = content.schema().type();
                seen.add(type);
                counts.merge(type, content.rows().size(), Integer::sum);
            }
        }
    }

    private static GameParameters parameters(PlayerDifficulty a, PlayerDifficulty b) {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(a, b))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
    }
}
