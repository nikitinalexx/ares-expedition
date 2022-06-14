package com.terraforming.ares.model.milestones;

import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public class TycoonMilestone extends Milestone {

    @Override
    public boolean isAchievable(Player player, CardService cardService) {
        return cardService.countPlayedCards(player, Set.of(CardColor.BLUE)) >= 6;
    }

    @Override
    public MilestoneType getType() {
        return MilestoneType.TYCOON;
    }
}
