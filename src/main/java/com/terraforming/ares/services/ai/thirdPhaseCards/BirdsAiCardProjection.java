package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.Birds;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BirdsAiCardProjection<T extends Card> implements AiCardProjection<Birds> {

    @Override
    public Class<Birds> getType() {
        return Birds.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference diff, MarsGame game, Player player, Card card, int network) {
        player.getCardResourcesCount().put(Birds.class, player.getCardResourcesCount().get(Birds.class) + 1);
        return new MarsGameRowDifference();
    }
}
