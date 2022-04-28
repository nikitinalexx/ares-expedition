package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.StageChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class PickStageProcessor implements TurnProcessor<StageChoiceTurn> {

    @Override
    public void processTurn(StageChoiceTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());
        playerContext.setCurrentStage(turn.getStageId());
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_STAGE;
    }
}
