package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.RegolithEaters;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class RegolithEatersActionProcessor implements BlueActionCardProcessor<RegolithEaters> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;
    private final CardResourceService cardResourceService;

    @Override
    public Class<RegolithEaters> getType() {
        return RegolithEaters.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer input = inputParameters.get(InputFlag.ADD_DISCARD_MICROBE.getId()).get(0);

        if (input == 1) {
            cardResourceService.addResources(player, actionCard, 1);
        } else if (input == 2) {
            player.removeResources(actionCard, 2);

            terraformingService.raiseOxygen(marsContextProvider.provide(game, player));
        }

        return null;
    }
}
