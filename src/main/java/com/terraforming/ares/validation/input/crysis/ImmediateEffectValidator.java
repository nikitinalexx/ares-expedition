package com.terraforming.ares.validation.input.crysis;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.Player;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public interface ImmediateEffectValidator<T extends CrysisCard> {

    Class<T> getType();

    String validate(MarsGame game, CrysisCard card, Player player, Map<Integer, List<Integer>> input);

}
