package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.AiProjectionService;
import com.terraforming.ares.services.ai.CardValueService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnFlow.BestTurnFlow;
import com.terraforming.ares.services.ai.turnFlow.BestTurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;

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
    private final AiProjectionService aiProjectionService;
    private final AiBuildProjectService aiBuildProjectService;

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        BestTurnFlow bestTurnFlow = new BestTurnFlow(deepNetwork.testState(game, player));

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            MarsGame stateAfterUnmi = aiProjectionService.projectUnmiTurn(game, player);
            float projectedChance = deepNetwork.testState(stateAfterUnmi, stateAfterUnmi.getPlayerByUuid(player.getUuid()));

            bestTurnFlow.addScenarioToFlow(projectedChance, BestTurnType.UNMI);
        }

        if (possibleTurns.contains(TurnType.BUILD_BLUE_RED_PROJECT)) {
            BuildProjectPrediction prediction = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.RED, CardColor.BLUE), ProjectionStrategy.FROM_PHASE);

            if (prediction.isCanBuild()) {
                bestTurnFlow.addScenarioToFlow(prediction.getExpectedValue(), BestTurnType.PROJECT, prediction.getCard());
            }
        }

        if (possibleTurns.contains(TurnType.BUILD_GREEN_PROJECT)) {
            BuildProjectPrediction prediction = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.GREEN), ProjectionStrategy.FROM_PHASE);

            if (prediction.isCanBuild()) {
                bestTurnFlow.addScenarioToFlow(prediction.getExpectedValue(), BestTurnType.PROJECT, prediction.getCard());
            }
        }

        if (possibleTurns.contains(TurnType.PICK_EXTRA_CARD)) {
            MarsGame stateAfterTakingExtraCard = aiProjectionService.projectTakeExtraCard(game, player);
            float projectedChance = deepNetwork.testState(stateAfterTakingExtraCard, stateAfterTakingExtraCard.getPlayerByUuid(player.getUuid()));

            bestTurnFlow.addScenarioToFlow(projectedChance, BestTurnType.EXTRA_CARD);
        }

        if (bestTurnFlow.getBestTurnType() == BestTurnType.SKIP) {
            aiTurnService.skipTurn(player);
        } else if (bestTurnFlow.getBestTurnType() == BestTurnType.EXTRA_CARD) {
            aiTurnService.pickExtraCardTurnAsync(player);
        } else if (bestTurnFlow.getBestTurnType() == BestTurnType.UNMI) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else if (bestTurnFlow.getBestTurnType() == BestTurnType.PROJECT) {
            aiTurnService.buildProject(
                    game,
                    player,
                    bestTurnFlow.getCard().getId(),
                    aiPaymentHelper.getCardPayments(player, bestTurnFlow.getCard()),
                    aiCardParamsHelper.getInputParamsForBuild(player, bestTurnFlow.getCard())
            );
        } else {
            throw new IllegalStateException("Not reachable");
        }
    }

}
