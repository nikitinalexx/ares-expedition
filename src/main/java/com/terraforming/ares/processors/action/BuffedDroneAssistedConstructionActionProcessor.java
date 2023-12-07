package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedDroneAssistedConstruction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedDroneAssistedConstructionActionProcessor implements BlueActionCardProcessor<BuffedDroneAssistedConstruction> {
    private final DroneAssistedConstructionActionProcessor processor;

    @Override
    public Class<BuffedDroneAssistedConstruction> getType() {
        return BuffedDroneAssistedConstruction.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        return processor.process(game, player, actionCard);
    }

}
