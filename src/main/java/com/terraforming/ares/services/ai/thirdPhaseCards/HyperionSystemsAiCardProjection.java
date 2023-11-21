package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class HyperionSystemsAiCardProjection<T extends Card> implements AiCardProjection<HyperionSystemsCorporation> {
    @Override
    public Class<HyperionSystemsCorporation> getType() {
        return HyperionSystemsCorporation.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        player.setMc(player.getMc() + (player.getChosenPhase() == Constants.PERFORM_BLUE_ACTION_PHASE ? 2 : 1));

        return new MarsGameRowDifference();
    }
}
