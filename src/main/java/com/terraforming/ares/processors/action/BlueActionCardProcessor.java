package com.terraforming.ares.processors.action;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public interface BlueActionCardProcessor<T extends ProjectCard> {

    Class<T> getType();

    TurnResponse process(MarsGame game, PlayerContext player);
}
