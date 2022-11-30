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
import com.terraforming.ares.validation.action.ActionValidator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public AiCardActionHelper(List<ActionValidator<?>> validators, CardService cardService, TerraformingService terraformingService) {
        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );
        this.cardService = cardService;
        this.terraformingService = terraformingService;
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

    public String validateRandomAction(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null) {
            return null;
        }

        if (ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return validator.validate(game, player);
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (actionInputData.getType() == ActionInputDataType.MICROBE_ANIMAL_CARD) {
                    if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                        if (hasCardWithAnimalOrMicrobe(player)) {
                            return null;
                        } else {
                            return "No animal or microbe on any card";
                        }
                    } else {
                        if (hasAnimalOrMicrobeCard(player)) {
                            return null;
                        } else {
                            return "No animal or microbe card";
                        }
                    }

                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    if (actionInputData.getMax() == 1) {
                        if (player.getHand().size() != 0) {
                            return null;
                        } else {
                            return "No cards to discard";
                        }
                    } else {
                        int randomNumberOfCardsToDiscard = random.nextInt(actionInputData.getMax());
                        if (randomNumberOfCardsToDiscard == 0) {
                            return "Ignore action";
                        } else {
                            if (player.getHand().size() < randomNumberOfCardsToDiscard) {
                                return "No cards to disard";
                            } else {
                                return null;
                            }
                        }
                    }

                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    return null;
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_HEAT) {
                    if (player.getHeat() < 1 || random.nextBoolean()) {
                        return "No need to discard heat";
                    } else {
                        return null;
                    }
                } else if (actionInputData.getType() == ActionInputDataType.MICROBE_CARD) {
                    if (hasMicrobeCard(player)) {
                        return null;
                    } else {
                        return "No animal or microbe card";
                    }
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                return null;//always can take a plant
            }
        }
        throw new IllegalStateException("NOT REACHABLE");
    }

    public List<Integer> getActionInputParamsSmart(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null || ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return List.of();
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (actionInputData.getType() == ActionInputDataType.MICROBE_ANIMAL_CARD) {
                    if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                        return List.of(getLeastValuableCardWithAnimalOrMicrobePresent(game, player));
                    } else {
                        return List.of(getRandomAnimalOrMicrobeCard(player));
                    }
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    if (actionInputData.getMax() == 1) {
                        return List.of(player.getHand().getCards().get(random.nextInt(player.getHand().getCards().size())));
                    } else {
                        return getRandomHandCards(player, actionInputData.getMax());
                    }
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    if (cardMetadata.getCardAction() == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game)
                            || cardMetadata.getCardAction() == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game)
                            || cardMetadata.getCardAction() == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game)) {
                        return List.of(1);
                    }
                    return (player.getCardResourcesCount().get(card.getClass()) >= actionInputData.getMax()) ? List.of(actionInputData.getMax()) : List.of(1);
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_HEAT) {
                    int maxHeatToDiscard = Math.min(player.getHeat(), actionInputData.getMax());
                    if (maxHeatToDiscard == 1) {
                        return List.of(1);
                    }
                    int heatToDiscard = random.nextInt(maxHeatToDiscard - 1) + 1;
                    return List.of(heatToDiscard);
                } else if (actionInputData.getType() == ActionInputDataType.MICROBE_CARD) {
                    return List.of(getRandomMicrobeCard(player).get().getId());
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                Optional<Card> microbeCard = getRandomMicrobeCard(player);
                return microbeCard.map(value -> {
                            if (random.nextBoolean()) {
                                return List.of(
                                        InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(),
                                        value.getId());
                            } else {
                                return List.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId());
                            }
                        }
                ).orElseGet(() -> List.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()));
            }
        }

        throw new IllegalStateException("NOT REACHABLE");
    }

    public List<Integer> getActionInputParamsRandom(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null || ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return List.of();
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (actionInputData.getType() == ActionInputDataType.MICROBE_ANIMAL_CARD) {
                    if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                        return List.of(getRandomCardWithAnimalOrMicrobePresent(player));
                    } else {
                        return List.of(getRandomAnimalOrMicrobeCard(player));
                    }
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    if (actionInputData.getMax() == 1) {
                        return List.of(player.getHand().getCards().get(random.nextInt(player.getHand().getCards().size())));
                    } else {
                        return getRandomHandCards(player, actionInputData.getMax());
                    }
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    if (cardMetadata.getCardAction() == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game)
                            || cardMetadata.getCardAction() == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game)
                            || cardMetadata.getCardAction() == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game)) {
                        return List.of(1);
                    }
                    return (player.getCardResourcesCount().get(card.getClass()) >= actionInputData.getMax()) ? List.of(actionInputData.getMax()) : List.of(1);
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_HEAT) {
                    int maxHeatToDiscard = Math.min(player.getHeat(), actionInputData.getMax());
                    if (maxHeatToDiscard == 1) {
                        return List.of(1);
                    }
                    int heatToDiscard = random.nextInt(maxHeatToDiscard - 1) + 1;
                    return List.of(heatToDiscard);
                } else if (actionInputData.getType() == ActionInputDataType.MICROBE_CARD) {
                    return List.of(getRandomMicrobeCard(player).get().getId());
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                Optional<Card> microbeCard = getRandomMicrobeCard(player);
                return microbeCard.map(value -> {
                            if (random.nextBoolean()) {
                                return List.of(
                                        InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(),
                                        value.getId());
                            } else {
                                return List.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId());
                            }
                        }
                ).orElseGet(() -> List.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()));
            }
        }

        throw new IllegalStateException("NOT REACHABLE");
    }

    private List<Integer> getRandomHandCards(Player player, int max) {
        List<Card> cards = player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toList());

        max = (max == 1) ? 1 : random.nextInt(max - 1) + 1;

        List<Integer> randomCards = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            if (cards.isEmpty()) {
                break;
            }
            int cardIndex = random.nextInt(cards.size());
            randomCards.add(cards.get(cardIndex).getId());
            cards.remove(cardIndex);
        }
        return randomCards;
    }


    private boolean hasCardWithAnimalOrMicrobe(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .anyMatch(card -> player.getCardResourcesCount().get(card.getClass()) > 0);
    }

    private boolean hasAnimalOrMicrobeCard(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE);
    }

    private boolean hasMicrobeCard(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(card -> card.getCollectableResource() == CardCollectableResource.MICROBE);
    }

    private static Map<Class<?>, Integer> MICROBE_ANIMAL_DISCARD_PRIORITIES;
    private static Map<Class<?>, Integer> CRITICAL_RESOURCE_VALUES_ON_CARDS;

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

    private int getRandomCardWithAnimalOrMicrobePresent(Player player) {
        List<Card> animalMicrobeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> player.getCardResourcesCount().get(card.getClass()) > 0)
                .collect(Collectors.toList());

        return animalMicrobeCards.get(random.nextInt(animalMicrobeCards.size())).getId();
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

        if (cardsByPriorities.containsKey(1)) {
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
                        if (!animalMicrobeCards.isEmpty()) {
                            break;
                        }
                    }
                    if (animalMicrobeCards.isEmpty()) {
                        throw new IllegalStateException("Not reachable");
                    }
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

    private int getRandomAnimalOrMicrobeCard(Player player) {
        List<Card> animalMicrobeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .collect(Collectors.toList());

        return animalMicrobeCards.get(random.nextInt(animalMicrobeCards.size())).getId();
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
