package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public abstract class AbstractState implements State {
    protected final MarsGame marsGame;

    protected AbstractState(MarsGame marsGame) {
        this.marsGame = marsGame;
    }

    @Override
    public Action nextAction(String playerUuid) {
        if (getPossibleTurns(playerUuid).isEmpty()) {
            return Action.WAIT;
        } else {
            return Action.TURN;
        }
    }

    protected void performStateTransferFromPhase(int phaseNumber) {
        Set<Integer> pickedPhases = marsGame.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());

        if (phaseNumber <= 1 && pickedPhases.contains(1)) {
            marsGame.setStateType(StateType.BUILD_GREEN_PROJECTS);
        } else if (phaseNumber <= 2 && pickedPhases.contains(2)) {
            marsGame.setStateType(StateType.BUILD_BLUE_RED_PROJECTS);
        } else if (phaseNumber <= 3 && pickedPhases.contains(3)) {
            marsGame.setStateType(StateType.PERFORM_BLUE_ACTION);
        } else if (phaseNumber <= 4 && pickedPhases.contains(4)) {
            marsGame.setStateType(StateType.COLLECT_INCOME);
        } else if (phaseNumber <= 5 && pickedPhases.contains(5)) {
            marsGame.setStateType(StateType.DRAFT_CARDS);
        } else if (marsGame.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > 10)) {
            marsGame.setStateType(StateType.SELL_EXTRA_CARDS);
        } else {
            marsGame.setStateType(StateType.PICK_PHASE);
        }
    }
}
