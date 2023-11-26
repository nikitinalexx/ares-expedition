package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class CelestiorAiCardProjection<T extends Card> implements AiCardProjection<CelestiorCorporation> {
    @Override
    public Class<CelestiorCorporation> getType() {
        return CelestiorCorporation.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder()
                .redCards(3f * Constants.RED_CARDS_RATIO)
                .build();
    }
}
