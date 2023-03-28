package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.SolarPunk;
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
public class SolarPunkAiCardProjection<T extends Card> implements AiCardProjection<SolarPunk> {
    private final DeepNetwork deepNetwork;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<SolarPunk> getType() {
        return SolarPunk.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        int forestPrice = Math.max(0, 15 - 2 * player.getTitaniumIncome());

        if (player.getMc() >= forestPrice) {
            player.setMc(player.getMc() - forestPrice);

            terraformingService.buildForest(marsContextProvider.provide(game, player));
        }

        return new MarsGameRowDifference();
    }
}
