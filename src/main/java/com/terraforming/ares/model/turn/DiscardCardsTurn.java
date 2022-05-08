package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@AllArgsConstructor
@Getter
public class DiscardCardsTurn implements Turn {
    private final String playerUuid;
    private final List<Integer> cards;
    private final int size;

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

}
