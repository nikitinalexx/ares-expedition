package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.DroneAssistedConstruction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@Component
public class DroneAssistedConstructionActionProcessor implements BlueActionCardProcessor<DroneAssistedConstruction> {
    @Override
    public Class<DroneAssistedConstruction> getType() {
        return DroneAssistedConstruction.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setMc(player.getMc() + 2);

        if (player.isPhaseUpgraded(player.getChosenPhase())) {
            player.setMc(player.getMc() + 2);
        }
        return null;
    }
}
