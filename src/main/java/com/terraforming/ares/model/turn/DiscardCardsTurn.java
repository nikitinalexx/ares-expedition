package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Only from selected cards means that user may discard cards only from the provided list of cards
 * and not from all the cards that he has in hand.
 * <p>
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@AllArgsConstructor
@Getter
public class DiscardCardsTurn implements Turn {
    private final String playerUuid;
    private final List<Integer> cards;
    private final int size;
    private final boolean onlyFromSelectedCards;

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

}
