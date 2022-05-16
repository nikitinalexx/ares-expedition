package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.LargeConvoy;
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
public class LargeConvoyOnBuiltEffectValidator implements OnBuiltEffectValidator<LargeConvoy> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "LargeConvoy requires a choice between Plants and Animals";
    private final CardService cardService;

    @Override
    public Class<LargeConvoy> getType() {
        return LargeConvoy.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.LARGE_CONVOY_PICK_PLANT.getId())
                && !input.containsKey(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId())) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        if (input.containsKey(InputFlag.LARGE_CONVOY_PICK_PLANT.getId())) {
            return null;
        }

        List<Integer> animalsInput = input.get(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId());

        if (CollectionUtils.isEmpty(animalsInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer animalsCardId = animalsInput.get(0);

        if (!player.getPlayed().containsCard(animalsCardId)) {
            return "Player doesn't have the selected animal card built";
        }
        Card animalsCard = cardService.getCard(animalsCardId);
        if (animalsCard.getCollectableResource() != CardCollectableResource.ANIMAL) {
            return "Selected card doesn't collect animals";
        }

        return null;
    }
}
