package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CaretakerContract;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaretakerContractAiCardProjection<T extends Card> implements AiCardProjection<CaretakerContract> {

    @Override
    public Class<CaretakerContract> getType() {
        return CaretakerContract.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getHeat() >= 8) {
            player.setTerraformingRating(player.getTerraformingRating() + 1);
            player.setHeat(player.getHeat() - 8);
        }

        return new MarsGameRowDifference();
    }
}
