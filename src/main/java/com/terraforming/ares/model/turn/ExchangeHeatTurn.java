package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExchangeHeatTurn implements Turn {
    String playerUuid;
    int value;

    public TurnType getType() {
        return TurnType.EXCHANGE_HEAT;
    }

}
