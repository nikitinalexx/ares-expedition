package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.ExperimentalTechnology;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import org.springframework.stereotype.Component;

@Component
public class ExperimentalTechnologyAiCardProjection<T extends Card> extends PhaseUpgradeAiCardProjection<ExperimentalTechnology> {

    public ExperimentalTechnologyAiCardProjection(AiDiscoveryDecisionService aiDiscoveryDecisionService) {
        super(aiDiscoveryDecisionService);
    }

    @Override
    public Class<ExperimentalTechnology> getType() {
        return ExperimentalTechnology.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getTerraformingRating() <= 0) {
            return new MarsGameRowDifference();
        }

        player.setTerraformingRating(player.getTerraformingRating() - 1);

        return produceDifferenceForPhaseUpgrade(game, player);
    }

}
