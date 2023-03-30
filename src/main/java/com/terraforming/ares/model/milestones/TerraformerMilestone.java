package com.terraforming.ares.model.milestones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class TerraformerMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return player.getTerraformingRating() >= 15;
    }

    @Override
    public int getValue(Player player, CardService cardService) {
        return Math.min(player.getTerraformingRating(), 15);
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.TERRAFORMER;
    }

    @Override
    public int getMaxValue() {
        return 15;
    }
}
