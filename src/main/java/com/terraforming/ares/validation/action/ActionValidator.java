package com.terraforming.ares.validation.action;

import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public interface ActionValidator<T extends ProjectCard> {

    Class<T> getType();

    String validate(Planet planet, PlayerContext player);

}
