package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 22.02.2023
 */
@Component
public class HyperionSystemsActionProcessor implements BlueActionCardProcessor<HyperionSystemsCorporation> {
    @Override
    public Class<HyperionSystemsCorporation> getType() {
        return HyperionSystemsCorporation.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setMc(player.getMc() + (player.getChosenPhase() == 3 ? 2 : 1));
        return null;
    }
}
