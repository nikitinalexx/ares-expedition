package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.WoodBurningStoves;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class WoodBurningStovesActionValidator implements ActionValidator<WoodBurningStoves> {
    private final TerraformingService terraformingService;

    @Override
    public Class<WoodBurningStoves> getType() {
        return WoodBurningStoves.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (!terraformingService.canRaiseTemperature()) {
            return "Can't raise Temperature anymore";
        }

        int price = player.getChosenPhase() == 3 ? 3 : 4;

        if (player.getPlants() < price) {
            return "Not enough Plants to perform the action";
        }

        return null;
    }
}
