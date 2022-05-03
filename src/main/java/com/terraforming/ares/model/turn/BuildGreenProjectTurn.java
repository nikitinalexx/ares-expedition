package com.terraforming.ares.model.turn;

import com.terraforming.ares.model.payments.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@AllArgsConstructor
@Getter
public class BuildGreenProjectTurn implements Turn {
    private String playerUuid;
    private int projectId;
    private List<Payment> payments;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

}
