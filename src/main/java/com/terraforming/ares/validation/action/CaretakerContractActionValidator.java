package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.CaretakerContract;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
public class CaretakerContractActionValidator implements ActionValidator<CaretakerContract> {
    @Override
    public Class<CaretakerContract> getType() {
        return CaretakerContract.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (player.getHeat() < 8) {
            return "Not enough HEAT to get TR";
        }

        return null;
    }
}
