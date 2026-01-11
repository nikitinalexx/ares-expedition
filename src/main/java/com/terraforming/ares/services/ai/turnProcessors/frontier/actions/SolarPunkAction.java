package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class SolarPunkAction extends BlueAction {

    public SolarPunkAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return canAffordByMc(state, costToPay(context));
    }

    @Override
    public void applyInternal(StateContext context, State state) {
        pay(state, costToPay(context));
        context.forestBuilt(state);
    }

    private int costToPay(StateContext context) {
        return Math.max(0, 15 - context.getPlayer().getTitaniumIncome() * 2);
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
