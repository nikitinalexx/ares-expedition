package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class AiCentralAction extends BlueAction {

    public AiCentralAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.cards += 2;
    }

}
