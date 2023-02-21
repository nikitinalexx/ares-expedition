package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.PowerInfrastructure;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer heatToSell = inputParameters.get(InputFlag.DISCARD_HEAT.getId()).get(0);

        player.setHeat(player.getHeat() - heatToSell);
        player.setMc(player.getMc() + heatToSell);

        return null;
    }

}
