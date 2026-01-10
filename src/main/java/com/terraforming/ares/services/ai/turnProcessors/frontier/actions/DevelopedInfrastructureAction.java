package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class DevelopedInfrastructureAction extends BlueAction {

    public DevelopedInfrastructureAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return !stateContext.isTemperatureMax() && state.mc >= (10 - (stateContext.getBlueCardsCount() >= 5 ? 5 : 0));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (10 - (stateContext.getBlueCardsCount() >= 5 ? 5 : 0));
        stateContext.temperatureBuilt(state);
    }

}
