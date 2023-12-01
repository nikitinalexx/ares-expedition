package com.terraforming.ares.services.ai.helpers;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.dto.ResourceValue;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terraforming.ares.services.ai.dto.ResourceValue.*;

@Service
public class ResourcePriorityService {
    private final CardService cardService;
    private final Random random = new Random();

    private final Map<Class<?>, ResourceValue> RESOURCE_VALUES;
    private final Map<Class<?>, Integer> CRITICAL_RESOURCE_VALUES_ON_CARDS;
    private final Set<Class<?>> VALUABLE_RESOURCE_CARDS = Set.of(
            Birds.class, Fish.class, Livestock.class, Zoos.class, BacterialAggregates.class
    );

    public ResourcePriorityService(CardService cardService) {
        this.cardService = cardService;

        RESOURCE_VALUES = new HashMap<>();
        //microbes
        RESOURCE_VALUES.put(Tardigrades.class, MIN);             // 1/3 vp
        RESOURCE_VALUES.put(Decomposers.class, MIN);              // 1/3 vp
        RESOURCE_VALUES.put(GhgProductionBacteria.class, MIN);// 1/3 vp
        RESOURCE_VALUES.put(NitriteReductingBacteria.class, MIN);// 1/3 vp
        RESOURCE_VALUES.put(RegolithEaters.class, MIN);// 1/3 vp
        RESOURCE_VALUES.put(FibrousCompositeMaterial.class, MIN); // 1/3 vp
        RESOURCE_VALUES.put(AnaerobicMicroorganisms.class, MEDIUM);// 1/2 vp
        RESOURCE_VALUES.put(DecomposingFungus.class, MEDIUM); // 1/2 vp
        RESOURCE_VALUES.put(SelfReplicatingBacteria.class, MEDIUM);// 1/2 vp
        RESOURCE_VALUES.put(BacterialAggregates.class, MAX);// affects 5th phase

        //animals
        RESOURCE_VALUES.put(FilterFeeders.class, MIN);// 1/3vp
        RESOURCE_VALUES.put(ArclightCorporation.class, MEDIUM);// 1/2vp
        RESOURCE_VALUES.put(BuffedArclightCorporation.class, MEDIUM);// 1/2vp
        RESOURCE_VALUES.put(EcologicalZone.class, MEDIUM);// 1/2vp
        RESOURCE_VALUES.put(SmallAnimals.class, MEDIUM);// 1/2vp
        RESOURCE_VALUES.put(Herbivores.class, MEDIUM);// 1/2vp
        RESOURCE_VALUES.put(Birds.class, MAX);// 1vp
        RESOURCE_VALUES.put(Fish.class, MAX);// 1vp
        RESOURCE_VALUES.put(Livestock.class, MAX);// 1vp
        RESOURCE_VALUES.put(Zoos.class, MAX);// 1vp

        RESOURCE_VALUES.put(PhysicsComplex.class, MEDIUM);// 1/2vp

        CRITICAL_RESOURCE_VALUES_ON_CARDS = new HashMap<>();
        //microbes
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Tardigrades.class, 3);             // 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Decomposers.class, Integer.MAX_VALUE);              // 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(GhgProductionBacteria.class, 2);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(NitriteReductingBacteria.class, 3);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(RegolithEaters.class, 2);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(FibrousCompositeMaterial.class, 3);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(AnaerobicMicroorganisms.class, 2);// 1/2 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(DecomposingFungus.class, Integer.MAX_VALUE); // 1/2 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(SelfReplicatingBacteria.class, 5);// 1/2 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(BacterialAggregates.class, Integer.MAX_VALUE);// 1/2 vp

        //animals
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(FilterFeeders.class, 3);// 1/3vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(ArclightCorporation.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(BuffedArclightCorporation.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(EcologicalZone.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(SmallAnimals.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Herbivores.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(PhysicsComplex.class, 2);// 1/2vp
    }

    public boolean isValuableResourceCard(Card card) {
        return VALUABLE_RESOURCE_CARDS.contains(card.getClass());
    }

    public ResourceValue getCardValue(Card card) {
        return RESOURCE_VALUES.get(card.getClass());
    }

    public Optional<Integer> getMostValuableResourceCard(MarsGame game, Player player, Set<CardCollectableResource> resourceTypes) {
        //TODO should take into account that it may be putting 3 microbes and 2 animals
        Stream<Card> animalMicrobeCardsStream = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> resourceTypes.contains(card.getCollectableResource()));

        animalMicrobeCardsStream = filterCardsByGlobalParametersAndAward(game, animalMicrobeCardsStream);

        Map<ResourceValue, List<Card>> cardsByPriorities = animalMicrobeCardsStream
                .collect(Collectors.groupingBy(this::getCardValue));

        if (cardsByPriorities.containsKey(ResourceValue.MAX)) {
            List<Card> bestCards = cardsByPriorities.get(ResourceValue.MAX);
            return Optional.of(bestCards.get(random.nextInt(bestCards.size())).getId());
        }

        for (ResourceValue rv : List.of(ResourceValue.MEDIUM, ResourceValue.MIN)) {
            Card card = getCriticalCardByPriority(cardsByPriorities.get(rv), player);
            if (card != null) {
                return Optional.of(card.getId());
            }
        }

        for (ResourceValue rv : List.of(ResourceValue.MEDIUM, ResourceValue.MIN)) {
            List<Card> cards = cardsByPriorities.get(rv);
            if (cards != null && !cards.isEmpty()) {
                return Optional.of(cards.get(0).getId());//todo prefer cards with discount?
            }
        }

        return Optional.empty();
    }

    public boolean hasProfitableCardWithCollectableResource(MarsGame game, Player player, Set<CardCollectableResource> resources) {
        Stream<Card> cardStream = player.getPlayed().getCards().stream()
                .map(cardService::getCard);

        cardStream = filterCardsByGlobalParametersAndAward(game, cardStream);

        return cardStream.anyMatch(card -> resources.contains(card.getCollectableResource()));
    }

    private Stream<Card> filterCardsByGlobalParametersAndAward(MarsGame game, Stream<Card> cardStream) {
        if (game.getAwards().stream().noneMatch(award -> award.getType() == AwardType.COLLECTOR)) {
            if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                cardStream = cardStream.filter(card -> card.getClass() != NitriteReductingBacteria.class);
            }

            if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
                cardStream = cardStream.filter(card -> card.getClass() != GhgProductionBacteria.class);
            }

            if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                cardStream = cardStream.filter(card -> card.getClass() != RegolithEaters.class);
            }
        }

        return cardStream;
    }

    public boolean hasCardWithCheapAnimalOrMicrobe(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> !this.isValuableResourceCard(card))
                .anyMatch(card -> player.getCardResourcesCount().get(card.getClass()) > 0);
    }

    public Integer getDecomposingFungusDiscardResourceCard(MarsGame game, Player player) {
        List<Card> animalMicrobeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> player.getCardResourcesCount().get(card.getClass()) > 0)
                .collect(Collectors.toList());

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            final Optional<Card> nitriteReductinBacteria = animalMicrobeCards.stream().filter(card -> card.getClass() == NitriteReductingBacteria.class).findFirst();
            if (nitriteReductinBacteria.isPresent()) {
                return nitriteReductinBacteria.get().getId();
            }
        }

        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            final Optional<Card> ghgProductionBacteria = animalMicrobeCards.stream().filter(card -> card.getClass() == GhgProductionBacteria.class).findFirst();
            if (ghgProductionBacteria.isPresent()) {
                return ghgProductionBacteria.get().getId();
            }
        }

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            final Optional<Card> regolithEaters = animalMicrobeCards.stream().filter(card -> card.getClass() == RegolithEaters.class).findFirst();
            if (regolithEaters.isPresent()) {
                return regolithEaters.get().getId();
            }
        }

        Map<ResourceValue, List<Card>> cardsByPriorities = animalMicrobeCards
                .stream()
                .collect(Collectors.groupingBy(card -> RESOURCE_VALUES.get(card.getClass())));

        for (ResourceValue rv: List.of(MIN, MEDIUM)) {
            Card nonCriticalCard = getNonCriticalCardByPriority(cardsByPriorities.get(rv), player);
            if (nonCriticalCard != null) {
                return nonCriticalCard.getId();
            }
        }

        return null;
    }


    private Card getNonCriticalCardByPriority(List<Card> cards, Player player) {
        if (cards == null || cards.isEmpty()) {
            return null;
        }
        for (Card card : cards) {
            if (player.getCardResourcesCount().get(card.getClass()) % CRITICAL_RESOURCE_VALUES_ON_CARDS.get(card.getClass()) != 0) {
                return card;
            }
        }
        return null;
    }

    private Card getCriticalCardByPriority(List<Card> cards, Player player) {
        if (cards == null || cards.isEmpty()) {
            return null;
        }
        for (Card card : cards) {
            if ((player.getCardResourcesCount().get(card.getClass()) + 1) % CRITICAL_RESOURCE_VALUES_ON_CARDS.get(card.getClass()) == 0) {
                return card;
            }
        }
        return null;
    }

    private Optional<Card> getRandomMicrobeCard(Player player) {
        final List<Card> microbeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.MICROBE)
                .collect(Collectors.toList());
        if (microbeCards.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(microbeCards.get(random.nextInt(microbeCards.size())));
        }
    }

}
