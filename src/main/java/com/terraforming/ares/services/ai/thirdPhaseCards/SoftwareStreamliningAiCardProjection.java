package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.SoftwareStreamlining;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class SoftwareStreamliningAiCardProjection<T extends Card> implements AiCardProjection<SoftwareStreamlining> {

    @Override
    public Class<SoftwareStreamlining> getType() {
        return SoftwareStreamlining.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder()
                .mc(-6)//let's assume that 2 cards it discards are bad, their only value is the money behind them
                .cards(2)
                .build();
    }

}
