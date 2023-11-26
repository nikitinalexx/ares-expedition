package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.FarmingCoops;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class FarmingCoopsAiCardProjection<T extends Card> implements AiCardProjection<FarmingCoops> {
    @Override
    public Class<FarmingCoops> getType() {
        return FarmingCoops.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        //todo TEST rounding
        int cardsAvailable = player.getHand().size() + (int) (initialDifference.getBlueCards() + initialDifference.getRedCards() + initialDifference.getGreenCards());

        if (cardsAvailable < 1) {
            return new MarsGameRowDifference();
        }

        player.setPlants(player.getPlants() + 3);

        return MarsGameRowDifference.builder()
                .greenCards(-Constants.GREEN_CARDS_RATIO)
                .redCards(-Constants.RED_CARDS_RATIO)
                .blueCards(-Constants.BLUE_CARDS_RATIO)
                .build();
    }
}
