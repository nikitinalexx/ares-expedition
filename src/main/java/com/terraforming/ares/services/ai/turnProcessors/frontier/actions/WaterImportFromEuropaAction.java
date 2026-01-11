package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class WaterImportFromEuropaAction extends BlueAction {

    public WaterImportFromEuropaAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext ctx, State s) {
        return !ctx.isOceansMax() && canAffordByMc(s, costToPay(ctx));
    }

    @Override
    public void applyInternal(StateContext ctx, State s) {
        pay(s, costToPay(ctx));
        ctx.oceanBuilt(s);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return mcContext(costToPay(stateContext));
    }

    private int costToPay(StateContext ctx) {
        return Math.max(0, 12 - ctx.getPlayer().getTitaniumIncome());
    }

}
