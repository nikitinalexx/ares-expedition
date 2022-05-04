package com.terraforming.ares.services;

import com.terraforming.ares.model.PlayerContext;
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
    private final DeckService deckService;

    public boolean ownsSpecialEffect(PlayerContext playerContext, SpecialEffect specialEffect) {
        return playerContext
                .getPlayed()
                .getCards()
                .stream()
                .map(deckService::getProjectCard)
                .anyMatch(card -> card.getSpecialEffects().contains(specialEffect));
    }

}
