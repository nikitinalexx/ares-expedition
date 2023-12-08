package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.blue.BlueCard;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;

/**
 * Created by oleksii.nikitin
 * Creation date 06.12.2023
 */
public interface ExperimentExpansionBlueCard extends BlueCard {

    @Override
    default Expansion getExpansion() {
        return Expansion.EXPERIMENTAL;
    }

}
