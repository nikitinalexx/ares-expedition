package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.agent.AgentDecisionService;
import com.terraforming.ares.services.agent.DecisionLogs;
import com.terraforming.ares.services.agent.OnePlySearch;
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
 * A rollout must not write to the corpus.
 *
 * Why this test exists (owner, 19/11: "didn't you have a test for this? it would have been needed"): a rollout plays through
 * the full agent, so every hypothetical decision was recorded as if the game had taken it. That polluted the corpora with
 * phantom rows (labelled with the real outcome) and grew the write queue until the self-play JVM died every ~250 games - 921k
 * pending rows and 8 GB of observation vectors, found with a class histogram of the live JVM.
 */
class RolloutRecordingTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    AgentDecisionService agentDecisionService;
    @Autowired
    OnePlySearch onePlySearch;
    @Autowired
    DecisionLogs decisionLogs;

    @TempDir
    Path dir;

    private MarsGame newGame() {
        return gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build());
    }

    private static Player firstPlayer(MarsGame game) {
        return game.getPlayerUuidToPlayer().values().stream().min(java.util.Comparator.comparing(Player::getName)).orElseThrow();
    }

    @Test
    void aDecisionInsideAHypotheticalComputationIsNotRecorded() {
        decisionLogs.startRecording(dir);
        try {
            MarsGame game = newGame();
            long before = decisionLogs.rowsAccepted();
            assertFalse(onePlySearch.isBusy(), "nothing hypothetical is running yet");

            onePlySearch.beginHypothetical();
            try {
                assertTrue(onePlySearch.isBusy());
                agentDecisionService.pickPhase(game, firstPlayer(game));
                assertEquals(before, decisionLogs.rowsAccepted(),
                        "a phase picked inside a rollout must not reach the corpus");
            } finally {
                onePlySearch.endHypothetical();
            }
        } finally {
            decisionLogs.stopRecording();
        }
    }

    @Test
    void aRealDecisionIsStillRecorded() {
        decisionLogs.startRecording(dir);
        try {
            MarsGame game = newGame();
            long before = decisionLogs.rowsAccepted();
            agentDecisionService.pickPhase(game, firstPlayer(game));
            assertEquals(before + 1, decisionLogs.rowsAccepted(),
                    "a real decision must be recorded exactly once - the guard must not silence the corpus");
        } finally {
            decisionLogs.stopRecording();
        }
    }
}
