package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class DraftCardsState extends AbstractState {

    public DraftCardsState(MarsGame marsGame) {
        super(marsGame);
    }

    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null && player.getNextTurn().getType() == TurnType.DISCARD_CARDS) {
            return List.of(TurnType.DISCARD_DRAFTED_CARDS);
        } else {
            return List.of();
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            performStateTransferFromPhase(6);
        }
    }

}
