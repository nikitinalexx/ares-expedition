package com.terraforming.ares.model.turn;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@NoArgsConstructor
@Data
public class CrysisImmediateAllTurn implements Turn {
    String playerUuid;
    boolean expectedAsNextTurn;

    public CrysisImmediateAllTurn(String playerUuid, boolean expectedAsNextTurn) {
        this.playerUuid = playerUuid;
        this.expectedAsNextTurn = expectedAsNextTurn;
    }

    @Override
    public boolean expectedAsNextTurn() {
        return expectedAsNextTurn;
    }

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_IMMEDIATE_ALL;
    }

}
