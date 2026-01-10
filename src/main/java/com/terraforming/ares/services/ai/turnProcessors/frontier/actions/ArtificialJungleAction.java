package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class ArtificialJungleAction extends BlueAction {

    public ArtificialJungleAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.plants > 0;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.plants--;
        state.cards++;
    }

}
