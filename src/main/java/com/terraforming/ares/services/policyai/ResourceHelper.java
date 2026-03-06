package com.terraforming.ares.services.policyai;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.TableContext;

import java.util.ArrayList;
import java.util.List;

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

    public static int countCardsWithAtLeastOneMicrobeOrAnimal(TableContext context) {
        Player player = context.getPlayer();
        int count = 0;
        for (Card card : context.getPlayedCards()) {
            if ((card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL) && player.getCardResourcesCount().getOrDefault(card.getClass(), 0) > 0) {
                count++;
            }
        }
        return count;
    }

    public static List<Card> getCardsWithAtLeastOneMicrobeOrAnimal(TableContext context) {
        Player player = context.getPlayer();
        List<Card> result = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if ((card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL) && player.getCardResourcesCount().getOrDefault(card.getClass(), 0) > 0) {
                result.add(card);
            }
        }
        return result;
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

    public static List<Card> getAnyAvailableCollectableResources(TableContext context) {
        Player player = context.getPlayer();
        List<Card> collectingCards = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.SCIENCE) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        collectingCards.add(card);
                    }
                } else {
                    collectingCards.add(card);
                }
            }
        }
        return collectingCards;
    }

    public static List<Card> getAnyAvailableAnimalsAndMicrobes(TableContext context) {
        Player player = context.getPlayer();
        List<Card> collectingCards = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE || card.getCollectableResource() == CardCollectableResource.ANIMAL) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        collectingCards.add(card);
                    }
                } else {
                    collectingCards.add(card);
                }
            }
        }
        return collectingCards;
    }

    public static List<Card> getAnyAvailableMicrobes(TableContext context) {
        Player player = context.getPlayer();
        List<Card> collectingCards = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.MICROBE) {
                if (card.getClass() == BacterialAggregates.class) {
                    if (player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5) {
                        collectingCards.add(card);
                    }
                } else {
                    collectingCards.add(card);
                }
            }
        }
        return collectingCards;
    }

    public static List<Card> getAnyAvailableAnimals(TableContext context) {
        List<Card> collectingCards = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if (card.getCollectableResource() == CardCollectableResource.ANIMAL) {
                collectingCards.add(card);
            }
        }
        return collectingCards;
    }

    public static List<Card> getBuiltRedCards(TableContext context) {
        List<Card> redCards = new ArrayList<>();
        for (Card card : context.getPlayedCards()) {
            if (card.getColor() == CardColor.RED) {
                redCards.add(card);
            }
        }
        return redCards;
    }

}
