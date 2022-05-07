package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.CommunityGardens;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
public class CommunityGardensActionProcessor implements BlueActionCardProcessor<CommunityGardens> {
    @Override
    public Class<CommunityGardens> getType() {
        return CommunityGardens.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        player.setMc(player.getMc() + 2);
        if (player.getChosenPhase() == 3) {
            player.setPlants(player.getPlants() + 1);
        }
        return null;
    }
}
