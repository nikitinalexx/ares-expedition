package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.GasCooledReactors;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
public class GasCooledReactorsActionValidator implements ActionValidator<GasCooledReactors> {

    @Override
    public Class<GasCooledReactors> getType() {
        return GasCooledReactors.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        final int upgradedPhaseCards = (int) player.getPhaseCards().stream().filter(card -> card != 0).count();
        int temperaturePrice = Math.max(0, 12 - 2 * upgradedPhaseCards);

        if (player.getMc() < temperaturePrice) {
            return "Not enough MC to raise temperature";
        }

        return null;
    }
}
