package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.GhgProductionBacteria;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class GhgProductionBacteriaActionValidator implements ActionValidator<GhgProductionBacteria> {
    private final TerraformingService terraformingService;

    @Override
    public Class<GhgProductionBacteria> getType() {
        return GhgProductionBacteria.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "GhgProductionBacteria expects input to add or remove microbes";
        }

        Integer input = inputParameters.get(0);
        if (input != 1 && input != 2) {
            return "GhgProductionBacteria invalid input, should be either 1 or 2";
        }

        if (input == 2 && player.getCardResourcesCount().get(GhgProductionBacteria.class) < 2) {
            return "Not enough microbes to raise temperature";
        }

        if (input == 2 && !terraformingService.canIncreaseTemperature(game)) {
            return "Can not increase temperature anymore";
        }

        return null;
    }
}
