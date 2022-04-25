package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.states.BuildGreenProjectsState;
import com.terraforming.ares.states.PickCorporationsState;
import com.terraforming.ares.states.PickStageState;
import com.terraforming.ares.states.State;
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
            default:
                throw new IllegalArgumentException(String.format("State %s is not supported", marsGame.getStateType()));
        }
    }

}
