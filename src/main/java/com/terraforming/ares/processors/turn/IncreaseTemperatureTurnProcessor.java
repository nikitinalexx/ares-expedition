package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.IncreaseTemperatureTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class IncreaseTemperatureTurnProcessor implements TurnProcessor<IncreaseTemperatureTurn> {
    private final TerraformingService terraformingService;

    @Override
    public TurnType getType() {
        return TurnType.INCREASE_TEMPERATURE;
    }

    @Override
    public TurnResponse processTurn(IncreaseTemperatureTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setHeat(player.getHeat() - Constants.TEMPERATURE_HEAT_COST);
        terraformingService.increaseTemperature(game, player);

        return null;
    }
}
