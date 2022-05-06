package com.terraforming.ares.model.turn;

import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class PerformBlueActionTurn implements Turn {
    String playerUuid;
    int projectId;
    List<Integer> inputParams;

    @Override
    public TurnType getType() {
        return TurnType.PERFORM_BLUE_ACTION;
    }

}
