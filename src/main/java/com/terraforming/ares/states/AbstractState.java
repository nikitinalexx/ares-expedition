package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public abstract class AbstractState implements State {
    protected final MarsGame marsGame;
    protected final CardService cardService;
    protected final StateTransitionService stateTransitionService;

    protected AbstractState(MarsGame marsGame, CardService cardService, StateTransitionService stateTransitionService) {
        this.marsGame = marsGame;
        this.cardService = cardService;
        this.stateTransitionService = stateTransitionService;
    }

    @Override
    public Action nextAction(StateContext stateContext) {
        if (getPossibleTurns(stateContext).isEmpty()) {
            return Action.WAIT;
        } else {
            return Action.TURN;
        }
    }
}
