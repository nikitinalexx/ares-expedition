package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.PrivateInvestorBeach;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@Component
public class PrivateInvestorBeachOnBuiltEffectValidator implements OnBuiltEffectValidator<PrivateInvestorBeach> {

    @Override
    public Class<PrivateInvestorBeach> getType() {
        return PrivateInvestorBeach.class;
    }

    @Override
    public String validate(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> input) {
        if (game.getMilestones().stream().noneMatch(milestone -> milestone.isAchieved(player))) {
            return "Project requires a Milestone";
        }

        return null;
    }
}
