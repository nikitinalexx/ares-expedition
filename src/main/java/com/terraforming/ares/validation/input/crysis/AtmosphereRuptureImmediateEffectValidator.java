package com.terraforming.ares.validation.input.crysis;

import com.terraforming.ares.cards.crysis.AtmosphereRupture;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
@Component
public class AtmosphereRuptureImmediateEffectValidator implements ImmediateEffectValidator<AtmosphereRupture> {
    private static final String ERROR_MESSAGE = "Atmosphere Rupture expects input with choice of MC or TR";

    @Override
    public Class<AtmosphereRupture> getType() {
        return AtmosphereRupture.class;
    }

    @Override
    public String validate(MarsGame game, CrysisCard card, Player player, Map<Integer, List<Integer>> input) {
        if (CollectionUtils.isEmpty(input) || !input.containsKey(InputFlag.CRYSIS_INPUT_FLAG.getId())) {
            return ERROR_MESSAGE;
        }

        final List<Integer> crysisInput = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (CollectionUtils.isEmpty(crysisInput)) {
            return ERROR_MESSAGE;
        }
        int choiceOption = crysisInput.get(0);

        if (choiceOption != InputFlag.CRYSIS_INPUT_OPTION_1.getId() && choiceOption != InputFlag.CRYSIS_INPUT_OPTION_2.getId()) {
            return ERROR_MESSAGE;
        }

        if (choiceOption == InputFlag.CRYSIS_INPUT_OPTION_1.getId() && player.getMc() < 5) {
            return "Not enough MC";
        }

        return null;
    }
}
