package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.Steelworks;
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
public class SteelworksAiCardProjection<T extends Card> implements AiCardProjection<Steelworks> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<Steelworks> getType() {
        return Steelworks.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (player.getHeat() < 6 || !terraformingService.canIncreaseOxygen(game)) {
            return new MarsGameRowDifference();
        }

        player.setHeat(player.getHeat() - 6);
        player.setMc(player.getMc() + 2);
        terraformingService.raiseOxygen(marsContextProvider.provide(game, player));

        return new MarsGameRowDifference();
    }
}
