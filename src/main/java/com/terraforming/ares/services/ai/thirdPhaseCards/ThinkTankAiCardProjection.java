package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.ThinkTank;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class ThinkTankAiCardProjection<T extends Card> implements AiCardProjection<ThinkTank> {
    @Override
    public Class<ThinkTank> getType() {
        return ThinkTank.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getMc() < 2) {
            return new MarsGameRowDifference();
        }

        player.setMc(player.getMc() - 2);

        return MarsGameRowDifference.builder()
                .cards(1)
                .build();
    }
}
