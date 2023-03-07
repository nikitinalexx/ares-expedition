package com.terraforming.ares.states;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.StateContext;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StateTransitionService;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
public abstract class AbstractState implements State {
    protected final MarsContext context;
    protected final StateTransitionService stateTransitionService;

    protected AbstractState(MarsContext context, StateTransitionService stateTransitionService) {
        this.context = context;
        this.stateTransitionService = stateTransitionService;
    }

    protected boolean isSellVpTurnAvailable() {
        final MarsGame game = context.getGame();
        final CardService cardService = context.getCardService();
        return game.isCrysis() &&
                game.getCrysisData().getOpenedCards()
                        .stream()
                        .map(cardService::getCrysisCard)
                        .anyMatch(
                                CrysisCard::endGameCard
                        );
    }

    @Override
    public Action nextAction(StateContext stateContext) {
        if (getPossibleTurns(stateContext).isEmpty()) {
            return Action.WAIT;
        } else {
            return Action.TURN;
        }
    }
}
