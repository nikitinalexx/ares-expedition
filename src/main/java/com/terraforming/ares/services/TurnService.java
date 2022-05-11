package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameUpdateResult;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.repositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

    public void choosePhaseTurn(String playerUuid, int phaseId) {
        performAsyncTurn(new PhaseChoiceTurn(playerUuid, phaseId), playerUuid, game -> {
            if (phaseId < 1 || phaseId > 5) {
                return "Phase is not within [1..5] range";
            }

            Player player = game.getPlayerByUuid(playerUuid);
            if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == phaseId) {
                return "This phase already picked in previous round";
            }

            return null;
        });
    }

    public void skipTurn(String playerUuid) {
        performAsyncTurn(new SkipTurn(playerUuid), playerUuid, game -> null);
    }

    public TurnResponse sellCards(String playerUuid, List<Integer> cards) {
        return performSyncTurn(new SellCardsTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getHand().getCards().containsAll(cards)) {
                        return "Can't sell cards that you don't have";
                    }

                    return null;
                });
    }

    public TurnResponse discardCards(String playerUuid, List<Integer> cards) {
        return performSyncTurn(
                new DiscardCardsTurn(playerUuid, cards, cards.size(), false),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getNextTurn().getType() != TurnType.DISCARD_CARDS) {
                        return "Invalid next turn. Expected " + player.getNextTurn().getType();
                    }

                    DiscardCardsTurn expectedTurn = (DiscardCardsTurn) player.getNextTurn();
                    if (expectedTurn.getSize() != cards.size()) {
                        return "Incorrect number of cards to discard, expected: " + expectedTurn.getSize();
                    }

                    if (!player.getHand().getCards().containsAll(cards)) {
                        return "Can't discard cards that you don't have";
                    }

                    if (expectedTurn.isOnlyFromSelectedCards()) {
                        List<Integer> expectedCardsToBeRemovedFrom = expectedTurn.getCards();
                        for (Integer card : cards) {
                            if (!expectedCardsToBeRemovedFrom.contains(card)) {
                                return "You can't discard cards other than from those that you received";
                            }
                        }
                    }

                    return null;
                });
    }

    public void buildGreenProjectCard(String playerUuid, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        performAsyncTurn(
                new BuildGreenProjectTurn(playerUuid, projectId, payments, inputParams),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game, projectId, payments, inputParams);
                });
    }

    public void buildBlueRedProjectCard(String playerUuid, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        performAsyncTurn(
                new BuildBlueRedProjectTurn(playerUuid, projectId, payments, inputParams),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game, projectId, payments, inputParams);
                });
    }

    public TurnResponse performBlueAction(String playerUuid, int projectId, List<Integer> inputParams) {
        return performSyncTurn(
                new PerformBlueActionTurn(playerUuid, projectId, inputParams),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateBlueAction(player, game, projectId, inputParams);
                }
        );
    }

    private void performAsyncTurn(Turn turn, String playerUuid, Function<MarsGame, String> turnSpecificValidations) {
        long gameId = gameRepository.getGameIdByPlayerUuid(playerUuid);

        GameUpdateResult<?> updateResult = gameRepository.updateMarsGame(
                gameId,
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(turn.getType())) {
                        return "Incorrect game state for a turn " + turn.getType();
                    }

                    return turnSpecificValidations.apply(game);
                },
                game -> {
                    game.getPlayerByUuid(playerUuid).setNextTurn(turn);
                    return null;
                }
        );

        if (updateResult.finishedWithError()) {
            throw new IllegalStateException(updateResult.getError());
        }

        gameProcessorService.registerAsyncGameUpdate(gameId);
    }

    private TurnResponse performSyncTurn(Turn turn, String playerUuid, Function<MarsGame, String> turnSpecificValidations) {
        long gameId = gameRepository.getGameIdByPlayerUuid(playerUuid);

        GameUpdateResult<TurnResponse> updateResult = gameProcessorService.syncPlayerUpdate(gameId,
                turn,
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(playerUuid).contains(turn.getType())) {
                        return "Incorrect game state for a turn " + turn.getType();
                    }

                    return turnSpecificValidations.apply(game);
                }
        );

        if (updateResult.finishedWithError()) {
            throw new IllegalStateException(updateResult.getError());
        }

        return updateResult.getResult();
    }

}
