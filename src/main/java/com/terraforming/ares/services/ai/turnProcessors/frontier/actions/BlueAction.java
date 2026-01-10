package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public abstract class BlueAction extends Action {

    public BlueAction(int id) {
        super(id);
    }

    public void applyInternal(StateContext stateContext, State state) {
        if (stateContext.isHasAssemblyLines()) {
            state.mc++;
        }
        applyInternal(stateContext, state);
    }

}
