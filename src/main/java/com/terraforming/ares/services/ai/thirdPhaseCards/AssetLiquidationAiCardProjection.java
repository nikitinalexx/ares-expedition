package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.AiCentral;
import com.terraforming.ares.cards.blue.AssetLiquidation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
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
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getTerraformingRating() <= 0) {
            return new MarsGameRowDifference();
        }

        player.setTerraformingRating(player.getTerraformingRating() - 1);

        return MarsGameRowDifference.builder()
                .greenCards(3 * Constants.GREEN_CARDS_RATIO)
                .redCards(3 * Constants.RED_CARDS_RATIO)
                .blueCards(3 * Constants.BLUE_CARDS_RATIO)
                .build();
    }
}
