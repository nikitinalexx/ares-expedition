package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.StateTransitionService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class SellExtraCardsState extends AbstractState {

    public SellExtraCardsState(MarsContext context, StateTransitionService stateTransitionService) {
        super(context, stateTransitionService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        final MarsGame marsGame = context.getGame();
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() == null && player.getHand().size() > 10) {
            return Collections.singletonList(TurnType.SELL_CARDS_LAST_ROUND);
        } else {
            return List.of();
        }
    }

    @Override
    public void updateState() {
        stateTransitionService.performStateTransferFromPhase(context.getGame(), 6);
    }
}
