package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.NetDataWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How many states a game produces for the corpus, as a test rather than as a discovery after a night of generation.
 * <p>
 * The recorder used to be called once per iteration of the state loop, i.e. once per round: 66 rows per game, 33 per player. The owner logged a
 * post-state after every step (each blue action, each phase-5 discard) and got 300-400 per player, ~1000 per game. At the same game count that is a
 * corpus six times thinner, and no batch size or architecture can recover states that were never written. The hook now fires inside the step loops
 * (measured 1123 post-states per game, 427 distinct after the owner's own per-game dedup), and this test fails if it ever moves back out.
 */
class NetDataRecorderDensityTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    NetDataWriter writer;

    @Test
    void a_game_records_every_step_not_every_round() {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());

        simulation.processSimulation(game, writer::afterStep);
        long steps = writer.steps();
        writer.finish(game);

        // Two post-states per step (one per player), so `steps / 2` is states per player for the whole game.
        double perPlayer = steps / 2.0;
        int rounds = game.getTurns();
        assertTrue(rounds > 10, "the game ended after " + rounds + " rounds: the setup is wrong and the bound below means nothing");
        assertTrue(perPlayer >= 100,
                "recorded " + perPlayer + " states per player over " + rounds + " rounds: a round-level recorder writes ~1 per round "
                        + "(the owner's corpus is 300-400 per player), so the hook must be inside the step loops");
    }
}
