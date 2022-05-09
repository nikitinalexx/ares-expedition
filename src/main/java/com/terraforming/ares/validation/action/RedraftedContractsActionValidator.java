package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.RedraftedContracts;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class RedraftedContractsActionValidator implements ActionValidator<RedraftedContracts>{
    @Override
    public Class<RedraftedContracts> getType() {
        return RedraftedContracts.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters) || inputParameters.size() > 3) {
            return "RedraftedContracts expects an input of 1-3 cards";
        }

        for (Integer cardId : inputParameters) {
            if (!player.getHand().containsCard(cardId)) {
                return "RedraftedContracts can't discard a card that you don't have";
            }
        }

        return null;
    }
}
