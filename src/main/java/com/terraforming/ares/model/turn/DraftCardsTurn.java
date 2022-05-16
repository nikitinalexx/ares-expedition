package com.terraforming.ares.model.turn;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class DraftCardsTurn implements Turn {
    String playerUuid;

    @Override
    public TurnType getType() {
        return TurnType.DRAFT_CARDS;
    }

}
