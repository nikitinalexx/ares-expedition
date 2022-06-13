package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class EnergizerMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return player.getHeatIncome() >= 10;
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.ENERGIZER;
    }
}
