package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.GreenHouses;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class GreenHousesActionValidator implements ActionValidator<GreenHouses> {
    private static final String ERROR_MESSAGE = "GreenHouses action expects an input parameter";

    @Override
    public Class<GreenHouses> getType() {
        return GreenHouses.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> discardHeatInput = inputParameters.get(InputFlag.DISCARD_HEAT.getId());
        if (CollectionUtils.isEmpty(discardHeatInput)) {
            return ERROR_MESSAGE;
        }

        Integer input = discardHeatInput.get(0);
        if (input < 1 || input > 4) {
            return "GreenHouses action may only seel 1 to 4 heat";
        }

        if (player.getHeat() < input) {
            return "Not enough heat";
        }

        return null;
    }
}
