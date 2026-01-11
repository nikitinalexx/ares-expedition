package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class ProgressivePoliciesAction extends BlueAction {

    public ProgressivePoliciesAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return !context.isOxygenMax() && canAffordByMc(state, costToPay(context));
    }

    @Override
    public void applyInternal(StateContext context, State state) {
        pay(state, costToPay(context));
        context.oxygenBuilt(state);
    }

    private int costToPay(StateContext context) {
        return 10 - (context.getEventTagCount() >= 4 ? 5 : 0);
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
