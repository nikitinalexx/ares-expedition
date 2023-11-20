package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.dto.PhaseChoiceProjection;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 24.03.2023
 */
public interface AiCardProjection<T extends Card> {

    Class<T> getType();

    MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card);


}
