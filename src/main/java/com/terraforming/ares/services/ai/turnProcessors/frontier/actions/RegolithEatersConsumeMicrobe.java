package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class RegolithEatersConsumeMicrobe extends BlueAction {

    public RegolithEatersConsumeMicrobe(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOxygenMax() && state.regolithEaters >= 2;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.regolithEaters -= 2;
        stateContext.oxygenBuilt(state);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return 2;
    }
}
