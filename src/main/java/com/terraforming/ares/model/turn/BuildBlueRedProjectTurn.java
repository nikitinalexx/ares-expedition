package com.terraforming.ares.model.turn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terraforming.ares.model.payments.Payment;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public class BuildBlueRedProjectTurn extends GenericBuildProjectTurn {

    @JsonCreator
    public BuildBlueRedProjectTurn(@JsonProperty String playerUuid,
                                   @JsonProperty int projectId,
                                   @JsonProperty List<Payment> payments,
                                   @JsonProperty Map<Integer, List<Integer>> inputParams) {
        super(playerUuid, projectId, payments, inputParams);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_BLUE_RED_PROJECT;
    }

}
