package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.*;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiBuildGreenProjectTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final ICardValueService cardValueService;
    private final AiBuildProjectService aiBuildProjectService;
    private final DeepNetwork deepNetwork;
    private final Random random = new Random();
    private final AiBalanceService aiBalanceService;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
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
            return true;
        }

        Card selectedCard;

        if (RandomBotHelper.isRandomBot(player)) {
            selectedCard = availableCards.get(random.nextInt(availableCards.size()));
        } else {
            selectedCard = cardValueService.getBestCardToBuild(game, player, availableCards, game.getTurns(), true);

            final float currentState = deepNetwork.testState(game, player);

            if (Constants.LOG_NET_COMPARISON) {
                System.out.println("Available cards: " + availableCards.stream().map(Card::getClass).map(Class::getSimpleName).collect(Collectors.joining(",")));

                System.out.println("Chosen card with % " + (selectedCard != null ? selectedCard.getClass().getSimpleName() : null));
            }

            final BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.GREEN), ProjectionStrategy.FROM_PHASE);

//            if (player.getUuid().endsWith("0")) {
//                if (bestProjectToBuild.isCanBuild() && game.getTurns() < 10 && bestProjectToBuild.getExpectedValue() >= currentState * aiBalanceService.getExtraRatio(player)) {
//                    selectedCard = bestProjectToBuild.getCard();
//                }
//            }



            if (Constants.LOG_NET_COMPARISON) {
                if (bestProjectToBuild.isCanBuild()) {
                    System.out.println("Deep network state " + currentState);
                    System.out.println("Deep network card " + bestProjectToBuild.getCard().getClass().getSimpleName() + " with projected chance " + bestProjectToBuild.getExpectedValue());
                } else {
                    System.out.println("Deep network : do not build");
                }
                System.out.println();
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
                    aiCardParamsHelper.getInputParamsForBuild(game, player, selectedCard)
            );
        }

        return true;
    }

}
