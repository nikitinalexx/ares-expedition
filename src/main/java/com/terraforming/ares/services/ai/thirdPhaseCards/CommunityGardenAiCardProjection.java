package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CommunityGardens;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class CommunityGardenAiCardProjection<T extends Card> implements AiCardProjection<CommunityGardens> {
    @Override
    public Class<CommunityGardens> getType() {
        return CommunityGardens.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        player.setMc(player.getMc() + 2);
        player.setPlants(player.getPlants() + 1);

        return new MarsGameRowDifference();
    }
}
