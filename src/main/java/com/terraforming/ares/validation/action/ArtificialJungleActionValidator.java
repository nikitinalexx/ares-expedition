package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ArtificialJungle;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
public class ArtificialJungleActionValidator implements ActionValidator<ArtificialJungle> {
    @Override
    public Class<ArtificialJungle> getType() {
        return ArtificialJungle.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getPlants() < 1) {
            return "Not enough plants to get a card";
        }

        return null;
    }

}
