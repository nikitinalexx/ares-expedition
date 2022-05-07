package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.VolcanicPools;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class VolcanicPoolsActionProcessor implements BlueActionCardProcessor<VolcanicPools> {
    private final TerraformingService terraformingService;
    private final CardService cardService;

    @Override
    public Class<VolcanicPools> getType() {
        return VolcanicPools.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        int energyTags = (int) player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .flatMap(projectCard -> projectCard.getTags().stream())
                .filter(Tag.ENERGY::equals).count();

        int flipPrice = Math.max(0, 12 - energyTags);

        player.setMc(player.getMc() - flipPrice);

        terraformingService.revealOcean(game, player);

        return null;
    }


}
