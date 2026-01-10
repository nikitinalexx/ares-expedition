package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class DevelopmentCenterAction extends BlueAction {

    public DevelopmentCenterAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.heat >= 2;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.heat -= 2;
        state.cards++;
    }

}
