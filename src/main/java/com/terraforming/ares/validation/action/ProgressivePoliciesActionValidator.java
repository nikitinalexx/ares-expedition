package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ProgressivePolicies;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ProgressivePoliciesActionValidator implements ActionValidator<ProgressivePolicies> {
    private final TerraformingService terraformingService;
    private final CardService cardService;

    @Override
    public Class<ProgressivePolicies> getType() {
        return ProgressivePolicies.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canIncreaseOxygen(game)) {
            return "Can not increase Oxygen anymore";
        }

        int eventTags = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
        int price = (eventTags >= 4) ? 5 : 10;

        if (player.getMc() < 5) {
            return "Not enough MC to perform the action";
        }

        return null;
    }
}
