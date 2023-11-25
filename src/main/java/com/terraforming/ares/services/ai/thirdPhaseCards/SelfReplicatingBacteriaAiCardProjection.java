package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelfReplicatingBacteriaAiCardProjection<T extends Card> implements AiCardProjection<SelfReplicatingBacteria> {
    private final CardResourceService cardResourceService = new CardResourceService();

    @Override
    public Class<SelfReplicatingBacteria> getType() {
        return SelfReplicatingBacteria.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference diff, MarsGame game, Player player, Card card) {
        //todo project building a card
//        player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
        cardResourceService.addResources(player,SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
        return new MarsGameRowDifference();
    }
}
