package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.CommunityAfforestation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
public class CommunityAfforestationActionValidator implements ActionValidator<CommunityAfforestation> {

    @Override
    public Class<CommunityAfforestation> getType() {
        return CommunityAfforestation.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        int forestPrice = Math.max(0, 14 - 4 * milestonesAchieved);

        if (player.getMc() < forestPrice) {
            return "Not enough MC to build a forest";
        }

        return null;
    }
}
