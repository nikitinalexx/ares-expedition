package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class WoodBurningStovesAction extends BlueAction {

    public WoodBurningStovesAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isTemperatureMax() && state.plants >= (stateContext.isChose3rdPhase() ? 3 : 4);
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.plants -= (stateContext.isChose3rdPhase() ? 3 : 4);
        stateContext.temperatureBuilt(state);
    }

}
