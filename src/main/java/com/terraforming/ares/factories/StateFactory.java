package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.states.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class StateFactory {
    private final CardService cardService;

    public State getCurrentState(MarsGame marsGame) {
        switch (marsGame.getStateType()) {
            case PICK_CORPORATIONS:
                return new PickCorporationsState(marsGame, cardService);
            case PICK_PHASE:
                return new PickPhaseState(marsGame, cardService);
            case BUILD_GREEN_PROJECTS:
                return new BuildGreenProjectsState(marsGame, cardService);
            case BUILD_BLUE_RED_PROJECTS:
                return new BuildBlueRedProjectsState(marsGame, cardService);
            case PERFORM_BLUE_ACTION:
                return new PerformBlueActionState(marsGame, cardService);
            case COLLECT_INCOME:
                return new CollectIncomeState(marsGame, cardService);
            case DRAFT_CARDS:
                return new DraftCardsState(marsGame, cardService);
            case SELL_EXTRA_CARDS:
                return new SellExtraCardsState(marsGame, cardService);
            case GAME_END:
                return new GameEndState(marsGame, cardService);
            default:
                throw new IllegalArgumentException(String.format("State %s is not supported", marsGame.getStateType()));
        }
    }

}
