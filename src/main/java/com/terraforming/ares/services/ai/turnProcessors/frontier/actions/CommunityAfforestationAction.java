package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.RequiresMcPayment;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class CommunityAfforestationAction extends BlueAction {

    public CommunityAfforestationAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext ctx, State s) {
        return canAffordByMc(s, costToPay(ctx));
    }

    @Override
    public void applyInternal(StateContext ctx, State s) {
        pay(s, costToPay(ctx));
        ctx.forestBuilt(s);
    }

    private int costToPay(StateContext ctx) {
        return Math.max(0, 14 - ctx.getMilestoneAchieved() *  4);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return mcContext(costToPay(stateContext));
    }

}
