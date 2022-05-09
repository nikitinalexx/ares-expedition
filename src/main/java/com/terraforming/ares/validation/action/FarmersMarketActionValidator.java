package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.FarmersMarket;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class FarmersMarketActionValidator implements ActionValidator<FarmersMarket> {
    @Override
    public Class<FarmersMarket> getType() {
        return FarmersMarket.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getMc() < 1) {
            return "Not enough MC to perform action";
        }
        return null;
    }
}
