package com.terraforming.ares.model.turn;

import com.terraforming.ares.model.payments.Payment;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public class BuildGreenProjectTurn extends GenericBuildProjectTurn {

    public BuildGreenProjectTurn(String playerUuid, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        super(playerUuid, projectId, payments, inputParams);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

}
