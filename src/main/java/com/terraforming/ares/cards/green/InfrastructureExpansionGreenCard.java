package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
public interface InfrastructureExpansionGreenCard extends GreenCard {

    @Override
    default CardColor getColor() {
        return CardColor.GREEN;
    }

    @Override
    default boolean isActiveCard() {
        return false;
    }

    @Override
    default Expansion getExpansion() {
        return Expansion.INFRASTRUCTURE;
    }

}
