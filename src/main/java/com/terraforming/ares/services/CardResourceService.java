package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.BuffedFilterFeeders;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.03.2023
 */
@Service
public class CardResourceService {

    public void addResources(Player player, Card toCard, int count) {
        final Map<Class<?>, Integer> cardResourcesCount = player.getCardResourcesCount();

        if (toCard.getClass() == BacterialAggregates.class && cardResourcesCount.get(BacterialAggregates.class) + count > 5) {
            count = 5 - cardResourcesCount.get(BacterialAggregates.class);
        }

        cardResourcesCount.put(
                toCard.getClass(),
                cardResourcesCount.get(toCard.getClass()) + count
        );
        if (count > 0 && toCard.getCollectableResource() == CardCollectableResource.MICROBE && (cardResourcesCount.containsKey(FilterFeeders.class) || cardResourcesCount.containsKey(BuffedFilterFeeders.class))) {
            Class<?> resourceCard = (cardResourcesCount.containsKey(FilterFeeders.class) ? FilterFeeders.class : BuffedFilterFeeders.class);
            cardResourcesCount.put(resourceCard, cardResourcesCount.get(resourceCard) + 1);
        }
    }

}
