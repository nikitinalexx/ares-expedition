package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public interface TurnProcessor<T extends Turn> {

    TurnType getType();

    TurnResponse processTurn(T turn, MarsGame game);

}
