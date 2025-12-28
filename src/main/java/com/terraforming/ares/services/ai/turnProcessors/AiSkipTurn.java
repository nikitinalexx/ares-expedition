package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.AiEndgameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiSkipTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final AiEndgameService aiEndgameService;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        if (game.getStateType() == StateType.PERFORM_BLUE_ACTION && aiEndgameService.doFinalActionsIfGameFinished(game, player)) {
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }

}
