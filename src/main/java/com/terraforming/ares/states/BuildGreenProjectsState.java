package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildGreenProjectsState extends AbstractState {

    public BuildGreenProjectsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else if (player.getCanBuildInFirstPhase() == 0) {
            if (player.isUnmiCorporation() && player.isHasUnmiAction()) {
                return List.of(TurnType.UNMI_RT, TurnType.SKIP_TURN);
            }
            return List.of();
        } else {
            return List.of(
                    TurnType.BUILD_GREEN_PROJECT,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            );
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getCanBuildInFirstPhase() == 0
                        && player.getNextTurn() == null
                        && !player.isHasUnmiAction()
        )) {
            performStateTransferFromPhase(2);
        }
    }

}
