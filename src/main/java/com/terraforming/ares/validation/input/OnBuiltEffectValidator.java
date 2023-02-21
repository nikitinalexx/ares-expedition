package com.terraforming.ares.validation.input;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public interface OnBuiltEffectValidator<T extends Card> {

    Class<T> getType();

    default String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        return null;
    }

    default String validate(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> input) {
        return validate(card, player, input);
    }

}
