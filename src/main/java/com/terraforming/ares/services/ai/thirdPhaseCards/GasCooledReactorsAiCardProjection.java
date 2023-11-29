package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.GasCooledReactors;
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
public class GasCooledReactorsAiCardProjection<T extends Card> implements AiCardProjection<GasCooledReactors> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<GasCooledReactors> getType() {
        return GasCooledReactors.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (!terraformingService.canIncreaseTemperature(game)) {
            return new MarsGameRowDifference();
        }

        int temperaturePrice = 12 - player.countPhaseUpgrades() * 2;

        if (player.getMc() < temperaturePrice) {
            return new MarsGameRowDifference();
        }

        terraformingService.increaseTemperature(marsContextProvider.provide(game, player));
        player.setMc(player.getMc() - temperaturePrice);

        return new MarsGameRowDifference();
    }

}
