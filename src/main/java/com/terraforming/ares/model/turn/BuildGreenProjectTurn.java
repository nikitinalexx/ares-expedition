package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@AllArgsConstructor
@Getter
public class BuildGreenProjectTurn implements Turn {
    private String playerUuid;
    private int projectId;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

}
