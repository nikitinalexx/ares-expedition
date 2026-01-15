package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Service
@RequiredArgsConstructor
public class DraftCardsService {
    private final SpecialEffectsService specialEffectsService;

    public DraftCardsDto countExtraCardsToTakeAndDraft(Player player) {
        Set<SpecialEffect> playerSpecialEffects = specialEffectsService.getPlayerSpecialEffects(player);
        return new DraftCardsDto(countExtraCardsToTake(playerSpecialEffects), countExtraCardsToSee(player, playerSpecialEffects));
    }

    public DraftCardsDto countCardsToTakeAndDraftByChosenPhase(Player player) {
        int initialCardsToSee = (player.getChosenPhase() == 5 ? 5 : 2);
        int initialCardsToTake = (player.getChosenPhase() == 5 ? 2 : 1);

        if (player.getChosenPhase() == 5 && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_KEEP_EXTRA)) {
            initialCardsToSee = 4;
            initialCardsToTake = 3;
        }

        if (player.getChosenPhase() == 5 && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_SEE_EXTRA)) {
            initialCardsToSee = 8;
        }

        DraftCardsDto draftCardsDto = countExtraCardsToTakeAndDraft(player);

        int cardsToDraft = initialCardsToSee + draftCardsDto.getCardsToSee();
        int cardsToTake = initialCardsToTake + draftCardsDto.getCardsToTake();

        if (player.getLunaProjectOffice() > 0) {
            cardsToDraft++;
            cardsToTake++;
            player.setLunaProjectOffice(player.getLunaProjectOffice() - 1);
        }

        cardsToTake = Math.min(cardsToTake, cardsToDraft);

        return new DraftCardsDto(cardsToTake, cardsToDraft);
    }

    private int countExtraCardsToTake(Set<SpecialEffect> playerSpecialEffects) {
        int extraCardsToTake = 0;

        if (playerSpecialEffects.contains(SpecialEffect.EXTENDED_RESOURCES)) {
            extraCardsToTake++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.INTERPLANETARY_RELATIONS)) {
            extraCardsToTake++;
        }


        if (playerSpecialEffects.contains(SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            extraCardsToTake++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.THARSIS_REPUBLIC)) {
            extraCardsToTake++;
        }

        return extraCardsToTake;
    }

    private int countExtraCardsToSee(Player player, Set<SpecialEffect> playerSpecialEffects) {
        int extraCardsToDraft = 0;

        if (playerSpecialEffects.contains(SpecialEffect.INTERPLANETARY_RELATIONS)) {
            extraCardsToDraft++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.INTERNS)) {
            extraCardsToDraft += 2;
        }

        if (playerSpecialEffects.contains(SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            extraCardsToDraft++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.THARSIS_REPUBLIC)) {
            extraCardsToDraft++;
        }

        if (playerSpecialEffects.contains(SpecialEffect.BACTERIAL_AGGREGATES)) {
            extraCardsToDraft += player.getCardResourcesCount().get(BacterialAggregates.class);
        }

        return extraCardsToDraft;
    }

}
