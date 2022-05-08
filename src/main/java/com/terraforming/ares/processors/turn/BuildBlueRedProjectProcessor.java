package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.BuildBlueRedProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildBlueRedProjectProcessor extends GenericBuildProjectProcessor<BuildBlueRedProjectTurn> {

    public BuildBlueRedProjectProcessor(CardService marsDeckService, TerraformingService terraformingService) {
        super(marsDeckService, terraformingService);
    }

    @Override
    protected void processTurnInternal(BuildBlueRedProjectTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        if (player.getCanBuildInSecondPhase() < 1) {
            throw new IllegalStateException("Can't build a project while project limit for this phase is < 1");
        }

        player.setCanBuildInSecondPhase(player.getCanBuildInSecondPhase() - 1);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_BLUE_RED_PROJECT;
    }
}
