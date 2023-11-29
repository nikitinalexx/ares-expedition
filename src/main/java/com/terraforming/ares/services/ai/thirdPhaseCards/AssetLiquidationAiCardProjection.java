package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.AssetLiquidation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetLiquidationAiCardProjection<T extends Card> implements AiCardProjection<AssetLiquidation> {

    @Override
    public Class<AssetLiquidation> getType() {
        return AssetLiquidation.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getTerraformingRating() <= 0) {
            return new MarsGameRowDifference();
        }

        player.setTerraformingRating(player.getTerraformingRating() - 1);

        return MarsGameRowDifference.builder()
                .cards(3)
                .build();
    }
}
