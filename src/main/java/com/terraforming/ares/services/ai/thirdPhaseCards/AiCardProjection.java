package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;

/**
 * Created by oleksii.nikitin
 * Creation date 24.03.2023
 */
public interface AiCardProjection<T extends Card> {

    Class<T> getType();

    MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network);


}
