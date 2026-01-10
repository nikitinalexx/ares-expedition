package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class CommunityGardensAction extends BlueAction {

    public CommunityGardensAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc += 2;
        if (stateContext.isChose3rdPhase()) {
            state.plants++;
        }
    }

}
