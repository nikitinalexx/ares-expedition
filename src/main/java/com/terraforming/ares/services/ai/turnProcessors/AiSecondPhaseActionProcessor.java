package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.RandomBotHelper;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnFlow.AvailableTurnFlow;
import com.terraforming.ares.services.ai.turnFlow.BestTurnType;
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
public class AiSecondPhaseActionProcessor {
    private final AiTurnService aiTurnService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final DeepNetwork deepNetwork;
    private final AiBuildProjectService aiBuildProjectService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final ICardValueService cardValueService;
    private final Random random = new Random();

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        AvailableTurnFlow availableTurnFlow = new AvailableTurnFlow();

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            availableTurnFlow.addScenarioToFlow(BestTurnType.UNMI);
        }

        if (possibleTurns.contains(TurnType.PICK_EXTRA_BONUS_SECOND_PHASE) && !availableTurnFlow.nonSkipTurnAvailable()) {
            availableTurnFlow.addScenarioToFlow(BestTurnType.EXTRA_CARD);
        }

        if (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            List<Card> availableCards = player.getHand()
                    .getCards()
                    .stream()
                    .map(cardService::getCard)
                    .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED || possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT) && card.getColor() == CardColor.GREEN)
                    .filter(card ->
                    {
                        String errorMessage = cardValidationService.validateCard(
                                player, game, card.getId(),
                                aiPaymentHelper.getCardPayments(game, player, card),
                                aiCardParamsHelper.getInputParamsForValidation(player, card)
                        );
                        return errorMessage == null;
                    })
                    .collect(Collectors.toList());

            if (!availableCards.isEmpty()) {
                Card selectedCard = RandomBotHelper.isRandomBot(player)
                        ? availableCards.get(random.nextInt(availableCards.size()))
                        : cardValueService.getBestCardToBuild(game, player, availableCards, game.getTurns(), true);

                logComputerCardSelection(availableCards, selectedCard, game, player);

                if (selectedCard != null) {
                    availableTurnFlow.addScenarioToFlow(BestTurnType.PROJECT, selectedCard);
                }
            }
        }


        if (availableTurnFlow.getBestTurnType() == BestTurnType.SKIP) {
            aiTurnService.skipTurn(player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.EXTRA_CARD) {
            aiTurnService.pickExtraCardTurnAsync(player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.UNMI) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.PROJECT) {
            aiTurnService.buildProject(
                    game,
                    player,
                    availableTurnFlow.getCard().getId(),
                    aiPaymentHelper.getCardPayments(game, player, availableTurnFlow.getCard()),
                    aiCardParamsHelper.getInputParamsForBuild(player, availableTurnFlow.getCard())
            );
        } else {
            throw new IllegalStateException("Not reachable");
        }
    }

    private void logComputerCardSelection(List<Card> availableCards, Card selectedCard, MarsGame game, Player player) {
        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Available cards: " + availableCards.stream().map(Card::getClass).map(Class::getSimpleName).collect(Collectors.joining(",")));
            System.out.println("Chosen card with % " + (selectedCard != null ? selectedCard.getClass().getSimpleName() : null));
            final BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.BLUE, CardColor.RED), ProjectionStrategy.FROM_PHASE);
            if (bestProjectToBuild.isCanBuild()) {
                System.out.println("Deep network state " + deepNetwork.testState(game, player));
                System.out.println("Deep network card " + bestProjectToBuild.getCard().getClass().getSimpleName() + " with projected chance " + bestProjectToBuild.getExpectedValue());
            } else {
                System.out.println("Deep network : do not build");
            }
            System.out.println();
        }
    }

}