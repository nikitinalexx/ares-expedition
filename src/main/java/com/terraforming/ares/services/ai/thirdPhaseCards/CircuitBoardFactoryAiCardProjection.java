package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CircuitBoardFactory;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
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
                .cards(1)
                .build();
    }
}
