package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class SellExtraCardsState extends AbstractState {

    public SellExtraCardsState(MarsGame marsGame, CardService cardService) {
        super(marsGame, cardService);
    }

    @Override
    public List<TurnType> getPossibleTurns(StateContext stateContext) {
        Player player = marsGame.getPlayerByUuid(stateContext.getPlayerUuid());
        if (player.getNextTurn() == null && player.getHand().size() > 10) {
            return Collections.singletonList(TurnType.SELL_CARDS_LAST_ROUND);
        } else {
            return List.of();
        }
    }

    @Override
    public void updateState() {
        marsGame.setStateType(StateType.PICK_PHASE, cardService, true);
    }
}
