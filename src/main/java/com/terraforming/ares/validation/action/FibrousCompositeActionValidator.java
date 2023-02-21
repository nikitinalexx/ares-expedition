package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.FibrousCompositeMaterial;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.validation.input.OnBuiltEffectValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
@RequiredArgsConstructor
public class FibrousCompositeActionValidator implements ActionValidator<FibrousCompositeMaterial> {
    public static final String ERROR_MESSAGE = "FibrousCompositeMaterial expects input to add or remove science resources";
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    @Override
    public Class<FibrousCompositeMaterial> getType() {
        return FibrousCompositeMaterial.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> addDiscardInput = inputParameters.get(InputFlag.ADD_DISCARD_MICROBE.getId());
        if (CollectionUtils.isEmpty(addDiscardInput)) {
            return ERROR_MESSAGE;
        }

        Integer input = addDiscardInput.get(0);
        if (input != 1 && input != 3) {
            return "FibrousCompositeMaterial invalid input, should be either 1 or 3";
        }

        if (input == 3 && player.getCardResourcesCount().get(FibrousCompositeMaterial.class) < 3) {
            return "Not enough science to upgrade a phase card";
        }

        if (input == 3) {
            return onBuiltEffectValidationService.validatePhaseUpgrade(inputParameters);
        }

        return null;
    }
}
