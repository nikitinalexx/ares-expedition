package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.ProjectCard;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface GreenCard extends ProjectCard {

    @Override
    default CardColor getColor() {
        return CardColor.GREEN;
    }

}
