package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.PowerInfrastructure;
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
public class PowerInfrastructureActionValidator implements ActionValidator<PowerInfrastructure> {
    @Override
    public Class<PowerInfrastructure> getType() {
        return PowerInfrastructure.class;
    }

    @Override
    public String validate(Planet planet, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "PowerInfrastructure requires an input of how much Heat you'd like to sell";
        }

        Integer heatToSell = inputParameters.get(0);
        if (player.getHeat() < heatToSell) {
            return "Not enough Heat to perform the action";
        }

        return null;
    }

}
