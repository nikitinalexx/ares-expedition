package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.IronWorks;
import com.terraforming.ares.mars.MarsGame;
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
public class IronWorksActionValidator implements ActionValidator<IronWorks> {
    private final TerraformingService terraformingService;

    @Override
    public Class<IronWorks> getType() {
        return IronWorks.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (player.getHeat() < 4) {
            return "Not enough heat to perform the action";
        }

        if (!terraformingService.canIncreaseOxygen(game)) {
            return "Can not increase Oxygen anymore";
        }

        return null;
    }
}
