package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.ProjectCard;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface RedCard extends ProjectCard {

    @Override
    default CardColor getColor() {
        return CardColor.RED;
    }

    @Override
    default boolean isActiveCard() {
        return false;
    }

}
