package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.WaterImportFromEuropa;
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
public class WaterImportFromEuropaActionProcessor implements BlueActionCardProcessor<WaterImportFromEuropa> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider contextProvider;

    @Override
    public Class<WaterImportFromEuropa> getType() {
        return WaterImportFromEuropa.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int flipPrice = Math.max(0, 12 - player.getTitaniumIncome());

        player.setMc(player.getMc() - flipPrice);

        terraformingService.revealOcean(contextProvider.provide(game, player));

        return null;
    }


}
