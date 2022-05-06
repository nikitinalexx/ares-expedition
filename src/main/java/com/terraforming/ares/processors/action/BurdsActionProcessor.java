package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Birds;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Service
public class BurdsActionProcessor implements BlueActionCardProcessor<Birds> {
    @Override
    public Class<Birds> getType() {
        return Birds.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        Integer animalsCount = player.getCardResourcesCount().get(Birds.class);
        player.getCardResourcesCount().put(Birds.class, animalsCount + 1);
        return null;
    }
}
