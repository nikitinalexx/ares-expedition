package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SolarPunk;
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
public class SolarPunkActionProcessor implements BlueActionCardProcessor<SolarPunk> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<SolarPunk> getType() {
        return SolarPunk.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int forestPrice = Math.max(0, 15 - 2 * player.getTitaniumIncome());

        player.setMc(player.getMc() - forestPrice);

        terraformingService.buildForest(marsContextProvider.provide(game, player));

        return null;
    }

}
