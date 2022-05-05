package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SellCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class SellCardsProcessor implements TurnProcessor<SellCardsTurn> {

    @Override
    public TurnResponse processTurn(SellCardsTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());

        playerContext.getHand().removeCards(turn.getCards());
        playerContext.setMc(playerContext.getMc() + turn.getCards().size() * 3);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS;
    }
}
