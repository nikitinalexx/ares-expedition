package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class GasCooledReactorsAction extends BlueAction {

    public GasCooledReactorsAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext context, State state) {
        return !context.isTemperatureMax() && canAffordByMc(state, costToPay(context));
    }

    @Override
    public void applyInternal(StateContext context, State state) {
        pay(state, costToPay(context));
        context.temperatureBuilt(state);
    }

    private int costToPay(StateContext context) {
        return 12 - context.getPlayer().countPhaseUpgrades() * 2;
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(costToPay(context));
    }

}
