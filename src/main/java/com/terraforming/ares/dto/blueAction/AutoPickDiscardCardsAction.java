package com.terraforming.ares.dto.blueAction;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.TurnResponseType;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Some cards are picked from the deck, and some get picked, while others discarded.
 * Auto means that player doesn't make a decision, the choice is performed by game
 * based on the rules stated in the card.
 *
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
@Builder
public class AutoPickDiscardCardsAction implements TurnResponse {
    @Singular
    List<CardDto> takenCards;
    @Singular
    List<CardDto> discardedCards;

    @Override
    public TurnResponseType getResponseType() {
        return TurnResponseType.AUTO_PICK_DISCARD_CARDS;
    }

}
