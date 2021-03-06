package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.GhgProductionBacteria;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class GhgProductionBacteriaActionProcessor implements BlueActionCardProcessor<GhgProductionBacteria> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<GhgProductionBacteria> getType() {
        return GhgProductionBacteria.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        Integer input = inputParameters.get(0);

        if (input == 1) {
            player.addResources(actionCard, 1);
        } else if (input == 2) {
            player.addResources(actionCard, -2);

            terraformingService.increaseTemperature(game, player);

            player.getPlayed().getCards().stream().map(cardService::getCard).forEach(
                    projectCard -> projectCard.onTemperatureChangedEffect(player)
            );
        }

        return null;
    }
}
