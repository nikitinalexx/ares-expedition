package com.terraforming.ares.processors.turn;

import com.terraforming.ares.model.turn.BuildGreenProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.DeckService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildGreenProjectProcessor extends GenericBuildProjectProcessor<BuildGreenProjectTurn> {

    public BuildGreenProjectProcessor(DeckService marsDeckService) {
        super(marsDeckService);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }
}
