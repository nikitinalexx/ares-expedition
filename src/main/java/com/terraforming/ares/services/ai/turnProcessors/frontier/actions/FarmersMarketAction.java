package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class FarmersMarketAction extends BlueAction {

    public FarmersMarketAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return canAffordByMc(state, costToPay(context));
    }

    private int costToPay(StateContext context) {
        return 1;
    }

    @Override
    public void applyInternal(StateContext context, State state) {
        pay(state, costToPay(context));
        state.plants += 2;
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
