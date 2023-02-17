package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.red.RedCard;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
public interface DiscoveryExpansionGreenCard extends RedCard {

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
        return Expansion.DISCOVERY;
    }

}
