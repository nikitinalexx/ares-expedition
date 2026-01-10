package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class ThinkTankAction extends BlueAction {

    public ThinkTankAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.mc >= 2;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= 2;
        state.cards++;
    }

}
