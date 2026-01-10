package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class FarmersMarketAction extends BlueAction {

    public FarmersMarketAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.mc > 0;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc--;
        state.plants += 2;
    }

}
