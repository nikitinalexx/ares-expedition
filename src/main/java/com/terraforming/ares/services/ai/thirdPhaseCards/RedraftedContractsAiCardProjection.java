package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.RedraftedContracts;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedraftedContractsAiCardProjection<T extends Card> implements AiCardProjection<RedraftedContracts> {

    @Override
    public Class<RedraftedContracts> getType() {
        return RedraftedContracts.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder().cards(1).build();
    }
}
