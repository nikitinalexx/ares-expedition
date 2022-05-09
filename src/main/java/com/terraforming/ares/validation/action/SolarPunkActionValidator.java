package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.SolarPunk;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
public class SolarPunkActionValidator implements ActionValidator<SolarPunk> {

    @Override
    public Class<SolarPunk> getType() {
        return SolarPunk.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        int forestPrice = Math.max(0, 15 - 2 * player.getTitaniumIncome());

        if (player.getMc() < forestPrice) {
            return "Not enough MC to build a forest";
        }

        return null;
    }
}
