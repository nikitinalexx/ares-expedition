package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.turn.SkipTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
public class SkipTurnProcessor implements TurnProcessor<SkipTurn> {
    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public void processTurn(SkipTurn turn, MarsGame game) {
        //skip turn = do nothing
    }
}
