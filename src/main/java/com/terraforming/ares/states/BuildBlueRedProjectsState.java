package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class BuildBlueRedProjectsState extends AbstractState {

    public BuildBlueRedProjectsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null && player.getNextTurn().getType().isIntermediate()) {
            return List.of(player.getNextTurn().getType());
        } else if (player.getNextTurn() != null || player.getActionsInSecondPhase() == 0) {
            return List.of();
        } else {
            List<TurnType> actions = new ArrayList<>(List.of(
                    TurnType.BUILD_BLUE_RED_PROJECT,
                    TurnType.SELL_CARDS,
                    TurnType.SKIP_TURN
            ));
            if (player.getChosenPhase() == 2 && !player.isPickedCardInSecondPhase()) {
                actions.add(TurnType.PICK_EXTRA_CARD);
            }
            return actions;
        }
    }

    @Override
    public void updateState() {
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getActionsInSecondPhase() == 0 && player.getNextTurn() == null
        )) {
            performStateTransferFromPhase(3);
        }
    }
}
