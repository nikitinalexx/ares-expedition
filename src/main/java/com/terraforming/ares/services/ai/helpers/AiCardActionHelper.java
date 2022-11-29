package com.terraforming.ares.services.ai.helpers;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
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
        //TODO Self-Replicating Bacteria

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

    private int getRandomCardWithAnimalOrMicrobePresent(Player player) {
        List<Card> animalMicrobeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL || card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> player.getCardResourcesCount().get(card.getClass()) > 0)
                .collect(Collectors.toList());

        return animalMicrobeCards.get(random.nextInt(animalMicrobeCards.size())).getId();
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
