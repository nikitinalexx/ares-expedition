package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.WoodBurningStoves;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.DeepNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WoodBurningStovesAiCardProjection<T extends Card> implements AiCardProjection<WoodBurningStoves> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<WoodBurningStoves> getType() {
        return WoodBurningStoves.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getPlants() < 3 || !terraformingService.canIncreaseTemperature(game)) {
            return new MarsGameRowDifference();
        }

        player.setPlants(player.getPlants() - 3);
        terraformingService.increaseTemperature(marsContextProvider.provide(game, player));

        return new MarsGameRowDifference();
    }
}
