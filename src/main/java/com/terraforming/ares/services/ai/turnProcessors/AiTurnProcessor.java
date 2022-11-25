package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
public interface AiTurnProcessor {

    TurnType getType();

    boolean processTurn(MarsGame game, Player player);

}
