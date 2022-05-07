package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Steelworks;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
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

    @Override
    public Class<Steelworks> getType() {
        return Steelworks.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.setHeat(player.getHeat() - 6);
        player.setMc(player.getMc() + 2);

        terraformingService.raiseOxygen(game, player);

        return null;
    }

}
