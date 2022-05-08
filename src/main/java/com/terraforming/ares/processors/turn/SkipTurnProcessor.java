package com.terraforming.ares.processors.turn;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SkipTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class SkipTurnProcessor implements TurnProcessor<SkipTurn> {
    private final StateFactory stateFactory;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public TurnResponse processTurn(SkipTurn turn, MarsGame game) {
        int currentPhase = stateFactory.getCurrentState(game).getCurrentPhase();
        if (currentPhase == 1) {
            game.getPlayerByUuid(turn.getPlayerUuid()).setCanBuildInFirstPhase(0);
        } else if (currentPhase == 2) {
            game.getPlayerByUuid(turn.getPlayerUuid()).setCanBuildInSecondPhase(0);
        }

        return null;
    }
}
