package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class MatterGeneratorActionValidator implements ActionValidator<MatterGenerator> {

    @Override
    public Class<MatterGenerator> getType() {
        return MatterGenerator.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "Matter Generator expects a card to sell for 6 MC";
        }

        Integer cardIdToSell = inputParameters.get(0);

        if (!player.getHand().containsCard(cardIdToSell)) {
            return "Player doesn't have a selected card to sell for 6 MC";
        }

        return null;
    }

}
