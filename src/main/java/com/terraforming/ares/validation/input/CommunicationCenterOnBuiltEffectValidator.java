package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.CommunicationCenter;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class CommunicationCenterOnBuiltEffectValidator implements OnBuiltEffectValidator<CommunicationCenter> {

    @Override
    public Class<CommunicationCenter> getType() {
        return CommunicationCenter.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getHeatIncome() < 1) {
            return "Not enough heat income to build the project";
        }

        return null;
    }
}
