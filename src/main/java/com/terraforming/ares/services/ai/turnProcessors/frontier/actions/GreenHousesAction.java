package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class GreenHousesAction extends BlueAction {
    private int exchangeRate;

    public GreenHousesAction(int id, int exchangeRate) {
        super(id);
        this.exchangeRate = exchangeRate;
        assert exchangeRate <= 4;
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.heat >= exchangeRate && !state.heatAsMc;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.heat -= exchangeRate;
        state.plants += exchangeRate;
    }

}
