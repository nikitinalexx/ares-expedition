package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedGhgProductionBacteria;
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
public class BuffedGhgProductionBacteriaActionProcessor implements BlueActionCardProcessor<BuffedGhgProductionBacteria> {
    private final GhgProductionBacteriaActionProcessor processor;

    @Override
    public Class<BuffedGhgProductionBacteria> getType() {
        return BuffedGhgProductionBacteria.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        return processor.process(game, player, actionCard, inputParameters);
    }

}
