package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
public class DiscardDraftedCardsTurn implements Turn {
    private String playerUuid;
    private List<Integer> cards;

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_DRAFTED_CARDS;
    }

}