package com.terraforming.ares.model.turn;

import com.terraforming.ares.model.payments.Payment;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public class BuildGreenProjectTurn extends GenericBuildProjectTurn {

    public BuildGreenProjectTurn(String playerUuid, int projectId, List<Payment> payments) {
        super(playerUuid, projectId, payments);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

}
