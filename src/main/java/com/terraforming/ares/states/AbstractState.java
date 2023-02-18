package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import com.terraforming.ares.services.CardService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public abstract class AbstractState implements State {
    protected final MarsGame marsGame;
    protected final CardService cardService;

    protected AbstractState(MarsGame marsGame, CardService cardService) {
        this.marsGame = marsGame;
        this.cardService = cardService;
    }

    @Override
    public Action nextAction(StateContext stateContext) {
        if (getPossibleTurns(stateContext).isEmpty()) {
            return Action.WAIT;
        } else {
            return Action.TURN;
        }
    }

    protected void performStateTransferFromPhase(int phaseNumber) {
        if (marsGame.gameEndCondition()) {
            marsGame.setStateType(StateType.GAME_END, cardService, true);
            return;
        }

        Set<Integer> pickedPhases = marsGame.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());

        if (phaseNumber <= 1 && pickedPhases.contains(1)) {
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1));
            marsGame.setStateType(StateType.BUILD_GREEN_PROJECTS, cardService, false);
        } else if (phaseNumber <= 2 && pickedPhases.contains(2)) {
            marsGame.getPlayerUuidToPlayer().values().forEach(
                    player -> {
                        player.setActionsInSecondPhase(player.getChosenPhase() == 2 ? 2 : 1);
                        if (player.getChosenPhase() == 2 && player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                            player.getHand().addCards(cardService.dealCards(marsGame, 1));
                        }
                    });
            marsGame.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, cardService, true);
        } else if (phaseNumber <= 3 && pickedPhases.contains(3)) {
            marsGame.setStateType(StateType.PERFORM_BLUE_ACTION, cardService, true);
            marsGame.getPlayerUuidToPlayer().values().forEach(
                    player -> {
                        if (player.getChosenPhase() == 3 && player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_REVEAL_CARDS)) {
                            final List<Integer> cards = cardService.dealCards(marsGame, 3);
                            for (Integer cardId : cards) {
                                final Card card = cardService.getCard(cardId);
                                if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
                                    player.getHand().addCard(cardId);
                                }
                            }
                        }
                    });
        } else if (phaseNumber <= 4 && pickedPhases.contains(4)) {
            marsGame.setStateType(StateType.COLLECT_INCOME, cardService, true);
        } else if (phaseNumber <= 5 && pickedPhases.contains(5)) {
            marsGame.setStateType(StateType.DRAFT_CARDS, cardService, true);
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.addNextTurn(new DraftCardsTurn(player.getUuid())));
        } else if (marsGame.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > 10)) {
            marsGame.setStateType(StateType.SELL_EXTRA_CARDS, cardService, true);
        } else {
            marsGame.setStateType(StateType.PICK_PHASE, cardService, true);
        }
    }
}
