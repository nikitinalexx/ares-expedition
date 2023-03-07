package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardResourceService;
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
public class SelfReplicatingBacteriaActionProcessor implements BlueActionCardProcessor<SelfReplicatingBacteria> {
    private final CardResourceService cardResourceService;

    @Override
    public Class<SelfReplicatingBacteria> getType() {
        return SelfReplicatingBacteria.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer input = inputParameters.get(InputFlag.ADD_DISCARD_MICROBE.getId()).get(0);

        if (input == 1) {
            cardResourceService.addResources(player, actionCard, 1);
        } else if (input == 5) {
            player.removeResources(actionCard, 5);

            player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 25));
        }

        return null;
    }
}
