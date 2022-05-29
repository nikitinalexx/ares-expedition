package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@Component
@RequiredArgsConstructor
public class SyntheticCatastropheOnBuiltEffectValidator implements OnBuiltEffectValidator<SyntheticCatastrophe> {
    private static final String ERROR_MESSAGE_INPUT_MISSING = "SyntheticCatastrophe: you are required to select a card";
    private final CardService cardService;

    @Override
    public Class<SyntheticCatastrophe> getType() {
        return SyntheticCatastrophe.class;
    }

    @Override
    public String validate(Card builtCard, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId())) {
            return ERROR_MESSAGE_INPUT_MISSING;
        }

        List<Integer> cardInput = input.get(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId());
        if (CollectionUtils.isEmpty(cardInput)) {
            return ERROR_MESSAGE_INPUT_MISSING;
        }

        int cardId = cardInput.get(0);

        if (!player.getPlayed().containsCard(cardId)) {
            return "You can't select a card you haven't built";
        }

        Card targetCard = cardService.getCard(cardId);
        if (targetCard.getColor() != CardColor.RED) {
            return "Selected card is not RED";
        }

        return null;
    }
}
