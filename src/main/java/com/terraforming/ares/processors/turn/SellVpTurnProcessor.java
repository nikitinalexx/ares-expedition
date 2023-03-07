package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SellVpTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class SellVpTurnProcessor implements TurnProcessor<SellVpTurn> {

    @Override
    public TurnResponse processTurn(SellVpTurn turn, MarsGame game) {
        final CrysisData crysisData = game.getCrysisData();

        crysisData.setCrisisVp(crysisData.getCrisisVp() - 1);

        final Player player = game.getPlayerByUuid(turn.getPlayerUuid());
        player.setMc(player.getMc() + 5);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.SELL_VP;
    }
}
