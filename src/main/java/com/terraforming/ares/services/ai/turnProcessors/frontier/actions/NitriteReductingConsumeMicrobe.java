package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class NitriteReductingConsumeMicrobe extends BlueAction {

    public NitriteReductingConsumeMicrobe(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOceansMax() && state.nitriteReductingBacteria >= 3;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.nitriteReductingBacteria -= 3;
        stateContext.oceanBuilt(state);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return 3;
    }
}
