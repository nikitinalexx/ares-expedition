package com.terraforming.ares.services;

import com.terraforming.ares.dto.ActionsDto;
import com.terraforming.ares.factories.GameFactory;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import com.terraforming.ares.states.State;
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
    private final CachingGameRepository gameRepository;
    private final StateFactory stateFactory;
    private final StateContextProvider stateContextProvider;

    public MarsGame startNewGame(GameParameters gameParameters) {
        MarsGame game = gameFactory.createMarsGame(gameParameters);

        gameRepository.newGame(game);

        return game;
    }

    public MarsGame getGame(String playerUuid) {
        return gameRepository.getGameByPlayerUuid(playerUuid);
    }

    public ActionsDto getNextActions(String playerUuid) {
        MarsGame game = gameRepository.getGameByPlayerUuid(playerUuid);
        State currentState = stateFactory.getCurrentState(game);

        ActionsDto.ActionsDtoBuilder builder = ActionsDto.builder();

        builder.playersToNextAction(playerUuid, currentState.nextAction(stateContextProvider.createStateContext(playerUuid)).name());

        game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getUuid)
                .filter(uuid -> !uuid.equals(playerUuid))
                .forEach(uuid -> builder.playersToNextAction(uuid, currentState.nextAction(stateContextProvider.createStateContext(uuid)).name()));

        return builder.build();
    }

    public List<TurnType> getPossibleTurns(String playerUuid) {
        MarsGame game = gameRepository.getGameByPlayerUuid(playerUuid);

        return stateFactory.getCurrentState(game).getPossibleTurns(stateContextProvider.createStateContext(playerUuid));
    }

}
