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
public class CrisisEndStepState extends AbstractState {

    public CrisisEndStepState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        final Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() != null) {
            return List.of();
        }

        final int winPoints = context.getWinPointsService().countCrysisWinPoints(marsGame);
        if (winPoints >= 2 && !marsGame.getCrysisData().getCardToTokensCount().isEmpty()) {
            return List.of(TurnType.CRISIS_VP_TO_TOKEN, TurnType.SKIP_TURN);
        }

        return List.of();
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            stateTransitionService.performStateTransferIntoResolveDetrimentTokens(marsGame);
        }
    }
}
