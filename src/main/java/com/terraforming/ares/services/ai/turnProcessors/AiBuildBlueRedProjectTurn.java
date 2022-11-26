package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.CardValueService;
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
public class AiBuildBlueRedProjectTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final CardValueService cardValueService;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_BLUE_RED_PROJECT;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
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

        if (availableCards.isEmpty()) {
            aiTurnService.skipTurn(player);
            return true;
        }

        Card selectedCard = cardValueService.getBestCardAsCard(game, player, availableCards, game.getTurns());

        aiTurnService.buildBlueRedProject(
                game,
                player,
                selectedCard.getId(),
                aiPaymentHelper.getCardPayments(player, selectedCard),
                aiCardParamsHelper.getInputParamsForBuild(player, selectedCard)
        );

        return true;
    }

}
