package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.AdvancedScreeningTechnology;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class AdvancedScreeningTechAiCardProjection<T extends Card> implements AiCardProjection<AdvancedScreeningTechnology> {
    @Override
    public Class<AdvancedScreeningTechnology> getType() {
        return AdvancedScreeningTechnology.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        //10 green  cards
        //4 red
        //17 blue

        //plants
        //19 green
        //2 red
        //8 blue

        return MarsGameRowDifference.builder()
                .greenCards(7f / 216)
                .redCards(188f / 216)
                .blueCards(75f / 216)
                .build();
    }
}
