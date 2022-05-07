package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@AllArgsConstructor
@Getter
public class PhaseChoiceTurn implements Turn {
    private final String playerUuid;
    private final int phaseId;

    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }
}
