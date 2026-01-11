package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class ThinkTankAction extends BlueAction {

    public ThinkTankAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return canAffordByMc(state, costToPay(context));
    }

    private int costToPay(StateContext context) {
        return 2;
    }

    @Override
    public void applyInternal(StateContext context, State state) {
        pay(state, costToPay(context));
        state.cards++;
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
