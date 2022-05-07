package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Tardigrades;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class TardigradesActionProcessor implements BlueActionCardProcessor<Tardigrades> {

    @Override
    public Class<Tardigrades> getType() {
        return Tardigrades.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.getCardResourcesCount().put(
                Tardigrades.class,
                player.getCardResourcesCount().get(Tardigrades.class) + 1
        );

        return null;
    }

}
