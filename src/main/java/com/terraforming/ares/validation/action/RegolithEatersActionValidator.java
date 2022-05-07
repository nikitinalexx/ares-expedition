package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.RegolithEaters;
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
public class RegolithEatersActionValidator implements ActionValidator<RegolithEaters> {
    @Override
    public Class<RegolithEaters> getType() {
        return RegolithEaters.class;
    }

    @Override
    public String validate(Planet planet, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "RegolithEaters expects input to add or remove microbes";
        }

        Integer input = inputParameters.get(0);
        if (input != 1 && input != 2) {
            return "RegolithEaters invalid input, should be either 1 or 2";
        }

        if (input == 2 && player.getCardResourcesCount().get(RegolithEaters.class) < 2) {
            return "Not enough microbes to raise oxygen";
        }

        if (input == 2 && planet.isOxygenMax()) {
            //TODO not applicable if got max this phase
            return "Can't raise the Oxygen level, it's already max";
        }

        return null;
    }
}
