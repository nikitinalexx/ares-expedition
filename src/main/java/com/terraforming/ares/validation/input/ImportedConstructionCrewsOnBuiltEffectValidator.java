package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.ImportedConstructionCrews;
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
public class ImportedConstructionCrewsOnBuiltEffectValidator implements OnBuiltEffectValidator<ImportedConstructionCrews> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Project requires an input with 2 phase cards to upgrade";

    @Override
    public Class<ImportedConstructionCrews> getType() {
        return ImportedConstructionCrews.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        if (CollectionUtils.isEmpty(cardInput) || cardInput.size() != 2) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer firstCard = cardInput.get(0);
        Integer secondCard = cardInput.get(1);

        if (firstCard < 0 || firstCard > 9 || secondCard < 0 || secondCard > 9) {
            return "Invalid phase upgrade chosen, only from 0 to 9 applicable";
        }

        if (firstCard.equals(secondCard) || (firstCard / 2 == secondCard / 2)) {
            return "Invalid phase upgrade chosen, must update different phases";
        }

        return null;
    }
}
