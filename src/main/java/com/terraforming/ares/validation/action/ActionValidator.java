package com.terraforming.ares.validation.action;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface ActionValidator<T extends Card> {

    Class<T> getType();

    default String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        return validate(game, player);
    }

    default String validate(MarsGame game, Player player) {
        return null;
    }

}
