package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

public class AdvancedScreeningTechAction extends BlueAction {

    public AdvancedScreeningTechAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return true;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        state.extraMcValue += 9 * (float) (Constants.TAG_TO_CARDS_COUNT.get(Tag.SCIENCE) + Constants.TAG_TO_CARDS_COUNT.get(Tag.PLANT) + Constants.CARDS_WITH_DYNAMIC_TAG) / Constants.TOTAL_CARDS_COUNT;
    }

}
