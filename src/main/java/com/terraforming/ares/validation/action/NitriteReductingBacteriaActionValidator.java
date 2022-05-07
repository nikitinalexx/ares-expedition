package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.NitriteReductingBacteria;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class NitriteReductingBacteriaActionValidator implements ActionValidator<NitriteReductingBacteria> {
    @Override
    public Class<NitriteReductingBacteria> getType() {
        return NitriteReductingBacteria.class;
    }

    @Override
    public String validate(Planet planet, Player player, List<Integer> inputParameters) {
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

        if (input == 3 && planet.allOceansRevealed()) {
            //TODO not applicable if got max this phase
            return "Can't flip an Ocean, all oceans already revealed";
        }

        return null;
    }
}
