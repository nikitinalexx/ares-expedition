package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.GasCooledReactors;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class GasCooledReactorsActionProcessor implements BlueActionCardProcessor<GasCooledReactors> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<GasCooledReactors> getType() {
        return GasCooledReactors.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        final int upgradedPhaseCards = (int) player.getPhaseCards().stream().filter(card -> card != 0).count();
        int temperaturePrice = Math.max(0, 12 - 2 * upgradedPhaseCards);

        player.setMc(player.getMc() - temperaturePrice);

        terraformingService.increaseTemperature(marsContextProvider.provide(game, player));

        return null;
    }

}
