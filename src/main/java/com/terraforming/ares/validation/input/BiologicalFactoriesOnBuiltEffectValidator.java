package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.BiologicalFactories;
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
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BiologicalFactoriesOnBuiltEffectValidator implements OnBuiltEffectValidator<BiologicalFactories> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Project requires an input with the phase 4 upgrade";

    @Override
    public Class<BiologicalFactories> getType() {
        return BiologicalFactories.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer upgrade = cardInput.get(0);

        int phaseIndex = upgrade / 2;

        if (phaseIndex != 3) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        return null;
    }
}
