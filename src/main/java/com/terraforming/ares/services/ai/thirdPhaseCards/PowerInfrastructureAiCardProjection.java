package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.PowerInfrastructure;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerInfrastructureAiCardProjection<T extends Card> implements AiCardProjection<PowerInfrastructure> {
    private final TerraformingService terraformingService;

    @Override
    public Class<PowerInfrastructure> getType() {
        return PowerInfrastructure.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (!terraformingService.canIncreaseTemperature(game)) {
            player.setMc(player.getMc() + player.getHeat());
            player.setHeat(0);
        }
        return new MarsGameRowDifference();
    }
}
