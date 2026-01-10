package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class CaretakerContractAction extends BlueAction {

    public CaretakerContractAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.heat >= 8;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.heat -= 8;
        state.tr++;
    }

}
