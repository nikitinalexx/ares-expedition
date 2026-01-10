package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public abstract class Action {
    public int id;

    public Action(int id) {
        this.id = id;
    }

    public abstract boolean canApplyInternal(StateContext stateContext, State state);

    public void apply(StateContext stateContext, State state) {
        applyInternal(stateContext, state);
    }

    protected abstract void applyInternal(StateContext stateContext, State state);

    public Object getContext(StateContext stateContext) {
        return null;
    }
}
