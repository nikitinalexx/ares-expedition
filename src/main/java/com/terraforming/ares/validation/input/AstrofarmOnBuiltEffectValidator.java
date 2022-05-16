package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Astrofarm;
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
 * Creation date 08.05.2022
 */
@Component
@RequiredArgsConstructor
public class AstrofarmOnBuiltEffectValidator implements OnBuiltEffectValidator<Astrofarm> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Astrofarm: requires an input with the card to put resources on";
    private final CardService cardService;

    @Override
    public Class<Astrofarm> getType() {
        return Astrofarm.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.ASTROFARM_PUT_RESOURCE.getId());

        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer cardId = cardInput.get(0);

        if (cardId == InputFlag.SKIP_ACTION.getId()) {
            return null;
        }

        if (!player.getPlayed().containsCard(cardId)) {
            return "Selected card was not built by player";
        }

        Card inputCard = cardService.getCard(cardId);
        if (inputCard.getCollectableResource() != CardCollectableResource.MICROBE) {
            return "Selected card does not collect Microbes";
        }

        return null;
    }
}
