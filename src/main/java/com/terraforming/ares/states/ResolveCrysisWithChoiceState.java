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
public class ResolveCrysisWithChoiceState extends AbstractState {

    public ResolveCrysisWithChoiceState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());

        if (player.getNextTurn() != null
                && stateContext.getTurnTypeService().isIntermediate(player.getNextTurn().getType())
                && player.getNextTurn().expectedAsNextTurn()) {
            return List.of(player.getNextTurn().getType(), TurnType.SELL_CARDS);
        }

        return List.of();
    }

    @Override
    public void updateState() {
        final MarsGame marsGame = context.getGame();
        if (marsGame.getPlayerUuidToPlayer().values().stream().allMatch(
                player -> player.getNextTurn() == null
        )) {
            stateTransitionService.performStateTransferIntoTakeNextCrysisCard(marsGame);
        }
    }
}
