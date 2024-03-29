package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.green.TopographicMapping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.*;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnFlow.AvailableTurnFlow;
import com.terraforming.ares.services.ai.turnFlow.BestTurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
    private final AiCardBuildParamsService aiCardParamsHelper;
    private final DeepNetwork deepNetwork;
    private final AiBuildProjectService aiBuildProjectService;
    private final CardService cardService;
    private final ICardValueService cardValueService;
    private final AiCardValidationService aiCardValidationService;

    private final Random random = new Random();

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        AvailableTurnFlow availableTurnFlow = new AvailableTurnFlow();

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            availableTurnFlow.addScenarioToFlow(BestTurnType.UNMI);
        }

        if (possibleTurns.contains(TurnType.PICK_EXTRA_BONUS_SECOND_PHASE) && !availableTurnFlow.nonSkipTurnAvailable()) {
            availableTurnFlow.addScenarioToFlow(BestTurnType.EXTRA_CARD);
        }

        if (!player.cantBuildAnything()) {
            Card selectedCard = null;

            {//log random or smart
                List<Card> availableCards = aiBuildProjectService.getAvailableCardsToBuild(game, player);

                selectedCard = (player.getDifficulty().BUILD == AiTurnChoice.RANDOM)
                        ? (availableCards.isEmpty() ? null : availableCards.get(random.nextInt(availableCards.size())))
                        : cardValueService.getBestCardToBuild(game, player, availableCards, game.getTurns(), true);

                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("Available cards: " + availableCards.stream().map(Card::getClass).map(Class::getSimpleName).collect(Collectors.joining(",")));
                    System.out.println("Chosen card with % " + (selectedCard != null ? selectedCard.getClass().getSimpleName() : null));
                }
            }

            if (player.getDifficulty().BUILD == AiTurnChoice.NETWORK) {
                final BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(game, player, null, ProjectionStrategy.FROM_PHASE);

                logComputerCardSelection(bestProjectToBuild, game, player);

                if (bestProjectToBuild.isCanBuild()) {
                    selectedCard = bestProjectToBuild.getCard();
                } else {
                    selectedCard = null;
                }
            }

            if (selectedCard != null) {
                availableTurnFlow.addScenarioToFlow(BestTurnType.PROJECT, selectedCard);
            }
        }


        if (availableTurnFlow.getBestTurnType() == BestTurnType.SKIP) {
            aiTurnService.skipTurn(player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.EXTRA_CARD) {
            aiTurnService.pickExtraCardTurnAsync(player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.UNMI) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.PROJECT) {
            Map<Integer, List<Integer>> inputParams = aiCardParamsHelper.getInputParamsForBuild(game, player, availableTurnFlow.getCard());
            aiTurnService.buildProject(
                    game,
                    player,
                    availableTurnFlow.getCard().getId(),
                    aiPaymentHelper.getCardPayments(game, player, availableTurnFlow.getCard(), inputParams),
                    inputParams
            );
        } else {
            throw new IllegalStateException("Not reachable");
        }
    }

    private void logComputerCardSelection(BuildProjectPrediction bestProjectToBuild, MarsGame game, Player player) {
        if (Constants.LOG_NET_COMPARISON) {
            if (bestProjectToBuild.isCanBuild()) {
                try {
                    System.out.println("Deep network state " + deepNetwork.testState(game, player));
                    System.out.println("Deep network card " + bestProjectToBuild.getCard().getClass().getSimpleName() + " with projected chance " + bestProjectToBuild.getExpectedValue());
                } catch (NullPointerException e) {
                    System.out.println("NPE " + bestProjectToBuild);
                }
            } else {
                System.out.println("Deep network : do not build");
            }
            System.out.println();
        }
    }

}