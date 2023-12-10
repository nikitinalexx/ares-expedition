package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class MinimalistMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return player.getHand().size() <= getMaxValue();
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return player.getHand().size();
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.MINIMALIST;
    }

    @Override
    public int getMaxValue() {
        return 1;
    }
}
