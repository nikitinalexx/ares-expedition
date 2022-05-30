package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SelfReplicatingBacteriaActionProcessor implements BlueActionCardProcessor<SelfReplicatingBacteria> {

    @Override
    public Class<SelfReplicatingBacteria> getType() {
        return SelfReplicatingBacteria.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        Integer input = inputParameters.get(0);

        if (input == 1) {
            player.addResources(actionCard, 1);
        } else if (input == 5) {
            player.addResources(actionCard, -5);

            player.setCanBuildInFirstPhase(1);
            player.setActionsInSecondPhase(1);
            player.setSelfReplicatingDiscount(true);
        }

        return null;
    }
}
