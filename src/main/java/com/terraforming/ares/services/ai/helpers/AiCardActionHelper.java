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

    public String validateAction(MarsGame game, Player player, Card card) {
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
                    if (player.getHand().size() != 0) {
                        return null;
                    } else {
                        return "No cards to discard";
                    }
                } else if (actionInputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
                    return null;
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_HEAT) {
                    if (player.getHeat() < 8) {
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

    public List<Integer> getActionInputParams(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null) {
            return List.of();
        }

        if (ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return List.of();
        }

        CardMetadata cardMetadata = card.getCardMetadata();
        if (cardMetadata != null) {
            List<ActionInputData> actionsInputData = cardMetadata.getActionsInputData();
            if (!actionsInputData.isEmpty()) {
                ActionInputData actionInputData = actionsInputData.get(0);
                if (actionInputData.getType() == ActionInputDataType.MICROBE_ANIMAL_CARD) {
                    if (cardMetadata.getCardAction() == CardAction.DECOMPOSING_FUNGUS) {
                        return List.of(getCardWithAnimalOrMicrobe(player));
                    } else {
                        return List.of(getAnimalOrMicrobeCard(player));
                    }
                } else if (actionInputData.getType() == ActionInputDataType.DISCARD_CARD) {
                    if (actionInputData.getMax() == 1) {
                        return List.of(player.getHand().getCards().getLast());
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
                    return List.of(Math.min(4, player.getHeat()));
                } else if (actionInputData.getType() == ActionInputDataType.MICROBE_CARD) {
                    return List.of(getMicrobeCard(player).get().getId());
                }
            }

            if (cardMetadata.getCardAction() == CardAction.EXTREME_COLD_FUNGUS) {
                Optional<Card> microbeCard = getMicrobeCard(player);
                return microbeCard.map(value -> List.of(
                        InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(),
                        value.getId()
                )).orElseGet(() -> List.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()));
            }
        }

        throw new IllegalStateException("NOT REACHABLE");
    }

    private List<Integer> getRandomHandCards(Player player, int max) {
        List<Card> cards = player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toList());

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

    private int getCardWithAnimalOrMicrobe(Player player) {
        Optional<Card> animalCard = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL)
                .filter(card -> player.getCardResourcesCount().get(card.getClass()) > 0)
                .findFirst();

        return animalCard.map(Card::getId).orElseGet(() -> player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.MICROBE)
                .filter(card -> player.getCardResourcesCount().get(card.getClass()) > 0)
                .findFirst().get().getId());
    }

    private int getAnimalOrMicrobeCard(Player player) {
        Optional<Card> animalCard = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.ANIMAL)
                .findFirst();

        return animalCard.map(Card::getId).orElseGet(() -> player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.MICROBE)
                .findFirst().get().getId());
    }

    private Optional<Card> getMicrobeCard(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> card.getCollectableResource() == CardCollectableResource.MICROBE)
                .findFirst();
    }
}
