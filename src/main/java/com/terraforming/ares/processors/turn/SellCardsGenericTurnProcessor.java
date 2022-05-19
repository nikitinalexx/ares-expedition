package com.terraforming.ares.processors.turn;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.services.SpecialEffectsService;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.05.2022
 */
public abstract class SellCardsGenericTurnProcessor<T extends Turn> implements TurnProcessor<T> {
    private final SpecialEffectsService specialEffectsService;

    protected SellCardsGenericTurnProcessor(SpecialEffectsService specialEffectsService) {
        this.specialEffectsService = specialEffectsService;
    }

    protected void sell(Player player, List<Integer> cards) {
        player.getHand().removeCards(cards);

        int cardCost = 3 + (
                specialEffectsService.ownsSpecialEffect(player, SpecialEffect.SOLD_CARDS_COST_1_MC_MORE)
                        ? 1
                        : 0);

        player.setMc(player.getMc() + cards.size() * cardCost);
    }

}
