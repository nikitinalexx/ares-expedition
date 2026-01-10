package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnFlow.AvailableTurnFlow;
import com.terraforming.ares.services.ai.turnFlow.BestTurnType;
import com.terraforming.ares.services.ai.turnProcessors.network2.Network2FirstSecondPhaseActionProcessor;
import com.terraforming.ares.services.ai.turnProcessors.random.AiRandomFirstPhaseActionProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiFirstPhaseActionProcessor {
    private final AiRandomFirstPhaseActionProcessor aiRandomFirstPhaseActionProcessor;
    private final Network2FirstSecondPhaseActionProcessor network2FirstSecondPhaseActionProcessor;
    private final AiBuildProjectService aiBuildProjectService;
    private final ICardValueService cardValueService;
    private final AiTurnService aiTurnService;
    private final AiCardBuildParamsService aiCardBuildParamsService;
    private final AiPaymentService aiPaymentHelper;

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            network2FirstSecondPhaseActionProcessor.processTurn(possibleTurns, game, player);
            return;
        }
        if (player.getDifficulty().BUILD == AiTurnChoice.RANDOM) {
            aiRandomFirstPhaseActionProcessor.processTurn(possibleTurns, game, player);
            return;
        }

        AvailableTurnFlow availableTurnFlow = new AvailableTurnFlow();

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            availableTurnFlow.addScenarioToFlow(BestTurnType.UNMI);
        }

        if (!player.cantBuildAnything()) {
            Card selectedCard = null;

            {//log random or smart
                List<Card> availableCards = aiBuildProjectService.getAvailableCardsToBuild(game, player);

                selectedCard = cardValueService.getBestCardToBuild(game, player, availableCards, game.getTurns(), true);

                if (Constants.LOG_NET_COMPARISON) {
                    System.out.println("Available cards: " + availableCards.stream().map(Card::getClass).map(Class::getSimpleName).collect(Collectors.joining(",")));
                    System.out.println("Chosen card with % " + (selectedCard != null ? selectedCard.getClass().getSimpleName() : null));
                }
            }

            if (player.getDifficulty().BUILD == AiTurnChoice.NETWORK) {
                final BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(game, player, null, ProjectionStrategy.FROM_PHASE);

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
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.UNMI) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else if (availableTurnFlow.getBestTurnType() == BestTurnType.PROJECT) {
            Map<Integer, List<Integer>> inputParams = aiCardBuildParamsService.getInputParamsForBuild(game, player, availableTurnFlow.getCard());
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

}
