package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Tardigrades;
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
public class TardigradesActionProcessor implements BlueActionCardProcessor<Tardigrades> {

    @Override
    public Class<Tardigrades> getType() {
        return Tardigrades.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.addResources(actionCard, 1);

        return null;
    }

}
