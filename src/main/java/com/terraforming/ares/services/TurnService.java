package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

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
    private final CardValidationService cardValidationService;

    public void chooseCorporationTurn(String playerUuid, int corporationCardId) {
        performAsyncTurn(
                new CorporationChoiceTurn(playerUuid, corporationCardId),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getCorporations().containsCard(corporationCardId)) {
                        return "Can't pick corporation that is not in your choice deck";
                    }
                    return null;
                }
        );
    }

    public void chooseStageTurn(String playerUuid, int stageId) {
        performAsyncTurn(new StageChoiceTurn(playerUuid, stageId), playerUuid, game -> {
            if (stageId < 1 || stageId > 5) {
                return "Stage is not within [1..5] range";
            }

            PlayerContext player = game.getPlayerByUuid(playerUuid);
            if (player.getPreviousChosenStage() != null && player.getPreviousChosenStage() == stageId) {
                return "This stage already picked in previous round";
            }

            return null;
        });
    }

    public void skipTurn(String playerUuid) {
        performAsyncTurn(new SkipTurn(playerUuid), playerUuid, game -> null);
    }

    public void sellCards(String playerUuid, List<Integer> cards) {
        performSyncTurn(
                new SellCardsTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getHand().getCards().containsAll(cards)) {
                        return "Can't sell cards that you don't have";
                    }

                    return null;
                }
        );
    }

    public void buildGreenProjectCard(String playerUuid, int projectId, List<Payment> payments) {
        performAsyncTurn(
                new BuildGreenProjectTurn(playerUuid, projectId, payments),
                playerUuid,
                game -> {
                    PlayerContext player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game.getPlanet(), projectId, payments);
                });
    }

    public void buildBlueRedProjectCard(String playerUuid, int projectId, List<Payment> payments) {
        performAsyncTurn(
                new BuildBlueRedProjectTurn(playerUuid, projectId, payments),
                playerUuid,
                game -> {
                    PlayerContext player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game.getPlanet(), projectId, payments);
                });
    }

    private void performAsyncTurn(Turn turn, String playerUuid, Function<MarsGame, String> turnSpecificValidations) {
        performTurn(turn, playerUuid, turnSpecificValidations, false);
    }

    private void performSyncTurn(Turn turn, String playerUuid, Function<MarsGame, String> turnSpecificValidations) {
        performTurn(turn, playerUuid, turnSpecificValidations, true);
    }

    private void performTurn(Turn turn, String playerUuid, Function<MarsGame, String> turnSpecificValidations, boolean sync) {
        long gameId = gameRepository.getGameIdByPlayerUuid(playerUuid);

        String errorMessage = gameRepository.updateMarsGame(
                gameId,
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(turn.getType())) {
                        return "Incorrect game state for a turn " + turn.getType();
                    }

                    return turnSpecificValidations.apply(game);
                },
                game -> game.getPlayerByUuid(playerUuid).setNextTurn(turn)
        );

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        if (sync) {
            gameProcessorService.syncGameUpdate(gameId);
        } else {
            gameProcessorService.registerAsyncGameUpdate(gameId);
        }
    }


}
