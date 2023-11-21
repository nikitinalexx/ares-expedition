package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.DroneAssistedConstruction;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class DroneAssistedConstructionAiCardProjection<T extends Card> implements AiCardProjection<DroneAssistedConstruction> {
    @Override
    public Class<DroneAssistedConstruction> getType() {
        return DroneAssistedConstruction.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        player.setMc(player.getMc() + (player.getChosenPhase() == Constants.PERFORM_BLUE_ACTION_PHASE ? 4 : 2));

        return new MarsGameRowDifference();
    }
}
