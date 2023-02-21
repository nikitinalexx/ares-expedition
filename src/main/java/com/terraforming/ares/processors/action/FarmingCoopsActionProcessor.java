package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.FarmingCoops;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class FarmingCoopsActionProcessor implements BlueActionCardProcessor<FarmingCoops> {

    @Override
    public Class<FarmingCoops> getType() {
        return FarmingCoops.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer cardToSell = inputParameters.get(InputFlag.CARD_CHOICE.getId()).get(0);

        player.getHand().removeCards(List.of(cardToSell));

        player.setPlants(player.getPlants() + 3);

        return null;
    }

}
