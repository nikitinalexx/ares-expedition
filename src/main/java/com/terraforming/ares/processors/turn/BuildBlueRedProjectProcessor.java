package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.BuildBlueRedProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.DeckService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildBlueRedProjectProcessor extends GenericBuildProjectProcessor<BuildBlueRedProjectTurn> {

    public BuildBlueRedProjectProcessor(DeckService marsDeckService) {
        super(marsDeckService);
    }

    @Override
    protected void processTurnInternal(BuildBlueRedProjectTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());

        if (playerContext.getCanBuildInSecondStage() < 1) {
            throw new IllegalStateException("Can't build a project while project limit for this stage is < 1");
        }

        playerContext.setCanBuildInSecondStage(playerContext.getCanBuildInSecondStage() - 1);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_BLUE_RED_PROJECT;
    }
}
