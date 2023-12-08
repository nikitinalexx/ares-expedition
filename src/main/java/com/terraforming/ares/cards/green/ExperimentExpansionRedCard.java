package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.red.RedCard;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;

/**
 * Created by oleksii.nikitin
 * Creation date 06.12.2023
 */
public interface ExperimentExpansionRedCard extends RedCard {

    @Override
    default Expansion getExpansion() {
        return Expansion.EXPERIMENTAL;
    }

}
