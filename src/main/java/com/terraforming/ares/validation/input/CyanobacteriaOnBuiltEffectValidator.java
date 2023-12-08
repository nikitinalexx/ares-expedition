package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.Cyanobacteria;
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
public class CyanobacteriaOnBuiltEffectValidator implements OnBuiltEffectValidator<Cyanobacteria> {
    private static final String INCORRECT_INPUT_ERROR_MESSAGE =
            "Cyanobacteria requires input for Microbes";
    private final CardService cardService;

    @Override
    public Class<Cyanobacteria> getType() {
        return Cyanobacteria.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (!input.containsKey(InputFlag.ADD_MICROBE.getId())) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        List<Integer> microbesInput = input.get(InputFlag.ADD_MICROBE.getId());

        if (CollectionUtils.isEmpty(microbesInput)) {
            return INCORRECT_INPUT_ERROR_MESSAGE;
        }

        Integer microbesCardId = microbesInput.get(0);

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
