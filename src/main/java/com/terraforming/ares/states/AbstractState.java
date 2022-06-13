package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import com.terraforming.ares.services.CardService;

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
            marsGame.setStateType(StateType.GAME_END, cardService);
            return;
        }

        Set<Integer> pickedPhases = marsGame.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());

        if (phaseNumber <= 1 && pickedPhases.contains(1)) {
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.setCanBuildInFirstPhase(1));
            marsGame.setStateType(StateType.BUILD_GREEN_PROJECTS, cardService);
        } else if (phaseNumber <= 2 && pickedPhases.contains(2)) {
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.setActionsInSecondPhase(player.getChosenPhase() == 2 ? 2 : 1));
            marsGame.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, cardService);
        } else if (phaseNumber <= 3 && pickedPhases.contains(3)) {
            marsGame.setStateType(StateType.PERFORM_BLUE_ACTION, cardService);
        } else if (phaseNumber <= 4 && pickedPhases.contains(4)) {
            marsGame.setStateType(StateType.COLLECT_INCOME, cardService);
        } else if (phaseNumber <= 5 && pickedPhases.contains(5)) {
            marsGame.setStateType(StateType.DRAFT_CARDS, cardService);
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.addNextTurn(new DraftCardsTurn(player.getUuid())));
        } else if (marsGame.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > 10)) {
            marsGame.setStateType(StateType.SELL_EXTRA_CARDS, cardService);
        } else {
            marsGame.setStateType(StateType.PICK_PHASE, cardService);
        }
    }
}
