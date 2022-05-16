package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.ImportedNitrogen;
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
public class ImportedNitrogenOnBuiltEffectValidator implements OnBuiltEffectValidator<ImportedNitrogen> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "ImportedNitrogen requires input both for Animals and Microbes action";
    private final CardService cardService;

    @Override
    public Class<ImportedNitrogen> getType() {
        return ImportedNitrogen.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId())
                || !input.containsKey(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId())) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        List<Integer> animalsInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId());
        List<Integer> microbesInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId());

        if (CollectionUtils.isEmpty(animalsInput) || CollectionUtils.isEmpty(microbesInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer animalsCardId = animalsInput.get(0);
        Integer microbesCardId = microbesInput.get(0);

        if (animalsCardId != InputFlag.SKIP_ACTION.getId()) {
            if (!player.getPlayed().containsCard(animalsCardId)) {
                return "Player doesn't have the selected animal card built";
            }
            Card animalsCard = cardService.getCard(animalsCardId);
            if (animalsCard.getCollectableResource() != CardCollectableResource.ANIMAL) {
                return "Selected card doesn't collect animals";
            }
        }

        if (microbesCardId != InputFlag.SKIP_ACTION.getId()) {
            if (!player.getPlayed().containsCard(microbesCardId)) {
                return "Player doesn't have the selected microbe card built";
            }
            Card microbeCard = cardService.getCard(microbesCardId);
            if (microbeCard.getCollectableResource() != CardCollectableResource.MICROBE) {
                return "Selected card doesn't collect microbes";
            }
        }

        return null;
    }

}
