package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class MatterGeneratorActionValidator implements ActionValidator<MatterGenerator> {
    public static final String ERROR_MESSAGE = "Matter Generator expects a card to sell for 6 MC";

    @Override
    public Class<MatterGenerator> getType() {
        return MatterGenerator.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> cardIdInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());
        if (CollectionUtils.isEmpty(cardIdInput)) {
            return ERROR_MESSAGE;
        }

        Integer cardIdToSell = cardIdInput.get(0);

        if (!player.getHand().containsCard(cardIdToSell)) {
            return "Player doesn't have a selected card to sell for 6 MC";
        }

        return null;
    }

}
