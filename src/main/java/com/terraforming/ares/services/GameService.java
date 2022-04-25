package com.terraforming.ares.services;

import com.terraforming.ares.factories.GameFactory;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class GameService {
    private final GameFactory gameFactory;
    private final GameRepository gameRepository;
    private final StateFactory stateFactory;

    public MarsGame startNewGame(GameParameters gameParameters) {
        MarsGame game = gameFactory.createMarsGame(gameParameters);

        gameRepository.save(game);

        return game;
    }

    public String getNextAction(String playerUuid) {
        MarsGame game = gameRepository.getGameByPlayerUuid(playerUuid);

        return stateFactory.getCurrentState(game).nextAction(playerUuid).name();
    }

    public List<TurnType> getPossibleTurns(String playerUuid) {
        MarsGame game = gameRepository.getGameByPlayerUuid(playerUuid);

        return stateFactory.getCurrentState(game).getPossibleTurns(playerUuid);
    }

}
