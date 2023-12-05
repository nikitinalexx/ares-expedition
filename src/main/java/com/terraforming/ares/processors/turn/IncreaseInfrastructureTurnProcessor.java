package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.IncreaseInfrastructureTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class IncreaseInfrastructureTurnProcessor implements TurnProcessor<IncreaseInfrastructureTurn> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.INCREASE_INFRASTRUCTURE;
    }

    @Override
    public TurnResponse processTurn(IncreaseInfrastructureTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setHeat(player.getHeat() - Constants.INFRASTRUCTURE_HEAT_COST);
        player.setPlants(player.getPlants() - Constants.INFRASTRUCTURE_PLANT_COST);

        terraformingService.increaseInfrastructure(marsContextProvider.provide(game, player, turn.getInputParams()));

        return null;
    }
}
