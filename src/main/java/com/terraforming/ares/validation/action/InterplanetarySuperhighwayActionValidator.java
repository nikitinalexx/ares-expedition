package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.InterplanetarySuperhighway;
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
public class InterplanetarySuperhighwayActionValidator implements ActionValidator<InterplanetarySuperhighway> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<InterplanetarySuperhighway> getType() {
        return InterplanetarySuperhighway.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canIncreaseInfrastructure(game)) {
            return "Can not increase infrastructure anymore";
        }

        int scienceTagsPlayed = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));

        int infrastructurePrice = 10;

        if (scienceTagsPlayed >= 4) {
            infrastructurePrice -= 5;
        }

        if (player.getMc() < infrastructurePrice) {
            return "Not enough MC to increase infrastructure";
        }

        return null;
    }
}
