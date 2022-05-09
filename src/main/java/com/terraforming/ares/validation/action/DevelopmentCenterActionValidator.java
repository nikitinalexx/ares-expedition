package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.DevelopmentCenter;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class DevelopmentCenterActionValidator implements ActionValidator<DevelopmentCenter> {
    @Override
    public Class<DevelopmentCenter> getType() {
        return DevelopmentCenter.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getHeat() < 2) {
            return "You need at least 2 HEAT to perform the action";
        }

        return null;
    }
}
