package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
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

    protected void performStateTransferFromStage(int stageNumber) {
        Set<Integer> pickedStages = marsGame.getPlayerContexts()
                .values()
                .stream()
                .map(PlayerContext::getChosenStage)
                .collect(Collectors.toSet());

        if (stageNumber <= 1 && pickedStages.contains(1)) {
            marsGame.setStateType(StateType.BUILD_GREEN_PROJECTS);
        } else if (stageNumber <= 2 && pickedStages.contains(2)) {
            marsGame.setStateType(StateType.BUILD_BLUE_RED_PROJECTS);
        } else if (stageNumber <= 3 && pickedStages.contains(3)) {
            marsGame.setStateType(StateType.PERFORM_BLUE_ACTION);
        } else if (stageNumber <= 4 && pickedStages.contains(4)) {
            marsGame.setStateType(StateType.COLLECT_INCOME);
        } else if (stageNumber <= 5 && pickedStages.contains(5)) {
            marsGame.setStateType(StateType.DRAFT_CARDS);
        } else if (marsGame.getPlayerContexts().values().stream().anyMatch(player -> player.getHand().size() > 10)) {
            marsGame.setStateType(StateType.SELL_EXTRA_CARDS);
        } else {
            marsGame.setStateType(StateType.PICK_STAGE);
        }
    }
}
