package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface BaseExpansionRedCard extends RedCard {

    @Override
    default CardColor getColor() {
        return CardColor.RED;
    }

    @Override
    default boolean isActiveCard() {
        return false;
    }

    @Override
    default Expansion getExpansion() {
        return Expansion.BASE;
    }

}
