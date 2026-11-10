package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.model.StateType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeTest extends EngineTestBase {
    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulationProcessorService;

    @Test
    void randomVsRandomGameFinishes() {
        GameParameters parameters = GameParameters.builder()
                .playerNames(List.of("A", "B"))
                .computers(List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
        MarsGame game = gameService.createNewSimulation(parameters);
        simulationProcessorService.processSimulation(game);
        assertEquals(StateType.GAME_END, game.getStateType());
    }
}
