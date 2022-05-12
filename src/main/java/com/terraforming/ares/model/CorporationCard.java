package com.terraforming.ares.model;


/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
public interface CorporationCard extends GenericCard{

    @Override
    default int getPrice() {
        return 0;
    }

    @Override
    default CardColor getColor() {
        return CardColor.CORPORATION;
    }
}
