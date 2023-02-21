package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.PerfluorocarbonProduction;
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
public class PerfluorocarbonProductionOnBuiltEffectValidator implements OnBuiltEffectValidator<PerfluorocarbonProduction> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Project requires an input with the phase 1 upgrade";

    @Override
    public Class<PerfluorocarbonProduction> getType() {
        return PerfluorocarbonProduction.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer upgrade = cardInput.get(0);

        int phaseIndex = upgrade / 2;

        if (phaseIndex != 0) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        return null;
    }
}
