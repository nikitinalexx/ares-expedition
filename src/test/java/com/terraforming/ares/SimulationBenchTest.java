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
        SimulationBenchController.BenchResult result = controller.bench(24, 2, PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, 4, false);

        assertEquals(24, result.games());
        assertEquals(0, result.failedGames());
        assertEquals(2, result.threads());
        assertTrue(result.gamesPerSecond() > 0);
        assertTrue(result.avgStepsPerGame() > 10, "a game has many steps: " + result.avgStepsPerGame());
        assertTrue(result.avgRounds() > 5 && result.avgRounds() < 100, "rounds per game: " + result.avgRounds());
        assertEquals(result.avgStepsPerGame() * result.games() / result.seconds(), result.stepsPerSecond(), 1e-6 * result.stepsPerSecond());
    }

    @Test
    void benchWithEncodingCountsTwoEncodesPerStep() throws Exception {
        SimulationBenchController.BenchResult result = controller.bench(10, 2, PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, 2, true);

        assertEquals(10, result.games());
        assertEquals(0, result.failedGames());
        // 2 players are encoded after every step
        assertEquals(2 * result.stepsPerSecond(), result.encodesPerSecond(), 1e-6 * result.encodesPerSecond());
    }

    @Test
    void benchReportsNeuralNetworkInferenceVolume() throws Exception {
        SimulationBenchController.BenchResult random = controller.bench(6, 2, PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM, 1, false);
        assertEquals(0.0, random.nnStatesPerGame(), "random players never call the network");

        SimulationBenchController.BenchResult network = controller.bench(2, 2, PlayerDifficulty.NETWORK_V2, PlayerDifficulty.NETWORK_V2, 0, false);
        assertTrue(network.nnCallsPerGame() > 10, "NETWORK_V2 evaluates states with the network: " + network.nnCallsPerGame());
        assertTrue(network.nnStatesPerGame() >= network.nnCallsPerGame(), "a call evaluates at least one state");
        assertEquals(network.nnStatesPerGame() * network.games() / network.seconds(), network.nnStatesPerSecond(), 1e-6 * network.nnStatesPerSecond());
    }
}
