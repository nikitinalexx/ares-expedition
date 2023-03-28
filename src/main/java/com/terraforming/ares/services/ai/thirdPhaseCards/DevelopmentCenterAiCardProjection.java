package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.DevelopmentCenter;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevelopmentCenterAiCardProjection<T extends Card> implements AiCardProjection<DevelopmentCenter> {

    @Override
    public Class<DevelopmentCenter> getType() {
        return DevelopmentCenter.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getHeat() >= 2) {
            player.setHeat(player.getHeat() - 2);
            return MarsGameRowDifference.builder()
                    .greenCards(Constants.GREEN_CARDS_RATIO)
                    .redCards(Constants.RED_CARDS_RATIO)
                    .blueCards(Constants.BLUE_CARDS_RATIO)
                    .build();
        }

        return new MarsGameRowDifference();
    }
}
