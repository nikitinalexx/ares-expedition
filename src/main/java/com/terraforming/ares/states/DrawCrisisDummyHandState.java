package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class DrawCrisisDummyHandState extends AbstractState {

    public DrawCrisisDummyHandState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().size() == 1) {
            if (marsGame.getPlayerByUuid(stateContext.getPlayerUuid()).getNextTurn() != null) {
                return List.of();
            } else {
                return List.of(TurnType.CRISIS_CHOOSE_DUMMY_HAND);
            }
        } else {
            throw new UnsupportedOperationException("Only 1 player supported so far");
        }
    }

    @Override
    public void updateState() {
        final MarsGame game = context.getGame();
        if (game.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            game.setStateType(StateType.PICK_PHASE, context);
        }
    }
}
