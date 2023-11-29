package com.terraforming.ares.services.ai.helpers;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Service
@RequiredArgsConstructor
public class AiCardBuildParamsService {
    private final Random random = new Random();
    private final CardService cardService;
    private final ICardValueService cardValueService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;

    private AiPickCardProjectionService aiPickCardProjectionService;

    @Lazy
    @Autowired
    public void setAiPickCardProjectionService(AiPickCardProjectionService aiPickCardProjectionService) {
        this.aiPickCardProjectionService = aiPickCardProjectionService;
    }

    private final Set<CardAction> CARDS_WITH_PHASE_UPGRADE_EFFECT = Set.of(
            CardAction.UPDATE_PHASE_CARD,
            CardAction.SOFTWARE_STREAMLINING
    );

    public Map<Integer, List<Integer>> getInputParamsForValidation(MarsGame game, Player player, Card card) {
        Map<Integer, List<Integer>> result = null;
        CardAction cardAction = card.getCardMetadata().getCardAction();

        if (cardAction == CardAction.ASTROFARM) {
            result = Map.of(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        if (cardAction == CardAction.EOS_CHASMA) {
            result = Map.of(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        if (!card.getCardMetadata().getResourcesOnBuild().isEmpty()
                && card.getCardMetadata().getResourcesOnBuild().get(0).getType() == CardCollectableResource.ANY) {
            List<Card> playerCardsWithAnyResource = getPlayerCardsWithAnyResource(player);
            if (!playerCardsWithAnyResource.isEmpty()) {
                result = Map.of(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(), List.of(playerCardsWithAnyResource.get(0).getId()));
            }
        }

        if (cardAction == CardAction.IMPORTED_HYDROGEN) {
            result = Map.of(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
        }

        if (cardAction == CardAction.IMPORTED_NITROGEN) {
            result = Map.of(
                    InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(), List.of(InputFlag.SKIP_ACTION.getId()),
                    InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(), List.of(InputFlag.SKIP_ACTION.getId())
            );
        }

        if (cardAction == CardAction.LARGE_CONVOY) {
            result = Map.of(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
        }

        if (cardAction == CardAction.LOCAL_HEAT_TRAPPING) {
            result = Map.of(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        if (cardAction == CardAction.CRYOGENIC_SHIPMENT) {
            result = Map.of(
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0),
                    InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId())
            );
        }

        if (cardAction == CardAction.UPDATE_PHASE_CARD_TWICE) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0, 2));
        }

        if (cardAction == CardAction.TOPOGRAPHIC_MAPPING) {
            result = Map.of(
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0),
                    InputFlag.TAG_INPUT.getId(), List.of(Tag.ANIMAL.ordinal())
            );
        }

        if (cardAction == CardAction.UPDATE_PHASE_2_CARD) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(2));
        }

        if (cardAction == CardAction.CHOOSE_TAG) {
            result = Map.of(InputFlag.TAG_INPUT.getId(), List.of(Tag.ANIMAL.ordinal()));
        }

        if (cardAction == CardAction.UPDATE_PHASE_1_CARD) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0));
        }

        if (cardAction == CardAction.UPDATE_PHASE_4_CARD) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(6));
        }

        if (cardAction == CardAction.COMMUNICATIONS_STREAMLINING) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(4));
        }

        if (cardAction == CardAction.BIOMEDICAL_IMPORTS) {
            result = Map.of(
                    InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId(), List.of(-1),
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0)
            );
        }

        if (cardAction != null && CARDS_WITH_PHASE_UPGRADE_EFFECT.contains(cardAction)) {
            result = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(0));
        }

        List<Card> playedCards = player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard).collect(Collectors.toList());

        if (cardAction == CardAction.SYNTHETIC_CATASTROPHE) {
            Optional<Card> redCard = playedCards.stream()
                    .filter(c -> c.getColor() == CardColor.RED)
                    .findFirst();
            if (redCard.isPresent()) {
                result = Map.of(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), List.of(redCard.get().getId()));
            }
        }

        if (cardActionTriggered(playedCards, card, CardAction.DECOMPOSERS)) {
            int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), result);

            if (tagsCount != 0) {
                if (result == null) {
                    result = new HashMap<>();
                } else {
                    result = new HashMap<>(result);
                }
                result.put(DECOMPOSERS_TAKE_MICROBE.getId(), List.of(tagsCount));
            }
        }

        if (cardActionTriggered(playedCards, card, CardAction.MARS_UNIVERSITY)) {
            int scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), result);

            if (scienceTagsCount != 0) {
                if (result == null) {
                    result = new HashMap<>();
                } else {
                    result = new HashMap<>(result);
                }
                result.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            }
        }

        if (cardActionTriggered(playedCards, card, CardAction.VIRAL_ENHANCERS)) {
            int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), result);

            if (tagsCount != 0) {
                if (result == null) {
                    result = new HashMap<>();
                } else {
                    result = new HashMap<>(result);
                }
                result.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(tagsCount));
            }
        }

        return result;
    }

    public Map<Integer, List<Integer>> getInputParamsForBuild(MarsGame game, Player player, Card card) {
        return getInputParamsForBuild(game, player, card, false);
    }

    public Map<Integer, List<Integer>> getInputParamsForBuild(MarsGame game, Player player, Card card, boolean ignoreMarsUniversity) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        CardAction cardAction = card.getCardMetadata().getCardAction();

        if (cardAction == CardAction.CRYOGENIC_SHIPMENT) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player)));

            List<Card> playerCardsWithAnimalsAndMicrobes = getPlayerCardsWithAnimalsAndMicrobes(player);

            if (CollectionUtils.isEmpty(playerCardsWithAnimalsAndMicrobes)) {
                result.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            } else {
                result.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(playerCardsWithAnimalsAndMicrobes.get(random.nextInt(playerCardsWithAnimalsAndMicrobes.size())).getId()));
            }
        }

        if (cardAction == CardAction.UPDATE_PHASE_CARD_TWICE) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), aiDiscoveryDecisionService.chooseTwoPhaseUpgrades(game, player));
        }

        if (cardAction == CardAction.TOPOGRAPHIC_MAPPING) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player)));
            result.put(InputFlag.TAG_INPUT.getId(), List.of(aiDiscoveryDecisionService.chooseDynamicTagValue(player, List.of())));
        }

        if (cardAction == CardAction.UPDATE_PHASE_2_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.BUILD_BLUE_RED_PROJECTS_PHASE)));
        }

        if (cardAction == CardAction.CHOOSE_TAG) {
            result.put(InputFlag.TAG_INPUT.getId(), List.of(aiDiscoveryDecisionService.chooseDynamicTagValue(player, List.of())));
        }

        if (cardAction == CardAction.UPDATE_PHASE_1_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.BUILD_GREEN_PROJECTS_PHASE)));
        }

        if (cardAction == CardAction.COMMUNICATIONS_STREAMLINING) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.PERFORM_BLUE_ACTION_PHASE)));
        }

        if (cardAction == CardAction.UPDATE_PHASE_4_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.COLLECT_INCOME_PHASE)));
        }

        if (cardAction != null && CARDS_WITH_PHASE_UPGRADE_EFFECT.contains(cardAction)) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player)));
        }

        if (cardAction == CardAction.BIOMEDICAL_IMPORTS) {
            //TODO expansion project double action
            result.put(InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId(), List.of(-1));
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player)));
        }

        if (cardAction == CardAction.ASTROFARM) {
            List<Card> playerCardsWithMicrobes = getPlayerCardsWithMicrobe(player);
            result.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), getRandomCardInput(playerCardsWithMicrobes));
        }

        if (cardAction == CardAction.EOS_CHASMA) {
            List<Card> playerCardsWithAnimal = getPlayerCardsWithAnimals(player);
            result.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), getRandomCardInput(playerCardsWithAnimal));
        }

        if (!card.getCardMetadata().getResourcesOnBuild().isEmpty()
                && card.getCardMetadata().getResourcesOnBuild().get(0).getType() == CardCollectableResource.ANY) {
            List<Card> playerCardsWithAnyResource = getPlayerCardsWithAnyResource(player);
            result.put(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(), getRandomCardInput(playerCardsWithAnyResource));
        }

        if (cardAction == CardAction.IMPORTED_HYDROGEN) {
            List<Card> playerCardsWithAnimalsAndMicrobes = getPlayerCardsWithAnimalsAndMicrobes(player);

            int chosenOption = random.nextInt(playerCardsWithAnimalsAndMicrobes.size() + 1);
            if (chosenOption == 0) {//plants
                result.put(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
            } else {
                result.put(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId(), List.of(playerCardsWithAnimalsAndMicrobes.get(chosenOption - 1).getId()));
            }
        }

        if (cardAction == CardAction.IMPORTED_NITROGEN) {
            List<Card> playerCardsWithAnimals = getPlayerCardsWithAnimals(player);
            List<Card> playerCardsWithMicrobes = getPlayerCardsWithMicrobe(player);

            result.put(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(), getRandomCardInput(playerCardsWithAnimals));
            result.put(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(), getRandomCardInput(playerCardsWithMicrobes));
        }

        if (cardAction == CardAction.LARGE_CONVOY) {
            List<Card> playerCardsWithAnimals = getPlayerCardsWithAnimals(player);

            int chosenOption = random.nextInt(playerCardsWithAnimals.size() + 1);
            if (chosenOption == 0) {//plants
                result.put(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
            } else {
                result.put(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId(), List.of(playerCardsWithAnimals.get(chosenOption - 1).getId()));
            }
        }

        if (cardAction == CardAction.LOCAL_HEAT_TRAPPING) {
            List<Card> playerCardsWithAnimalsAndMicrobes = getPlayerCardsWithAnimalsAndMicrobes(player);

            result.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), getRandomCardInput(playerCardsWithAnimalsAndMicrobes));
        }

        List<Card> playedCards = player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard).collect(Collectors.toList());

        if (cardAction == CardAction.SYNTHETIC_CATASTROPHE) {
            List<Card> playedRedCards = playedCards.stream().filter(c -> c.getColor() == CardColor.RED)
                    .collect(Collectors.toList());
            //TODO take back the best card
            result.put(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), getRandomCardInput(playedRedCards));
        }

        result.putAll(getActiveInputFromCards(game, player, playedCards, card, result, ignoreMarsUniversity));

        return result;
    }

    public Map<Integer, List<Integer>> getActiveInputFromCards(MarsGame game, Player player, List<Card> playedCards, Card card, Map<Integer, List<Integer>> input, boolean ignoreMarsUniversity) {
        Map<Integer, List<Integer>> decomposersInputIfApplicable = getDecomposersInputIfApplicable(player, playedCards, card, input);
        Map<Integer, List<Integer>> viralEnhancersInputIfApplicable = getViralEnhancersInputIfApplicable(playedCards, card, input);

        Map<Integer, List<Integer>> result = new HashMap<>();
        result.putAll(decomposersInputIfApplicable);
        result.putAll(viralEnhancersInputIfApplicable);

        if (!ignoreMarsUniversity) {
            Map<Integer, List<Integer>> marsUniversityInputIfApplicable = getMarsUniversityInputIfApplicable(game, player, playedCards, card, input);
            result.putAll(marsUniversityInputIfApplicable);
        } else {
            result.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        return result;
    }

    private Map<Integer, List<Integer>> getDecomposersInputIfApplicable(Player player, List<Card> playedCards, Card card, Map<Integer, List<Integer>> input) {
        if (cardActionTriggered(playedCards, card, CardAction.DECOMPOSERS)) {
            int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);

            if (tagsCount != 0) {
                int takeMicrobes = 0;
                int takeCards = 0;
                int microbesLeftOnCard = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0);
                for (int i = 0; i < tagsCount; i++) {
                    if (microbesLeftOnCard > 0) {
                        takeCards++;
                        microbesLeftOnCard--;
                    } else {
                        takeMicrobes++;
                        microbesLeftOnCard++;
                    }
                }
                return Map.of(
                        DECOMPOSERS_TAKE_MICROBE.getId(), List.of(takeMicrobes),
                        DECOMPOSERS_TAKE_CARD.getId(), List.of(takeCards)
                );
            }
        }
        return Map.of();
    }

    private Map<Integer, List<Integer>> getViralEnhancersInputIfApplicable(List<Card> playedCards, Card card, Map<Integer, List<Integer>> input) {
        if (cardActionTriggered(playedCards, card, CardAction.VIRAL_ENHANCERS)) {
            int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);

            if (tagsCount != 0) {
                List<Card> playerCardsWithPlayed = new ArrayList<>(playedCards);
                playerCardsWithPlayed.add(card);

                List<Card> playerCardsWithAnimals = getCardsWithCollectable(playerCardsWithPlayed, CardCollectableResource.ANIMAL);
                if (!playerCardsWithAnimals.isEmpty()) {
                    Integer chosenCard = getRandomCards(playerCardsWithAnimals, 1).get(0);
                    return Map.of(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), tagsCount == 1 ? List.of(chosenCard) : List.of(chosenCard, chosenCard));
                } else {
                    List<Card> playerCardsWithMicrobes = getCardsWithCollectable(playerCardsWithPlayed, CardCollectableResource.MICROBE);
                    if (!playerCardsWithMicrobes.isEmpty()) {
                        Integer chosenCard = getRandomCards(playerCardsWithMicrobes, 1).get(0);
                        return Map.of(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), tagsCount == 1 ? List.of(chosenCard) : List.of(chosenCard, chosenCard));
                    } else {
                        return Map.of(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(tagsCount));
                    }
                }
            }
        }
        return Map.of();
    }

    private Map<Integer, List<Integer>> getMarsUniversityInputIfApplicable(MarsGame game, Player player, List<Card> playedCards, Card card, Map<Integer, List<Integer>> input) {
        if (cardActionTriggered(playedCards, card, CardAction.MARS_UNIVERSITY)) {
            int scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);


            if (scienceTagsCount != 0) {
                List<Card> handCards = player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toList());


                return Map.of(InputFlag.MARS_UNIVERSITY_CARD.getId(), getMarsUniversityInput(game, player.getUuid(), card, handCards, scienceTagsCount));
            }
        }
        return Map.of();
    }


    private List<Integer> getRandomCardInput(List<Card> cards) {
        if (cards.isEmpty()) {
            return List.of(InputFlag.SKIP_ACTION.getId());
        }
        return List.of(
                cards.size() == 1
                        ? cards.get(0).getId()
                        : cards.get(random.nextInt(cards.size())).getId()
        );
    }

    private List<Card> getPlayerCardsWithMicrobe(Player player) {
        return getPlayerCardsWithCollectable(player, CardCollectableResource.MICROBE);
    }

    private List<Card> getPlayerCardsWithAnimals(Player player) {
        return getPlayerCardsWithCollectable(player, CardCollectableResource.ANIMAL);
    }

    private List<Card> getPlayerCardsWithAnimalsAndMicrobes(Player player) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL
                        || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .collect(Collectors.toList());
    }

    private List<Card> getPlayerCardsWithAnyResource(Player player) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL
                        || card.getCollectableResource() == CardCollectableResource.MICROBE
                        || card.getCollectableResource() == CardCollectableResource.SCIENCE)
                .collect(Collectors.toList());
    }

    private List<Card> getPlayerCardsWithCollectable(Player player, CardCollectableResource resource) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == resource)
                .collect(Collectors.toList());
    }

    private List<Card> getCardsWithCollectable(List<Card> cards, CardCollectableResource resource) {
        return cards.stream().filter(card -> card.getCollectableResource() == resource).collect(Collectors.toList());
    }

    private boolean cardActionTriggered(List<Card> playedCards, Card card, CardAction cardAction) {
        return card.getCardMetadata().getCardAction() == cardAction
                || playedCards.stream().anyMatch(c -> c.getCardMetadata().getCardAction() == cardAction);
    }

    private List<Integer> getMarsUniversityInput(MarsGame game, String playerUuid, Card card, List<Card> handCards, int scienceTagsCount) {
        List<Card> copy = handCards.stream()
                .filter(c -> c.getId() != card.getId())
                .collect(Collectors.toList());

        if (copy.isEmpty() || scienceTagsCount == 0) {
            return List.of(InputFlag.SKIP_ACTION.getId());
        }

        if (copy.size() == 1) {
            return List.of(copy.get(0).getId());
        }

        if (copy.size() == 2 && scienceTagsCount == 2) {
            return List.of(copy.get(0).getId(), copy.get(1).getId());
        }

        List<Integer> cardsToDiscard = new ArrayList<>(scienceTagsCount);

        List<Integer> plantCards = copy.stream()
                .filter(c -> c.getTags().contains(Tag.PLANT) && !c.getTags().contains(Tag.DYNAMIC) && !c.getTags().contains(Tag.SCIENCE))
                .map(Card::getId)
                .collect(Collectors.toList());

        //TODO what if card with plant is good?
        cardsToDiscard.addAll(getWorstCardsForMarsUniversity(game, playerUuid, plantCards, scienceTagsCount));

        if (cardsToDiscard.size() == scienceTagsCount) {
            return cardsToDiscard;
        }

        //TODO not checking science card because it will create a loop, maybe find a solution?
        List<Integer> nonPlantCards = copy.stream()
                .filter(c -> !c.getTags().contains(Tag.PLANT) && !c.getTags().contains(Tag.DYNAMIC) && !c.getTags().contains(Tag.SCIENCE))
                .map(Card::getId)
                .collect(Collectors.toList());
        cardsToDiscard.addAll(getWorstCardsForMarsUniversity(game, playerUuid, nonPlantCards, scienceTagsCount - cardsToDiscard.size()));

        if (CollectionUtils.isEmpty(cardsToDiscard)) {
            return List.of(InputFlag.SKIP_ACTION.getId());
        }

        return cardsToDiscard;
    }

    private List<Integer> getWorstCardsForMarsUniversity(MarsGame game, String playerUuid, List<Integer> cards, int count) {
        Player player = game.getPlayerByUuid(playerUuid);
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            if (cards.isEmpty()) {
                break;
            }

            Integer card = null;
            switch (player.getDifficulty().CARDS_PICK) {
                case NETWORK_PROJECTION:
                    CardValueResponse worstCard = aiPickCardProjectionService.getWorstCard(game, player, cards);
                    if (worstCard.getWorth() < 0) {
                        card = worstCard.getCardId();
                    }
                    break;
                case FILE_VALUE:
                    card = cardValueService.getWorstCard(game, player, cards, game.getTurns()).getCardId();
                    break;
                case RANDOM:
                    card = cards.get(random.nextInt(cards.size()));
                    break;
            }
            if (card != null) {
                result.add(card);
                cards.remove(card);
            } else {
                break;
            }
        }
        return result;
    }

    private List<Integer> getRandomCards(List<Card> cards, int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (cards.isEmpty()) {
                return result;
            }
            int randomIndex = random.nextInt(cards.size());
            result.add(cards.get(randomIndex).getId());
            cards.remove(randomIndex);
        }
        return result;
    }

}
