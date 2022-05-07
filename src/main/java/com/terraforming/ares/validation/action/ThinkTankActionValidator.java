package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ThinkTank;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class ThinkTankActionValidator implements ActionValidator<ThinkTank>{

    @Override
    public Class<ThinkTank> getType() {
        return ThinkTank.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (player.getMc() < 2) {
            return "Not enough MC to perform the action";
        }

        return null;
    }
}
