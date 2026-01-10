package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class GasCooledReactorsAction extends BlueAction {

    public GasCooledReactorsAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isTemperatureMax() && state.mc >= (12 - stateContext.getPlayer().countPhaseUpgrades() * 2);
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (12 - stateContext.getPlayer().countPhaseUpgrades() * 2);
        stateContext.temperatureBuilt(state);
    }

}
