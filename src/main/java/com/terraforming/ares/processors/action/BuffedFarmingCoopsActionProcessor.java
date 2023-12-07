package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedFarmingCoops;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
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
public class BuffedFarmingCoopsActionProcessor implements BlueActionCardProcessor<BuffedFarmingCoops> {
    private final FarmingCoopsActionProcessor processor;

    @Override
    public Class<BuffedFarmingCoops> getType() {
        return BuffedFarmingCoops.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        return processor.process(game, player, actionCard, inputParameters);
    }

}
