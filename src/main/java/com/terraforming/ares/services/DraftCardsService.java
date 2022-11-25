package com.terraforming.ares.services;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Service
@RequiredArgsConstructor
public class DraftCardsService {
    private final SpecialEffectsService specialEffectsService;

    public int countExtraCardsToTake(Player player) {
        int extraCardsToTake = 0;

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.EXTENDED_RESOURCES)) {
            extraCardsToTake++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_RELATIONS)) {
            extraCardsToTake++;
        }


        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            extraCardsToTake++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.THARSIS_REPUBLIC)) {
            extraCardsToTake++;
        }

        return extraCardsToTake;
    }

    public int countExtraCardsToDraft(Player player) {
        int extraCardsToDraft = 0;

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_RELATIONS)) {
            extraCardsToDraft++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERNS)) {
            extraCardsToDraft += 2;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            extraCardsToDraft++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.THARSIS_REPUBLIC)) {
            extraCardsToDraft++;
        }

        return extraCardsToDraft;
    }

}
