package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@AllArgsConstructor
@Getter
public class SellCardsTurn implements Turn {
    private String playerUuid;
    private List<Integer> cards;

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS;
    }

}
