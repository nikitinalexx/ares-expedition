package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

@Component
@RequiredArgsConstructor
public class AiRandomCardBuildParamsService {
    private static final int PLANT_FLAG = -1;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final CardService cardService;
    private final AiUtility aiUtility;
    private final Random random = new Random();

    /**
     * Creates minimal logical input for random play.
     */
    public Map<Integer, List<Integer>> getInputParamsForBuild(MarsGame game, Player player, Card card) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        CardAction cardAction = card.getCardMetadata().getCardAction();

        if (cardAction == CardAction.CRYOGENIC_SHIPMENT) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomSingleUpgrade(player, Set.of())));
            addCryogenicShipmentResourceInput(player, result);
        }
        if (cardAction == CardAction.UPDATE_PHASE_CARD_TWICE) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    aiDiscoveryDecisionService.pickTwoRandomNonCompetingUpgrades(player));
        }
        if (cardAction == CardAction.TOPOGRAPHIC_MAPPING) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomSingleUpgrade(player, Set.of())));
            result.put(InputFlag.TAG_INPUT.getId(),
                    List.of(aiDiscoveryDecisionService.chooseDynamicTagValue(player, List.of())));
        }
        if (cardAction == CardAction.UPDATE_PHASE_2_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomUpgradeForPhase(player, Constants.BUILD_BLUE_RED_PROJECTS_PHASE)));
        }
        if (cardAction == CardAction.CHOOSE_TAG) {
            result.put(InputFlag.TAG_INPUT.getId(),
                    List.of(aiDiscoveryDecisionService.chooseDynamicTagValue(player, List.of())));
        }
        if (cardAction == CardAction.UPDATE_PHASE_1_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomUpgradeForPhase(player, Constants.BUILD_GREEN_PROJECTS_PHASE)));
        }
        if (cardAction == CardAction.COMMUNICATIONS_STREAMLINING) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomUpgradeForPhase(player, Constants.PERFORM_BLUE_ACTION_PHASE)));
        }
        if (cardAction == CardAction.UPDATE_PHASE_4_CARD) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomUpgradeForPhase(player, Constants.COLLECT_INCOME_PHASE)));
        }
        if (cardAction != null && AiConstants.CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT.contains(cardAction)) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomSingleUpgrade(player, Set.of())));
        }
        if (cardAction == CardAction.BIOMEDICAL_IMPORTS) {
            addBiomedicalImportsInput(game, player, result);
        }
        if (cardAction == CardAction.ASTROFARM) {
            addResourceInput(player, result, InputFlag.ASTROFARM_PUT_RESOURCE,
                    Set.of(CardCollectableResource.MICROBE));
        }
        if (cardAction == CardAction.EOS_CHASMA) {
            addResourceInput(player, result, InputFlag.EOS_CHASMA_PUT_RESOURCE,
                    Set.of(CardCollectableResource.ANIMAL));
        }
        if (cardAction == CardAction.IMPORTED_HYDROGEN) {
            addImportedHydrogenInput(player, result);
        }
        if (cardAction == CardAction.IMPORTED_NITROGEN) {
            addImportedNitrogenInput(player, result);
        }
        if (cardAction == CardAction.LARGE_CONVOY) {
            addLargeConvoyInput(player, result);
        }
        if (cardAction == CardAction.LOCAL_HEAT_TRAPPING) {
            addLocalHeatTrappingInput(player, result);
        }

        if (!card.getCardMetadata().getResourcesOnBuild().isEmpty()
                && card.getCardMetadata().getResourcesOnBuild().get(0).getType() == CardCollectableResource.ANY) {
            List<Card> cardsToPickFrom = aiUtility.getPlayerCardsWithResource(player,
                    Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL, CardCollectableResource.SCIENCE));
            if (cardsToPickFrom.isEmpty()) {
                return null;
            }
            result.put(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(),
                    List.of(getRandomCardIdFromList(cardsToPickFrom)));
        }

        List<Card> playedCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .collect(Collectors.toList());

        if (cardAction == CardAction.SYNTHETIC_CATASTROPHE) {
            List<Card> playedRedCards = playedCards.stream()
                    .filter(c -> c.getColor() == CardColor.RED)
                    .toList();
            if (playedRedCards.isEmpty()) {
                return null;
            }
            result.put(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(),
                    List.of(playedRedCards.get(random.nextInt(playedRedCards.size())).getId()));
        }

        result.putAll(getActiveInputFromCards(player, playedCards, card, result));

        return result;
    }

    private void addCryogenicShipmentResourceInput(Player player, Map<Integer, List<Integer>> result) {
        List<Card> microbes = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.MICROBE));
        List<Card> animals = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));

        if (microbes.isEmpty() && animals.isEmpty()) {
            result.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(),
                    List.of(InputFlag.SKIP_ACTION.getId()));
        } else {
            List<Card> chosen = chooseRandomResourceCards(microbes, animals);
            result.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(),
                    List.of(getRandomCardIdFromList(chosen)));
        }
    }

    private void addBiomedicalImportsInput(MarsGame game, Player player, Map<Integer, List<Integer>> result) {
        boolean upgradePhase = random.nextBoolean() || game.getPlanetAtTheStartOfThePhase().isOxygenMax();

        if (upgradePhase) {
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(),
                    List.of(aiDiscoveryDecisionService.pickRandomSingleUpgrade(player, Set.of())));
        } else {
            result.put(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId(), List.of(0));
        }
    }

    private void addImportedHydrogenInput(Player player, Map<Integer, List<Integer>> result) {
        List<Card> microbes = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.MICROBE));
        List<Card> animals = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));

        List<Integer> possibleOptions = new ArrayList<>();
        if (!microbes.isEmpty()) {
            possibleOptions.add(getRandomCardIdFromList(microbes));
        }
        if (!animals.isEmpty()) {
            possibleOptions.add(getRandomCardIdFromList(animals));
        }
        possibleOptions.add(PLANT_FLAG);

        int option = possibleOptions.get(random.nextInt(possibleOptions.size()));
        if (option == PLANT_FLAG) {
            result.put(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
        } else {
            result.put(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId(), List.of(option));
        }
    }

    private void addImportedNitrogenInput(Player player, Map<Integer, List<Integer>> result) {
        List<Card> microbeCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.MICROBE));
        List<Card> animalCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));

        result.put(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(),
                List.of(animalCards.isEmpty() ? InputFlag.SKIP_ACTION.getId() : getRandomCardIdFromList(animalCards)));
        result.put(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(),
                List.of(microbeCards.isEmpty() ? InputFlag.SKIP_ACTION.getId() : getRandomCardIdFromList(microbeCards)));
    }

    private void addLargeConvoyInput(Player player, Map<Integer, List<Integer>> result) {
        List<Card> animalCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));
        boolean shouldPickPlants = random.nextBoolean() || animalCards.isEmpty();

        if (shouldPickPlants) {
            result.put(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
        } else {
            result.put(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId(), List.of(getRandomCardIdFromList(animalCards)));
        }
    }

    private void addLocalHeatTrappingInput(Player player, Map<Integer, List<Integer>> result) {
        List<Card> animalCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.ANIMAL));
        List<Card> microbeCards = aiUtility.getPlayerCardsWithResource(player, Set.of(CardCollectableResource.MICROBE));

        List<Integer> options = new ArrayList<>();
        if (!animalCards.isEmpty()) {
            options.add(getRandomCardIdFromList(animalCards));
        }
        if (!microbeCards.isEmpty()) {
            options.add(getRandomCardIdFromList(microbeCards));
        }

        result.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(),
                List.of(options.isEmpty() ? InputFlag.SKIP_ACTION.getId() : options.get(random.nextInt(options.size()))));
    }

    private void addResourceInput(Player player, Map<Integer, List<Integer>> result,
                                  InputFlag inputFlag, Set<CardCollectableResource> resourceTypes) {
        List<Card> cards = aiUtility.getPlayerCardsWithResource(player, resourceTypes);
        result.put(inputFlag.getId(),
                List.of(cards.isEmpty() ? InputFlag.SKIP_ACTION.getId() : getRandomCardIdFromList(cards)));
    }

    public Map<Integer, List<Integer>> getActiveInputFromCards(Player player, List<Card> playedCards,
                                                               Card card, Map<Integer, List<Integer>> input) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        result.putAll(getDecomposersInputIfApplicable(player, playedCards, card, input));
        result.putAll(getViralEnhancersInputIfApplicable(playedCards, card, input));
        result.putAll(getMarsUniversityInputIfApplicable(player, playedCards, card, input));
        return result;
    }

    private Map<Integer, List<Integer>> getMarsUniversityInputIfApplicable(Player player, List<Card> playedCards,
                                                                           Card card, Map<Integer, List<Integer>> input) {
        if (!cardActionTriggered(playedCards, card, CardAction.MARS_UNIVERSITY)) {
            return Map.of();
        }

        int scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);
        if (scienceTagsCount == 0) {
            return Map.of();
        }

        List<Integer> handCards = new ArrayList<>(player.getHand().getCards());

        scienceTagsCount = Math.min(scienceTagsCount, handCards.size());

        if (scienceTagsCount == 0 || random.nextInt(10) <= 2) {
            return Map.of(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }
        List<Integer> resultCards = new ArrayList<>();
        for (int i = 0; i < scienceTagsCount; i++) {
            int chosenIndex = random.nextInt(handCards.size());
            resultCards.add(handCards.get(chosenIndex));
            handCards.remove(chosenIndex);
        }

        return Map.of(InputFlag.MARS_UNIVERSITY_CARD.getId(), resultCards);
    }

    private Map<Integer, List<Integer>> getDecomposersInputIfApplicable(Player player, List<Card> playedCards,
                                                                        Card card, Map<Integer, List<Integer>> input) {
        if (!cardActionTriggered(playedCards, card, CardAction.DECOMPOSERS)) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }

        int takeMicrobes = 0;
        int takeCards = 0;
        int microbesOnCard = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0);

        for (int i = 0; i < tagsCount; i++) {
            if (microbesOnCard > 0 && random.nextBoolean()) {
                takeCards++;
                microbesOnCard--;
            } else {
                takeMicrobes++;
                microbesOnCard++;
            }
        }

        Map<Integer, List<Integer>> result = new HashMap<>();
        if (takeMicrobes != 0) {
            result.put(DECOMPOSERS_TAKE_MICROBE.getId(), List.of(takeMicrobes));
        }
        if (takeCards != 0) {
            result.put(DECOMPOSERS_TAKE_CARD.getId(), List.of(takeCards));
        }
        return result;
    }

    private Map<Integer, List<Integer>> getViralEnhancersInputIfApplicable(List<Card> playedCards, Card card,
                                                                           Map<Integer, List<Integer>> input) {
        if (!cardActionTriggered(playedCards, card, CardAction.VIRAL_ENHANCERS)) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }

        List<Card> playerCardsWithPlayed = new ArrayList<>(playedCards);
        playerCardsWithPlayed.add(card);

        List<Card> animalCards = getCardsWithResource(playerCardsWithPlayed, Set.of(CardCollectableResource.ANIMAL));
        List<Card> microbeCards = getCardsWithResource(playerCardsWithPlayed, Set.of(CardCollectableResource.MICROBE));

        List<Integer> possibleOptions = new ArrayList<>();
        if (!microbeCards.isEmpty()) {
            possibleOptions.add(getRandomCardIdFromList(microbeCards));
        }
        if (!animalCards.isEmpty()) {
            possibleOptions.add(getRandomCardIdFromList(animalCards));
        }
        possibleOptions.add(PLANT_FLAG);

        Map<Integer, Integer> optionToCount = new HashMap<>();
        for (int i = 0; i < tagsCount; i++) {
            int option = possibleOptions.get(random.nextInt(possibleOptions.size()));
            optionToCount.merge(option, 1, Integer::sum);
        }

        Map<Integer, List<Integer>> result = new HashMap<>();
        if (optionToCount.containsKey(PLANT_FLAG)) {
            result.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(optionToCount.get(PLANT_FLAG)));
            optionToCount.remove(PLANT_FLAG);
        }
        if (!optionToCount.isEmpty()) {
            List<Integer> chosenCards = new ArrayList<>();
            optionToCount.forEach((cardId, count) -> {
                for (int i = 0; i < count; i++) {
                    chosenCards.add(cardId);
                }
            });
            result.put(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), chosenCards);
        }
        return result;
    }

    private boolean cardActionTriggered(List<Card> playedCards, Card card, CardAction cardAction) {
        return card.getCardMetadata().getCardAction() == cardAction
                || playedCards.stream().anyMatch(c -> c.getCardMetadata().getCardAction() == cardAction);
    }

    private int getRandomCardIdFromList(List<Card> cards) {
        return cards.get(random.nextInt(cards.size())).getId();
    }

    private List<Card> getCardsWithResource(List<Card> cards, Set<CardCollectableResource> resources) {
        return cards.stream()
                .filter(card -> resources.contains(card.getCollectableResource()))
                .collect(Collectors.toList());
    }

    private List<Card> chooseRandomResourceCards(List<Card> microbes, List<Card> animals) {
        boolean pickMicrobe = random.nextBoolean();
        if (pickMicrobe && !microbes.isEmpty()) {
            return microbes;
        } else if (!pickMicrobe && !animals.isEmpty()) {
            return animals;
        } else {
            return microbes.isEmpty() ? animals : microbes;
        }
    }

}