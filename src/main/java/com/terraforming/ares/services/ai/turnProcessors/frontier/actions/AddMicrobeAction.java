package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class AddMicrobeAction extends BlueAction {
    private final PutMicrobeEffect stateEffect;

    public AddMicrobeAction(int id, PutMicrobeEffect stateEffect) {
        super(id);
        this.stateEffect = stateEffect;
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return stateEffect.isApplicable(stateContext, state);
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        stateContext.onMicrobeGained(state, 1);
        stateEffect.apply(state);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return stateEffect;
    }

}
