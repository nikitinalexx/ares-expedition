package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.PowerInfrastructure;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class PowerInfrastructureActionProcessor implements BlueActionCardProcessor<PowerInfrastructure> {

    @Override
    public Class<PowerInfrastructure> getType() {
        return PowerInfrastructure.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        Integer heatToSell = inputParameters.get(0);

        player.setHeat(player.getHeat() - heatToSell);
        player.setMc(player.getMc() + heatToSell);

        return null;
    }

}
