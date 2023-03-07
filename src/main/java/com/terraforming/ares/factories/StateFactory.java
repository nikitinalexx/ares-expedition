package com.terraforming.ares.factories;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.StateTransitionService;
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
    private final StateTransitionService stateTransitionService;
    private final MarsContextProvider contextProvider;

    public State getCurrentState(MarsGame marsGame) {
        final MarsContext context = contextProvider.provide(marsGame);
        switch (marsGame.getStateType()) {
            case PICK_CORPORATIONS:
                return new PickCorporationsState(context, stateTransitionService);
            case PICK_PHASE:
                return new PickPhaseState(context, stateTransitionService);
            case BUILD_GREEN_PROJECTS:
                return new BuildGreenProjectsState(context, stateTransitionService);
            case BUILD_BLUE_RED_PROJECTS:
                return new BuildBlueRedProjectsState(context, stateTransitionService);
            case PERFORM_BLUE_ACTION:
                return new PerformBlueActionState(context, stateTransitionService);
            case COLLECT_INCOME:
                return new CollectIncomeState(context, stateTransitionService);
            case DRAFT_CARDS:
                return new DraftCardsState(context, stateTransitionService);
            case SELL_EXTRA_CARDS:
                return new SellExtraCardsState(context, stateTransitionService);
            case GAME_END:
                return new GameEndState(context, stateTransitionService);
            case RESOLVE_PERSISTENT_ALL:
                return new ResolvePersistentAllState(context, stateTransitionService);
            case RESOLVE_CRYSIS_WITH_CHOICE:
                return new ResolveCrysisWithChoiceState(context, stateTransitionService);
            case DRAW_CRYSIS_CARD:
                return new DrawCrysisCardState(context, stateTransitionService);
            case CRISIS_DRAW_DUMMY_HAND:
                return new DrawCrisisDummyHandState(context, stateTransitionService);
            case CRISIS_END_STEP:
                return new CrisisEndStepState(context, stateTransitionService);
            case RESOLVE_OCEAN_DETRIMENT:
                return new ResolveOceanDetrimentState(context, stateTransitionService);
            default:
                throw new IllegalArgumentException(String.format("State %s is not supported", marsGame.getStateType()));
        }
    }

}
