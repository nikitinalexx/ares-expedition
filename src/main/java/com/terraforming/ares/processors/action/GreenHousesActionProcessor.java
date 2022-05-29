package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.GreenHouses;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class GreenHousesActionProcessor implements BlueActionCardProcessor<GreenHouses> {
    @Override
    public Class<GreenHouses> getType() {
        return GreenHouses.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        Integer input = inputParameters.get(0);

        player.setHeat(player.getHeat() - input);
        player.setPlants(player.getPlants() + input);

        return null;
    }
}
