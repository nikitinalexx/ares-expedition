package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.AiCentral;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class AiCentralAiCardProjection<T extends Card> implements AiCardProjection<AiCentral> {
    @Override
    public Class<AiCentral> getType() {
        return AiCentral.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder()
                .greenCards(2f * Constants.GREEN_CARDS_RATIO)
                .redCards(2f * Constants.RED_CARDS_RATIO)
                .blueCards(2f * Constants.BLUE_CARDS_RATIO)
                .build();
    }
}
