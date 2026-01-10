package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class ProgressivePoliciesAction extends BlueAction {

    public ProgressivePoliciesAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOxygenMax() && state.mc >= (10 - (stateContext.getEventTagCount() >= 4 ? 5 : 0));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (10 - (stateContext.getEventTagCount() >= 4 ? 5 : 0));
        stateContext.oxygenBuilt(state);
    }

}
