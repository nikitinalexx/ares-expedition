package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.RedraftedContracts;
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
public class RedraftedContractsActionValidator implements ActionValidator<RedraftedContracts> {
    public static final String ERROR_MESSAGE = "RedraftedContracts expects an input of 1-3 cards";

    @Override
    public Class<RedraftedContracts> getType() {
        return RedraftedContracts.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> cardsInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());
        if (CollectionUtils.isEmpty(cardsInput) || cardsInput.size() > 3) {
            return ERROR_MESSAGE;
        }

        for (Integer cardId : cardsInput) {
            if (!player.getHand().containsCard(cardId)) {
                return "RedraftedContracts can't discard a card that you don't have";
            }
        }

        return null;
    }
}
