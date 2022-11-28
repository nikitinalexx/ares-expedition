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
public class AiBuildGreenProjectTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final CardValueService cardValueService;
    private final DeepNetwork deepNetwork;
    private final AiProjectionService aiProjectionService;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        List<Card> availableCards = player.getHand()
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
                .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            aiTurnService.skipTurn(player);
            return;
        }

        Card selectedCard = null;
        if (player.getUuid().endsWith("0") && Constants.FIRST_BOT_IS_RANDOM) {
            selectedCard = availableCards.get(random.nextInt(availableCards.size()));
        } else {
            float bestChance = deepNetwork.testState(game, player);
            Card bestCard = null;

            for (Card playableCard : availableCards) {
                MarsGame stateAfterPlayingTheCard = aiProjectionService.projectBuildCard(game, player, playableCard, ProjectionStrategy.FROM_PHASE);

                float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

                if (projectedChance > bestChance) {
                    bestChance = projectedChance;
                    bestCard = playableCard;
                }
            }
            if (bestCard != null) {
                selectedCard = bestCard;
            }
        }

        if (selectedCard == null) {
            aiTurnService.skipTurn(player);
        } else {
            aiTurnService.buildGreenProject(
                    game,
                    player,
                    selectedCard.getId(),
                    aiPaymentHelper.getCardPayments(player, selectedCard),
                    aiCardParamsHelper.getInputParamsForBuild(player, selectedCard)
            );
        }

        return;
    }

}
