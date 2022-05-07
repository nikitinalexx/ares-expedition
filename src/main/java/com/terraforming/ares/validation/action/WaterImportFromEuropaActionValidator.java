package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.WaterImportFromEuropa;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
@RequiredArgsConstructor
public class WaterImportFromEuropaActionValidator implements ActionValidator<WaterImportFromEuropa> {
    private final TerraformingService terraformingService;

    @Override
    public Class<WaterImportFromEuropa> getType() {
        return WaterImportFromEuropa.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (!terraformingService.canRevealOcean()) {
            return "Can't flip an ocean anymore";
        }

        int flipPrice = Math.max(0, 12 - player.getTitaniumIncome());

        if (player.getMc() < flipPrice) {
            return "Not enough MC to flip the ocean";
        }

        return null;
    }

}
