package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.NitriteReductingBacteria;
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
public class NitriteReductingBacteriaActionValidator implements ActionValidator<NitriteReductingBacteria> {
    private final TerraformingService terraformingService;

    @Override
    public Class<NitriteReductingBacteria> getType() {
        return NitriteReductingBacteria.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "NitriteReductingBacteria expects input to add or remove microbes";
        }

        Integer input = inputParameters.get(0);
        if (input != 1 && input != 3) {
            return "NitriteReductingBacteria invalid input, should be either 1 or 3";
        }

        if (input == 3 && player.getCardResourcesCount().get(NitriteReductingBacteria.class) < 3) {
            return "Not enough microbes to flip an ocean";
        }

        if (input == 3 && !terraformingService.canRevealOcean(game)) {
            return "Can not reveal oceans anymore";
        }

        return null;
    }
}
