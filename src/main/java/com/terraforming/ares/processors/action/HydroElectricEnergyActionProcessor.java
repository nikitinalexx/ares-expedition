package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.HydroElectricEnergy;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class HydroElectricEnergyActionProcessor implements BlueActionCardProcessor<HydroElectricEnergy> {
    @Override
    public Class<HydroElectricEnergy> getType() {
        return HydroElectricEnergy.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.setMc(player.getMc() - 1);
        player.setHeat(player.getHeat() + 2 + (player.getChosenPhase() == 3 ? 1 : 0));

        return null;
    }
}
