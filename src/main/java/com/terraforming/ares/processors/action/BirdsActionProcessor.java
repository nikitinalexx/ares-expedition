package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Birds;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Service
public class BirdsActionProcessor implements BlueActionCardProcessor<Birds> {
    @Override
    public Class<Birds> getType() {
        return Birds.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.addResources(actionCard, 1);
        return null;
    }
}
