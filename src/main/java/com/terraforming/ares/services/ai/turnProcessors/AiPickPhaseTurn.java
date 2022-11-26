package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.CardValueService;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickPhaseTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiCardActionHelper aiCardActionHelper;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final CardValueService cardValueService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        Integer previousChosenPhase = player.getPreviousChosenPhase();


        List<Integer> possiblePhases = new ArrayList<>();

        if (previousChosenPhase == null || previousChosenPhase != 1 && previousChosenPhase != 2) {
            int phase = chooseBetweenFirstAndSecondPhase(game, player);
            if (phase != 0) {
                possiblePhases.add(phase);
            }
        } else {
            if (mayPlayPhaseOne(game, player)) {
                possiblePhases.add(1);
            }

            if (mayPlayPhaseTwo(game, player)) {
                possiblePhases.add(2);
            }
        }

        if (mayPlayPhaseThree(game, player)) {
            possiblePhases.add(3);
        }

        if (mayPlayPhaseFour(game, player)) {
            possiblePhases.add(4);
        }

        if (mayPlayPhaseFive(game, player)) {
            possiblePhases.add(5);
        }

        int chosenPhase;

        if (possiblePhases.isEmpty()) {
            if (player.getUuid().endsWith("0") && Constants.FIRST_BOT_IS_RANDOM) {
                chosenPhase = random.nextInt(previousChosenPhase != null ? 4 : 5) + 1;
                if (previousChosenPhase != null && chosenPhase == previousChosenPhase) {
                    chosenPhase++;
                }
            } else {
                if (previousChosenPhase == null) {
                    chosenPhase = 5;
                } else {
                    chosenPhase = (previousChosenPhase == 4) ? 5 : 4;
                }
            }
        } else {
            chosenPhase = possiblePhases.get(random.nextInt(possiblePhases.size()));
        }

        aiTurnService.choosePhaseTurn(player, chosenPhase);

        return true;
    }

    private int chooseBetweenFirstAndSecondPhase(MarsGame game, Player player) {
        List<Card> playableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                }).collect(Collectors.toList());

        if (playableCards.isEmpty()) {
            return 0;
        }

        Card bestCard = cardValueService.getBestCardAsCard(game, player, playableCards, game.getTurns());
        return (bestCard.getColor() == CardColor.GREEN ? 1 : 2);
    }

    private boolean mayPlayPhaseOne(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 1) {
            return false;
        }

        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;
    }

    private boolean mayPlayPhaseTwo(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 2) {
            return false;
        }

        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;
    }

    private boolean mayPlayPhaseFour(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 4) {
            return false;
        }

        if (player.getCardIncome() >= 3) {//when can get a lot of card
            return true;
        }

        if (player.getMc() <= 5) {//when running really low unable to do anything
            return true;
        }

        //when have a good income
        if (player.getMcIncome() > 20 || player.getHeatIncome() > 15 || player.getPlantsIncome() > 5) {
            return true;
        }

        //when need and can fill restructured resources
        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)
                && player.getPlantsIncome() > 0
                && player.getPlants() == 0) {
            return true;
        }

        return false;
    }

    private boolean mayPlayPhaseThreeSmart(MarsGame game, Player player) {
        List<Card> activeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard).collect(Collectors.toList());

        Set<CardAction> cardActions = activeCards.stream()
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (cardActions.contains(CardAction.AI_CENTRAL)) {
            return true;
        }

        int requiredActiveCardsCount = 5;
        if (cardActions.contains(CardAction.SCREENING_TECHNOLOGY)) {
            requiredActiveCardsCount--;
        }
        if (cardActions.contains(CardAction.CIRCUIT_BOARD)) {
            requiredActiveCardsCount--;
        }

        if ((cardActions.contains(CardAction.EXTREME_COLD_FUNGUS) || cardActions.contains(CardAction.SYMBIOTIC_FUNGUD))
                && (cardActions.contains(CardAction.GHG_PRODUCTION)
                || cardActions.contains(CardAction.NITRITE_REDUCTING)
                || cardActions.contains(CardAction.REGOLITH_EATERS)
                || cardActions.contains(CardAction.SELF_REPLICATING_BACTERIA))) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.ASSET_LIQUIDATION)) {
            requiredActiveCardsCount++;
        }

        if (cardActions.contains(CardAction.PROGRESSIVE_POLICIES)
                && cardService.countPlayedTags(player, Set.of(Tag.EVENT)) >= 4
                && player.getMc() >= 15) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.DEVELOPED_INFRASTRUCTURE)
                && cardService.countPlayedCards(player, Set.of(CardColor.BLUE)) >= 5
                && player.getMc() >= 15) {
            requiredActiveCardsCount--;
        }

        if ((cardActions.contains(CardAction.STEELWORKS) && player.getHeat() >= 8 || cardActions.contains(CardAction.IRON_WORKS) && player.getHeat() >= 12)
                && !game.getPlanet().isOxygenMax()) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.SOLAR_PUNK)) {
            if (player.getTitaniumIncome() < 3) {
                requiredActiveCardsCount++;
            } else {
                int solarpunkCost = Math.max(0, 15 - player.getTitaniumIncome() * 2) * 2;
                if (player.getMc() - 5 < solarpunkCost) {
                    requiredActiveCardsCount++;
                } else {
                    requiredActiveCardsCount--;
                }
            }
        }

        if (player.getHeat() >= 40 && !game.getPlanet().isTemperatureMax() || player.getPlants() >= 32 && !game.getPlanet().isOxygenMax()) {
            return true;
        }

        requiredActiveCardsCount = Math.max(0, requiredActiveCardsCount);

        return activeCards
                .stream()
                .filter(card -> aiCardActionHelper.validateAction(game, player, card) == null)
                .limit(requiredActiveCardsCount)
                .count() == requiredActiveCardsCount;
    }

    private boolean mayPlayPhaseThreeRandom(MarsGame game, Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> aiCardActionHelper.validateAction(game, player, card) == null)
                .limit(3)
                .count() == 3;
    }

    private boolean mayPlayPhaseThree(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 3) {
            return false;
        }

        if (player.getUuid().endsWith("0") && Constants.FIRST_BOT_IS_RANDOM) {
            return mayPlayPhaseThreeRandom(game, player);
        } else {
            return mayPlayPhaseThreeSmart(game, player);
        }
    }

    private boolean mayPlayPhaseFive(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 5) {
            return false;
        }

        if (draftCardsService.countExtraCardsToTake(player) >= 2
                || draftCardsService.countExtraCardsToDraft(player) >= 2) {
            return true;
        }

        boolean hasCardsToBuild = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;

        if (!hasCardsToBuild) {
            hasCardsToBuild = player.getHand()
                    .getCards()
                    .stream()
                    .map(cardService::getCard)
                    .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                    .filter(card ->
                    {
                        String errorMessage = cardValidationService.validateCard(
                                player, game, card.getId(),
                                aiPaymentHelper.getCardPayments(player, card),
                                aiCardParamsHelper.getInputParamsForValidation(player, card)
                        );
                        return errorMessage == null;
                    })
                    .limit(2)
                    .count() == 2;
        }

        if (!hasCardsToBuild) {
            return true;
        }

        return false;
    }


}
