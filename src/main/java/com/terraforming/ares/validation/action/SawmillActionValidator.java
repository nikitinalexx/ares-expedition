package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.CommunityAfforestation;
import com.terraforming.ares.cards.blue.Sawmill;
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
 * Creation date 03.12.2023
 */
@Component
@RequiredArgsConstructor
public class SawmillActionValidator implements ActionValidator<Sawmill> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<Sawmill> getType() {
        return Sawmill.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canIncreaseInfrastructure(game)) {
            return "Can not increase infrastructure anymore";
        }

        int plantTagsPlayed = cardService.countPlayedTags(player, Set.of(Tag.PLANT));

        int infrastructurePrice = Math.max(0, 10 - 2 * plantTagsPlayed);

        if (player.getMc() < infrastructurePrice) {
            return "Not enough MC to increase infrastructure";
        }

        return null;
    }
}
