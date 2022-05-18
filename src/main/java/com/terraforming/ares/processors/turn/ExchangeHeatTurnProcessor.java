package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.ExchangeHeatTurn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class ExchangeHeatTurnProcessor implements TurnProcessor<ExchangeHeatTurn> {

    @Override
    public TurnType getType() {
        return TurnType.EXCHANGE_HEAT;
    }

    @Override
    public TurnResponse processTurn(ExchangeHeatTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setMc(player.getMc() + turn.getValue());
        player.setHeat(player.getHeat() - turn.getValue());
        
        return null;
    }
}
