package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.PowerInfrastructure;
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
public class PowerInfrastructureActionValidator implements ActionValidator<PowerInfrastructure> {
    public static final String ERROR_MESSAGE = "PowerInfrastructure requires an input of how much Heat you'd like to sell";

    @Override
    public Class<PowerInfrastructure> getType() {
        return PowerInfrastructure.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> heatToSellInput = inputParameters.get(InputFlag.DISCARD_HEAT.getId());
        if (CollectionUtils.isEmpty(heatToSellInput)) {
            return ERROR_MESSAGE;
        }

        Integer heatToSell = heatToSellInput.get(0);
        if (player.getHeat() < heatToSell) {
            return "Not enough Heat to perform the action";
        }

        return null;
    }

}
