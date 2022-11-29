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
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiBuildGreenProjectTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final AiBuildProjectService aiBuildProjectService;

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        final BuildProjectPrediction prediction = aiBuildProjectService.getBestProjectToBuild(game, player, Set.of(CardColor.GREEN));

        if (!prediction.isCanBuild()) {
            aiTurnService.skipTurn(player);
            return;
        }

        aiTurnService.buildGreenProject(
                game,
                player,
                prediction.getCard().getId(),
                aiPaymentHelper.getCardPayments(player, prediction.getCard()),
                aiCardParamsHelper.getInputParamsForBuild(player, prediction.getCard())
        );
    }

}
