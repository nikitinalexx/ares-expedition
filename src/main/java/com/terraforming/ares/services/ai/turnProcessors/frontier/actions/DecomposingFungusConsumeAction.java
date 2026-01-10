package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class DecomposingFungusConsumeAction extends BlueAction {
    private final ConsumeResourceEffect stateEffect;

    public DecomposingFungusConsumeAction(int id, ConsumeResourceEffect stateEffect) {
        super(id);
        this.stateEffect = stateEffect;
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return stateEffect.isApplicable(stateContext, state);
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        stateEffect.apply(state);
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return stateEffect;
    }
}
