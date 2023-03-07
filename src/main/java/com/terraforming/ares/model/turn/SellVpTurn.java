package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class SellVpTurn implements Turn {
    private String playerUuid;

    @Override
    public TurnType getType() {
        return TurnType.SELL_VP;
    }

}
