package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
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
public class AiPlantForestTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;

    @Override
    public TurnType getType() {
        return TurnType.PLANT_FOREST;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        aiTurnService.plantForest(game, player);
    }

}
