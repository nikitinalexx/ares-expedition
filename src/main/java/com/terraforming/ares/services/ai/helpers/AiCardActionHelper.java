package com.terraforming.ares.services.ai.helpers;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.AiBalanceService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.dto.ActionInputParamsResponse;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import com.terraforming.ares.validation.action.ActionValidator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Service
public class AiCardActionHelper {
    private final Map<Class<?>, ActionValidator<?>> blueActionValidators;
    private final CardService cardService;
    private final TerraformingService terraformingService;
    private final Random random = new Random();
    private final ICardValueService cardValueService;
    private final AiBalanceService aiBalanceService;

    public AiCardActionHelper(List<ActionValidator<?>> validators, CardService cardService, TerraformingService terraformingService, ICardValueService cardValueService, AiBalanceService aiBalanceService) {
        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );
        this.cardService = cardService;
        this.terraformingService = terraformingService;
        this.cardValueService = cardValueService;
        this.aiBalanceService = aiBalanceService;
    }

    private Set<Class<?>> ACTIONS_WITHOUT_INPUT_PARAMS = Set.of(
            AquiferPumping.class,
            ArtificialJungle.class,
            AssetLiquidation.class,
            Birds.class,
            BrainstormingSession.class,
            CaretakerContract.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            DevelopedInfrastructure.class,
            DevelopmentCenter.class,
            FarmersMarket.class,
            HydroElectricEnergy.class,
            IronWorks.class,
            MatterManufactoring.class,
            SolarPunk.class,
            Steelworks.class,
            Tardigrades.class,
            ThinkTank.class,
            VolcanicPools.class,
            WaterImportFromEuropa.class,
            WoodBurningStoves.class,
            ProgressivePolicies.class
    );

    /**
     * Shows if card is smart to play in advance. At the moment of making that blue action the situation might be different
     */
    public boolean isSmartPlayAction(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null) {
            return true;
        }

        if (ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            String validationResult = validator.validate(game, player);
            if (validationResult != null) {
                return false;
            }

            return Optional.ofNullable(card.getCardMetadata()).map(CardMetadata::getCardAction)
                    .map(
                            cardAction -> {
                                if (cardAction == CardAction.DEVELOPED_INFRASTRUCTURE) {
                                    return player.getPlayed().getCards().stream()
                                            .map(cardService::getCard)
                                            .map(Card::getColor)
                                            .filter(CardColor.BLUE::equals)
                                            .limit(5)
                                            .count() == 5;
                                } else if (cardAction == CardAction.PROGRESSIVE_POLICIES) {
                                    return cardService.countPlayedTags(player, Set.of(Tag.EVENT)) >= 4;
                                }
                                return true;
                            }
                    ).orElse(true);
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                    return hasCardWithCheapAnimalOrMicrobe(player);
                } else if (cardMetadata.getCardAction() == CardAction.CONSERVED_BIOME) {
                    return hasProfitableCardWithCollectableResource(game, player, Set.of(CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL));
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    return player.getHand().size() != 0;
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    return isUsefulAddDiscardMicrobeAction(game, card);
                } else if (cardMetadata.getCardAction() == CardAction.POWER_INFRASTRUCTURE) {
                    return game.getPlanetAtTheStartOfThePhase().isTemperatureMax() && player.getHeat() >= 10;
                } else if (cardMetadata.getCardAction() == CardAction.GREEN_HOUSES) {
                    return !game.getPlanetAtTheStartOfThePhase().isOxygenMax() && player.getHeat() >= 4;
                } else if (cardMetadata.getCardAction() == CardAction.SYMBIOTIC_FUNGUD) {
                    return hasProfitableCardWithCollectableResource(game, player, Set.of(CardCollectableResource.MICROBE));
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                return true;//always can take a plant
            }
        }
        throw new IllegalStateException("NOT REACHABLE");
    }

    public ActionInputParamsResponse getActionInputParamsForRandom(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null || ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return ActionInputParamsResponse.makeAction();
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getLeastValuableCardWithAnimalOrMicrobePresent(game, player))));
                } else if (cardMetadata.getCardAction() == CardAction.CONSERVED_BIOME) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getMostValuableAnimalOrMicrobeCard(game, player, Set.of(CardCollectableResource.ANIMAL, CardCollectableResource.MICROBE)).get())));

                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), getRandomHandCards(player, actionInputData.getMax())));
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    if (cardMetadata.getCardAction() == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game)
                            || cardMetadata.getCardAction() == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game)
                            || cardMetadata.getCardAction() == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game)) {
                        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
                    }
                    return ActionInputParamsResponse.makeActionWithParams((player.getCardResourcesCount().get(card.getClass()) >= actionInputData.getMax()) ? Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(actionInputData.getMax())) : Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_HEAT) {
                    int maxHeatToDiscard = Math.min(player.getHeat(), actionInputData.getMax());
                    if (maxHeatToDiscard == 1) {
                        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(1)));
                    }
                    int heatToDiscard = random.nextInt(maxHeatToDiscard - 1) + 1;
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(heatToDiscard)));
                } else if (actionInputData.getType() == ActionInputDataType.MICROBE_CARD) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getRandomMicrobeCard(player).get().getId())));
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                Optional<Card> microbeCard = getRandomMicrobeCard(player);
                return ActionInputParamsResponse.makeActionWithParams(microbeCard.map(value ->
                        Map.of(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(), List.of(value.getId()))
                ).orElseGet(() -> Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0))));
            }
        }

        throw new IllegalStateException("NOT REACHABLE");
    }

    public ActionInputParamsResponse getActionInputParamsForSmart(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null || ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return ActionInputParamsResponse.makeAction();
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getLeastValuableCardWithAnimalOrMicrobePresent(game, player))));
                } else if (cardMetadata.getCardAction() == CardAction.CONSERVED_BIOME) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getMostValuableAnimalOrMicrobeCard(game, player, Set.of(CardCollectableResource.ANIMAL, CardCollectableResource.MICROBE)).get())));
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    List<Integer> cardsToDiscard = getCardsToDiscardSmart(game, player, actionInputData.getMax());
                    return ActionInputParamsResponse.builder().makeAction(!cardsToDiscard.isEmpty()).inputParams(Map.of(InputFlag.CARD_CHOICE.getId(), cardsToDiscard)).build();
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    if (cardMetadata.getCardAction() == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game)
                            || cardMetadata.getCardAction() == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game)
                            || cardMetadata.getCardAction() == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game)) {
                        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
                    }
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(),
                            (player.getCardResourcesCount().get(card.getClass()) >= actionInputData.getMax()) ? List.of(actionInputData.getMax()) : List.of(1)));
                } else if (cardMetadata.getCardAction() == CardAction.POWER_INFRASTRUCTURE) {
                    if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax() && player.getHeat() >= 10) {
                        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(player.getHeat())));
                    } else {
                        return ActionInputParamsResponse.noAction();
                    }
                } else if (cardMetadata.getCardAction() == CardAction.GREEN_HOUSES) {
                    if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax() && player.getHeat() >= 1) {
                        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(Math.min(4, player.getHeat()))));
                    } else {
                        return ActionInputParamsResponse.noAction();
                    }
                } else if (cardMetadata.getCardAction() == CardAction.SYMBIOTIC_FUNGUD) {
                    return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.CARD_CHOICE.getId(), List.of(getMostValuableAnimalOrMicrobeCard(game, player, Set.of(CardCollectableResource.MICROBE)).get())));
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                getMostValuableAnimalOrMicrobeCard(game, player, Set.of(CardCollectableResource.MICROBE));

                return ActionInputParamsResponse.makeActionWithParams(getMostValuableAnimalOrMicrobeCard(game, player,
                        Set.of(CardCollectableResource.MICROBE)).map(value ->
                        Map.of(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(), List.of(value))
                ).orElseGet(() -> Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0))));
            }
        }

        throw new IllegalStateException("NOT REACHABLE");
    }

    private boolean isUsefulAddDiscardMicrobeAction(MarsGame game, Card card) {
        CardAction cardAction = card.getCardMetadata().getCardAction();

        if (cardAction == CardAction.SELF_REPLICATING_BACTERIA) {
            return true;
        }

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax() && cardAction == CardAction.NITRITE_REDUCTING
                || game.getPlanetAtTheStartOfThePhase().isTemperatureMax() && cardAction == CardAction.GHG_PRODUCTION
                || game.getPlanetAtTheStartOfThePhase().isOxygenMax() && cardAction == CardAction.REGOLITH_EATERS) {
            return false;
        }

        return true;
    }

    private List<Integer> getRandomHandCards(Player player, int max) {
        List<Integer> cards = new ArrayList<>(player.getHand().getCards());

        max = (max == 1) ? 1 : random.nextInt(max - 1) + 1;

        List<Integer> randomCards = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            if (cards.isEmpty()) {
                break;
            }
            Integer cardId = cards.get(random.nextInt(cards.size()));
            randomCards.add(cardId);
            cards.remove(cardId);
        }
        return randomCards;
    }


    public List<Integer> getCardsToDiscardSmart(MarsGame game, Player player, int max) {
        List<Integer> cards = new ArrayList<>(player.getHand().getCards());

        List<Integer> cardsToDiscard = new ArrayList<>();
        while (cardsToDiscard.size() != max) {
            if (cards.isEmpty()) {
                break;
            }
            CardValueResponse cardValueResponse = cardValueService.getWorstCard(game, player, cards, game.getTurns());

            if (aiBalanceService.isCardWorthToDiscard(player, cardValueResponse.getWorth())) {
                cardsToDiscard.add(cardValueResponse.getCardId());
                cards.remove(cardValueResponse.getCardId());
            } else {
                break;
            }
        }
        return cardsToDiscard;
    }

    private boolean hasCardWithCheapAnimalOrMicrobe(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> !VALUABLE_ANIMAL_CARDS.contains(card.getClass()))
                .anyMatch(card -> player.getCardResourcesCount().get(card.getClass()) > 0);
    }

    private boolean hasProfitableCardWithCollectableResource(MarsGame game, Player player, Set<CardCollectableResource> resources) {
        Stream<Card> cardStream = player.getPlayed().getCards().stream()
                .map(cardService::getCard);

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            cardStream = cardStream.filter(card -> card.getClass() != NitriteReductingBacteria.class);
        }

        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            cardStream = cardStream.filter(card -> card.getClass() != GhgProductionBacteria.class);
        }

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            cardStream = cardStream.filter(card -> card.getClass() != RegolithEaters.class);
        }
        return cardStream
                .anyMatch(card -> resources.contains(card.getCollectableResource()));
    }

    private static Map<Class<?>, Integer> MICROBE_ANIMAL_DISCARD_PRIORITIES;
    private static Map<Class<?>, Integer> CRITICAL_RESOURCE_VALUES_ON_CARDS;
    private static Set<Class<?>> VALUABLE_ANIMAL_CARDS = Set.of(
            Birds.class, Fish.class, Livestock.class
    );

    static {
        MICROBE_ANIMAL_DISCARD_PRIORITIES = new HashMap<>();
        //microbes
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Tardigrades.class, 1);             // 1/3 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Decomposers.class, 1);              // 1/3 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(GhgProductionBacteria.class, 1);// 1/3 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(NitriteReductingBacteria.class, 1);// 1/3 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(RegolithEaters.class, 1);// 1/3 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(AnaerobicMicroorganisms.class, 2);// 1/2 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(DecomposingFungus.class, 2); // 1/2 vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(SelfReplicatingBacteria.class, 2);// 1/2 vp

        //animals
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(FilterFeeders.class, 1);// 1/3vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(ArclightCorporation.class, 2);// 1/2vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(BuffedArclightCorporation.class, 2);// 1/2vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(EcologicalZone.class, 2);// 1/2vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(SmallAnimals.class, 2);// 1/2vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Herbivores.class, 2);// 1/2vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Birds.class, 3);// 1vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Fish.class, 3);// 1vp
        MICROBE_ANIMAL_DISCARD_PRIORITIES.put(Livestock.class, 3);// 1vp

        CRITICAL_RESOURCE_VALUES_ON_CARDS = new HashMap<>();
        //microbes
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Tardigrades.class, 3);             // 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Decomposers.class, Integer.MAX_VALUE);              // 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(GhgProductionBacteria.class, 2);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(NitriteReductingBacteria.class, 3);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(RegolithEaters.class, 2);// 1/3 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(AnaerobicMicroorganisms.class, 2);// 1/2 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(DecomposingFungus.class, Integer.MAX_VALUE); // 1/2 vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(SelfReplicatingBacteria.class, 5);// 1/2 vp

        //animals
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(FilterFeeders.class, 3);// 1/3vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(ArclightCorporation.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(BuffedArclightCorporation.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(EcologicalZone.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(SmallAnimals.class, 2);// 1/2vp
        CRITICAL_RESOURCE_VALUES_ON_CARDS.put(Herbivores.class, 2);// 1/2vp
    }

    private Optional<Integer> getMostValuableAnimalOrMicrobeCard(MarsGame game, Player player, Set<CardCollectableResource> resourceTypes) {
        Stream<Card> animalMicrobeCardsStream = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> resourceTypes.contains(card.getCollectableResource()));

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            animalMicrobeCardsStream = animalMicrobeCardsStream.filter(card -> card.getClass() != NitriteReductingBacteria.class);
        }

        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            animalMicrobeCardsStream = animalMicrobeCardsStream.filter(card -> card.getClass() != GhgProductionBacteria.class);
        }

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            animalMicrobeCardsStream = animalMicrobeCardsStream.filter(card -> card.getClass() != RegolithEaters.class);
        }

        Map<Integer, List<Card>> cardsByPriorities = animalMicrobeCardsStream
                .collect(Collectors.groupingBy(card -> MICROBE_ANIMAL_DISCARD_PRIORITIES.get(card.getClass())));

        if (cardsByPriorities.get(3) != null && !cardsByPriorities.get(3).isEmpty()) {
            return Optional.of(cardsByPriorities.get(3).get(0).getId());
        }

        for (int i = 2; i >= 1; i--) {
            Card card = getCriticalCardByPriority(cardsByPriorities.get(i), player);
            if (card != null) {
                return Optional.of(card.getId());
            }
        }

        for (int i = 2; i >= 1; i--) {
            List<Card> cards = cardsByPriorities.get(i);
            if (cards != null && !cards.isEmpty()) {
                return Optional.of(cards.get(0).getId());//todo prefer cards with discount?
            }
        }

        return Optional.empty();
    }

    private int getLeastValuableCardWithAnimalOrMicrobePresent(MarsGame game, Player player) {
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

        Map<Integer, List<Card>> cardsByPriorities = animalMicrobeCards
                .stream()
                .collect(Collectors.groupingBy(card -> MICROBE_ANIMAL_DISCARD_PRIORITIES.get(card.getClass())));

        Card nonCriticalCard = getNonCriticalCardByPriority(cardsByPriorities.get(1), player);
        if (nonCriticalCard != null) {
            return nonCriticalCard.getId();
        } else {
            nonCriticalCard = getNonCriticalCardByPriority(cardsByPriorities.get(2), player);
            if (nonCriticalCard != null) {
                return nonCriticalCard.getId();
            } else {
                for (int i = 1; i <= 3; i++) {
                    animalMicrobeCards = cardsByPriorities.get(i);
                    if (animalMicrobeCards != null && !animalMicrobeCards.isEmpty()) {
                        break;
                    }
                }
                if (animalMicrobeCards.isEmpty()) {
                    throw new IllegalStateException("Not reachable");
                }
            }
        }

        return animalMicrobeCards.get(random.nextInt(animalMicrobeCards.size())).getId();
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
