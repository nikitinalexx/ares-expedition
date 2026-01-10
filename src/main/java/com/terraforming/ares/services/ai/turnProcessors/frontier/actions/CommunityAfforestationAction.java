package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class CommunityAfforestationAction extends BlueAction {

    public CommunityAfforestationAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.mc >= (Math.max(0, 14 - stateContext.getMilestoneAchieved() *  4));
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.mc -= (Math.max(0, 14 - stateContext.getMilestoneAchieved() *  4));
        stateContext.forestBuilt(state);
    }

}
