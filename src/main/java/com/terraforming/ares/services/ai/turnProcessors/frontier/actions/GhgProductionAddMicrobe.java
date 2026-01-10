package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class GhgProductionAddMicrobe extends BlueAction {

    public GhgProductionAddMicrobe(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.ghgBacteriaCount++;
        stateContext.onMicrobeGained(state, 1);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return 1;
    }
}
