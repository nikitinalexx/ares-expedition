package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.green.BusinessNetwork;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class BusinessNetworkActionValidator implements ActionValidator<BusinessNetwork> {

    @Override
    public Class<BusinessNetwork> getType() {
        return BusinessNetwork.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getMc() < 3) {
            return "Not enough MC to perform the action";
        }

        return null;
    }
}
