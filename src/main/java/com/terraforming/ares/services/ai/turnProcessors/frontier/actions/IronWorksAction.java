package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class IronWorksAction extends BlueAction {

    public IronWorksAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOxygenMax() && state.heat >= 4;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.heat -= 4;
        stateContext.oxygenBuilt(state);
    }

}
