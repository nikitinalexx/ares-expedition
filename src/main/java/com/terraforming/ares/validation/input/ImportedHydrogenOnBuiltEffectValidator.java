package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.ImportedHydrogen;
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
public class ImportedHydrogenOnBuiltEffectValidator implements OnBuiltEffectValidator<ImportedHydrogen> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "ImportedHydrogen: requires an input with choise for either 3 plants or a card to put resources on";
    private final CardService cardService;

    @Override
    public Class<ImportedHydrogen> getType() {
        return ImportedHydrogen.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())
                && !input.containsKey(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId())) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        if (input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())) {
            return null;
        }

        List<Integer> cardInput = input.get(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId());
        if (CollectionUtils.isEmpty(cardInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer inputCardId = cardInput.get(0);

        if (!player.getPlayed().containsCard(inputCardId)) {
            return "Selected card was not built by player";
        }

        Card inputCard = cardService.getCard(inputCardId);
        if (inputCard.getCollectableResource() != CardCollectableResource.MICROBE
                && inputCard.getCollectableResource() != CardCollectableResource.ANIMAL) {
            return "Selected card does not collect Animals or Microbes";
        }

        return null;
    }
}
