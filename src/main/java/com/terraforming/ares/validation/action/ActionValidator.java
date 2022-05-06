package com.terraforming.ares.validation.action;

import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface ActionValidator<T extends ProjectCard> {

    Class<T> getType();

    default String validate(Planet planet, PlayerContext player, List<Integer> inputParameters) {
        return validate(planet, player);
    }

    default String validate(Planet planet, PlayerContext player) {
        return null;
    }

}
