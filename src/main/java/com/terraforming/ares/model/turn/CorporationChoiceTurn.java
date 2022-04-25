package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@AllArgsConstructor
@Getter
public class CorporationChoiceTurn implements Turn {
    private String playerUuid;
    private int corporationCardId;

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

}
