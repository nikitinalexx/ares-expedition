package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.BiomedicalImports;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@Component
@RequiredArgsConstructor
public class BiomedicalImportsOnBuiltEffectValidator implements OnBuiltEffectValidator<BiomedicalImports> {
    private final OnBuiltEffectValidationService onBuiltEffectValidationService;

    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Biomedical Imports: requires to raise the oxygen or choose a phase upgrade";

    @Override
    public Class<BiomedicalImports> getType() {
        return BiomedicalImports.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> raiseOxygenInput = input.get(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId());
        List<Integer> upgradePhaseFlag = input.get(InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId());

        if (CollectionUtils.isEmpty(raiseOxygenInput) && CollectionUtils.isEmpty(upgradePhaseFlag)
                || !CollectionUtils.isEmpty(raiseOxygenInput) && !CollectionUtils.isEmpty(upgradePhaseFlag)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        if (!CollectionUtils.isEmpty(raiseOxygenInput)) {
            return null;
        }

        List<Integer> upgradePhaseInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        if (CollectionUtils.isEmpty(upgradePhaseInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        return onBuiltEffectValidationService.validatePhaseUpgrade(upgradePhaseInput);
    }
}
