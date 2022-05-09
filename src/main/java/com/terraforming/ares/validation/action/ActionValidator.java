package com.terraforming.ares.validation.action;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface ActionValidator<T extends ProjectCard> {

    Class<T> getType();

    default String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        return validate(game, player);
    }

    default String validate(MarsGame game, Player player) {
        return null;
    }

}
