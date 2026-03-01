package com.terraforming.ares.services.policyai;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.TableContext;

public class ResourceHelper {

    public static int countAvailableAnimals(TableContext context) {
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.ANIMAL) {
                count++;
            }
        }
        return count;
    }

    public static int countPlayedMicrobeCards(TableContext context) {
        Player player = context.getPlayer();
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    public static int countCardsWithAtLeastOneMicrobe(TableContext context) {
        Player player = context.getPlayer();
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE && player.getCardResourcesCount().getOrDefault(card.getClass(), 0) > 0) {
                count++;
            }
        }
        return count;
    }

    public static int countAvailableAnimalsAndMicrobes(TableContext context) {
        Player player = context.getPlayer();
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    public static int countAnyAvailableCollectableResources(TableContext context) {
        Player player = context.getPlayer();
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.SCIENCE) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
        }
        return count;
    }

}
