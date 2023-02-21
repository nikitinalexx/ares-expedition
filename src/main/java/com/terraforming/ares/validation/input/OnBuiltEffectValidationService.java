package com.terraforming.ares.validation.input;

import com.terraforming.ares.model.InputFlag;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 18.02.2023
 */
@Service
public class OnBuiltEffectValidationService {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Project requires an input with the phase card 0 to 9 to upgrade";

    public String validatePhaseUpgrade(Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        return validatePhaseUpgrade(cardInput);
    }

    public String validatePhaseUpgrade(List<Integer> cardInput) {
        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer cardId = cardInput.get(0);

        if (cardId < 0 || cardId > 9) {
            return "Invalid phase upgrade chosen, only from 0 to 9 applicable";
        }

        return null;
    }

}
