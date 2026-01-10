package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class FarmingCoopsAction extends BlueAction {

    public FarmingCoopsAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.cards > 0;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.cards--;
        state.plants += 3;
    }

}
