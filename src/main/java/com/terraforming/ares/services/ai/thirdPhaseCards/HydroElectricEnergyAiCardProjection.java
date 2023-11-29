package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.HydroElectricEnergy;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.DeepNetwork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HydroElectricEnergyAiCardProjection<T extends Card> implements AiCardProjection<HydroElectricEnergy> {
    private final DeepNetwork deepNetwork;

    @Override
    public Class<HydroElectricEnergy> getType() {
        return HydroElectricEnergy.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getMc() < 1) {
            return new MarsGameRowDifference();
        }

        player.setMc(player.getMc() - 1);
        player.setHeat(player.getHeat() + 3);

        return new MarsGameRowDifference();
    }
}
