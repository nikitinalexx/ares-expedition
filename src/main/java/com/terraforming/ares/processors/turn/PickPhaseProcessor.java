package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PhaseChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class PickPhaseProcessor implements TurnProcessor<PhaseChoiceTurn> {

    @Override
    public TurnResponse processTurn(PhaseChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.clearRoundResults();
        player.setPreviousChosenPhase(turn.getPhaseId());
        player.setChosenPhase(turn.getPhaseId());

        if (turn.getPhaseId() == 1 && player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)) {
            player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1);
            player.setCanBuildAnotherGreenWithPrice12(true);
        }

        if (turn.getPhaseId() == 3) {
            player.setBlueActionExtraActivationsLeft(1);

            if (player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT)) {
                player.setBlueActionExtraActivationsLeft(2);
            }
        }

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }
}
