package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.VolcanicPools;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
@RequiredArgsConstructor
public class VolcanicPoolsActionValidator implements ActionValidator<VolcanicPools> {
    private final TerraformingService terraformingService;
    private final CardService cardService;

    @Override
    public Class<VolcanicPools> getType() {
        return VolcanicPools.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canRevealOcean(game)) {
            return "Can't flip an ocean anymore";
        }

        int energyTags = (int) player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .flatMap(projectCard -> projectCard.getTags().stream())
                .filter(Tag.ENERGY::equals).count();

        int flipPrice = Math.max(0, 12 - energyTags);

        if (player.getMc() < flipPrice) {
            return "Not enough MC to flip the ocean";
        }

        return null;
    }

}
