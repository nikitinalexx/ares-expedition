package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
@NoArgsConstructor
public class DiscardCardsTurn implements Turn {
    private String playerUuid;
    private List<Integer> cards;
    private int size;
    private boolean onlyFromSelectedCards;
    private boolean expectedAsNextTurn;
    private List<String> alreadyDrafted;

    @Override
    public boolean expectedAsNextTurn() {
        return expectedAsNextTurn;
    }

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

}
