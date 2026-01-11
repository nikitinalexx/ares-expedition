package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class HydroElectricAction extends BlueAction {

    public HydroElectricAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext ctx, State s) {
        if (s.mc > 0) {
            return true;
        }
        if (s.heatAsMc && ctx.isHelionCorp() && s.heat > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        pay(state, 1);

        state.heat += 2;
        if (stateContext.isChose3rdPhase()) {
            state.heat++;
        }
    }

    @Override
    public Object getContext(StateContext context) {
        return mcContext(1);
    }

}
