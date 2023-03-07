package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class CollectIncomeState extends AbstractState {

    public CollectIncomeState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerByUuid(stateContext.getPlayerUuid()).getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(TurnType.COLLECT_INCOME);
        }
    }

    @Override
    public void updateState() {
        stateTransitionService.performStateTransferFromPhase(context.getGame(), 5);
    }

}
