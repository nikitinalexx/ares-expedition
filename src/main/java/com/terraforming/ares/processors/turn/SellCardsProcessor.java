package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SellCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class SellCardsProcessor implements TurnProcessor<SellCardsTurn> {
    private final SpecialEffectsService specialEffectsService;

    @Override
    public TurnResponse processTurn(SellCardsTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());

        playerContext.getHand().removeCards(turn.getCards());

        int cardCost = 3 + (
                specialEffectsService.ownsSpecialEffect(playerContext, SpecialEffect.SOLD_CARDS_COST_1_MC_MORE)
                        ? 1
                        : 0);

        playerContext.setMc(playerContext.getMc() + turn.getCards().size() * cardCost);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.SELL_CARDS;
    }
}
