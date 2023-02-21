package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.FarmersMarket;
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
public class FarmersMarketActionProcessor implements BlueActionCardProcessor<FarmersMarket> {
    @Override
    public Class<FarmersMarket> getType() {
        return FarmersMarket.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setMc(player.getMc() - 1);
        player.setPlants(player.getPlants() + 2);
        return null;
    }

}
