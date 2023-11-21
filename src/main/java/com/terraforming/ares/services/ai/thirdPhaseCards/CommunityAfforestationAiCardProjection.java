package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CommunityAfforestation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityAfforestationAiCardProjection<T extends Card> implements AiCardProjection<CommunityAfforestation> {
    private final MarsContextProvider marsContextProvider;

    private final TerraformingService terraformingService;

    @Override
    public Class<CommunityAfforestation> getType() {
        return CommunityAfforestation.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        int forestPrice = Math.max(0, 14 - milestonesAchieved * 4);

        if (player.getMc() >= forestPrice) {
            player.setMc(player.getMc() - forestPrice);

            terraformingService.buildForest(marsContextProvider.provide(game, player));
        }

        return new MarsGameRowDifference();
    }
}
