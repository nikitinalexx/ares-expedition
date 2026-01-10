package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class VolcanicPoolsAction extends BlueAction {

    public VolcanicPoolsAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOceansMax() && state.mc >= (Math.max(0, 12 - stateContext.getEnergyTagCount()));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (Math.max(0, 12 - stateContext.getEnergyTagCount()));
        stateContext.oceanBuilt(state);
    }

}
