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
public class BuildGreenProjectsState extends AbstractState {

    public BuildGreenProjectsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        if (marsGame.getPlayerByUuid(playerUuid).getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(
                    TurnType.BUILD_GREEN_PROJECT,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            );
        }
    }

    @Override
    public void updateState() {
        performStateTransferFromStage(2);
    }
}
