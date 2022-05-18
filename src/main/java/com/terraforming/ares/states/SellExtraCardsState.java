package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.TurnType;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public class SellExtraCardsState extends AbstractState {

    public SellExtraCardsState(MarsGame marsGame) {
        super(marsGame);
    }

    @Override
    public List<TurnType> getPossibleTurns(String playerUuid) {
        Player player = marsGame.getPlayerByUuid(playerUuid);
        if (player.getNextTurn() != null) {
            return Collections.emptyList();
        } else if (player.getHand().size() > 10) {
            return Collections.singletonList(TurnType.SELL_CARDS);
        } else {
            return List.of(TurnType.SKIP_TURN);
        }
    }

    @Override
    public void updateState() {
        marsGame.setStateType(StateType.PICK_PHASE);
    }
}
