package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.BuildGreenProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildGreenProjectProcessor extends GenericBuildProjectProcessor<BuildGreenProjectTurn> {

    public BuildGreenProjectProcessor(CardService marsDeckService, TerraformingService terraformingService) {
        super(marsDeckService, terraformingService);
    }

    @Override
    protected void processInternalBeforeBuild(BuildGreenProjectTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.setCanBuildAnotherGreenWith9Discount(false);
        player.setAssortedEnterprisesDiscount(false);
        player.setSelfReplicatingDiscount(false);
    }

    @Override
    protected void processInternalAfterBuild(BuildGreenProjectTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        if (player.getCanBuildInFirstPhase() < 1 && !player.isAssortedEnterprisesGreenAvailable()) {
            throw new IllegalStateException("Can't build a project while project limit for this phase is < 1");
        }

        if (player.getCanBuildInFirstPhase() >= 1) {
            player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() - 1);
            if (game.getCurrentPhase() == 3 && player.getActionsInSecondPhase() == 1) {
                player.setActionsInSecondPhase(0);
            }
        } else if (player.isAssortedEnterprisesGreenAvailable()) {
            player.setAssortedEnterprisesGreenAvailable(false);
            player.setActionsInSecondPhase(player.getActionsInSecondPhase() - 1);
        }
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }
}
