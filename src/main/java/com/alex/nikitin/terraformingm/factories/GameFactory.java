package com.alex.nikitin.terraformingm.factories;

import com.alex.nikitin.terraformingm.model.Game;
import com.alex.nikitin.terraformingm.model.GameParameters;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface GameFactory {

    Game createGame(GameParameters gameParameters);

}
