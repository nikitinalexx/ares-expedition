package com.terraforming.ares.services;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class SpecialEffectsService {
    private final CardService deckService;

    public boolean ownsSpecialEffect(Player player, SpecialEffect specialEffect) {
        return player
                .getPlayed()
                .getCards()
                .stream()
                .map(deckService::getCard)
                .anyMatch(card -> card.getSpecialEffects().contains(specialEffect));
    }

    public Set<SpecialEffect> getPlayerSpecialEffects(Player player) {
        return player.getPlayed().getCards().stream().map(deckService::getCard).flatMap(c -> c.getSpecialEffects().stream()).collect(Collectors.toSet());
    }

    public int getCardPrice(Player player) {
        int cardCost = 3;

        Set<SpecialEffect> playerSpecialEffects = getPlayerSpecialEffects(player);

        if (playerSpecialEffects.contains(SpecialEffect.SOLD_CARDS_COST_1_MC_MORE)) {
            cardCost++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.EXOCORP_SOLD_CARDS_COST_1_MC_MORE)) {
            cardCost++;
        }

        return cardCost;
    }

}
