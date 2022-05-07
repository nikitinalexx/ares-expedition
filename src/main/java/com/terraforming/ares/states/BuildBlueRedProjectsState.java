package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildBlueRedProjectsState extends AbstractState {

    public BuildBlueRedProjectsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null || player.getCanBuildInSecondPhase() == 0) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(
                    TurnType.BUILD_BLUE_RED_PROJECT,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            );
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getCanBuildInSecondPhase() == 0
        )) {
            performStateTransferFromPhase(3);
        }
    }
}
