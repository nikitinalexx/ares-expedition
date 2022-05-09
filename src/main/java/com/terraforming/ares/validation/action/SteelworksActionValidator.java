package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.Steelworks;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class SteelworksActionValidator implements ActionValidator<Steelworks> {

    @Override
    public Class<Steelworks> getType() {
        return Steelworks.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getHeat() < 6) {
            return "Not enough Heat to perform the action";
        }

        return null;
    }

}
