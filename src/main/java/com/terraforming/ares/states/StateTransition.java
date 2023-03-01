package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
public interface StateTransition<State> {

    void transitionIntoState(MarsGame game);

}
