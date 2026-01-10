package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.model.Constants;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class CelestiorAction extends BlueAction {

    public CelestiorAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.extraMcValue += Constants.RED_CARDS_RATIOO * 9;
    }

}
