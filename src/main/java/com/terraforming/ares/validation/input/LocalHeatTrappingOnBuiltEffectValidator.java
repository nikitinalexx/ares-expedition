package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.LocalHeatTrapping;
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
public class LocalHeatTrappingOnBuiltEffectValidator implements OnBuiltEffectValidator<LocalHeatTrapping> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "LocalHeatTrapping: requires an input with the card to put resources on";
    private final CardService cardService;

    @Override
    public Class<LocalHeatTrapping> getType() {
        return LocalHeatTrapping.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getHeat() < 3) {
            return "Not enough heat";
        }

        List<Integer> cardInput = input.get(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId());

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
        if (inputCard.getCollectableResource() != CardCollectableResource.MICROBE
                && inputCard.getCollectableResource() != CardCollectableResource.ANIMAL) {
            return "Selected card does not collect Animals or Microbes";
        }

        return null;
    }
}
