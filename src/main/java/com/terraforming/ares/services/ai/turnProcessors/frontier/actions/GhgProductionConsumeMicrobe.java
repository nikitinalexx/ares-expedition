package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class GhgProductionConsumeMicrobe extends BlueAction {

    public GhgProductionConsumeMicrobe(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isTemperatureMax() && state.ghgBacteriaCount >= 2;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.ghgBacteriaCount -= 2;
        stateContext.temperatureBuilt(state);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return 2;
    }
}
