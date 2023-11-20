package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.FarmersMarket;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class FarmersMarketAiCardProjection<T extends Card> implements AiCardProjection<FarmersMarket> {
    @Override
    public Class<FarmersMarket> getType() {
        return FarmersMarket.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getMc() > 0) {
            player.setMc(player.getMc() - 1);
            player.setPlants(player.getPlants() + 2);
        }

        return new MarsGameRowDifference();
    }
}
