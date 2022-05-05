package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.AquiferPumping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
public class AquiferPumpingActionValidator implements ActionValidator<AquiferPumping> {
    @Override
    public Class<AquiferPumping> getType() {
        return AquiferPumping.class;
    }

    @Override
    public String validate(Planet planet, PlayerContext player) {
        if (!planet.isValidNumberOfOceans(
                numberOfOceans -> numberOfOceans < Constants.MAX_OCEANS
        )) {
            //TODO not applicable if the last ocean flipped in current stage
            return "Number of oceans is already maximum";
        }

        int flipPrice = 10;
        flipPrice -= player.getSteelIncome() * 2;
        flipPrice = Math.max(0, flipPrice);

        if (player.getMc() < flipPrice) {
            return "Not enough MC to flip the ocean";
        }

        return null;
    }
}
