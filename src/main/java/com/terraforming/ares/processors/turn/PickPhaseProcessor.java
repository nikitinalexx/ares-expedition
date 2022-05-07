package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
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
        player.setChosenPhase(turn.getPhaseId());

        player.setCanBuildInSecondPhase(turn.getPhaseId() == 2 ? 2 : 1);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }
}
