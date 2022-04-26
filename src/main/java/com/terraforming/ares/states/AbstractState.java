package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public abstract class AbstractState implements State{
    protected final MarsGame marsGame;

    protected AbstractState(MarsGame marsGame) {
        this.marsGame = marsGame;
    }

    @Override
    public Action nextAction(String playerUuid) {
        if (marsGame.getPlayerByUuid(playerUuid).getNextTurn() != null) {
            return Action.WAIT;
        } else {
            return Action.TURN;
        }
    }
}
