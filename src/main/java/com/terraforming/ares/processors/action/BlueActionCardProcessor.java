package com.terraforming.ares.processors.action;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public interface BlueActionCardProcessor<T extends ProjectCard> {

    Class<T> getType();

    default TurnResponse process(MarsGame game, PlayerContext player) {
        return null;
    }

    default TurnResponse process(MarsGame game, PlayerContext player, List<Integer> inputParameters) {
        return process(game, player);
    }

}
