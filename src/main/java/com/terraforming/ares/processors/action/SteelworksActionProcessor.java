package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Steelworks;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SteelworksActionProcessor implements BlueActionCardProcessor<Steelworks> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<Steelworks> getType() {
        return Steelworks.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setHeat(player.getHeat() - 6);
        player.setMc(player.getMc() + 2);

        terraformingService.raiseOxygen(marsContextProvider.provide(game, player));

        return null;
    }

}
