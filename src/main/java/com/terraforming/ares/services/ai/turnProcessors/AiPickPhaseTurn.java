package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.*;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.val;
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
    private final AiProjectionService aiProjectionService;
    private final DeepNetwork deepNetwork;


    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        Integer previousChosenPhase = player.getPreviousChosenPhase();


        List<Integer> possiblePhases = new ArrayList<>();

//        if (previousChosenPhase == null || previousChosenPhase != 1 && previousChosenPhase != 2) {
//            int phase = chooseBetweenFirstAndSecondPhase(game, player);
//            if (phase != 0) {
//                possiblePhases.add(phase);
//            }
//        } else {
//
//        }

        if (RandomBotHelper.isRandomBot(player)) {
            if (mayPlayPhaseOne(game, player)) {
                possiblePhases.add(1);
            }

            if (mayPlayPhaseTwo(game, player)) {
                possiblePhases.add(2);
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

        } else {
            int phase = deepNetworkChoose1To4Phase(game, player);
            if (phase != 0) {
                possiblePhases.add(phase);
            } else if (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 5) {
                possiblePhases.add(5);
            }
        }

        int chosenPhase;

        if (possiblePhases.isEmpty()) {
            if (RandomBotHelper.isRandomBot(player)) {
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
                .anyMatch(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                });
    }

    private int deepNetworkChoose1To4Phase(MarsGame game, Player player) {
        List<Card> playableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 1) && card.getColor() == CardColor.GREEN
                        || (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 2) && (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED))
                .collect(Collectors.toList());


        float bestChance = deepNetwork.testState(game, player);
        Card bestCard = null;

        for (Card playableCard : playableCards) {
            MarsGame stateAfterPlayingTheCard = aiProjectionService.projectBuildCard(game, player, playableCard, ProjectionStrategy.FROM_PICK_PHASE);

            if (stateAfterPlayingTheCard == null) {
                continue;
            }
            float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                bestCard = playableCard;
            }
        }

        if (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 3) {
            MarsGame projectedState = aiProjectionService.projectPhaseThree(game, player, ProjectionStrategy.FROM_PICK_PHASE);

            float projectedChance = deepNetwork.testState(projectedState, projectedState.getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                return 3;
            }
        }

        if (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 4) {
            MarsGame projectedState = aiProjectionService.projectPhaseFour(game, player, ProjectionStrategy.FROM_PICK_PHASE);

            float projectedChance = deepNetwork.testState(projectedState, projectedState.getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                return 4;
            }
        }

        if (bestCard != null) {
            if (bestCard.getColor() == CardColor.GREEN) {
                return 1;
            } else {
                return 2;
            }
        } else if (player.getPreviousChosenPhase() == null || player.getPreviousChosenPhase() != 4 && player.getMc() < 20) {
            return 4;
        } else {
            return 0;
        }
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
                .anyMatch(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                });
    }

    private boolean mayPlayPhaseFour(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 4) {
            return false;
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

        val players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        if (players.size() == 2) {
            Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

            if (calcTotalIncome(game, player) / 2 > calcTotalIncome(game, anotherPlayer)) {
                return true;
            }
        }

        return false;
    }

    private int calcTotalIncome(MarsGame game, Player player) {
        return player.getMcIncome() +
                player.getSteelIncome() * 2 +
                player.getTitaniumIncome() * 3 +
                player.getPlantsIncome() * (game.getPlanet().isOxygenMax() ? 1 : 2) +
                player.getCardIncome() * 4 +
                player.getHeatIncome() * (game.getPlanet().isTemperatureMax() ? 1 : 2);
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

        if (cardActions.contains(CardAction.CARETAKER_CONTRACT) && player.getHeat() >= 16) {
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
                .filter(card -> aiCardActionHelper.validateRandomAction(game, player, card) == null)
                .limit(requiredActiveCardsCount)
                .count() == requiredActiveCardsCount;
    }

    private boolean mayPlayPhaseThreeRandom(MarsGame game, Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> aiCardActionHelper.validateRandomAction(game, player, card) == null)
                .limit(3)
                .count() == 3;
    }

    private boolean mayPlayPhaseThree(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 3) {
            return false;
        }

        return mayPlayPhaseThreeRandom(game, player);
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
