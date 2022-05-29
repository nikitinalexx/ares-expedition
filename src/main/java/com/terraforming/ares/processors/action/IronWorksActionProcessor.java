package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.IronWorks;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
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
public class IronWorksActionProcessor implements BlueActionCardProcessor<IronWorks> {
    private final TerraformingService terraformingService;

    @Override
    public Class<IronWorks> getType() {
        return IronWorks.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setHeat(player.getHeat() - 4);

        terraformingService.raiseOxygen(game, player);

        return null;
    }
}
