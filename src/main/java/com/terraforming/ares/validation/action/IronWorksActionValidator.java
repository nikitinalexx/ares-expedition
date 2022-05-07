package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.IronWorks;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class IronWorksActionValidator implements ActionValidator<IronWorks>{
    @Override
    public Class<IronWorks> getType() {
        return IronWorks.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (player.getHeat() < 4) {
            return "Not enough heat to perform the action";
        }

        if (planet.isOxygenMax()) {
            //TODO not applicable if maxed in this phase
            return "Oxygen is already max";
        }

        return null;
    }
}
