package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.turn.TurnType;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface TurnProcessor<T> {

    TurnType getType();

    void processTurn(T turn, MarsGame game);

}
