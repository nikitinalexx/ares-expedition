package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class DraftCardsState extends AbstractState {

    public DraftCardsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null && player.getNextTurn().getType().isIntermediate()) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else if (!player.isDraftedInFifthStage()) {
            return Collections.singletonList(TurnType.DRAFT_CARDS);
        } else {
            return Collections.singletonList(TurnType.SKIP_TURN);
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
