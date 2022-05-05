package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.TurnResponse;
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
    public TurnResponse processTurn(SkipTurn turn, MarsGame game) {
        return null;
    }
}
