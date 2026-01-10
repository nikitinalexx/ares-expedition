package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public interface StateEffect {
    void apply(State state);
    boolean isApplicable(StateContext stateContext, State state);
    boolean shouldTraverseSpecialEffect(StateContext stateContext);
}
