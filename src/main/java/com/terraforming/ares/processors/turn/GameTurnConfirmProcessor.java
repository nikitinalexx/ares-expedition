package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.GameEndConfirmTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class GameTurnConfirmProcessor implements TurnProcessor<GameEndConfirmTurn> {

    @Override
    public TurnType getType() {
        return TurnType.GAME_END_CONFIRM;
    }

    @Override
    public TurnResponse processTurn(GameEndConfirmTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());
        player.setConfirmedGameEndThirdPhase(true);

        return null;
    }
}
