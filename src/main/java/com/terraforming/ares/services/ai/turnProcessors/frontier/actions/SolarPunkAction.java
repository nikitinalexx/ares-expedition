package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class SolarPunkAction extends BlueAction {

    public SolarPunkAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.mc >= (Math.max(0, 15 - stateContext.getPlayer().getTitaniumIncome() * 2));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (Math.max(0, 15 - stateContext.getPlayer().getTitaniumIncome() * 2));
        stateContext.forestBuilt(state);
    }

}
