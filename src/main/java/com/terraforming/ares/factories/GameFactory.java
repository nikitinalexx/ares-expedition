package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface GameFactory {

    MarsGame createMarsGame(GameParameters gameParameters);

}
