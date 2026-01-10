package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class AquiferPumpingAction extends BlueAction {

    public AquiferPumpingAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isOceansMax() && state.mc >= (Math.max(0, 10 - stateContext.getPlayer().getSteelIncome() * 2));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (Math.max(0, 10 - stateContext.getPlayer().getSteelIncome() * 2));
        stateContext.oceanBuilt(state);
    }

}
