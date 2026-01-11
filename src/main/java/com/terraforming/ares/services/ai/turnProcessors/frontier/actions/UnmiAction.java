package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class UnmiAction extends BlueAction {

    public UnmiAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return state.trRaisedThisPhase && !state.unmiUsedThisPhase && canAffordByMc(state, costToPay(context));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        pay(state, costToPay(stateContext));

        state.unmiUsedThisPhase = true;
        state.tr++;
    }

    private int costToPay(StateContext context) {
        return 6;
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
