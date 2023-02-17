package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Warehouses;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
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
public class WarehousesOnBuiltEffectValidator implements OnBuiltEffectValidator<Warehouses> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Warehouses: requires an input with the phase card 0 to 9 to update";

    @Override
    public Class<Warehouses> getType() {
        return Warehouses.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer cardId = cardInput.get(0);

        if (cardId < 0 || cardId > 9) {
            return "Invalid phase upgrade card chosen, only from 0 to 9 applicable";
        }

        return null;
    }
}
