package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.*;
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
        if (stageId < 1 || stageId > 5) {
            throw new IllegalArgumentException("Stage is not within [1..5] range");
        }

        MarsGame marsGame = gameRepository.getGameByPlayerUuid(playerUuid);

        String errorMessage = gameRepository.updateMarsGame(
                marsGame.getId(),
                game -> {
                    PlayerContext player = game.getPlayerByUuid(playerUuid);
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(TurnType.PICK_STAGE)) {
                        return "Incorrect game state for picking stage";
                    }

                    if (player.getPreviousStage() != null && player.getPreviousStage() == stageId) {
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

    public void skipTurn(String playerUuid) {
        MarsGame marsGame = gameRepository.getGameByPlayerUuid(playerUuid);

        String errorMessage = gameRepository.updateMarsGame(
                marsGame.getId(),
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(TurnType.SKIP_TURN)) {
                        return "Incorrect game state for turn skip";
                    }

                    return null;
                },
                game -> game.getPlayerByUuid(playerUuid).setNextTurn(new SkipTurn(playerUuid))
        );

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        gameProcessorService.registerGameUpdate(marsGame.getId());
    }

    public void sellCards(String playerUuid, List<Integer> cards) {
        MarsGame marsGame = gameRepository.getGameByPlayerUuid(playerUuid);

        PlayerContext player = marsGame.getPlayerByUuid(playerUuid);

        if (!player.getHand().getCards().containsAll(cards)) {
            throw new IllegalArgumentException("Can't sell cards that you don't have");
        }

        String errorMessage = gameRepository.updateMarsGame(
                marsGame.getId(),
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(TurnType.SELL_CARDS)) {
                        return "Incorrect game state for selling cards";
                    }

                    return null;
                },
                game -> game.getPlayerByUuid(playerUuid).setNextTurn(new SellCardsTurn(playerUuid, cards))
        );

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        gameProcessorService.registerGameUpdate(marsGame.getId());
        //need a synchronous update
        gameProcessorService.process();
    }
}
