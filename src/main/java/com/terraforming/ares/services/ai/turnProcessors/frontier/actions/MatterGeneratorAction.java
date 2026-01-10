package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class MatterGeneratorAction extends BlueAction {

    public MatterGeneratorAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.cards >= 1;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.cards--;
        state.mc += 6;
    }

}
