package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.IronWorks;
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
public class IronworksAiCardProjection<T extends Card> implements AiCardProjection<IronWorks> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<IronWorks> getType() {
        return IronWorks.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getHeat() < 4 || !terraformingService.canIncreaseOxygen(game)) {
            return new MarsGameRowDifference();
        }

        player.setHeat(player.getHeat() - 4);
        terraformingService.raiseOxygen(marsContextProvider.provide(game, player));

        return new MarsGameRowDifference();
    }
}
