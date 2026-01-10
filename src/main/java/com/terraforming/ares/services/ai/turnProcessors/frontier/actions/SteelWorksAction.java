package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class SteelWorksAction extends BlueAction {

    public SteelWorksAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.heat >= 6;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.heat -= 6;
        state.mc += 2;
        if (!stateContext.isOxygenMax()) {
            stateContext.oxygenBuilt(state);
        }
    }

}
