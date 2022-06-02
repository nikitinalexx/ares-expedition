package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.model.turn.UnmiRtTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class UnmiRtTurnProcessor implements TurnProcessor<UnmiRtTurn> {

    @Override
    public TurnType getType() {
        return TurnType.UNMI_RT;
    }

    @Override
    public TurnResponse processTurn(UnmiRtTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setDidUnmiAction(true);
        player.setHasUnmiAction(false);

        player.setTerraformingRating(player.getTerraformingRating() + 1);
        player.setMc(player.getMc() - 6);

        return null;
    }
}
