package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiSkipTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final TerraformingService terraformingService;
    private final StandardProjectService standardProjectService;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        if (game.gameEndCondition() && game.getStateType() == StateType.PERFORM_BLUE_ACTION) {
            if (player.getHand().size() != 0) {
                aiTurnService.sellCards(player, game, player.getHand().getCards());
                return true;
            } else {
                if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                    String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                    if (validationResult == null) {
                        aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                        return true;
                    }
                }
                String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.OCEAN);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                    return true;
                }
                validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.TEMPERATURE);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                    return true;
                }
                validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                    return true;
                }
            }

            aiTurnService.confirmGameEnd(game, player);
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }

}
