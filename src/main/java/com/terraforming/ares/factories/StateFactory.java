package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.states.*;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class StateFactory {

    public State getCurrentState(MarsGame marsGame) {
        switch (marsGame.getStateType()) {
            case PICK_CORPORATIONS:
                return new PickCorporationsState(marsGame);
            case PICK_STAGE:
                return new PickStageState(marsGame);
            case BUILD_GREEN_PROJECTS:
                return new BuildGreenProjectsState(marsGame);
            case BUILD_BLUE_RED_PROJECTS:
                return new BuildBlueRedProjectsState(marsGame);
            case PERFORM_BLUE_ACTION:
                return new PerformBlueActionState(marsGame);
            case COLLECT_INCOME:
                return new CollectIncomeState(marsGame);
            case DRAFT_CARDS:
                return new DraftCardsState(marsGame);
            case SELL_EXTRA_CARDS:
                return new SellExtraCardsState(marsGame);
            default:
                throw new IllegalArgumentException(String.format("State %s is not supported", marsGame.getStateType()));
        }
    }

}
