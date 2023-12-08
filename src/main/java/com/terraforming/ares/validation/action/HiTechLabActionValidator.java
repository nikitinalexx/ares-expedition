package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.green.HiTechLab;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class HiTechLabActionValidator implements ActionValidator<HiTechLab> {
    private static final String ERROR_MESSAGE = "HiTechLab action expects an input parameter";

    @Override
    public Class<HiTechLab> getType() {
        return HiTechLab.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> discardHeatInput = inputParameters.get(InputFlag.DISCARD_HEAT.getId());
        if (CollectionUtils.isEmpty(discardHeatInput)) {
            return ERROR_MESSAGE;
        }

        Integer heatInput = discardHeatInput.get(0);
        if (heatInput < 1) {
            return "HiTechLab action requires an input with heat";
        }

        if (player.getHeat() < heatInput) {
            return "Not enough heat";
        }

        return null;
    }
}
