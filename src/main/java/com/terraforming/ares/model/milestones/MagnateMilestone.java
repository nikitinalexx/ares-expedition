package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class MagnateMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return cardService.countPlayedCards(player, Set.of(CardColor.GREEN)) >= 8;
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return Math.min(cardService.countPlayedCards(player, Set.of(CardColor.GREEN)), 8);
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.MAGNATE;
    }

    @Override
    public int getMaxValue() {
        return 8;
    }
}
