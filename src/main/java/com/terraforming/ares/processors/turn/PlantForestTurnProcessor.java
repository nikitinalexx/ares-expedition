package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PlantForestTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class PlantForestTurnProcessor implements TurnProcessor<PlantForestTurn> {
    private final TerraformingService terraformingService;
    private final PaymentValidationService paymentValidationService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.PLANT_FOREST;
    }

    @Override
    public TurnResponse processTurn(PlantForestTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setPlants(player.getPlants() - paymentValidationService.forestPriceInPlants(player));
        terraformingService.buildForest(marsContextProvider.provide(game, player));

        return null;
    }
}
