package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.RegolithEaters;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
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
public class RegolithEatersActionProcessor implements BlueActionCardProcessor<RegolithEaters> {
    private final TerraformingService terraformingService;

    @Override
    public Class<RegolithEaters> getType() {
        return RegolithEaters.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, List<Integer> inputParameters) {
        Integer input = inputParameters.get(0);

        Integer currentMicrobeNumber = player.getCardResourcesCount().get(RegolithEaters.class);

        if (input == 1) {
            player.getCardResourcesCount().put(RegolithEaters.class, currentMicrobeNumber + 1);
        } else if (input == 2) {
            player.getCardResourcesCount().put(RegolithEaters.class, currentMicrobeNumber - 2);

            terraformingService.raiseOxygen(game, player);
        }

        return null;
    }
}
