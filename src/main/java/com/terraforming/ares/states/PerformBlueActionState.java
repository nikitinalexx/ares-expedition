package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.turn.TurnType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class PerformBlueActionState extends AbstractState {

    public PerformBlueActionState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        if (marsGame.getPlayerByUuid(playerUuid).getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(
                    TurnType.PERFORM_BLUE_ACTION,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            );
        }
    }

    @Override
    public void updateState() {
        performStateTransferFromPhase(4);
    }
}
