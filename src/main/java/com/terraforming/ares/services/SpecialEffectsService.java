package com.terraforming.ares.services;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
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
        if (player == null
                || player.getPlayed() == null
                || player.getPlayed().getCards() == null
                || player.getPlayed().getCards().isEmpty()) {
            return EnumSet.noneOf(SpecialEffect.class);
        }

        EnumSet<SpecialEffect> result = EnumSet.noneOf(SpecialEffect.class);

        for (int cardRef : player.getPlayed().getCards()) {
            Card card = deckService.getCard(cardRef);
            if (card == null) continue;

            Set<SpecialEffect> effects = card.getSpecialEffects();
            if (effects == null || effects.isEmpty()) continue;

            result.addAll(effects);
        }

        return result;
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
