package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.StandardProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class StandardProjectTurnProcessor implements TurnProcessor<StandardProjectTurn> {
    private final StandardProjectService standardProjectService;
    private final TerraformingService terraformingService;

    @Override
    public TurnResponse processTurn(StandardProjectTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        int projectPrice = standardProjectService.getProjectPrice(player, turn.getProjectType());
        player.setMc(player.getMc() - projectPrice);

        switch (turn.getProjectType()) {
            case OCEAN:
                terraformingService.revealOcean(game, player);
                break;
            case FOREST:
                terraformingService.buildForest(game, player);
                break;
            case TEMPERATURE:
                terraformingService.increaseTemperature(game, player);
                break;
            default:
                throw new IllegalStateException("Unknown project type");
        }

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.STANDARD_PROJECT;
    }
}
