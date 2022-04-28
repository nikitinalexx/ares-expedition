package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@AllArgsConstructor
@Getter
public class SkipTurn implements Turn {
    private String playerUuid;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

}
