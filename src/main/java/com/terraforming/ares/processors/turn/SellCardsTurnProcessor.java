package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SellCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.SpecialEffectsService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class SellCardsTurnProcessor extends SellCardsGenericTurnProcessor<SellCardsTurn> {

    public SellCardsTurnProcessor(SpecialEffectsService specialEffectsService) {
        super(specialEffectsService);
    }


    @Override
    public TurnResponse processTurn(SellCardsTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        sell(player, turn.getCards());

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS;
    }
}
