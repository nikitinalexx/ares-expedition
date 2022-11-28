package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiCollectIncomeTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public void processTurn(MarsGame game, Player player) {
        aiTurnService.collectIncomeTurn(player);
    }

}
