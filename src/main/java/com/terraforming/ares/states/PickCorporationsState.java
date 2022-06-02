package com.terraforming.ares.states;


import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public class PickCorporationsState extends AbstractState {

    public PickCorporationsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn()) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return List.of();
        } else if (player.getSelectedCorporationCard() == null) {
            return Collections.singletonList(TurnType.PICK_CORPORATION);
        } else {
            List<TurnType> turns = new ArrayList<>();

            if (player.getActionsInSecondPhase() > 0) {
                turns.add(TurnType.BUILD_BLUE_RED_PROJECT);
            }

            if (player.isAssortedEnterprisesGreenAvailable() || player.getCanBuildInFirstPhase() > 0) {
                turns.add(TurnType.BUILD_GREEN_PROJECT);
            }

            if (!turns.isEmpty()) {
                turns.add(TurnType.SKIP_TURN);
            }

            return turns;
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getActionsInSecondPhase() == 0 && player.getCanBuildInFirstPhase() == 0 && player.getNextTurn() == null
        )) {
            marsGame.setStateType(StateType.PICK_PHASE);
        }
    }

}
