package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class TerranMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return cardService.countPlayedTags(player, Set.of(Tag.EARTH)) >= getMaxValue();
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return Math.min(cardService.countPlayedTags(player, Set.of(Tag.EARTH)), getMaxValue());
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.TERRAN;
    }

    @Override
    public int getMaxValue() {
        return 6;
    }
}
