package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public class PickPhaseState extends AbstractState {

    public PickPhaseState(MarsGame marsGame, CardService cardService, StateTransitionService stateTransitionService) {
        super(marsGame, cardService, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        if (marsGame.getPlayerByUuid(stateContext.getPlayerUuid()).getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(TurnType.PICK_PHASE);
        }
    }

    @Override
    public void updateState() {
        stateTransitionService.performStateTransferFromPhase(marsGame, 0);
    }

}
