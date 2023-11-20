package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.Tardigrades;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TardigradesAiCardProjection<T extends Card> implements AiCardProjection<Tardigrades> {

    @Override
    public Class<Tardigrades> getType() {
        return Tardigrades.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference diff, MarsGame game, Player player, Card card) {
        player.getCardResourcesCount().put(Tardigrades.class, player.getCardResourcesCount().get(Tardigrades.class) + 1);
        return new MarsGameRowDifference();
    }
}
