package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class GardenerMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return player.getForests() >= 3;
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return Math.min(player.getForests(), 3);
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.GARDENER;
    }

    @Override
    public int getMaxValue() {
        return 3;
    }
}
