package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.GreenHouses;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class GreenHousesActionValidator implements ActionValidator<GreenHouses> {
    @Override
    public Class<GreenHouses> getType() {
        return GreenHouses.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "GreenHouses action expects an input parameter";
        }

        Integer input = inputParameters.get(0);
        if (input < 1 || input > 4) {
            return "GreenHouses action may only seel 1 to 4 heat";
        }

        if (player.getHeat() < input) {
            return "Not enough heat";
        }

        return null;
    }
}
