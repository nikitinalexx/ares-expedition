package com.terraforming.ares.processors.action;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public interface BlueActionCardProcessor<T extends Card> {

    Class<T> getType();

    default TurnResponse process(MarsGame game, Player player, Card actionCard) {
        return null;
    }

    default TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        return process(game, player, actionCard);
    }

}
