package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class GameEndState extends AbstractState {

    public GameEndState(MarsGame marsGame, CardService cardService, StateTransitionService stateTransitionService) {
        super(marsGame, cardService, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        return List.of(TurnType.GAME_END);
    }

    @Override
    public void updateState() {
    }
}
