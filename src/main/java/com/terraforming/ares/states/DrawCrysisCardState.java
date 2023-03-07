package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class DrawCrysisCardState extends AbstractState {

    public DrawCrysisCardState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null
                && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())
                && player.getNextTurn().expectedAsNextTurn()) {
            List<TurnType> availableTurns = new ArrayList<>();
            if (player.getNextTurn().getType() != TurnType.DISCARD_CARDS) {
                availableTurns.add(TurnType.SELL_CARDS);
            }
            availableTurns.add(player.getNextTurn().getType());
            return availableTurns;
        }
        return List.of();
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            stateTransitionService.performStateTransferIntoCrisisDrawDummyHand(marsGame);
        }
    }
}
