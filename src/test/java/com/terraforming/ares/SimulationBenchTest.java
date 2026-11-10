package com.terraforming.ares;

import com.terraforming.ares.controllers.SimulationBenchController;
import com.terraforming.ares.model.PlayerDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the benchmark counts correctly (this is NOT a speed measurement: speed is only meaningful on the overnight machine).
 */
class SimulationBenchTest extends EngineTestBase {
    @Autowired
    SimulationBenchController controller;

    @Test
    void benchPlaysAllRequestedGamesAndCountsThem() throws Exception {
        SimulationBenchController.BenchResult result = controller.bench(24, 2, PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, 4);

        assertEquals(24, result.games());
        assertEquals(0, result.failedGames());
        assertEquals(2, result.threads());
        assertTrue(result.gamesPerSecond() > 0);
        assertTrue(result.avgStepsPerGame() > 10, "a game has many steps: " + result.avgStepsPerGame());
        assertTrue(result.avgRounds() > 5 && result.avgRounds() < 100, "rounds per game: " + result.avgRounds());
        assertEquals(result.avgStepsPerGame() * result.games() / result.seconds(), result.stepsPerSecond(), 1e-6 * result.stepsPerSecond());
    }
}
