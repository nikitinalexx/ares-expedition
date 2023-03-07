package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class DraftCardsState extends AbstractState {

    public DraftCardsState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn()) {
            return List.of(player.getNextTurn().getType());
        } else {
            return List.of();
        }
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            stateTransitionService.performStateTransferFromPhase(marsGame, 6);
        }
    }

}
