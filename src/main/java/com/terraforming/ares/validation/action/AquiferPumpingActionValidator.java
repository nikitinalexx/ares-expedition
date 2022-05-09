package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.AquiferPumping;
import com.terraforming.ares.mars.MarsGame;
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
public class AquiferPumpingActionValidator implements ActionValidator<AquiferPumping> {
    private final TerraformingService terraformingService;

    @Override
    public Class<AquiferPumping> getType() {
        return AquiferPumping.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canRevealOcean(game)) {
            return "Can not reveal oceans anymore";
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
