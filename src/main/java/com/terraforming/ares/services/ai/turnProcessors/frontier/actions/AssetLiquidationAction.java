package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class AssetLiquidationAction extends BlueAction {

    public AssetLiquidationAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return state.tr > 0;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.tr--;
        state.cards += 3;
    }

}
