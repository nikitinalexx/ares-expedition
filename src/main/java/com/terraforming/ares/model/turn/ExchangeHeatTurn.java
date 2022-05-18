package com.terraforming.ares.model.turn;

import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class ExchangeHeatTurn implements Turn {
    String playerUuid;
    int value;

    public TurnType getType() {
        return TurnType.EXCHANGE_HEAT;
    }

}
