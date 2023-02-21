package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.WoodBurningStoves;
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
public class WoodBurningStovesActionProcessor implements BlueActionCardProcessor<WoodBurningStoves> {
    private final TerraformingService terraformingService;

    @Override
    public Class<WoodBurningStoves> getType() {
        return WoodBurningStoves.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int price = player.getChosenPhase() == 3 ? 3 : 4;

        player.setPlants(player.getPlants() - price);

        terraformingService.increaseTemperature(game, player);

        return null;
    }
}
