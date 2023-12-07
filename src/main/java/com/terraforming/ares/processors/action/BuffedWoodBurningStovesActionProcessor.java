package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedWoodBurningStoves;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedWoodBurningStovesActionProcessor implements BlueActionCardProcessor<BuffedWoodBurningStoves> {
    private final WoodBurningStovesActionProcessor processor;

    @Override
    public Class<BuffedWoodBurningStoves> getType() {
        return BuffedWoodBurningStoves.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        return processor.process(game, player, actionCard);
    }
}
