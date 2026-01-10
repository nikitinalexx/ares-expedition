package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.model.Constants;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class BrainstormingSessionAction extends BlueAction {

    public BrainstormingSessionAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.extraMcValue += Constants.GREEN_CARDS_RATIOO + ((Constants.RED_CARDS_RATIOO + Constants.BLUE_CARDS_RATIOO) * 3);
    }

}
