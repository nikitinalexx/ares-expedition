package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.ProjectCard;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface BlueCard extends ProjectCard {

    @Override
    default CardColor getColor() {
        return CardColor.BLUE;
    }

}
