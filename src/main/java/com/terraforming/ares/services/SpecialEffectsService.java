package com.terraforming.ares.services;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .map(deckService::getProjectCard)
                .anyMatch(card -> card.getSpecialEffects().contains(specialEffect));
    }

}
