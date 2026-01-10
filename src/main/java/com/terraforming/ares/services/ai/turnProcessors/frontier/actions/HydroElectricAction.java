package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class HydroElectricAction extends BlueAction {

    public HydroElectricAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.mc >= 1;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc--;
        state.heat += 2;
        if (stateContext.isChose3rdPhase()) {
            state.heat++;
        }
    }

}
