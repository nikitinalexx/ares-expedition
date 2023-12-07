package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedExtremeColdFungus;
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
 * Creation date 06.12.2023
 */
@Component
@RequiredArgsConstructor
public class BuffedExtremeColdFungusActionProcessor implements BlueActionCardProcessor<BuffedExtremeColdFungus> {
    private final ExtremeColdFungusActionProcessor extremeColdFungusActionProcessor;

    @Override
    public Class<BuffedExtremeColdFungus> getType() {
        return BuffedExtremeColdFungus.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        return extremeColdFungusActionProcessor.process(game, player, actionCard, inputParameters);
    }
}
