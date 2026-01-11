package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.services.ai.turnProcessors.frontier.RequiresMcPayment;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public abstract class BlueAction extends Action {

    public BlueAction(int id) {
        super(id);
    }

    public void applyInternal(StateContext stateContext, State state) {
        if (stateContext.isHasAssemblyLines()) {
            state.mc++;
        }
        applyInternal(stateContext, state);
    }

    protected boolean canAffordByMc(State s, int cost) {
        return (s.mc + (s.heatAsMc ? s.heat : 0)) >= cost;
    }

    protected void pay(State s, int cost) {
        int mcUsed = Math.min(s.mc, cost);
        s.mc -= mcUsed;
        if (cost > mcUsed) {
            assert s.heatAsMc;
            s.heat -= (cost - mcUsed);
        };
    }

    protected Object mcContext(int cost) {
        return cost > 0 ? new RequiresMcPayment(cost) : null;
    }

}
