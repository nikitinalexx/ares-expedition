package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.StageChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class TurnService {
    private final StateFactory stateFactory;
    private final GameRepository gameRepository;
    private final GameProcessorService gameProcessorService;

    public void chooseCorporationTurn(String playerUuid, int corporationCardId) {
        MarsGame marsGame = gameRepository.getGameByPlayerUuid(playerUuid);

        String errorMessage = gameRepository.updateMarsGame(
                marsGame.getId(),
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(TurnType.PICK_CORPORATION)) {
                        return "Incorrent game state for corporation pick";
                    }
                    if (!game.getPlayerByUuid(playerUuid).getCorporations().containsCard(corporationCardId)) {
                        return "Can't pick corporation that is not in your choice deck";
                    }
                    return null;
                },
                game -> game.getPlayerByUuid(playerUuid).setNextTurn(
                        new CorporationChoiceTurn(playerUuid, corporationCardId)
                ));

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        gameProcessorService.registerGameUpdate(marsGame.getId());
    }

    public void chooseStageTurn(String playerUuid, int stageId) {
        MarsGame marsGame = gameRepository.getGameByPlayerUuid(playerUuid);

        String errorMessage = gameRepository.updateMarsGame(
                marsGame.getId(),
                game -> {
                    PlayerContext player = game.getPlayerByUuid(playerUuid);
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(TurnType.PICK_STAGE)) {
                        return "Incorrect game state for picking stage";
                    }

                    if (player.getPreviousStage() == stageId) {
                        return "This stage already picked in previous round";
                    }

                    return null;
                },
                game -> game.getPlayerByUuid(playerUuid).setNextTurn(new StageChoiceTurn(playerUuid, stageId))
        );

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        gameProcessorService.registerGameUpdate(marsGame.getId());
    }

}
