package com.terraforming.ares.model.turn;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class PickExtraCardTurn implements Turn {
    String playerUuid;

    @Override
    public TurnType getType() {
        return TurnType.PICK_EXTRA_CARD;
    }

}