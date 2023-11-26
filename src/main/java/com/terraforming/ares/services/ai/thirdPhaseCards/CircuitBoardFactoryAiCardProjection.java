package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CircuitBoardFactory;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class CircuitBoardFactoryAiCardProjection<T extends Card> implements AiCardProjection<CircuitBoardFactory> {
    @Override
    public Class<CircuitBoardFactory> getType() {
        return CircuitBoardFactory.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder()
                .greenCards(Constants.GREEN_CARDS_RATIO)
                .redCards(Constants.RED_CARDS_RATIO)
                .blueCards(Constants.BLUE_CARDS_RATIO)
                .build();
    }
}
