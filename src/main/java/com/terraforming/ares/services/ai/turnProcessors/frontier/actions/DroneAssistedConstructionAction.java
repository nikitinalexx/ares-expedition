package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class DroneAssistedConstructionAction extends BlueAction {

    public DroneAssistedConstructionAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        Player player = stateContext.getPlayer();
        if (player.isPhaseUpgraded(player.getChosenPhase())) {
            state.mc += 2;
        }
        state.mc += 2;
    }

}
