package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedHydroElectricEnergy;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class BuffedHydroElectricEnergyActionProcessor implements BlueActionCardProcessor<BuffedHydroElectricEnergy> {
    @Override
    public Class<BuffedHydroElectricEnergy> getType() {
        return BuffedHydroElectricEnergy.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setMc(player.getMc() - 1);
        player.setHeat(player.getHeat() + 2 + (player.getChosenPhase() == 3 ? 2 : 0));

        return null;
    }
}
