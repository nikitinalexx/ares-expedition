package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class CollectIncomeState extends AbstractState {

    public CollectIncomeState(MarsGame marsGame, CardService cardService) {
        super(marsGame, cardService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        if (marsGame.getPlayerByUuid(stateContext.getPlayerUuid()).getNextTurn() != null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(TurnType.COLLECT_INCOME);
        }
    }

    @Override
    public void updateState() {
        performStateTransferFromPhase(5);
    }

}
