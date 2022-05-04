package com.terraforming.ares.model.turn;

import com.terraforming.ares.model.payments.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@AllArgsConstructor
@Getter
public abstract class GenericBuildProjectTurn implements Turn {
    private final String playerUuid;
    private final int projectId;
    private final List<Payment> payments;
}
