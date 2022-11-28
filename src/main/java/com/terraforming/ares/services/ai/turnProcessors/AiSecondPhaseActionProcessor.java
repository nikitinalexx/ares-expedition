package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.AiProjectionService;
import com.terraforming.ares.services.ai.CardValueService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiSecondPhaseActionProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final CardValueService cardValueService;
    private final DeepNetwork deepNetwork;
    private final AiProjectionService aiProjectionService;

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        float bestChance = deepNetwork.testState(game, player);
        boolean bestTurnIsUnmi = false;

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            MarsGame stateAfterUnmi = aiProjectionService.projectUnmiTurn(game, player);
            float projectedChance = deepNetwork.testState(stateAfterUnmi, stateAfterUnmi.getPlayerByUuid(player.getUuid()));

            if (projectedChance > bestChance) {
                bestChance = projectedChance;
                bestTurnIsUnmi = true;
            }
        }

        if (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            List<Card> availableCards = player.getHand()
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
                    .collect(Collectors.toList());

            Card selectedCard = null;
            if (player.getUuid().endsWith("0") && Constants.FIRST_BOT_IS_RANDOM && !availableCards.isEmpty()) {
                selectedCard = availableCards.get(random.nextInt(availableCards.size()));
            } else {
                Card bestCard = null;

                for (Card playableCard : availableCards) {
                    MarsGame stateAfterPlayingTheCard = aiProjectionService.projectBuildCard(game, player, playableCard, ProjectionStrategy.FROM_PHASE);

                    float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

                    if (projectedChance > bestChance) {
                        bestChance = projectedChance;
                        bestCard = playableCard;
                        bestTurnIsUnmi = false;
                    }
                }

                if (possibleTurns.contains(TurnType.PICK_EXTRA_CARD)) {
                    MarsGame stateAfterTakingExtraCard = aiProjectionService.projectTakeExtraCard(game, player);
                    float projectedChance = deepNetwork.testState(stateAfterTakingExtraCard, stateAfterTakingExtraCard.getPlayerByUuid(player.getUuid()));

                    if (projectedChance > bestChance) {
                        aiTurnService.pickExtraCardTurnAsync(player);
                        return;
                    }
                }


                if (bestCard != null) {
                    selectedCard = bestCard;
                }
            }

            if (selectedCard == null && !bestTurnIsUnmi) {
                aiTurnService.skipTurn(player);
                return;
            } else if (!bestTurnIsUnmi){
                aiTurnService.buildBlueRedProject(
                        game,
                        player,
                        selectedCard.getId(),
                        aiPaymentHelper.getCardPayments(player, selectedCard),
                        aiCardParamsHelper.getInputParamsForBuild(player, selectedCard)
                );
                return;
            }
        }

        if (bestTurnIsUnmi) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else if (possibleTurns.contains(TurnType.SKIP_TURN)) {
            aiTurnService.skipTurn(player);
        } else {
            throw new IllegalStateException("Not reachable");
        }
    }

}
